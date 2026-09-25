package com.example.bookmarks.account;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.example.bookmarks.config.BookmarksProperties;

/**
 * 密码重置令牌，照搬 Django 的 PasswordResetTokenGenerator 思路：
 * <ul>
 *   <li>令牌 = 时间戳(36 进制) + "-" + HMAC(SECRET_KEY, 用户 id + 密码哈希 + 上次登录时间 + 邮箱 + 时间戳)</li>
 *   <li><b>无状态</b>：不用建表存令牌，服务器重新计算一遍 HMAC 就能验证</li>
 *   <li>用户改了密码或重新登录后，哈希输入变了，旧链接自动失效；超过有效期也失效</li>
 * </ul>
 */
@Component
public class PasswordResetTokens {

    private final byte[] key;
    private final Duration timeout;
    private final Clock clock;

    @Autowired
    public PasswordResetTokens(BookmarksProperties properties) {
        this(properties, Clock.systemUTC());
    }

    PasswordResetTokens(BookmarksProperties properties, Clock clock) {
        if (properties.secretKey() == null || properties.secretKey().isBlank()) {
            throw new IllegalStateException("bookmarks.secret-key must be set");
        }
        this.key = properties.secretKey().getBytes(StandardCharsets.UTF_8);
        this.timeout = properties.passwordResetTimeout();
        this.clock = clock;
    }

    /** urlsafe_base64_encode(force_bytes(user.pk)) */
    public String uid(User user) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(String.valueOf(user.getId()).getBytes(StandardCharsets.UTF_8));
    }

    public Optional<Long> decodeUid(String uidb64) {
        try {
            return Optional.of(Long.parseLong(new String(Base64.getUrlDecoder().decode(uidb64), StandardCharsets.UTF_8)));
        } catch (IllegalArgumentException e) {   // 包括 NumberFormatException
            return Optional.empty();
        }
    }

    public String make(User user) {
        long timestamp = clock.instant().getEpochSecond();
        return Long.toString(timestamp, 36) + "-" + hash(user, timestamp);
    }

    public boolean check(User user, String token) {
        if (token == null || !token.contains("-")) {
            return false;
        }
        String[] parts = token.split("-", 2);
        long timestamp;
        try {
            timestamp = Long.parseLong(parts[0], 36);
        } catch (NumberFormatException e) {
            return false;
        }
        boolean signatureOk = MessageDigest.isEqual(   // 常量时间比较，防计时攻击
                hash(user, timestamp).getBytes(StandardCharsets.UTF_8),
                parts[1].getBytes(StandardCharsets.UTF_8));
        long age = clock.instant().getEpochSecond() - timestamp;
        return signatureOk && age >= 0 && age <= timeout.toSeconds();
    }

    private String hash(User user, long timestamp) {
        String lastLogin = user.getLastLogin() == null ? "" : String.valueOf(user.getLastLogin().toEpochSecond());
        String value = user.getId() + user.getPassword() + lastLogin + timestamp + user.getEmail();
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            byte[] digest = mac.doFinal(("bookmarks.PasswordResetTokens" + value).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest).substring(0, 32);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
