package com.shanzai.recipe.modules.recommendation.conversation;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shanzai.recipe.common.BusinessException;
import com.shanzai.recipe.modules.profile.ProfileEntity;
import com.shanzai.recipe.modules.profile.ProfileMapper;
import com.shanzai.recipe.modules.recommendation.RecommendationHistoryService;
import com.shanzai.recipe.modules.recommendation.RecommendationRequest;
import com.shanzai.recipe.modules.recommendation.RecommendationResponse;
import com.shanzai.recipe.modules.recommendation.RecommendationService;
import com.shanzai.recipe.common.DietGoal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RecommendationConversationService {
    private static final String USER_ROLE = "USER";
    private static final String ASSISTANT_ROLE = "ASSISTANT";

    private final RecommendationConversationMapper conversationMapper;
    private final RecommendationConversationMessageMapper messageMapper;
    private final ConversationAnswerInterpreter interpreter;
    private final ConversationFlow flow;
    private final ProfileMapper profileMapper;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;
    private final RecommendationService recommendationService;
    private final RecommendationHistoryService historyService;
    private final Map<Long, Object> conversationLocks = new ConcurrentHashMap<>();

    public RecommendationConversationService(
            RecommendationConversationMapper conversationMapper,
            RecommendationConversationMessageMapper messageMapper,
            ConversationAnswerInterpreter interpreter,
            ConversationFlow flow,
            ProfileMapper profileMapper,
            ObjectMapper objectMapper,
            TransactionTemplate transactionTemplate,
            RecommendationService recommendationService,
            RecommendationHistoryService historyService
    ) {
        this.conversationMapper = conversationMapper;
        this.messageMapper = messageMapper;
        this.interpreter = interpreter;
        this.flow = flow;
        this.profileMapper = profileMapper;
        this.objectMapper = objectMapper;
        this.transactionTemplate = transactionTemplate;
        this.recommendationService = recommendationService;
        this.historyService = historyService;
    }

    public Optional<RecommendationConversationEntity> findActiveConversation(Long userId) {
        return conversationMapper.selectList(new LambdaQueryWrapper<RecommendationConversationEntity>()
                        .eq(RecommendationConversationEntity::getUserId, userId)
                        .in(RecommendationConversationEntity::getStatus,
                                ConversationStatus.ACTIVE.name(),
                                ConversationStatus.READY_TO_CONFIRM.name()))
                .stream()
                .max(Comparator.comparing(RecommendationConversationEntity::getUpdatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())));
    }

    public Optional<ConversationResponse> getActiveConversation(Long userId) {
        return findActiveConversation(userId).map(conversation -> getConversation(userId, conversation.getId()));
    }

    @Transactional
    public ConversationResponse startConversation(Long userId, boolean restart) {
        if (restart) {
            cancelIncompleteConversations(userId);
        }

        RecommendationConversationEntity conversation = new RecommendationConversationEntity();
        conversation.setUserId(userId);
        conversation.setStage(ConversationStage.INTENT.name());
        conversation.setStatus(ConversationStatus.ACTIVE.name());
        conversation.setInvalidAnswerCount(0);
        RecommendationConversationContext context = initialContext(userId);
        conversation.setContextJson(writeContext(context));
        conversationMapper.insert(conversation);
        return new ConversationResponse(
                conversation.getId(),
                ConversationStage.INTENT,
                ConversationStatus.ACTIVE,
                0,
                context,
                List.of(),
                false,
                quickOptionsFor(ConversationStage.INTENT, ConversationStatus.ACTIVE, 0)
        );
    }

    private void cancelIncompleteConversations(Long userId) {
        findIncompleteConversations(userId).forEach(existing -> {
            existing.setStatus(ConversationStatus.CANCELLED.name());
            conversationMapper.updateById(existing);
        });
    }

    private List<RecommendationConversationEntity> findIncompleteConversations(Long userId) {
        return conversationMapper.selectList(new LambdaQueryWrapper<RecommendationConversationEntity>()
                .eq(RecommendationConversationEntity::getUserId, userId)
                .in(RecommendationConversationEntity::getStatus,
                        ConversationStatus.ACTIVE.name(),
                        ConversationStatus.READY_TO_CONFIRM.name()));
    }

    public ConversationResponse sendMessage(Long userId, Long conversationId, ConversationMessageRequest request) {
        synchronized (lockForConversation(conversationId)) {
            return Objects.requireNonNull(transactionTemplate.execute(
                    status -> sendMessageLocked(userId, conversationId, request)));
        }
    }

    Object lockForConversation(Long conversationId) {
        return conversationLocks.computeIfAbsent(conversationId, ignored -> new Object());
    }

    private ConversationResponse sendMessageLocked(Long userId, Long conversationId, ConversationMessageRequest request) {
        RecommendationConversationEntity conversation = requireConversation(userId, conversationId);
        if (request == null || isBlank(request.content()) || isBlank(request.clientMessageId())) {
            throw new BusinessException("推荐对话不存在");
        }

        RecommendationConversationMessageEntity existing = messageMapper.selectOne(
                new LambdaQueryWrapper<RecommendationConversationMessageEntity>()
                        .eq(RecommendationConversationMessageEntity::getConversationId, conversationId)
                        .eq(RecommendationConversationMessageEntity::getClientMessageId, request.clientMessageId())
                        .eq(RecommendationConversationMessageEntity::getRole, USER_ROLE)
        );
        if (existing != null) {
            return getConversation(userId, conversationId);
        }

        RecommendationConversationMessageEntity userMessage = new RecommendationConversationMessageEntity();
        userMessage.setConversationId(conversationId);
        userMessage.setRole(USER_ROLE);
        userMessage.setContent(request.content().trim());
        userMessage.setClientMessageId(request.clientMessageId().trim());
        try {
            messageMapper.insert(userMessage);
        } catch (DuplicateKeyException exception) {
            return getConversation(userId, conversationId);
        }

        RecommendationConversationContext context = readContext(conversation.getContextJson());
        ConversationStage stage = ConversationStage.valueOf(conversation.getStage());
        ConversationStatus status = ConversationStatus.valueOf(conversation.getStatus());
        ConversationAnswerAnalysis analysis = interpreter.interpret(stage, userMessage.getContent(), context);
        ConversationTransition transition = flow.apply(
                stage,
                status,
                context,
                defaultInvalidAnswerCount(conversation),
                analysis
        );

        conversation.setStage(transition.stage().name());
        conversation.setStatus(transition.status().name());
        conversation.setInvalidAnswerCount(transition.invalidAnswerCount());
        conversation.setContextJson(writeContext(transition.context()));
        conversation.setUpdatedAt(LocalDateTime.now());
        conversationMapper.updateById(conversation);

        RecommendationConversationMessageEntity assistantMessage = new RecommendationConversationMessageEntity();
        assistantMessage.setConversationId(conversationId);
        assistantMessage.setRole(ASSISTANT_ROLE);
        assistantMessage.setContent(buildAssistantReply(transition.stage(), transition.guidanceMode()));
        messageMapper.insert(assistantMessage);

        return getConversation(userId, conversationId);
    }

    public ConversationResponse getConversation(Long userId, Long conversationId) {
        RecommendationConversationEntity conversation = requireConversation(userId, conversationId);
        RecommendationConversationContext context = readContext(conversation.getContextJson());
        List<ConversationMessageResponse> messages = messageMapper.selectList(
                        new LambdaQueryWrapper<RecommendationConversationMessageEntity>()
                                .eq(RecommendationConversationMessageEntity::getConversationId, conversationId))
                .stream()
                .sorted(Comparator.comparing(RecommendationConversationMessageEntity::getId,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .map(message -> new ConversationMessageResponse(
                        message.getId(),
                        message.getRole(),
                        message.getContent(),
                        message.getClientMessageId(),
                        message.getCreatedAt()
                ))
                .toList();
        ConversationStage stage = ConversationStage.valueOf(conversation.getStage());
        ConversationStatus status = ConversationStatus.valueOf(conversation.getStatus());
        return new ConversationResponse(
                conversation.getId(),
                stage,
                status,
                defaultInvalidAnswerCount(conversation),
                context,
                messages,
                status == ConversationStatus.READY_TO_CONFIRM,
                quickOptionsFor(stage, status, defaultInvalidAnswerCount(conversation))
        );
    }


    @Transactional
    public ConversationResponse patchContext(Long userId, Long conversationId, ConversationContextPatchRequest request) {
        RecommendationConversationEntity conversation = requireConversation(userId, conversationId);
        if (ConversationStatus.COMPLETED.name().equals(conversation.getStatus())
                || ConversationStatus.CANCELLED.name().equals(conversation.getStatus())) {
            throw new BusinessException("推荐对话不存在");
        }
        RecommendationConversationContext current = readContext(conversation.getContextJson());
        RecommendationConversationContext patched = new RecommendationConversationContext(
                patchText(request.intentText(), current.intentText()),
                patchDietGoal(request.dietGoal(), current.dietGoal()),
                patchAvailableIngredients(request.availableIngredients(), current.availableIngredients()),
                request.excludedIngredients() == null ? current.excludedIngredients() : request.excludedIngredients(),
                request.allergyIngredients() == null ? current.allergyIngredients() : request.allergyIngredients(),
                request.cookingTime() == null ? current.cookingTime() : request.cookingTime(),
                request.servings() == null ? current.servings() : request.servings(),
                current.unknownTerms(),
                resolvePatchConflicts(
                        current.conflicts(),
                        request.availableIngredients(),
                        request.dietGoal() != null,
                        invalidDietGoalPatch(request.dietGoal())
                ),
                request.excludedIngredients() != null || request.allergyIngredients() != null || current.restrictionsConfirmed()
        );
        ConversationStage nextStage = flow.firstMissingStage(patched);
        boolean hasConflict = !patched.conflicts().isEmpty() || !patched.unknownTerms().isEmpty();
        conversation.setStage(nextStage.name());
        conversation.setStatus(nextStage == ConversationStage.CONFIRM && !hasConflict
                ? ConversationStatus.READY_TO_CONFIRM.name()
                : ConversationStatus.ACTIVE.name());
        conversation.setInvalidAnswerCount(0);
        conversation.setContextJson(writeContext(patched));
        conversation.setUpdatedAt(LocalDateTime.now());
        conversationMapper.updateById(conversation);
        return getConversation(userId, conversationId);
    }

    public RecommendationResponse confirm(Long userId, Long conversationId) {
        synchronized (lockForConversation(conversationId)) {
            return Objects.requireNonNull(transactionTemplate.execute(
                    status -> confirmLocked(userId, conversationId)));
        }
    }

    private RecommendationResponse confirmLocked(Long userId, Long conversationId) {
        RecommendationConversationEntity conversation = requireConversation(userId, conversationId);
        ConversationStatus status = ConversationStatus.valueOf(conversation.getStatus());
        if (status == ConversationStatus.COMPLETED) {
            return historyService.getRecommendationResponse(userId, conversation.getRecommendationHistoryId());
        }
        if (status != ConversationStatus.READY_TO_CONFIRM) {
            throw new BusinessException("推荐条件尚未确认完整");
        }

        RecommendationConversationContext context = readContext(conversation.getContextJson());
        RecommendationRequest request = toRecommendationRequest(context);
        if (request.availableIngredients().isEmpty()
                || parseDietGoal(context.dietGoal()) == null
                || flow.firstMissingStage(context) != ConversationStage.CONFIRM
                || !context.unknownTerms().isEmpty()
                || !context.conflicts().isEmpty()) {
            throw new BusinessException("推荐条件尚未确认完整");
        }

        RecommendationResponse response = recommendationService.recommend(userId, request);
        historyService.attachConversationContext(userId, response.historyId(), context);
        conversation.setStatus(ConversationStatus.COMPLETED.name());
        conversation.setRecommendationHistoryId(response.historyId());
        conversation.setUpdatedAt(LocalDateTime.now());
        conversationMapper.updateById(conversation);
        return response;
    }

    private List<String> resolvePatchConflicts(
            List<String> currentConflicts,
            List<AvailableIngredientInput> patchedIngredients,
            boolean dietGoalPatched,
            boolean invalidDietGoalPatch
    ) {
        List<String> conflicts = currentConflicts == null ? List.of() : currentConflicts;
        if (patchedIngredients != null && !patchedIngredients.isEmpty()) {
            conflicts = conflicts.stream()
                    .filter(conflict -> !isQuantityConflictResolved(conflict, patchedIngredients))
                    .toList();
        }
        if (dietGoalPatched) {
            conflicts = conflicts.stream()
                    .filter(conflict -> !"饮食目标无效".equals(conflict))
                    .toList();
        }
        if (invalidDietGoalPatch && !conflicts.contains("饮食目标无效")) {
            return java.util.stream.Stream.concat(conflicts.stream(), java.util.stream.Stream.of("饮食目标无效"))
                    .toList();
        }
        return conflicts;
    }

    private boolean invalidDietGoalPatch(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        return parseDietGoal(value) == null;
    }

    private List<AvailableIngredientInput> patchAvailableIngredients(
            List<AvailableIngredientInput> next,
            List<AvailableIngredientInput> previous
    ) {
        if (next == null) {
            return previous;
        }
        return next.stream()
                .filter(Objects::nonNull)
                .filter(ingredient -> !isBlank(ingredient.name()))
                .map(ingredient -> new AvailableIngredientInput(
                        ingredient.name().trim(),
                        ingredient.quantity(),
                        cleanText(ingredient.unit()),
                        ingredient.quantityKnown()
                ))
                .distinct()
                .toList();
    }

    private boolean isQuantityConflictResolved(
            String conflict,
            List<AvailableIngredientInput> patchedIngredients
    ) {
        if (isBlank(conflict)) {
            return false;
        }
        return patchedIngredients.stream().anyMatch(ingredient ->
                ingredient != null
                        && ingredient.quantityKnown()
                        && ingredient.name() != null
                        && conflict.startsWith(ingredient.name().trim() + "数量"));
    }
    private RecommendationConversationEntity requireConversation(Long userId, Long conversationId) {
        RecommendationConversationEntity conversation = conversationMapper.selectById(conversationId);
        if (conversation == null || !Objects.equals(userId, conversation.getUserId())) {
            throw new BusinessException("推荐对话不存在");
        }
        return conversation;
    }


    private RecommendationRequest toRecommendationRequest(RecommendationConversationContext context) {
        List<String> availableIngredients = context.availableIngredients().stream()
                .map(AvailableIngredientInput::name)
                .filter(name -> !isBlank(name))
                .map(String::trim)
                .distinct()
                .toList();
        List<String> excludedIngredients = java.util.stream.Stream.concat(
                        context.excludedIngredients().stream(),
                        context.allergyIngredients().stream())
                .filter(value -> !isBlank(value))
                .map(String::trim)
                .distinct()
                .toList();
        return new RecommendationRequest(
                availableIngredients,
                excludedIngredients,
                parseDietGoal(context.dietGoal()),
                context.cookingTime(),
                context.servings()
        );
    }

    private DietGoal parseDietGoal(String value) {
        if (isBlank(value)) {
            return null;
        }
        try {
            return DietGoal.valueOf(value.trim());
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private String patchText(String next, String previous) {
        return next == null ? previous : cleanText(next);
    }

    private String patchDietGoal(String next, String previous) {
        if (next == null) {
            return previous;
        }
        DietGoal goal = parseDietGoal(next);
        return goal == null ? null : goal.name();
    }

    private RecommendationConversationContext initialContext(Long userId) {
        ProfileEntity profile = profileMapper.selectOne(
                new LambdaQueryWrapper<ProfileEntity>().eq(ProfileEntity::getUserId, userId));
        if (profile == null) {
            return RecommendationConversationContext.empty();
        }
        return new RecommendationConversationContext(
                null,
                cleanText(profile.getDietGoal()),
                List.of(),
                splitList(profile.getAvoidIngredients()),
                splitList(profile.getAllergyIngredients()),
                profile.getCookingTimePreference(),
                null,
                List.of(),
                List.of(),
                false
        );
    }

    private RecommendationConversationContext readContext(String contextJson) {
        if (isBlank(contextJson)) {
            return RecommendationConversationContext.empty();
        }
        try {
            return objectMapper.readValue(contextJson, RecommendationConversationContext.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("failed to read conversation context", exception);
        }
    }

    private String writeContext(RecommendationConversationContext context) {
        try {
            return objectMapper.writeValueAsString(
                    context == null ? RecommendationConversationContext.empty() : context);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("failed to write conversation context", exception);
        }
    }

    private int defaultInvalidAnswerCount(RecommendationConversationEntity conversation) {
        return conversation.getInvalidAnswerCount() == null ? 0 : conversation.getInvalidAnswerCount();
    }

    private List<String> quickOptionsFor(ConversationStage stage, ConversationStatus status, int invalidAnswerCount) {
        if (status == ConversationStatus.READY_TO_CONFIRM || stage == ConversationStage.CONFIRM) {
            return List.of("确认推荐", "继续补充");
        }
        if (invalidAnswerCount >= 3) {
            return List.of("重新开始", "继续补充");
        }
        return switch (stage) {
            case INTENT -> List.of("想吃清淡点", "想吃高蛋白", "推荐家常菜");
            case INGREDIENTS -> List.of("鸡胸肉 300g", "鸡蛋 2个", "西兰花 200g");
            case RESTRICTIONS -> List.of("不吃辣", "花生过敏", "没有忌口");
            case CONTEXT -> List.of("30分钟", "2人份", "20分钟 1人份");
            case CONFIRM -> List.of("确认推荐", "重新开始");
        };
    }

    private String buildAssistantReply(ConversationStage stage, GuidanceMode guidanceMode) {
        if (guidanceMode == GuidanceMode.EXAMPLE) {
            return "可以按示例补充，我会继续整理。";
        }
        if (guidanceMode == GuidanceMode.QUICK_OPTIONS) {
            return "可以直接点一个快捷选项继续。";
        }
        if (guidanceMode == GuidanceMode.RESTART_OPTION) {
            return "如果想重新开始，也可以直接发新的需求。";
        }
        return switch (stage) {
            case INTENT -> "先说说你这次想吃什么类型。";
            case INGREDIENTS -> "收到，请继续告诉我现有食材。";
            case RESTRICTIONS -> "再告诉我忌口或过敏信息。";
            case CONTEXT -> "还差烹饪时间和人数。";
            case CONFIRM -> "信息差不多了，确认后我再继续。";
        };
    }

    private List<String> splitList(String value) {
        if (isBlank(value)) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .distinct()
                .toList();
    }

    private String cleanText(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
