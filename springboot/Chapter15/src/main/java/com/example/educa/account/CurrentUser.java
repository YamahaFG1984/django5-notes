package com.example.educa.account;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * 当前用户（≈ request.user）。authorities 里既有角色（ROLE_STAFF），也有 Django 风格的权限代码
 * （courses.add_course），后者来自用户所在的组 —— @PreAuthorize("hasAuthority('courses.add_course')")
 * 就是 PermissionRequiredMixin 的对应物。
 */
public record CurrentUser(Long id, String username, String password, boolean enabled,
                          List<GrantedAuthority> authorities) implements UserDetails {

    public static CurrentUser from(User user) {
        Set<String> names = new LinkedHashSet<>();
        names.add("ROLE_USER");
        if (user.isStaff()) {
            names.add("ROLE_STAFF");
        }
        if (user.isSuperuser()) {
            names.add("ROLE_ADMIN");
            names.addAll(Permissions.ALL);
        }
        user.getGroups().forEach(group -> names.addAll(group.getPermissions()));
        return new CurrentUser(user.getId(), user.getUsername(), user.getPassword(), user.isActive(),
                names.stream().map(n -> (GrantedAuthority) new SimpleGrantedAuthority(n)).toList());
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
