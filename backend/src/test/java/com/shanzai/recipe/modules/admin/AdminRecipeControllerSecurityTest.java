package com.shanzai.recipe.modules.admin;

import com.shanzai.recipe.modules.recipe.RecipeService;
import com.shanzai.recipe.security.JwtAuthenticationFilter;
import com.shanzai.recipe.security.JwtTokenProvider;
import com.shanzai.recipe.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminRecipeController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class AdminRecipeControllerSecurityTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RecipeService recipeService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void adminRecipesRejectsAnonymousAndNormalUser() throws Exception {
        mockMvc.perform(get("/api/admin/recipes"))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/recipes").with(user("normal-user").roles("USER")))
            .andExpect(status().isForbidden());
    }

    @Test
    void staticRecipeImagesArePublic() throws Exception {
        mockMvc.perform(get("/images/recipes/missing.jpg"))
            .andExpect(status().isNotFound());
    }

    @Test
    void adminRecipesAllowsMaintainer() throws Exception {
        when(recipeService.listAdminRecipes(isNull(), isNull(), isNull(), isNull())).thenReturn(List.of());

        mockMvc.perform(get("/api/admin/recipes").with(user("maintainer").roles("MAINTAINER")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }
}
