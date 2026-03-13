package com.ecommerce.user.util;

import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class JwtUtil {

    public UUID extractUserId(Jwt jwt) {
        if (jwt == null) {
            throw new IllegalArgumentException("JWT principal is required");
        }

        String subject = jwt.getSubject();
        if (subject == null || subject.isBlank()) {
            Object claim = jwt.getClaims().get("user_id");
            if (claim == null) {
                throw new IllegalArgumentException("JWT does not contain a user identifier");
            }
            subject = claim.toString();
        }
        return UUID.fromString(subject);
    }
}
