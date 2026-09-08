package com.nemblex.security;

import com.nemblex.security.filter.JWTAuthenticationFilter;
import com.nemblex.security.filter.JWTAuthorizationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomAuthenticationManager authenticationManager;

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration-ms}")
    private long jwtExpirationMs;

    public SecurityConfig(CustomAuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        JWTAuthenticationFilter authenticationFilter =
                new JWTAuthenticationFilter(authenticationManager, jwtSecret, jwtExpirationMs);
        JWTAuthorizationFilter authorizationFilter = new JWTAuthorizationFilter(jwtSecret);

        http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/tickets/mine")
                            .hasAnyRole("TECHNICIAN", "SUPERVISOR", "ADMIN", "EMPLOYEE")
                        .requestMatchers(HttpMethod.GET, "/api/tickets/**")
                            .hasAnyRole("TECHNICIAN", "SUPERVISOR", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/tickets")
                            .hasAnyRole("TECHNICIAN", "SUPERVISOR", "ADMIN", "EMPLOYEE")
                        .requestMatchers(HttpMethod.PUT, "/api/tickets/*/assign-to-me")
                            .hasAnyRole("TECHNICIAN", "SUPERVISOR", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/tickets/*/unassign")
                            .hasAnyRole("TECHNICIAN", "SUPERVISOR", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/tickets/*/classify")
                            .hasAnyRole("TECHNICIAN", "SUPERVISOR", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/tickets/**")
                            .hasAnyRole("SUPERVISOR", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/tickets/**")
                            .hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/audit-logs")
                            .hasAnyRole("TECHNICIAN", "SUPERVISOR", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/audit-logs/resolve-now")
                            .hasAnyRole("TECHNICIAN", "SUPERVISOR", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/audit-logs/pending")
                            .hasAnyRole("SUPERVISOR", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/audit-logs/ticket/**")
                            .hasAnyRole("TECHNICIAN", "SUPERVISOR", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/audit-logs/*/resolve")
                            .hasAnyRole("SUPERVISOR", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/audit-logs/*/undo")
                            .hasAnyRole("SUPERVISOR", "ADMIN")
                        .anyRequest().authenticated())
                .addFilterBefore(authorizationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAt(authenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
