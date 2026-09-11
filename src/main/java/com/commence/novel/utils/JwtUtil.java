package com.commence.novel.utils;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtil {
    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);

    // JWT 密钥（配置在 application.yml 中，至少 256 位）
    @Value("${jwt.secret:your-secret-key-32bytes-long-1234567890}")
    private String secret;

    // Token 过期时间（2小时）
    @Value("${jwt.expire:7200000}")
    private long expireTime;

    // 生成密钥
    private SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    /**
     * 生成 Token
     * @param uid 用户ID
     * @param uname 用户名
     * @param role 角色
     * @return JWT Token
     */
    public String generateToken(Long uid, String uname, String role) {
        // 设置 Token 载荷
        Map<String, Object> claims = new HashMap<>();
        claims.put("uid", uid);
        claims.put("uname", uname);
        claims.put("role", role);

        return Jwts.builder()
                .setClaims(claims) // 载荷
                .setIssuedAt(new Date()) // 签发时间
                .setExpiration(new Date(System.currentTimeMillis() + expireTime)) // 过期时间
                .signWith(getSecretKey(), SignatureAlgorithm.HS256) // 签名
                .compact();
    }

    /**
     * 验证 Token 有效性
     * @param token Token字符串
     * @return 是否有效
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSecretKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.error("Token 已过期: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("不支持的 Token 格式: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.error("Token 格式错误: {}", e.getMessage());
        } catch (SignatureException e) {
            log.error("Token 签名验证失败: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("Token 为空或无效: {}", e.getMessage());
        }
        return false;
    }

    /**
     * 从 Token 中解析用户信息
     * @param token Token字符串
     * @return 用户信息（载荷）
     */
    public Claims parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSecretKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 从 Token 中获取用户ID
     * @param token Token字符串
     * @return 用户ID
     */
    public Long getUidFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("uid", Long.class);
    }

    /**
     * 从 Token 中获取角色
     * @param token Token字符串
     * @return 角色（READER/AUTHOR/ADMIN）
     */
    public String getRoleFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("role", String.class);
    }
}
