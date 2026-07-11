package com.shanzai.recipe.modules.recommendation.conversation;

import com.shanzai.recipe.modules.recommendation.RecommendationResponse;
import com.shanzai.recipe.modules.recommendation.RecommendedRecipeResponse;
import com.shanzai.recipe.security.JwtAuthenticationFilter;
import com.shanzai.recipe.security.JwtTokenProvider;
import com.shanzai.recipe.security.JwtUser;
import com.shanzai.recipe.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RecommendationConversationController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class RecommendationConversationControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RecommendationConversationService conversationService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void conversationRoutesRejectAnonymousRequests() throws Exception {
        mockMvc.perform(post("/api/recommendation-conversations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/recommendation-conversations/active"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/recommendation-conversations/10"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/recommendation-conversations/10/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"我想吃清淡一点\",\"clientMessageId\":\"m-1\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(patch("/api/recommendation-conversations/10/context")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/recommendation-conversations/10/confirm"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void startConversationPassesAuthenticatedUserAndReplaceActiveFlag() throws Exception {
        when(conversationService.startConversation(eq(42L), eq(true))).thenReturn(conversationResponse(10L));

        mockMvc.perform(post("/api/recommendation-conversations")
                        .with(authentication(jwtAuthentication()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"replaceActive\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(10));

        verify(conversationService).startConversation(42L, true);
    }

    @Test
    void startConversationDefaultsReplaceActiveToFalseWhenBodyIsMissing() throws Exception {
        when(conversationService.startConversation(eq(42L), eq(false))).thenReturn(conversationResponse(11L));

        mockMvc.perform(post("/api/recommendation-conversations")
                        .with(authentication(jwtAuthentication())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(11));

        verify(conversationService).startConversation(42L, false);
    }

    @Test
    void activeConversationReturnsNullWhenUserHasNoIncompleteConversation() throws Exception {
        when(conversationService.getActiveConversation(42L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/recommendation-conversations/active")
                        .with(authentication(jwtAuthentication())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(conversationService).getActiveConversation(42L);
    }

    @Test
    void getConversationPassesAuthenticatedUserIdToService() throws Exception {
        when(conversationService.getConversation(42L, 10L)).thenReturn(conversationResponse(10L));

        mockMvc.perform(get("/api/recommendation-conversations/10")
                        .with(authentication(jwtAuthentication())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(10));

        verify(conversationService).getConversation(42L, 10L);
    }

    @Test
    void sendMessagePassesAuthenticatedUserIdAndRequestToService() throws Exception {
        when(conversationService.sendMessage(eq(42L), eq(10L), any(ConversationMessageRequest.class)))
                .thenReturn(conversationResponse(10L));

        mockMvc.perform(post("/api/recommendation-conversations/10/messages")
                        .with(authentication(jwtAuthentication()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"我有番茄和鸡蛋\",\"clientMessageId\":\"m-1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(10));

        verify(conversationService).sendMessage(eq(42L), eq(10L), any(ConversationMessageRequest.class));
    }

    @Test
    void sendMessageRejectsBlankOrTooLongContentAndTooLongClientMessageId() throws Exception {
        mockMvc.perform(post("/api/recommendation-conversations/10/messages")
                        .with(authentication(jwtAuthentication()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"   \",\"clientMessageId\":\"m-1\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/recommendation-conversations/10/messages")
                        .with(authentication(jwtAuthentication()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"" + "a".repeat(1001) + "\",\"clientMessageId\":\"m-1\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/recommendation-conversations/10/messages")
                        .with(authentication(jwtAuthentication()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"可以\",\"clientMessageId\":\"" + "m".repeat(65) + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void patchContextPassesAuthenticatedUserIdAndRequestToService() throws Exception {
        when(conversationService.patchContext(eq(42L), eq(10L), any(ConversationContextPatchRequest.class)))
                .thenReturn(conversationResponse(10L));

        mockMvc.perform(patch("/api/recommendation-conversations/10/context")
                        .with(authentication(jwtAuthentication()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"intentText\":\"清淡晚餐\",\"dietGoal\":\"FAT_LOSS\",\"cookingTime\":30,\"servings\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(10));

        verify(conversationService).patchContext(eq(42L), eq(10L), any(ConversationContextPatchRequest.class));
    }

    @Test
    void confirmPassesAuthenticatedUserIdAndConversationIdToService() throws Exception {
        RecommendationResponse response = new RecommendationResponse(
                99L,
                "summary",
                "health",
                "shopping",
                true,
                List.of(new RecommendedRecipeResponse(1L, "番茄炒蛋", 88, "匹配食材", 180, null, null))
        );
        when(conversationService.confirm(42L, 10L)).thenReturn(response);

        mockMvc.perform(post("/api/recommendation-conversations/10/confirm")
                        .with(authentication(jwtAuthentication())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.historyId").value(99));

        verify(conversationService).confirm(42L, 10L);
    }

    private Authentication jwtAuthentication() {
        JwtUser user = new JwtUser(42L, "alice", "USER");
        return new UsernamePasswordAuthenticationToken(
                user,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    private ConversationResponse conversationResponse(Long id) {
        return new ConversationResponse(
                id,
                ConversationStage.INTENT,
                ConversationStatus.ACTIVE,
                0,
                RecommendationConversationContext.empty(),
                List.of(),
                false,
                List.of("清淡晚餐")
        );
    }
}

