package com.example.bookmarks.account;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * 给每个页面的模型里放一个 requestUser（最新的 User 实体，未登录时为 null），
 * 作用相当于 Django 的 auth 上下文处理器让模板能访问 {{ request.user }}。
 * 会话里的 CurrentUser 只是登录时的快照，用户改了名字后并不会更新，所以这里每次请求都查一次库 —— Django 也是这么做的。
 */
@ControllerAdvice
public class CurrentUserAdvice {

    private final UserRepository users;

    public CurrentUserAdvice(UserRepository users) {
        this.users = users;
    }

    @ModelAttribute("requestUser")
    public User requestUser(@AuthenticationPrincipal CurrentUser me) {
        return me == null ? null : users.findById(me.id()).orElse(null);
    }
}
