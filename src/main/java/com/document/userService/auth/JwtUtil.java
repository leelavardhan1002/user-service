package com.document.userService.auth;

import com.document.userService.constants.JwtUtilConstants;
import com.document.userService.entity.user.User;
import io.jsonwebtoken.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static com.document.userService.constants.JwtUtilConstants.ACCESS_TOKEN_VALIDITY;

@Component
@Slf4j
public class JwtUtil {

    private final Set<String> revokedTokens = ConcurrentHashMap.newKeySet();
    private final JwtParser jwtParser;

    public JwtUtil() {
        this.jwtParser = Jwts.parser().setSigningKey(JwtUtilConstants.SECRET_KEY);
    }

    public void revokeToken(String token) {
        revokedTokens.add(token);
        log.info("Token revoked successfully: {}", token);
    }

    public boolean isTokenRevoked(String token) {
        return revokedTokens.contains(token);
    }

    public String createToken(User user) {
        Claims claims = Jwts.claims().setSubject(String.valueOf(user.getUserId()));
        claims.put(JwtUtilConstants.USER_ID, user.getUserId());
        claims.put(JwtUtilConstants.FIRST_NAME, user.getFirstName());
        claims.put(JwtUtilConstants.LAST_NAME, user.getLastName());
        claims.put(JwtUtilConstants.EMAIL, user.getEmail());
        claims.put(JwtUtilConstants.ROLE, user.getRole());

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + TimeUnit.HOURS.toMillis(ACCESS_TOKEN_VALIDITY));

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(SignatureAlgorithm.HS256, JwtUtilConstants.SECRET_KEY)
                .compact();
    }

    public Claims parseJwtClaims(String token) {
        try {
            return jwtParser.parseClaimsJws(token).getBody();
        } catch (ExpiredJwtException e) {
            log.error("Expired JWT token: {}", token, e);
            throw e;
        } catch (JwtException e) {
            log.error("Invalid JWT token: {}", token, e);
            throw e;
        }
    }

    public Claims resolveClaims(HttpServletRequest req) {
        String token = resolveToken(req);
        if (token != null && !isTokenRevoked(token)) {
            return parseJwtClaims(token);
        }
        return null;
    }

    public String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(JwtUtilConstants.TOKEN_HEADER);
        if (bearerToken != null && bearerToken.startsWith(JwtUtilConstants.TOKEN_PREFIX)) {
            return bearerToken.substring(JwtUtilConstants.TOKEN_PREFIX.length());
        }
        return null;
    }

    public boolean validateClaims(Claims claims) {
        return claims.getExpiration().after(new Date());
    }

    public String getEmail(Claims claims) {
        return claims.getSubject();
    }
}
