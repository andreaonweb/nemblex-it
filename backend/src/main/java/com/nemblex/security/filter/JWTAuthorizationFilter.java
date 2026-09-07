package com.nemblex.security.filter;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.nemblex.security.SecurityConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class JWTAuthorizationFilter extends OncePerRequestFilter {

    private final String jwtSecret;

    public JWTAuthorizationFilter(String jwtSecret) {
        this.jwtSecret = jwtSecret;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader(SecurityConstants.HEADER_STRING);
        if (header == null || !header.startsWith(SecurityConstants.TOKEN_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String token = header.substring(SecurityConstants.TOKEN_PREFIX.length());
            DecodedJWT decoded = JWT.require(Algorithm.HMAC512(jwtSecret)).build().verify(token);

            String email = decoded.getSubject();
            String role = decoded.getClaim(SecurityConstants.ROLE_CLAIM).asString();
            List<SimpleGrantedAuthority> authorities = role == null
                    ? List.of()
                    : List.of(new SimpleGrantedAuthority(role));

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(email, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (Exception e) {
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}
