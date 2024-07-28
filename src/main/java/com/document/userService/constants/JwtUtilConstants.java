package com.document.userService.constants;

public class JwtUtilConstants {
    public static final String USER_ID = "userId";
    public static final String FIRST_NAME = "firstName";
    public static final String LAST_NAME = "lastName";
    public static final String EMAIL = "email";
    public static final String ROLE = "role";
    public static final String SECRET_KEY = System.getenv().getOrDefault("SECRET_KEY", "mysecretkey");
    public static final String TOKEN_HEADER = "Authorization";
    public static final String TOKEN_PREFIX = "Bearer ";
    public static final long ACCESS_TOKEN_VALIDITY = 2;

    private JwtUtilConstants() {}
}