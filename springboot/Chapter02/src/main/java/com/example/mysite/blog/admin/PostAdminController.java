package com.example.mysite.blog.admin;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.criteria.Predicate;
import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.mysite.account.UserRepository;
import com.example.mysite.blog.Post;
import com.example.mysite.blog.PostRepository;
import com.example.mysite.blog.PostStatus;
import com.example.mysite.common.NotFoundException;

/**
 * 手写的“迷你 admin”。Django 注册一个 ModelAdmin 就能得到完整的增删改查后台，
 * Spring 生态没有等价物，只能自己写控制器和模板 —— 这是两个框架差异最大的地方之一。
 * URL 刻意模仿 Django admin：/admin/blog/post/、/admin/blog/post/add/、/admin/blog/post/1/change/。
 */
@Controller
@RequestMapping("/admin")
public class PostAdminController {

    private static final int PAGE_SIZE = 20;

    private final PostRepository posts;
    private final UserRepository users;

    public PostAdminController(PostRepository posts, UserRepository users) {
        this.posts = posts;
        this.users = users;
    }

    @GetMapping({"", "/"})
    public String index() {
        return "redirect:/admin/blog/post/";
    }

    /** list_display + search_fields + list_filter + ordering + show_facets */
    @GetMapping("/blog/post/")
    public String list(@RequestParam(required = false) String q,
                       @RequestParam(required = false) PostStatus status,
                       @RequestParam(defaultValue = "1") int page,
                       Model model) {
        Sort ordering = Sort.by("status", "publish");   // ordering = ['status', 'publish']
        Page<Post> result = posts.findAll(filter(q, status), PageRequest.of(Math.max(page, 1) - 1, PAGE_SIZE, ordering));
        model.addAttribute("page", result);
        model.addAttribute("q", q);
        model.addAttribute("status", status);
        model.addAttribute("statuses", PostStatus.values());
        model.addAttribute("draftCount", posts.countByStatus(PostStatus.DRAFT));
        model.addAttribute("publishedCount", posts.countByStatus(PostStatus.PUBLISHED));
        return "admin/post/list";
    }

    @GetMapping("/blog/post/add/")
    public String addForm(Model model) {
        return renderForm(model, new PostForm(), null);
    }

    @PostMapping("/blog/post/add/")
    public String add(@Valid @ModelAttribute("form") PostForm form, BindingResult errors,
                      Model model, RedirectAttributes flash) {
        validateUniqueForDate(form, null, errors);
        if (errors.hasErrors()) {
            return renderForm(model, form, null);
        }
        Post post = new Post(form.getTitle(), form.getSlug(), users.getReferenceById(form.getAuthorId()), form.getBody());
        apply(form, post);
        posts.save(post);
        flash.addFlashAttribute("message", "The post “" + post.getTitle() + "” was added successfully.");
        return "redirect:/admin/blog/post/";
    }

    @GetMapping("/blog/post/{id}/change/")
    public String changeForm(@PathVariable Long id, Model model) {
        Post post = load(id);
        return renderForm(model, PostForm.of(post), post);
    }

    @PostMapping("/blog/post/{id}/change/")
    public String change(@PathVariable Long id, @Valid @ModelAttribute("form") PostForm form,
                         BindingResult errors, Model model, RedirectAttributes flash) {
        Post post = load(id);
        validateUniqueForDate(form, id, errors);
        if (errors.hasErrors()) {
            return renderForm(model, form, post);
        }
        apply(form, post);
        posts.save(post);
        flash.addFlashAttribute("message", "The post “" + post.getTitle() + "” was changed successfully.");
        return "redirect:/admin/blog/post/";
    }

    @PostMapping("/blog/post/{id}/delete/")
    public String delete(@PathVariable Long id, RedirectAttributes flash) {
        Post post = load(id);
        posts.delete(post);
        flash.addFlashAttribute("message", "The post “" + post.getTitle() + "” was deleted successfully.");
        return "redirect:/admin/blog/post/";
    }

    /**
     * unique_for_date='publish'：Django 的 ModelForm 会自动做这项检查（它不是数据库约束）。
     * Spring 里要自己写，通过 errors.rejectValue 把错误挂到 slug 字段上。
     */
    private void validateUniqueForDate(PostForm form, Long excludeId, BindingResult errors) {
        if (form.getSlug() == null || form.getPublish() == null || errors.hasFieldErrors("slug")) {
            return;
        }
        if (posts.slugTakenOn(form.getSlug(), form.getPublish().toLocalDate(), excludeId)) {
            errors.rejectValue("slug", "unique_for_date", "Slug must be unique for Publish date.");
        }
    }

    private Post load(Long id) {
        return posts.findById(id).orElseThrow(() -> new NotFoundException("Post " + id + " does not exist"));
    }

    private void apply(PostForm form, Post post) {
        post.setTitle(form.getTitle());
        post.setSlug(form.getSlug());
        post.setAuthor(users.getReferenceById(form.getAuthorId()));
        post.setBody(form.getBody());
        post.setPublish(form.getPublish().atOffset(java.time.ZoneOffset.UTC));
        post.setStatus(form.getStatus());
    }

    private String renderForm(Model model, PostForm form, Post post) {
        model.addAttribute("form", form);
        model.addAttribute("post", post);
        model.addAttribute("authors", users.findAllByOrderByUsernameAsc());
        model.addAttribute("statuses", PostStatus.values());
        return "admin/post/form";
    }

    /** search_fields = ['title', 'body'] 与 list_filter = ['status'] 组合成一个 JPA Specification（动态 WHERE）。 */
    static Specification<Post> filter(String q, PostStatus status) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (q != null && !q.isBlank()) {
                String like = "%" + q.strip().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), like),
                        cb.like(cb.lower(root.get("body")), like)));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
