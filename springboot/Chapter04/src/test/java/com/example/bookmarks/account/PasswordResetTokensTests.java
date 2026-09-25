package com.example.bookmarks.account;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.bookmarks.config.BookmarksProperties;

/** 纯单元测试：用固定的 Clock 控制“现在”，验证过期与篡改。 */
class PasswordResetTokensTests {

    private final BookmarksProperties properties =
            new BookmarksProperties("x", "test-secret", Path.of("media"), Duration.ofDays(3));

    private User user() {
        User user = new User("alice", "{noop}secret", "alice@example.com");
        ReflectionTestUtils.setField(user, "id", 42L);
        return user;
    }

    private PasswordResetTokens at(String instant) {
        return new PasswordResetTokens(properties, Clock.fixed(Instant.parse(instant), ZoneOffset.UTC));
    }

    @Test
    void validTokenExpiresAfterTimeout() {
        User user = user();
        String token = at("2025-01-01T00:00:00Z").make(user);
        assertThat(at("2025-01-03T23:59:00Z").check(user, token)).isTrue();
        assertThat(at("2025-01-04T00:00:01Z").check(user, token)).isFalse();
    }

    @Test
    void tokenIsInvalidatedByPasswordChangeOrTampering() {
        User user = user();
        PasswordResetTokens tokens = at("2025-01-01T00:00:00Z");
        String token = tokens.make(user);
        assertThat(tokens.check(user, token + "0")).isFalse();
        assertThat(tokens.check(user, "garbage")).isFalse();
        user.setPassword("{noop}changed");
        assertThat(tokens.check(user, token)).isFalse();
    }

    @Test
    void uidRoundTrip() {
        PasswordResetTokens tokens = at("2025-01-01T00:00:00Z");
        assertThat(tokens.decodeUid(tokens.uid(user()))).contains(42L);
        assertThat(tokens.decodeUid("!!!")).isEmpty();
    }
}
