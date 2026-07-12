package com.shanzai.recipe.security;

import com.shanzai.recipe.modules.auth.UserEntity;
import com.shanzai.recipe.modules.auth.UserMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtTokenProvider jwtTokenProvider;
    private final UserMapper userMapper;

    @Autowired
    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, ObjectProvider<UserMapper> userMapperProvider) {
        this(jwtTokenProvider, userMapperProvider.getIfAvailable());
    }

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, UserMapper userMapper) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userMapper = userMapper;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            authenticate(authorization.substring(7));
        }
        filterChain.doFilter(request, response);
    }

    private void authenticate(String token) {
        try {
            JwtUser jwtUser = jwtTokenProvider.parse(token);
            if (userMapper == null) {
                setAuthentication(jwtUser);
                return;
            }
            UserEntity user = userMapper.selectById(jwtUser.userId());
            if (user == null || user.getStatus() == null || user.getStatus() != 1) {
                SecurityContextHolder.clearContext();
                return;
            }
            setAuthentication(jwtUser);
        } catch (RuntimeException ignored) {
            SecurityContextHolder.clearContext();
        }
    }

    private void setAuthentication(JwtUser jwtUser) {
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
            jwtUser,
            null,
            List.of(new SimpleGrantedAuthority("ROLE_" + jwtUser.role()))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
