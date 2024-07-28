package com.document.userService.controller;

import com.document.userService.service.tokenService.TokenValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/validate-token")
@Slf4j
@RequiredArgsConstructor
public class TokenValidationController {

    @Autowired
    TokenValidationService tokenValidationService;

    @PostMapping
    public ResponseEntity<String> validateToken(@RequestBody String token) {
        log.info("Received request to validate token: {}", token);
        return tokenValidationService.validateToken(token);
    }
}
