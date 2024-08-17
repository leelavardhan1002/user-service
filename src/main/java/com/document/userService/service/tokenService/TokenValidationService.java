package com.document.userService.service.tokenService;

import com.document.userService.auth.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenValidationService {

    @Autowired
    JwtUtil jwtUtil;

    public ResponseEntity<String> validateToken(String token) {
        try {
            Claims claims = jwtUtil.parseJwtClaims(token);
            if (jwtUtil.validateClaims(claims)) {
                log.info("TokenValidationService::validateToken::Token validation successful: {}", token);
                return ResponseEntity.ok("Valid token");
            }
        } catch (ExpiredJwtException expiredJwtException) {
            log.error(
                    "TokenValidationService::validateToken::Token expired: {}",
                    expiredJwtException.getMessage());
            return ResponseEntity.badRequest().body("Expired token");
        } catch (JwtException jwtException) {
            log.error(
                    "TokenValidationService::validateToken::Error validating token: {}",
                    jwtException.getMessage());
        }
        return ResponseEntity.badRequest().body("Invalid token");
    }
}