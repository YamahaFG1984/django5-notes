package com.example.mysite.blog;

import java.time.DateTimeException;
import java.time.LocalDate;

import jakarta.validation.Valid;

import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.example.mysite.common.NotFoundException;
import com.example.mysite.config.MysiteProperties;

/** blog/views.py + blog/urls.py */
@Controller
@RequestMapping("/blog")
public class BlogController {

    private final PostRepository posts;
    private final PostService postService;
    private final TagRepository tags;
    private final CommentRepository comments;
    private final MailSender mailSender;
    private final MysiteProperties properties;

    public BlogController(PostRepository posts, PostService postService, TagRepository tags,
                          CommentRepository comments, MailSender mailSender, MysiteProperties properties) {
        this.posts = posts;
        this.postService = postService;
        this.tags = tags;
        this.comments = comments;
        this.mailSender = mailSender;
        this.properties = properties;
    }

    /** path('', views.post_list, name='post_list') */
    @GetMapping({"", "/"})
    public String postList(@RequestParam(name = "page", required = false) String page, Model model) {
        return renderList(null, page, model);
    }

    /** path('tag/&lt;slug:tag_slug&gt;/', views.post_list, name='post_list_by_tag') —— 同一个视图，两条 URL */
    @GetMapping("/tag/{tagSlug}/")
    public String postListByTag(@PathVariable String tagSlug,
                                @RequestParam(name = "page", required = false) String page, Model model) {
        Tag tag = tags.findBySlug(tagSlug).orElseThrow(() -> new NotFoundException("No Tag matches the given query."));
        return renderList(tag, page, model);
    }

    private String renderList(Tag tag, String page, Model model) {
        model.addAttribute("posts", postService.publishedPage(tag, parsePage(page)));
        model.addAttribute("tag", tag);
        return "blog/post/list";
    }

    @GetMapping("/{year}/{month}/{day}/{slug}/")
    public String postDetail(@PathVariable int year, @PathVariable int month, @PathVariable int day,
                             @PathVariable String slug, Model model) {
        Post post = posts.findPublishedBySlugAndDate(slug, dateOrNotFound(year, month, day))
                .orElseThrow(() -> new NotFoundException("No Post matches the given query."));
        model.addAttribute("post", post);
        model.addAttribute("comments", comments.findByPostAndActiveTrueOrderByCreatedAsc(post));
        model.addAttribute("form", new CommentForm());
        model.addAttribute("similarPosts", postService.similarPosts(post, 4));
        return "blog/post/detail";
    }

    @GetMapping("/{postId}/share/")
    public String shareForm(@PathVariable Long postId, Model model) {
        model.addAttribute("post", publishedPost(postId));
        model.addAttribute("form", new EmailPostForm());
        model.addAttribute("sent", false);
        return "blog/post/share";
    }

    @PostMapping("/{postId}/share/")
    public String share(@PathVariable Long postId, @Valid @ModelAttribute("form") EmailPostForm form,
                        BindingResult errors, Model model) {
        Post post = publishedPost(postId);
        boolean sent = false;
        if (!errors.hasErrors()) {
            String postUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path(post.getAbsoluteUrl()).toUriString();
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(properties.defaultFromEmail());
            message.setTo(form.getTo());
            message.setSubject("%s (%s) recommends you read %s".formatted(form.getName(), form.getEmail(), post.getTitle()));
            message.setText("Read %s at %s%n%n%s's comments: %s".formatted(
                    post.getTitle(), postUrl, form.getName(), form.getComments()));
            mailSender.send(message);
            sent = true;
        }
        model.addAttribute("post", post);
        model.addAttribute("sent", sent);
        return "blog/post/share";
    }

    @PostMapping("/{postId}/comment/")
    public String postComment(@PathVariable Long postId, @Valid @ModelAttribute("form") CommentForm form,
                              BindingResult errors, Model model) {
        Post post = publishedPost(postId);
        Comment comment = null;
        if (!errors.hasErrors()) {
            comment = comments.save(form.toComment(post));
        }
        model.addAttribute("post", post);
        model.addAttribute("comment", comment);
        return "blog/post/comment";
    }

    /**
     * 搜索。GET 表单：带了 query 参数才查询，否则只显示空表单。
     * 注意 BindingResult 必须紧跟在被校验的 @ModelAttribute 参数后面。
     */
    @GetMapping("/search/")
    public String postSearch(@Valid @ModelAttribute("form") SearchForm form, BindingResult errors,
                             @RequestParam(required = false) String query, Model model) {
        if (query == null) {
            // 首次打开页面：换成一个新的空表单，原来的“必填”校验错误会随之丢弃
            model.addAttribute("form", new SearchForm());
        } else if (!errors.hasErrors()) {
            model.addAttribute("query", form.getQuery());
            model.addAttribute("results", posts.searchByTrigram(form.getQuery()));
        }
        return "blog/post/search";
    }

    private Post publishedPost(Long id) {
        return posts.findPublishedById(id)
                .orElseThrow(() -> new NotFoundException("No Post matches the given query."));
    }

    private static int parsePage(String value) {
        try {
            return Math.max(Integer.parseInt(value), 1);
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    private static LocalDate dateOrNotFound(int year, int month, int day) {
        try {
            return LocalDate.of(year, month, day);
        } catch (DateTimeException e) {
            throw new NotFoundException("Invalid date");
        }
    }
}
