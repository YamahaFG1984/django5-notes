package com.example.bookmarks.account;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.bookmarks.actions.ActionService;

@Service
@Transactional
public class FollowService {

    private final ContactRepository contacts;
    private final UserRepository users;
    private final ActionService actions;

    public FollowService(ContactRepository contacts, UserRepository users, ActionService actions) {
        this.contacts = contacts;
        this.users = users;
        this.actions = actions;
    }

    /** 返回 false 表示目标用户不存在或是自己（书中允许关注自己，这里拦掉） */
    public boolean follow(Long meId, Long targetId, boolean follow) {
        if (meId.equals(targetId)) {
            return false;
        }
        return users.findById(targetId).filter(User::isActive).map(target -> {
            if (follow) {
                if (!contacts.existsByUserFromIdAndUserToId(meId, targetId)) {   // get_or_create
                    contacts.save(new Contact(users.getReferenceById(meId), target));
                }
                actions.create(meId, "is following", target);
            } else {
                contacts.unfollow(meId, targetId);
            }
            return true;
        }).orElse(false);
    }

    @Transactional(readOnly = true)
    public boolean isFollowing(Long meId, Long targetId) {
        return contacts.existsByUserFromIdAndUserToId(meId, targetId);
    }

    @Transactional(readOnly = true)
    public long followers(Long userId) {
        return contacts.countByUserToId(userId);
    }
}
