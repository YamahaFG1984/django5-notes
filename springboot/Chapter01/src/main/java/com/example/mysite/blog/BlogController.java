package com.example.mysite.blog;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.mysite.account.CurrentUser;
import com.example.mysite.account.UserRepository;
import com.example.mysite.common.NotFoundException;

/**
 * blog/views.py + blog/urls.py 合二为一：
 * 每个方法是一个“视图函数”，方法上的 @GetMapping / @PostMapping 就是它的 URL 规则。
 */
@Controller
@RequestMapping("/blog")
public class BlogController {

    private final PostRepository posts;
    private final FavouritePostRepository favourites;
    private final UserRepository users;

    public BlogController(PostRepository posts, FavouritePostRepository favourites, UserRepository users) {
        this.posts = posts;
        this.favourites = favourites;
        this.users = users;
    }

    /** path('', views.post_list, name='post_list') */
    @GetMapping({"", "/"})
    public String postList(Model model) {
        model.addAttribute("posts", posts.findPublished());
        return "blog/post/list";
    }

    /** path('&lt;int:id&gt;/', views.post_detail, name='post_detail') */
    @GetMapping("/{id}/")
    public String postDetail(@PathVariable Long id, @AuthenticationPrincipal CurrentUser me, Model model) {
        Post post = posts.findPublishedById(id)
                .orElseThrow(() -> new NotFoundException("No Post matches the given query."));
        boolean isFavourite = me != null
                && favourites.existsById(new FavouritePostId(me.id(), post.getId()));
        model.addAttribute("post", post);
        model.addAttribute("isFavourite", isFavourite);
        return "blog/post/detail";
    }

    /**
     * 收藏一篇文章。和原书不同：这里只接受 POST（改数据的操作不该用 GET），
     * 并且 SecurityConfig 要求先登录，匿名用户会被重定向到登录页。
     */
    @PostMapping("/favourite/add/{id}/")
    public String addFavourite(@PathVariable Long id, @AuthenticationPrincipal CurrentUser me) {
        Post post = posts.findPublishedById(id)
                .orElseThrow(() -> new NotFoundException("No Post matches the given query."));
        FavouritePostId key = new FavouritePostId(me.id(), post.getId());
        if (!favourites.existsById(key)) {   // get_or_create 的“get”部分
            favourites.save(new FavouritePost(users.getReferenceById(me.id()), post));
        }
        return "redirect:" + post.getAbsoluteUrl();
    }

    /** @login_required def favourites(request) */
    @GetMapping("/favourites/")
    public String favourites(@AuthenticationPrincipal CurrentUser me, Model model) {
        model.addAttribute("favouritePosts", posts.findFavouritesOf(me.id()));
        return "blog/post/favourites";
    }
}
