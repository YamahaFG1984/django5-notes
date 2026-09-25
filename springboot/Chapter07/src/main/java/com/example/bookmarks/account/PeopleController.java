package com.example.bookmarks.account;

import java.util.Map;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.example.bookmarks.common.NotFoundException;
import com.example.bookmarks.images.ImageService;

/** account/views.py 里的 user_list、user_detail、user_follow（“People” 菜单）。 */
@Controller
@RequestMapping("/account/users")
public class PeopleController {

    private final UserRepository users;
    private final ProfileRepository profiles;
    private final FollowService follows;
    private final ImageService images;

    public PeopleController(UserRepository users, ProfileRepository profiles, FollowService follows, ImageService images) {
        this.users = users;
        this.profiles = profiles;
        this.follows = follows;
        this.images = images;
    }

    @GetMapping("/")
    public String list(Model model) {
        model.addAttribute("section", "people");
        model.addAttribute("profiles", profiles.findAllOfActiveUsers());
        return "account/user/list";
    }

    /** 固定路径 /follow/ 比 /{username}/ 更具体，Spring 会优先匹配它，不用担心顺序 */
    @PostMapping("/follow/")
    @ResponseBody
    public Map<String, String> follow(@AuthenticationPrincipal CurrentUser me,
                                      @RequestParam(required = false) Long id,
                                      @RequestParam(required = false) String action) {
        if (id != null && action != null && follows.follow(me.id(), id, "follow".equals(action))) {
            return Map.of("status", "ok");
        }
        return Map.of("status", "error");
    }

    @GetMapping("/{username}/")
    public String detail(@PathVariable String username, @AuthenticationPrincipal CurrentUser me, Model model) {
        User user = users.findByUsername(username).filter(User::isActive)
                .orElseThrow(() -> new NotFoundException("No User matches the given query."));
        model.addAttribute("section", "people");
        model.addAttribute("user", user);
        model.addAttribute("profile", profiles.findByUserId(user.getId()).orElse(null));
        model.addAttribute("totalFollowers", follows.followers(user.getId()));
        model.addAttribute("following", follows.isFollowing(me.id(), user.getId()));
        model.addAttribute("images", images.createdBy(user.getId()));
        return "account/user/detail";
    }
}
