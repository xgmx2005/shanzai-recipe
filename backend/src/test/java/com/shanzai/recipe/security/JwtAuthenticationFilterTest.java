package com.shanzai.recipe.security;

import com.shanzai.recipe.common.Role;
import com.shanzai.recipe.modules.auth.UserEntity;
import com.shanzai.recipe.modules.auth.UserMapper;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JwtAuthenticationFilterTest {
    private final JwtTokenProvider jwtTokenProvider = mock(JwtTokenProvider.class);
    private final UserMapper userMapper = mock(UserMapper.class);
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtTokenProvider, userMapper);

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void authenticatesActiveUserToken() throws Exception {
        when(jwtTokenProvider.parse("token")).thenReturn(new JwtUser(1L, "user1", Role.USER.name()));
        when(userMapper.selectById(1L)).thenReturn(user(1));
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(requestWithToken(), new MockHttpServletResponse(), chain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rejectsDisabledUserToken() throws Exception {
        UserEntity disabled = user(0);
        when(jwtTokenProvider.parse("token")).thenReturn(new JwtUser(1L, "user1", Role.USER.name()));
        when(userMapper.selectById(1L)).thenReturn(disabled);
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(requestWithToken(), new MockHttpServletResponse(), chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    private static MockHttpServletRequest requestWithToken() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token");
        return request;
    }

    private static UserEntity user(int status) {
        UserEntity user = new UserEntity();
        user.setId(1L);
        user.setUsername("user1");
        user.setRole(Role.USER.name());
        user.setStatus(status);
        return user;
    }
}
