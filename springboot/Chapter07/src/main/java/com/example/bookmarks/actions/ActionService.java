package com.example.bookmarks.actions;

import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.bookmarks.account.ContactRepository;
import com.example.bookmarks.account.Profile;
import com.example.bookmarks.account.ProfileRepository;
import com.example.bookmarks.account.User;
import com.example.bookmarks.account.UserRepository;
import com.example.bookmarks.images.Image;
import com.example.bookmarks.images.ImageRepository;

/** actions/utils.py 的 create_action，加上仪表盘活动流的查询。 */
@Service
@Transactional
public class ActionService {

    /** 模板要用的一条动态：谁、做了什么、对谁/什么 */
    public record ActionView(Action action, User actor, String actorPhoto,
                             String targetLabel, String targetUrl, String targetImage) {
    }

    private final ActionRepository actions;
    private final ContactRepository contacts;
    private final ImageRepository images;
    private final UserRepository users;
    private final ProfileRepository profiles;
    private final Clock clock = Clock.systemUTC();

    public ActionService(ActionRepository actions, ContactRepository contacts, ImageRepository images,
                         UserRepository users, ProfileRepository profiles) {
        this.actions = actions;
        this.contacts = contacts;
        this.images = images;
        this.users = users;
        this.profiles = profiles;
    }

    /**
     * 记录一条动态；如果同一个用户在最近 60 秒内对同一目标做过同样的事，就不重复记录。
     * 返回是否真的新建了。
     */
    public boolean create(Long userId, String verb, ActionTarget target) {
        OffsetDateTime lastMinute = OffsetDateTime.now(clock).minus(Duration.ofSeconds(60));
        String type = target == null ? null : target.targetType();
        Long targetId = target == null ? null : target.getId();
        if (actions.existsSimilar(userId, verb, type, targetId, lastMinute)) {
            return false;
        }
        actions.save(new Action(users.getReferenceById(userId), verb, target));
        return true;
    }

    /**
     * 仪表盘的“What's happening”：默认看所有人的动态（除了自己）；关注了别人就只看关注的人。
     * Django 版用 select_related('user', 'user__profile').prefetch_related('target')，
     * 这里对应地分几步批量加载：动态 + 用户（1 条）、头像（1 条）、每种目标类型各 1 条。
     */
    @Transactional(readOnly = true)
    public List<ActionView> feedFor(Long userId, int count) {
        List<Long> following = contacts.findFollowingIds(userId);
        List<Action> latest = following.isEmpty()
                ? actions.findByUserIdNotOrderByCreatedDesc(userId, Limit.of(count))
                : actions.findByUserIdInOrderByCreatedDesc(following, Limit.of(count));

        Set<Long> actorIds = latest.stream().map(a -> a.getUser().getId()).collect(Collectors.toSet());
        Map<Long, String> photos = profiles.findByUserIdIn(actorIds).stream()
                .collect(Collectors.toMap(p -> p.getUser().getId(), Profile::getPhoto));

        Map<Long, Image> targetImages = byId(images.findAllById(idsOf(latest, Image.TARGET_TYPE)), Image::getId);
        Map<Long, User> targetUsers = byId(users.findAllById(idsOf(latest, User.TARGET_TYPE)), User::getId);

        List<ActionView> views = new ArrayList<>();
        for (Action action : latest) {
            User actor = action.getUser();
            String label = null;
            String url = null;
            String picture = null;
            if (Image.TARGET_TYPE.equals(action.getTargetType()) && targetImages.containsKey(action.getTargetId())) {
                Image image = targetImages.get(action.getTargetId());
                label = image.getTitle();
                url = image.getAbsoluteUrl();
                picture = image.getImage();
            } else if (User.TARGET_TYPE.equals(action.getTargetType()) && targetUsers.containsKey(action.getTargetId())) {
                User target = targetUsers.get(action.getTargetId());
                label = target.getDisplayName();
                url = target.getAbsoluteUrl();
            }
            views.add(new ActionView(action, actor, photos.getOrDefault(actor.getId(), ""), label, url, picture));
        }
        return views;
    }

    private static List<Long> idsOf(List<Action> actions, String type) {
        return actions.stream().filter(a -> type.equals(a.getTargetType())).map(Action::getTargetId).distinct().toList();
    }

    private static <T> Map<Long, T> byId(List<T> items, Function<T, Long> id) {
        return items.stream().collect(Collectors.toMap(id, Function.identity()));
    }
}
