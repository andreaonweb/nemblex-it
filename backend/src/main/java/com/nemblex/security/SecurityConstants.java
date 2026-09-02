package com.nemblex.security;

public final class SecurityConstants {

    private SecurityConstants() {
    }

    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String HEADER_STRING = "Authorization";
    public static final String LOGIN_URL = "/api/auth/login";
    public static final String ROLE_CLAIM = "role";
}
