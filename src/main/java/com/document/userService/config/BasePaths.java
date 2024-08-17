package com.document.userService.config;

public class BasePaths {
    public static final String BASE_PATH_USERS = "/v1/users";
    public static final String BASE_PATH_OTP = "/v1/otp";
    private BasePaths() {
        throw new IllegalStateException("Base Class");
    }
}