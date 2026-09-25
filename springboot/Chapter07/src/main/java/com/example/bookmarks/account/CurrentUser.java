package com.example.bookmarks.account;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

/**
 * 当前登录用户（≈ request.user）。
 * 同时实现 UserDetails（表单登录）和 OidcUser（Google 登录），
 * 这样无论用哪种方式登录，控制器里 @AuthenticationPrincipal CurrentUser me 拿到的都是同一种对象。
 *
 * @param oidc Google 登录时的原始 OIDC 用户信息；表单登录时为 null
 */
public record CurrentUser(Long id, String username, String password, boolean enabled,
                          List<GrantedAuthority> authorities, OidcUser oidc) implements UserDetails, OidcUser {

    public static CurrentUser from(User user) {
        return from(user, null);
    }

    public static CurrentUser from(User user, OidcUser oidc) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        if (user.isStaff()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_STAFF"));
        }
        if (user.isSuperuser()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        }
        return new CurrentUser(user.getId(), user.getUsername(), user.getPassword(),
                user.isActive(), List.copyOf(authorities), oidc);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    /** Authentication.getName() 最终返回它：两种登录方式下都是本站用户名。 */
    @Override
    public String getName() {
        return username;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return oidc == null ? Map.of() : oidc.getAttributes();
    }

    @Override
    public Map<String, Object> getClaims() {
        return oidc == null ? Map.of() : oidc.getClaims();
    }

    @Override
    public OidcUserInfo getUserInfo() {
        return oidc == null ? null : oidc.getUserInfo();
    }

    @Override
    public OidcIdToken getIdToken() {
        return oidc == null ? null : oidc.getIdToken();
    }
}
