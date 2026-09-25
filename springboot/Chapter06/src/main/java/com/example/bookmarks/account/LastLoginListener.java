package com.example.bookmarks.account;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

/**
 * 登录成功后更新 last_login。
 * Django 里是 user_logged_in 信号的内置接收者 update_last_login 做这件事；
 * Spring Security 登录成功时会发布 AuthenticationSuccessEvent，用 @EventListener 监听即可。
 */
@Component
public class LastLoginListener {

    private final UserRepository users;

    public LastLoginListener(UserRepository users) {
        this.users = users;
    }

    @EventListener
    public void onSuccess(AuthenticationSuccessEvent event) {
        users.updateLastLogin(event.getAuthentication().getName(), OffsetDateTime.now(ZoneOffset.UTC));
    }
}
