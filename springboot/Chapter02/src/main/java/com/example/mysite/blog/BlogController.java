package com.example.mysite.blog;

import java.time.DateTimeException;
import java.time.LocalDate;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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

    private static final int PAGE_SIZE = 3;

    private final PostRepository posts;
    private final CommentRepository comments;
    private final MailSender mailSender;
    private final MysiteProperties properties;

    public BlogController(PostRepository posts, CommentRepository comments,
                          MailSender mailSender, MysiteProperties properties) {
        this.posts = posts;
        this.comments = comments;
        this.mailSender = mailSender;
        this.properties = properties;
    }

    /**
     * 分页列表。Django 书里先写了一个函数视图 + Paginator，又写了 ListView(paginate_by=3)。
     * Spring 没有“类视图”，分页靠 Spring Data 的 Pageable/Page；
     * 下面照搬了函数视图的容错逻辑：page 不是整数 → 第 1 页；超出范围 → 最后一页。
     */
    @GetMapping({"", "/"})
    public String postList(@RequestParam(name = "page", required = false) String pageParam, Model model) {
        int pageNumber = parsePage(pageParam);
        Page<Post> page = posts.findPublished(PageRequest.of(pageNumber - 1, PAGE_SIZE));
        if (page.getContent().isEmpty() && page.getTotalPages() > 0) {
            page = posts.findPublished(PageRequest.of(page.getTotalPages() - 1, PAGE_SIZE));
        }
        model.addAttribute("posts", page);
        return "blog/post/list";
    }

    /** path('&lt;int:year&gt;/&lt;int:month&gt;/&lt;int:day&gt;/&lt;slug:post&gt;/', ...) */
    @GetMapping("/{year}/{month}/{day}/{slug}/")
    public String postDetail(@PathVariable int year, @PathVariable int month, @PathVariable int day,
                             @PathVariable String slug, Model model) {
        Post post = posts.findPublishedBySlugAndDate(slug, dateOrNotFound(year, month, day))
                .orElseThrow(() -> new NotFoundException("No Post matches the given query."));
        model.addAttribute("post", post);
        model.addAttribute("comments", comments.findByPostAndActiveTrueOrderByCreatedAsc(post));
        model.addAttribute("form", new CommentForm());
        return "blog/post/detail";
    }

    /** GET 显示空表单 */
    @GetMapping("/{postId}/share/")
    public String shareForm(@PathVariable Long postId, Model model) {
        model.addAttribute("post", publishedPost(postId));
        model.addAttribute("form", new EmailPostForm());
        model.addAttribute("sent", false);
        return "blog/post/share";
    }

    /**
     * POST 校验并发送。@Valid 触发校验，结果放在紧随其后的 BindingResult 里，
     * 相当于 form = EmailPostForm(request.POST); form.is_valid()。
     */
    @PostMapping("/{postId}/share/")
    public String share(@PathVariable Long postId, @Valid @ModelAttribute("form") EmailPostForm form,
                        BindingResult errors, Model model) {
        Post post = publishedPost(postId);
        boolean sent = false;
        if (!errors.hasErrors()) {
            // request.build_absolute_uri(post.get_absolute_url())
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

    /** @require_POST def post_comment(request, post_id) —— 只映射 POST，GET 会得到 405。 */
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

    private Post publishedPost(Long id) {
        return posts.findPublishedById(id)
                .orElseThrow(() -> new NotFoundException("No Post matches the given query."));
    }

    private static int parsePage(String value) {
        try {
            return Math.max(Integer.parseInt(value), 1);
        } catch (NumberFormatException e) {   // PageNotAnInteger
            return 1;
        }
    }

    private static LocalDate dateOrNotFound(int year, int month, int day) {
        try {
            return LocalDate.of(year, month, day);
        } catch (DateTimeException e) {   // 比如 /blog/2025/13/40/...
            throw new NotFoundException("Invalid date");
        }
    }
}
