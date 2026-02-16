package com.fuyi.exam.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class JwtUtil {

    private static final String SECRET = "fuyi_exam_system_secret_key_secure_2026_version_plus";
    private static final long EXPIRE = 604800000; // 7天

    /**
     * 生成 token
     */
    public static String generateToken(Integer userId, String username, String role) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + EXPIRE);

        return Jwts.builder()
                .setSubject(userId + "")
                .claim("username", username)
                .claim("role", role)
                .setIssuedAt(now)
                .setExpiration(expiration)
                .signWith(SignatureAlgorithm.HS256, SECRET)
                .compact();
    }

    /**
     * 解析 token
     */
    public static Claims getClaimsByToken(String token) {
        if (token == null || token.trim().isEmpty() || "null".equals(token) || "undefined".equals(token)) {
            return null;
        }
        try {
            return Jwts.parser()
                    .setSigningKey(SECRET)
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            // 解析失败不抛异常，只返回null
            return null;
        }
    }

    public static String getUsernameFromToken(String token) {
        Claims claims = getClaimsByToken(token);
        return claims != null ? (String) claims.get("username") : null;
    }

    public static Integer getUserId(String token) {
        Claims claims = getClaimsByToken(token);
        if (claims != null && claims.getSubject() != null) {
            return Integer.parseInt(claims.getSubject());
        }
        return null;
    }
}