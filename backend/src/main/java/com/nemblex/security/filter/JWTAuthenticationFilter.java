package com.nemblex.security.filter;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nemblex.dto.request.LoginRequest;
import com.nemblex.dto.response.JwtResponse;
import com.nemblex.exception.ErrorResponse;
import com.nemblex.security.CustomAuthenticationManager;
import com.nemblex.security.SecurityConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Date;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

public class JWTAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().findAndRegisterModules();

    private final String jwtSecret;
    private final long expirationMs;

    public JWTAuthenticationFilter(CustomAuthenticationManager authenticationManager,
                                   String jwtSecret,
                                   long expirationMs) {
        super.setAuthenticationManager(authenticationManager);
        setFilterProcessesUrl(SecurityConstants.LOGIN_URL);
        this.jwtSecret = jwtSecret;
        this.expirationMs = expirationMs;
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException {
        try {
            LoginRequest credentials = OBJECT_MAPPER.readValue(request.getInputStream(), LoginRequest.class);
            return getAuthenticationManager().authenticate(
                    new UsernamePasswordAuthenticationToken(credentials.getEmail(), credentials.getPassword()));
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse authentication request", e);
        }
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response,
                                            FilterChain chain, Authentication authResult)
            throws IOException, ServletException {

        String role = authResult.getAuthorities().stream()
                .findFirst()
                .map(GrantedAuthority::getAuthority)
                .orElse("ROLE_TECHNICIAN");

        String token = JWT.create()
                .withSubject(authResult.getName())
                .withClaim(SecurityConstants.ROLE_CLAIM, role)
                .withExpiresAt(new Date(System.currentTimeMillis() + expirationMs))
                .sign(Algorithm.HMAC512(jwtSecret));

        JwtResponse body = new JwtResponse(token, authResult.getName());

        response.setContentType("application/json");
        response.addHeader(SecurityConstants.HEADER_STRING, SecurityConstants.TOKEN_PREFIX + token);
        response.getWriter().write(OBJECT_MAPPER.writeValueAsString(body));
        response.getWriter().flush();
    }

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response,
                                              AuthenticationException failed)
            throws IOException, ServletException {
        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                HttpServletResponse.SC_UNAUTHORIZED,
                "Unauthorized",
                "Invalid email or password",
                request.getRequestURI());

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write(OBJECT_MAPPER.writeValueAsString(error));
        response.getWriter().flush();
    }
}
