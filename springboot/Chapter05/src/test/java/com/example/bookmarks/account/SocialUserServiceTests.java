package com.example.bookmarks.account;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/** 不连 Google：手工构造一个 OIDC 用户，测试“第三方账号 → 本站用户”的映射逻辑。 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SocialUserServiceTests {

    @Autowired
    SocialUserService service;

    @Autowired
    UserRepository users;

    @Autowired
    ProfileRepository profiles;

    @Autowired
    UserSocialAuthRepository socialAuths;

    private OidcUser googleUser(String sub, String email) {
        OidcIdToken token = new OidcIdToken("token", Instant.now(), Instant.now().plusSeconds(60),
                Map.of("sub", sub, "email", email, "given_name", "Ada", "family_name", "Lovelace"));
        return new DefaultOidcUser(List.of(new SimpleGrantedAuthority("OIDC_USER")), token);
    }

    @Test
    void firstLoginCreatesUserProfileAndLink() {
        users.save(new User("ada", "{noop}x", "someone-else@example.com"));   // 用户名 ada 已被占用

        CurrentUser me = service.link("google", googleUser("google-123", "ada@example.com"));

        assertThat(me.getUsername()).isEqualTo("ada1");
        assertThat(me.getName()).isEqualTo("ada1");
        assertThat(me.getEmail()).isEqualTo("ada@example.com");   // 来自 OidcUser 的默认方法
        User user = users.findById(me.id()).orElseThrow();
        assertThat(user.getFullName()).isEqualTo("Ada Lovelace");
        assertThat(profiles.findByUserId(user.getId())).isPresent();
        assertThat(socialAuths.findByProviderAndUid("google", "google-123")).isPresent();
    }

    @Test
    void secondLoginReusesTheSameUser() {
        CurrentUser first = service.link("google", googleUser("google-456", "grace@example.com"));
        CurrentUser second = service.link("google", googleUser("google-456", "grace@new-domain.com"));
        assertThat(second.id()).isEqualTo(first.id());
        assertThat(users.count()).isEqualTo(1);
    }
}
