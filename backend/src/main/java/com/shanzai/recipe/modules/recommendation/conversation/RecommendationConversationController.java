package com.shanzai.recipe.modules.recommendation.conversation;

import com.shanzai.recipe.common.ApiResponse;
import com.shanzai.recipe.modules.recommendation.RecommendationResponse;
import com.shanzai.recipe.security.JwtUser;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recommendation-conversations")
public class RecommendationConversationController {
    private final RecommendationConversationService conversationService;

    public RecommendationConversationController(RecommendationConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @PostMapping
    public ApiResponse<ConversationResponse> start(
            Authentication authentication,
            @RequestBody(required = false) StartConversationRequest request
    ) {
        return ApiResponse.ok(conversationService.startConversation(
                currentUserId(authentication),
                request != null && request.replaceActive()
        ));
    }

    @GetMapping("/active")
    public ApiResponse<ConversationResponse> active(Authentication authentication) {
        return ApiResponse.ok(conversationService.getActiveConversation(currentUserId(authentication)).orElse(null));
    }

    @GetMapping("/{id}")
    public ApiResponse<ConversationResponse> get(Authentication authentication, @PathVariable Long id) {
        return ApiResponse.ok(conversationService.getConversation(currentUserId(authentication), id));
    }

    @PostMapping("/{id}/messages")
    public ApiResponse<ConversationResponse> sendMessage(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody ConversationMessageRequest request
    ) {
        return ApiResponse.ok(conversationService.sendMessage(currentUserId(authentication), id, request));
    }

    @PatchMapping("/{id}/context")
    public ApiResponse<ConversationResponse> patchContext(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody ConversationContextPatchRequest request
    ) {
        return ApiResponse.ok(conversationService.patchContext(currentUserId(authentication), id, request));
    }

    @PostMapping("/{id}/confirm")
    public ApiResponse<RecommendationResponse> confirm(Authentication authentication, @PathVariable Long id) {
        return ApiResponse.ok(conversationService.confirm(currentUserId(authentication), id));
    }

    private Long currentUserId(Authentication authentication) {
        return ((JwtUser) authentication.getPrincipal()).userId();
    }
}
