package com.example.bookmarks.images;

import java.util.Map;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.bookmarks.account.CurrentUser;
import com.example.bookmarks.common.MediaStorage.InvalidImageException;
import com.example.bookmarks.common.Messages;
import com.example.bookmarks.images.ImageDownloader.DownloadException;

/** images/views.py + images/urls.py（app_name = 'images'） */
@Controller
@RequestMapping("/images")
public class ImageController {

    private static final int PAGE_SIZE = 8;

    private final ImageService imageService;
    private final ImageRepository images;

    public ImageController(ImageService imageService, ImageRepository images) {
        this.imageService = imageService;
        this.images = images;
    }

    /** 书签小工具打开的页面：GET 参数（url、title）直接绑定到表单对象，作为初始值 */
    @GetMapping("/create/")
    public String createForm(@ModelAttribute("form") ImageCreateForm form, Model model) {
        model.addAttribute("section", "images");
        return "images/image/create";
    }

    @PostMapping("/create/")
    public String create(@AuthenticationPrincipal CurrentUser me,
                         @Valid @ModelAttribute("form") ImageCreateForm form, BindingResult errors,
                         Model model, RedirectAttributes redirect) {
        model.addAttribute("section", "images");
        if (errors.hasErrors()) {
            return "images/image/create";
        }
        try {
            Image image = imageService.create(me.id(), form);
            Messages.success(redirect, "Image added successfully");
            return "redirect:" + image.getAbsoluteUrl();
        } catch (DownloadException | InvalidImageException e) {
            errors.rejectValue("url", "download", e.getMessage());
            return "images/image/create";
        }
    }

    @GetMapping("/detail/{id}/{slug}/")
    public String detail(@PathVariable Long id, @PathVariable String slug,
                         @AuthenticationPrincipal CurrentUser me, Model model) {
        model.addAttribute("section", "images");
        model.addAttribute("detail", imageService.detail(id, slug, me.id()));
        return "images/image/detail";
    }

    /**
     * AJAX 点赞。@ResponseBody 表示返回值直接序列化成 JSON（≈ JsonResponse），而不是当作视图名。
     * 只映射 POST ≈ @require_POST；CSRF 令牌由前端放在请求头里。
     */
    @PostMapping("/like/")
    @ResponseBody
    public Map<String, String> like(@AuthenticationPrincipal CurrentUser me,
                                    @RequestParam(required = false) Long id,
                                    @RequestParam(required = false) String action) {
        if (id != null && action != null && imageService.like(id, me.id(), "like".equals(action))) {
            return Map.of("status", "ok");
        }
        return Map.of("status", "error");
    }

    /**
     * 列表 + 无限滚动：带 images_only 参数时只返回图片片段（给 fetch 用），
     * 页码超出范围时返回空内容，前端据此停止加载。
     */
    @GetMapping("/")
    public String list(@RequestParam(name = "page", required = false) String pageParam,
                       @RequestParam(name = "images_only", required = false) String imagesOnly,
                       Model model) {
        int pageNumber = parsePage(pageParam);
        Page<Image> page = images.findAllByOrderByCreatedDesc(PageRequest.of(pageNumber - 1, PAGE_SIZE));
        if (page.getContent().isEmpty() && page.getTotalPages() > 0 && imagesOnly == null) {
            page = images.findAllByOrderByCreatedDesc(PageRequest.of(page.getTotalPages() - 1, PAGE_SIZE));
        }
        model.addAttribute("section", "images");
        model.addAttribute("images", page.getContent());
        return imagesOnly != null ? "images/image/list_images" : "images/image/list";
    }

    private static int parsePage(String value) {
        try {
            return Math.max(Integer.parseInt(value), 1);
        } catch (NumberFormatException e) {
            return 1;
        }
    }
}
