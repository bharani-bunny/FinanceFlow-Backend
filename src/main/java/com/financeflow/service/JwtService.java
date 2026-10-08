package com.financeflow.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.financeflow.entity.Role;
import com.financeflow.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class JwtService {
    private static final String HEADER = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
    private static final long TOKEN_LIFETIME_SECONDS = 24 * 60 * 60;

    private final ObjectMapper objectMapper;
    private final byte[] secret;

    public JwtService(ObjectMapper objectMapper,
                      @Value("${app.jwt.secret}") String secret) {
        this.objectMapper = objectMapper;
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        if (this.secret.length < 32) {
            throw new IllegalArgumentException("JWT secret must be at least 32 bytes");
        }
    }

    public String createToken(User user) {
        try {
            Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
            String encodedHeader = encoder.encodeToString(HEADER.getBytes(StandardCharsets.UTF_8));
            Instant now = Instant.now();
            Map<String, Object> claims = new LinkedHashMap<>();
            claims.put("sub", user.getEmail());
            claims.put("userId", user.getId());
            claims.put("role", user.getRole().name());
            claims.put("iat", now.getEpochSecond());
            claims.put("exp", now.plusSeconds(TOKEN_LIFETIME_SECONDS).getEpochSecond());
            String encodedPayload = encoder.encodeToString(objectMapper.writeValueAsBytes(claims));
            String content = encodedHeader + "." + encodedPayload;
            return content + "." + encoder.encodeToString(sign(content));
        } catch (Exception exception) {
            throw new IllegalStateException("Could not create access token", exception);
        }
    }

    public TokenClaims parseToken(String token) {
        try {
            String[] parts = token.split("\\.", -1);
            if (parts.length != 3) return null;

            String content = parts[0] + "." + parts[1];
            byte[] suppliedSignature = Base64.getUrlDecoder().decode(parts[2]);
            if (!MessageDigest.isEqual(sign(content), suppliedSignature)) return null;

            Map<String, Object> claims = objectMapper.readValue(
                    Base64.getUrlDecoder().decode(parts[1]),
                    new TypeReference<>() { });
            Object expiration = claims.get("exp");
            Object userId = claims.get("userId");
            String email = (String) claims.get("sub");
            Role role = Role.valueOf((String) claims.get("role"));
            if (!(expiration instanceof Number) || !(userId instanceof Number) || email == null) return null;
            if (((Number) expiration).longValue() <= Instant.now().getEpochSecond()) return null;

            return new TokenClaims(email, ((Number) userId).longValue(), role);
        } catch (Exception ignored) {
            return null;
        }
    }

    private byte[] sign(String content) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret, "HmacSHA256"));
        return mac.doFinal(content.getBytes(StandardCharsets.UTF_8));
    }

    public record TokenClaims(String email, Long userId, Role role) { }
}
