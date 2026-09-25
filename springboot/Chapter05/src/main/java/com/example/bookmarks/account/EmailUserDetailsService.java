package com.example.bookmarks.account;

import java.util.List;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用邮箱登录，对应 Django 书中的 account/authentication.py 里的 EmailAuthBackend。
 * 登录表单里的“用户名”框填的是邮箱时，由它按邮箱找用户；密码校验交给 DaoAuthenticationProvider。
 */
@Service
public class EmailUserDetailsService implements UserDetailsService {

    private final UserRepository users;

    public EmailUserDetailsService(UserRepository users) {
        this.users = users;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) {
        List<User> matches = users.findByEmailIgnoreCase(email);
        // 和 EmailAuthBackend 一样：找不到或找到多个（User.MultipleObjectsReturned）都视为失败
        if (email.isBlank() || matches.size() != 1) {
            throw new UsernameNotFoundException(email);
        }
        return CurrentUser.from(matches.getFirst());
    }
}
