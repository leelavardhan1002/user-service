package com.document.userService.exception;

public class UserAlreadyVerifiedException extends Exception {
    public UserAlreadyVerifiedException(String message) {
        super(message);
    }
}
