package com.example.mysite.account;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * 登录后放进 SecurityContext 的“当前用户”，控制器里用 @AuthenticationPrincipal 取出，
 * 作用相当于 Django 的 request.user（但只是一个轻量快照，不是 JPA 实体）。
 */
public record CurrentUser(Long id, String username, String password, boolean enabled,
                          List<GrantedAuthority> authorities) implements UserDetails {

    public static CurrentUser from(User user) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        if (user.isStaff()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_STAFF"));
        }
        if (user.isSuperuser()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        }
        return new CurrentUser(user.getId(), user.getUsername(), user.getPassword(),
                user.isActive(), List.copyOf(authorities));
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
}
