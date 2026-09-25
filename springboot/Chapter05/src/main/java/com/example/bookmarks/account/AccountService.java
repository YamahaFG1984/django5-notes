package com.example.bookmarks.account;

import java.io.IOException;
import java.io.UncheckedIOException;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.bookmarks.common.MediaStorage;
import com.example.bookmarks.common.NotFoundException;

/**
 * 账号相关的写操作。放在 service 里是为了让“建用户 + 建资料”这类多步操作处于同一个事务：
 * 任何一步失败，整个操作回滚（Django 里要自己写 transaction.atomic 才有这个保证）。
 */
@Service
@Transactional
public class AccountService {

    private final UserRepository users;
    private final ProfileRepository profiles;
    private final PasswordEncoder passwordEncoder;
    private final MediaStorage media;

    public AccountService(UserRepository users, ProfileRepository profiles,
                          PasswordEncoder passwordEncoder, MediaStorage media) {
        this.users = users;
        this.profiles = profiles;
        this.passwordEncoder = passwordEncoder;
        this.media = media;
    }

    /** new_user.set_password(...) → new_user.save() → Profile.objects.create(user=new_user) */
    public User register(UserRegistrationForm form) {
        User user = new User(form.getUsername(), passwordEncoder.encode(form.getPassword()), form.getEmail());
        user.setFirstName(form.getFirstName());
        users.save(user);
        profiles.save(new Profile(user));
        return user;
    }

    @Transactional(readOnly = true)
    public User user(Long id) {
        return users.findById(id).orElseThrow(() -> new NotFoundException("User " + id + " not found"));
    }

    /** 找不到资料就补建一个，避免原书中“超级用户没有 Profile 时编辑页报错”的问题。 */
    public Profile profileOf(Long userId) {
        return profiles.findByUserId(userId)
                .orElseGet(() -> profiles.save(new Profile(user(userId))));
    }

    /** user_form.save() + profile_form.save()，两个表单在一个事务里保存。 */
    public void updateAccount(Long userId, UserEditForm userForm, ProfileEditForm profileForm) {
        User user = user(userId);
        user.setFirstName(userForm.getFirstName());
        user.setLastName(userForm.getLastName());
        user.setEmail(userForm.getEmail());

        Profile profile = profileOf(userId);
        profile.setDateOfBirth(profileForm.getDateOfBirth());
        if (profileForm.hasNewPhoto()) {
            try {
                profile.setPhoto(media.saveImage("users", profileForm.getPhoto().getOriginalFilename(),
                        profileForm.getPhoto().getBytes()));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        // 不需要调用 save：受管理的实体在事务提交时自动写回（脏检查）
    }

    @Transactional(readOnly = true)
    public boolean checkPassword(Long userId, String rawPassword) {
        return passwordEncoder.matches(rawPassword, user(userId).getPassword());
    }

    public void setPassword(Long userId, String rawPassword) {
        user(userId).setPassword(passwordEncoder.encode(rawPassword));
    }
}
