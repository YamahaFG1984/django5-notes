package com.example.bookmarks.account;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Google 登录成功、拿到用户信息之后，把第三方账号映射成本站用户。
 * 这一步在 python-social-auth 里由 SOCIAL_AUTH_PIPELINE 完成，下面的注释标出了对应的管道步骤。
 */
@Service
public class SocialUserService extends OidcUserService {

    private final UserRepository users;
    private final ProfileRepository profiles;
    private final UserSocialAuthRepository socialAuths;
    private final PasswordEncoder passwordEncoder;

    public SocialUserService(UserRepository users, ProfileRepository profiles,
                             UserSocialAuthRepository socialAuths, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.profiles = profiles;
        this.socialAuths = socialAuths;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest request) {
        OidcUser oidc = super.loadUser(request);   // social_details / social_uid：向 Google 取用户信息
        return link(request.getClientRegistration().getRegistrationId(), oidc);
    }

    /** 与网络无关的部分单独成方法，方便测试。 */
    @Transactional
    public CurrentUser link(String provider, OidcUser oidc) {
        User user = socialAuths.findByProviderAndUid(provider, oidc.getSubject())   // social_user
                .map(UserSocialAuth::getUser)
                .orElseGet(() -> createUser(provider, oidc));
        if (!user.isActive()) {                                                    // auth_allowed
            throw new org.springframework.security.authentication.DisabledException("User is disabled");
        }
        user.setLastLogin(OffsetDateTime.now(ZoneOffset.UTC));
        return CurrentUser.from(user, oidc);
    }

    private User createUser(String provider, OidcUser oidc) {
        // get_username + create_user：用邮箱前缀当用户名，重名时加数字后缀
        String email = oidc.getEmail() == null ? "" : oidc.getEmail();
        String base = email.contains("@") ? email.substring(0, email.indexOf('@')) : "user";
        base = base.replaceAll("[^\\w.@+-]", "").toLowerCase(Locale.ROOT);
        String username = base.isBlank() ? "user" : base;
        for (int i = 1; users.existsByUsername(username); i++) {
            username = base + i;
        }
        // 第三方登录的用户没有本站密码：存一个随机值的哈希，等于“不可用的密码”（≈ set_unusable_password）
        User user = new User(username, passwordEncoder.encode(UUID.randomUUID().toString()), email);
        user.setFirstName(oidc.getGivenName());                                  // user_details
        user.setLastName(oidc.getFamilyName());
        users.save(user);
        profiles.save(new Profile(user));                                         // account.authentication.create_profile
        socialAuths.save(new UserSocialAuth(user, provider, oidc.getSubject()));  // associate_user
        return user;
    }
}
