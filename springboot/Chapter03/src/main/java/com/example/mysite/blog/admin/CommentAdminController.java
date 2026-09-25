package com.example.mysite.blog.admin;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.criteria.Predicate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.mysite.blog.Comment;
import com.example.mysite.blog.CommentRepository;
import com.example.mysite.common.NotFoundException;

/**
 * 评论后台，对应：
 * <pre>
 * class CommentAdmin(admin.ModelAdmin):
 *     list_display = ['name', 'email', 'post', 'created', 'active']
 *     list_filter = ['active', 'created', 'updated']
 *     search_fields = ['name', 'email', 'body']
 * </pre>
 * 额外提供一个“启用/屏蔽”按钮，这是评论审核最常用的操作。
 */
@Controller
@RequestMapping("/admin/blog/comment")
public class CommentAdminController {

    private final CommentRepository comments;

    public CommentAdminController(CommentRepository comments) {
        this.comments = comments;
    }

    @GetMapping("/")
    public String list(@RequestParam(required = false) String q,
                       @RequestParam(required = false) Boolean active,
                       @RequestParam(defaultValue = "1") int page,
                       Model model) {
        Page<Comment> result = comments.findAll(filter(q, active),
                PageRequest.of(Math.max(page, 1) - 1, 20, Sort.by(Sort.Direction.DESC, "created")));
        model.addAttribute("page", result);
        model.addAttribute("q", q);
        model.addAttribute("active", active);
        model.addAttribute("activeCount", comments.countByActive(true));
        model.addAttribute("inactiveCount", comments.countByActive(false));
        return "admin/comment/list";
    }

    @PostMapping("/{id}/toggle/")
    @Transactional
    public String toggle(@PathVariable Long id, RedirectAttributes flash) {
        Comment comment = comments.findById(id)
                .orElseThrow(() -> new NotFoundException("Comment " + id + " does not exist"));
        comment.setActive(!comment.isActive());   // 事务提交时 Hibernate 自动发 UPDATE（脏检查）
        flash.addFlashAttribute("message", comment + (comment.isActive() ? " is now active." : " was hidden."));
        return "redirect:/admin/blog/comment/";
    }

    static Specification<Comment> filter(String q, Boolean active) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (q != null && !q.isBlank()) {
                String like = "%" + q.strip().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), like),
                        cb.like(cb.lower(root.get("email")), like),
                        cb.like(cb.lower(root.get("body")), like)));
            }
            if (active != null) {
                predicates.add(cb.equal(root.get("active"), active));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
