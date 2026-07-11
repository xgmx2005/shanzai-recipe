package com.shanzai.recipe.modules.recommendation.conversation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shanzai.recipe.common.BusinessException;
import com.shanzai.recipe.modules.profile.ProfileEntity;
import com.shanzai.recipe.modules.recommendation.RecommendationHistoryService;
import com.shanzai.recipe.modules.recommendation.RecommendationRequest;
import com.shanzai.recipe.modules.recommendation.RecommendationResponse;
import com.shanzai.recipe.modules.recommendation.RecommendationService;
import com.shanzai.recipe.modules.profile.ProfileMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RecommendationConversationServiceTest {
    private RecommendationConversationMapper conversationMapper;
    private RecommendationConversationMessageMapper messageMapper;
    private ConversationAnswerInterpreter interpreter;
    private ProfileMapper profileMapper;
    private RecommendationService recommendationService;
    private RecommendationHistoryService historyService;
    private RecordingTransactionManager transactionManager;
    private RecommendationConversationService service;

    @BeforeEach
    void setUp() {
        conversationMapper = mock(RecommendationConversationMapper.class);
        messageMapper = mock(RecommendationConversationMessageMapper.class);
        interpreter = mock(ConversationAnswerInterpreter.class);
        profileMapper = mock(ProfileMapper.class);
        recommendationService = mock(RecommendationService.class);
        historyService = mock(RecommendationHistoryService.class);
        transactionManager = new RecordingTransactionManager();
        service = new RecommendationConversationService(
                conversationMapper,
                messageMapper,
                interpreter,
                new ConversationFlow(),
                profileMapper,
                new ObjectMapper(),
                new TransactionTemplate(transactionManager),
                recommendationService,
                historyService
        );
    }

    @Test
    void startConversationLoadsProfileButStillStartsFromIntent() {
        ProfileEntity profile = new ProfileEntity();
        profile.setUserId(7L);
        profile.setDietGoal("FAT_LOSS");
        profile.setCookingTimePreference(20);
        profile.setAvoidIngredients("香菜");
        when(profileMapper.selectOne(any())).thenReturn(profile);
        when(conversationMapper.selectList(any())).thenReturn(List.of());
        when(conversationMapper.insert(any(RecommendationConversationEntity.class))).thenAnswer(invocation -> {
            RecommendationConversationEntity entity = invocation.getArgument(0);
            entity.setId(10L);
            return 1;
        });
        when(messageMapper.selectList(any())).thenReturn(List.of());

        ConversationResponse response = service.startConversation(7L, false);

        assertEquals(ConversationStage.INTENT, response.stage());
        assertEquals(ConversationStatus.ACTIVE, response.status());
        assertEquals(0, response.invalidAnswerCount());
        assertEquals("FAT_LOSS", response.context().dietGoal());
        assertEquals(20, response.context().cookingTime());
        assertEquals(List.of("香菜"), response.context().excludedIngredients());
    }

    @Test
    void findActiveConversationReturnsMostRecentlyUpdatedIncompleteConversation() {
        RecommendationConversationEntity older = activeConversation(10L, 7L);
        older.setUpdatedAt(LocalDateTime.of(2026, 7, 10, 8, 0));
        RecommendationConversationEntity newer = activeConversation(11L, 7L);
        newer.setStatus(ConversationStatus.READY_TO_CONFIRM.name());
        newer.setUpdatedAt(LocalDateTime.of(2026, 7, 10, 9, 0));
        when(conversationMapper.selectList(any())).thenReturn(List.of(older, newer));

        Optional<RecommendationConversationEntity> found = service.findActiveConversation(7L);

        assertEquals(11L, found.orElseThrow().getId());
    }

    @Test
    void restartConversationCancelsAllExistingIncompleteConversationsBeforeCreatingNewOne() {
        RecommendationConversationEntity activeConversation = activeConversation(11L, 7L);
        RecommendationConversationEntity readyToConfirmConversation = activeConversation(12L, 7L);
        readyToConfirmConversation.setStatus(ConversationStatus.READY_TO_CONFIRM.name());
        when(conversationMapper.selectList(any())).thenReturn(List.of(activeConversation, readyToConfirmConversation));
        when(conversationMapper.insert(any(RecommendationConversationEntity.class))).thenAnswer(invocation -> {
            RecommendationConversationEntity entity = invocation.getArgument(0);
            entity.setId(13L);
            return 1;
        });
        when(messageMapper.selectList(any())).thenReturn(List.of());

        service.startConversation(7L, true);

        assertEquals("CANCELLED", activeConversation.getStatus());
        assertEquals("CANCELLED", readyToConfirmConversation.getStatus());
        verify(conversationMapper).updateById(activeConversation);
        verify(conversationMapper).updateById(readyToConfirmConversation);
    }

    @Test
    void sendMessageWithDuplicateClientMessageIdDoesNotInsertSecondUserMessage() {
        RecommendationConversationEntity conversation = activeConversation(10L, 7L);
        conversation.setStage(ConversationStage.INGREDIENTS.name());
        conversation.setContextJson("{\"intentText\":\"想吃清淡点\",\"dietGoal\":null,\"availableIngredients\":[],\"excludedIngredients\":[],\"allergyIngredients\":[],\"cookingTime\":null,\"servings\":null,\"unknownTerms\":[],\"conflicts\":[],\"restrictionsConfirmed\":false}");
        when(conversationMapper.selectById(10L)).thenReturn(conversation);
        when(messageMapper.selectOne(any())).thenReturn(null, userMessage(1L, 10L, "message-001", "USER", "鸡胸肉"));
        when(interpreter.interpret(any(), any(), any())).thenReturn(ConversationAnswerAnalysis.invalid());
        when(messageMapper.insert(any(RecommendationConversationMessageEntity.class))).thenAnswer(invocation -> {
            RecommendationConversationMessageEntity entity = invocation.getArgument(0);
            if (entity.getId() == null) {
                entity.setId("USER".equals(entity.getRole()) ? 1L : 2L);
            }
            return 1;
        });
        when(messageMapper.selectList(any())).thenReturn(List.of(
                assistantMessage(2L, 10L, "已记录鸡胸肉"),
                userMessage(1L, 10L, "message-001", "USER", "鸡胸肉")
        ));

        ConversationMessageRequest request = new ConversationMessageRequest("鸡胸肉", "message-001");

        ConversationResponse first = service.sendMessage(7L, 10L, request);
        ConversationResponse second = service.sendMessage(7L, 10L, request);

        assertEquals(ConversationStage.INGREDIENTS, first.stage());
        assertEquals(2, second.messages().size());
        verify(messageMapper, times(1)).insert(argThat((RecommendationConversationMessageEntity message) ->
                "message-001".equals(message.getClientMessageId())));
    }

    @Test
    void sendMessageReturnsConversationWhenConcurrentDuplicateClientMessageIdHitsUniqueConstraint() {
        RecommendationConversationEntity conversation = activeConversation(10L, 7L);
        conversation.setStage(ConversationStage.INGREDIENTS.name());
        when(conversationMapper.selectById(10L)).thenReturn(conversation);
        when(messageMapper.selectOne(any())).thenReturn(null);
        when(messageMapper.insert(any(RecommendationConversationMessageEntity.class)))
                .thenThrow(new DuplicateKeyException("duplicate client message"));
        when(messageMapper.selectList(any())).thenReturn(List.of(
                userMessage(1L, 10L, "message-001", "USER", "鸡胸肉")
        ));

        ConversationResponse response = service.sendMessage(
                7L,
                10L,
                new ConversationMessageRequest("鸡胸肉", "message-001")
        );

        assertEquals(1, response.messages().size());
        assertEquals("message-001", response.messages().get(0).clientMessageId());
        verify(interpreter, never()).interpret(any(), any(), any());
        verify(conversationMapper, never()).updateById(any(RecommendationConversationEntity.class));
        verify(messageMapper, times(1)).insert(any(RecommendationConversationMessageEntity.class));
    }

    @Test
    void sendMessageRefreshesUpdatedAtBeforeUpdatingConversation() {
        RecommendationConversationEntity conversation = activeConversation(10L, 7L);
        LocalDateTime oldUpdatedAt = conversation.getUpdatedAt();
        when(conversationMapper.selectById(10L)).thenReturn(conversation);
        when(messageMapper.selectOne(any())).thenReturn(null);
        when(interpreter.interpret(any(), any(), any())).thenReturn(ConversationAnswerAnalysis.invalid());
        when(messageMapper.insert(any(RecommendationConversationMessageEntity.class))).thenAnswer(invocation -> {
            RecommendationConversationMessageEntity entity = invocation.getArgument(0);
            entity.setId("USER".equals(entity.getRole()) ? 1L : 2L);
            return 1;
        });
        when(messageMapper.selectList(any())).thenReturn(List.of(
                userMessage(1L, 10L, "message-001", "USER", "鸡胸肉"),
                assistantMessage(2L, 10L, "请继续补充")
        ));

        service.sendMessage(7L, 10L, new ConversationMessageRequest("鸡胸肉", "message-001"));

        ArgumentCaptor<RecommendationConversationEntity> captor =
                ArgumentCaptor.forClass(RecommendationConversationEntity.class);
        verify(conversationMapper).updateById(captor.capture());
        assertTrue(captor.getValue().getUpdatedAt().isAfter(oldUpdatedAt));
    }

    @Test
    void sendMessageRunsNormalPathInsideExplicitTransactionTemplate() {
        RecommendationConversationEntity conversation = activeConversation(10L, 7L);
        when(conversationMapper.selectById(10L)).thenReturn(conversation);
        when(messageMapper.selectOne(any())).thenReturn(null);
        when(interpreter.interpret(any(), any(), any())).thenReturn(ConversationAnswerAnalysis.invalid());
        when(messageMapper.insert(any(RecommendationConversationMessageEntity.class))).thenAnswer(invocation -> {
            RecommendationConversationMessageEntity entity = invocation.getArgument(0);
            entity.setId("USER".equals(entity.getRole()) ? 1L : 2L);
            return 1;
        });
        when(messageMapper.selectList(any())).thenReturn(List.of(
                userMessage(1L, 10L, "message-001", "USER", "鸡胸肉"),
                assistantMessage(2L, 10L, "请继续补充")
        ));

        service.sendMessage(7L, 10L, new ConversationMessageRequest("鸡胸肉", "message-001"));

        assertEquals(1, transactionManager.beginCount());
        assertEquals(1, transactionManager.commitCount());
    }

    @Test
    void conversationLockIsStablePerConversationOnly() {
        Object first = service.lockForConversation(10L);
        Object second = service.lockForConversation(10L);
        Object otherConversation = service.lockForConversation(11L);

        assertSame(first, second);
        assertNotSame(first, otherConversation);
    }

    @Test
    void getConversationRejectsConversationOwnedByAnotherUser() {
        RecommendationConversationEntity conversation = activeConversation(10L, 8L);
        when(conversationMapper.selectById(10L)).thenReturn(conversation);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.getConversation(7L, 10L));

        assertEquals("推荐对话不存在", exception.getMessage());
    }

    @Test
    void getConversationReturnsMessagesSortedByIdAscending() throws Exception {
        RecommendationConversationEntity conversation = activeConversation(10L, 7L);
        conversation.setStage(ConversationStage.INGREDIENTS.name());
        conversation.setContextJson(new ObjectMapper().writeValueAsString(
                RecommendationConversationContext.empty().merge(new ConversationAnswerAnalysis(
                        true,
                        "清淡饮食",
                        "FAT_LOSS",
                        List.of(),
                        List.of(),
                        List.of(),
                        null,
                        null,
                        List.of(),
                        List.of(),
                        BigDecimal.ONE
                ))
        ));
        when(conversationMapper.selectById(10L)).thenReturn(conversation);
        when(messageMapper.selectList(any())).thenReturn(List.of(
                assistantMessage(3L, 10L, "第二条"),
                userMessage(1L, 10L, "message-001", "USER", "第一条"),
                assistantMessage(2L, 10L, "中间")
        ));

        ConversationResponse response = service.getConversation(7L, 10L);

        assertEquals(List.of(1L, 2L, 3L),
                response.messages().stream().map(ConversationMessageResponse::id).toList());
        assertNotNull(response.quickOptions());
    }

    @Test
    void confirmRejectsConversationThatIsNotReadyToConfirm() {
        RecommendationConversationEntity conversation = activeConversation(10L, 7L);
        when(conversationMapper.selectById(10L)).thenReturn(conversation);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.confirm(7L, 10L));

        assertEquals("推荐条件尚未确认完整", exception.getMessage());
        verifyNoInteractions(recommendationService, historyService);
    }

    @Test
    void confirmBuildsRecommendationRequestFromContextAndCompletesConversation() throws Exception {
        RecommendationConversationContext context = new RecommendationConversationContext(
                "清淡晚餐",
                "FAT_LOSS",
                List.of(new AvailableIngredientInput("鸡胸肉", new BigDecimal("300"), "g", true)),
                List.of("辣椒"),
                List.of("花生"),
                30,
                2,
                List.of(),
                List.of(),
                true
        );
        RecommendationConversationEntity conversation = activeConversation(10L, 7L);
        conversation.setStage(ConversationStage.CONFIRM.name());
        conversation.setStatus(ConversationStatus.READY_TO_CONFIRM.name());
        conversation.setContextJson(new ObjectMapper().writeValueAsString(context));
        when(conversationMapper.selectById(10L)).thenReturn(conversation);
        RecommendationResponse expected = new RecommendationResponse(99L, "summary", "health", "shopping", true, List.of());
        when(recommendationService.recommend(any(), any())).thenReturn(expected);

        RecommendationResponse response = service.confirm(7L, 10L);

        assertSame(expected, response);
        ArgumentCaptor<RecommendationRequest> requestCaptor = ArgumentCaptor.forClass(RecommendationRequest.class);
        verify(recommendationService).recommend(eq(7L), requestCaptor.capture());
        RecommendationRequest request = requestCaptor.getValue();
        assertEquals(List.of("鸡胸肉"), request.availableIngredients());
        assertEquals(List.of("辣椒", "花生"), request.excludedIngredients());
        assertEquals(30, request.cookingTime());
        assertEquals(2, request.servings());
        verify(historyService).attachConversationContext(7L, 99L, context);
        assertEquals(ConversationStatus.COMPLETED.name(), conversation.getStatus());
        assertEquals(99L, conversation.getRecommendationHistoryId());
        verify(conversationMapper).updateById(conversation);
    }

    @Test
    void confirmCompletedConversationReturnsExistingRecommendationWithoutCreatingAnotherHistory() {
        RecommendationConversationEntity conversation = activeConversation(10L, 7L);
        conversation.setStatus(ConversationStatus.COMPLETED.name());
        conversation.setRecommendationHistoryId(99L);
        when(conversationMapper.selectById(10L)).thenReturn(conversation);
        RecommendationResponse expected = new RecommendationResponse(99L, "summary", "health", "shopping", true, List.of());
        when(historyService.getRecommendationResponse(7L, 99L)).thenReturn(expected);

        RecommendationResponse response = service.confirm(7L, 10L);

        assertSame(expected, response);
        verify(historyService).getRecommendationResponse(7L, 99L);
        verifyNoInteractions(recommendationService);
    }

    @Test
    void patchContextKeepsConversationActiveWhenExistingConflictRemains() throws Exception {
        RecommendationConversationContext context = new RecommendationConversationContext(
                "清淡晚餐",
                "FAT_LOSS",
                List.of(new AvailableIngredientInput("鸡胸肉", new BigDecimal("300"), "g", true)),
                List.of(),
                List.of(),
                30,
                2,
                List.of(),
                List.of("鸡胸肉数量无效"),
                true
        );
        RecommendationConversationEntity conversation = activeConversation(10L, 7L);
        conversation.setStage(ConversationStage.CONFIRM.name());
        conversation.setStatus(ConversationStatus.ACTIVE.name());
        conversation.setContextJson(new ObjectMapper().writeValueAsString(context));
        when(conversationMapper.selectById(10L)).thenReturn(conversation);
        when(messageMapper.selectList(any())).thenReturn(List.of());

        ConversationResponse response = service.patchContext(
                7L,
                10L,
                new ConversationContextPatchRequest("更清淡的晚餐", null, null, null, null, null, null)
        );

        assertEquals(ConversationStatus.ACTIVE, response.status());
        assertEquals(List.of("鸡胸肉数量无效"), response.context().conflicts());
    }

    @Test
    void patchContextWithBlankIngredientDoesNotBecomeReadyToConfirm() {
        RecommendationConversationEntity conversation = activeConversation(10L, 7L);
        when(conversationMapper.selectById(10L)).thenReturn(conversation);
        when(messageMapper.selectList(any())).thenReturn(List.of());

        ConversationResponse response = service.patchContext(
                7L,
                10L,
                new ConversationContextPatchRequest(
                        "清淡晚餐",
                        "FAT_LOSS",
                        List.of(new AvailableIngredientInput("   ", null, null, false)),
                        List.of(),
                        List.of(),
                        30,
                        2
                )
        );

        assertEquals(ConversationStage.INGREDIENTS, response.stage());
        assertEquals(ConversationStatus.ACTIVE, response.status());
        assertEquals(List.of(), response.context().availableIngredients());
    }

    @Test
    void confirmRejectsReadyConversationWithoutEffectiveIngredients() throws Exception {
        RecommendationConversationContext context = new RecommendationConversationContext(
                "清淡晚餐",
                "FAT_LOSS",
                List.of(new AvailableIngredientInput("   ", null, null, false)),
                List.of(),
                List.of(),
                30,
                2,
                List.of(),
                List.of(),
                true
        );
        RecommendationConversationEntity conversation = activeConversation(10L, 7L);
        conversation.setStage(ConversationStage.CONFIRM.name());
        conversation.setStatus(ConversationStatus.READY_TO_CONFIRM.name());
        conversation.setContextJson(new ObjectMapper().writeValueAsString(context));
        when(conversationMapper.selectById(10L)).thenReturn(conversation);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.confirm(7L, 10L));

        assertEquals("推荐条件尚未确认完整", exception.getMessage());
        verifyNoInteractions(recommendationService, historyService);
    }

    @Test
    void confirmRunsInsideConversationLockAndExplicitTransactionTemplate() throws Exception {
        RecommendationConversationContext context = new RecommendationConversationContext(
                "清淡晚餐",
                "FAT_LOSS",
                List.of(new AvailableIngredientInput("鸡胸肉", new BigDecimal("300"), "g", true)),
                List.of(),
                List.of(),
                30,
                2,
                List.of(),
                List.of(),
                true
        );
        RecommendationConversationEntity conversation = activeConversation(10L, 7L);
        conversation.setStage(ConversationStage.CONFIRM.name());
        conversation.setStatus(ConversationStatus.READY_TO_CONFIRM.name());
        conversation.setContextJson(new ObjectMapper().writeValueAsString(context));
        when(conversationMapper.selectById(10L)).thenReturn(conversation);
        when(recommendationService.recommend(any(), any()))
                .thenReturn(new RecommendationResponse(99L, "summary", "health", "shopping", true, List.of()));

        Object lock = service.lockForConversation(10L);
        Thread worker;
        synchronized (lock) {
            worker = new Thread(() -> service.confirm(7L, 10L));
            worker.start();
            Thread.sleep(100);
            assertTrue(worker.isAlive());
            assertEquals(0, transactionManager.beginCount());
        }
        worker.join(3000);
        assertEquals(1, transactionManager.beginCount());
        assertEquals(1, transactionManager.commitCount());
    }
    private RecommendationConversationEntity activeConversation(Long id, Long userId) {
        RecommendationConversationEntity entity = new RecommendationConversationEntity();
        entity.setId(id);
        entity.setUserId(userId);
        entity.setStage(ConversationStage.INTENT.name());
        entity.setStatus(ConversationStatus.ACTIVE.name());
        entity.setInvalidAnswerCount(0);
        entity.setContextJson("{\"intentText\":null,\"dietGoal\":null,\"availableIngredients\":[],\"excludedIngredients\":[],\"allergyIngredients\":[],\"cookingTime\":null,\"servings\":null,\"unknownTerms\":[],\"conflicts\":[],\"restrictionsConfirmed\":false}");
        entity.setCreatedAt(LocalDateTime.of(2026, 7, 10, 8, 0));
        entity.setUpdatedAt(LocalDateTime.of(2026, 7, 10, 8, 0));
        return entity;
    }

    private RecommendationConversationMessageEntity userMessage(
            Long id,
            Long conversationId,
            String clientMessageId,
            String role,
            String content
    ) {
        RecommendationConversationMessageEntity entity = new RecommendationConversationMessageEntity();
        entity.setId(id);
        entity.setConversationId(conversationId);
        entity.setClientMessageId(clientMessageId);
        entity.setRole(role);
        entity.setContent(content);
        entity.setCreatedAt(LocalDateTime.of(2026, 7, 10, 8, 0).plusSeconds(id));
        return entity;
    }

    private RecommendationConversationMessageEntity assistantMessage(
            Long id,
            Long conversationId,
            String content
    ) {
        return userMessage(id, conversationId, null, "ASSISTANT", content);
    }

    private static class RecordingTransactionManager implements PlatformTransactionManager {
        private final AtomicInteger beginCount = new AtomicInteger();
        private final AtomicInteger commitCount = new AtomicInteger();

        @Override
        public TransactionStatus getTransaction(TransactionDefinition definition) throws TransactionException {
            beginCount.incrementAndGet();
            return new SimpleTransactionStatus();
        }

        @Override
        public void commit(TransactionStatus status) throws TransactionException {
            commitCount.incrementAndGet();
        }

        @Override
        public void rollback(TransactionStatus status) throws TransactionException {
        }

        int beginCount() {
            return beginCount.get();
        }

        int commitCount() {
            return commitCount.get();
        }
    }
}
