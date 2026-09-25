package com.example.educa.courses.manage;

import java.io.IOException;
import java.util.Map;

import jakarta.validation.Valid;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.example.educa.account.CurrentUser;
import com.example.educa.common.MediaStorage;
import com.example.educa.common.NotFoundException;
import com.example.educa.courses.Course;
import com.example.educa.courses.Item;

/**
 * 模块与内容的管理，对应 CourseModuleUpdateView、ModuleContentListView、ContentCreateUpdateView、
 * ContentDeleteView、ModuleOrderView、ContentOrderView。
 * 这些视图在书中只校验“是不是自己的课程”，不要求权限代码；这里保持一致（/course/** 已要求登录）。
 */
@Controller
@RequestMapping("/course")
public class ModuleContentController {

    private final CourseManageService service;

    public ModuleContentController(CourseManageService service) {
        this.service = service;
    }

    // ---------------------------------------------------------------- 模块 formset

    @GetMapping("/{id}/module/")
    public String modules(@PathVariable Long id, @AuthenticationPrincipal CurrentUser user, Model model) {
        Course course = service.getCourse(id, user.id());
        model.addAttribute("course", course);
        model.addAttribute("formset", ModuleFormSet.of(service.modulesOf(course)));
        return "courses/manage/module/formset";
    }

    @PostMapping("/{id}/module/")
    public String saveModules(@PathVariable Long id, @AuthenticationPrincipal CurrentUser user,
                              @Valid @ModelAttribute("formset") ModuleFormSet formset, BindingResult result,
                              Model model) {
        Course course = service.getCourse(id, user.id());
        // 与 Django formset 相同：没勾“删除”的非空行，标题必填
        for (int i = 0; i < formset.getRows().size(); i++) {
            ModuleFormSet.Row row = formset.getRows().get(i);
            if (!row.isEmptyExtra() && !row.isDelete() && row.getTitle().isBlank()) {
                result.rejectValue("rows[" + i + "].title", "required", "This field is required.");
            }
        }
        if (result.hasErrors()) {
            model.addAttribute("course", course);
            return "courses/manage/module/formset";
        }
        service.saveModules(id, user.id(), formset);
        return "redirect:/course/mine/";
    }

    // ---------------------------------------------------------------- 内容列表

    @GetMapping("/module/{moduleId}/")
    public String contentList(@PathVariable Long moduleId, @AuthenticationPrincipal CurrentUser user, Model model) {
        CourseManageService.ModulePage page = service.modulePage(moduleId, user.id());
        model.addAttribute("course", page.course());
        model.addAttribute("module", page.module());
        model.addAttribute("modules", page.modules());
        model.addAttribute("contents", page.contents());
        return "courses/manage/module/content_list";
    }

    // ---------------------------------------------------------------- 新建 / 编辑条目

    @GetMapping({"/module/{moduleId}/content/{modelName}/create/", "/module/{moduleId}/content/{modelName}/{itemId}/"})
    public String itemForm(@PathVariable Long moduleId, @PathVariable String modelName,
                           @PathVariable(required = false) Long itemId,
                           @AuthenticationPrincipal CurrentUser user, Model model) {
        checkModelName(modelName);
        service.getModule(moduleId, user.id());
        Item item = itemId == null ? null : service.getItem(moduleId, modelName, itemId, user.id());
        return itemPage(model, modelName, item, item == null ? new ItemForm() : ItemForm.of(item));
    }

    @PostMapping({"/module/{moduleId}/content/{modelName}/create/", "/module/{moduleId}/content/{modelName}/{itemId}/"})
    public String saveItem(@PathVariable Long moduleId, @PathVariable String modelName,
                           @PathVariable(required = false) Long itemId,
                           @AuthenticationPrincipal CurrentUser user,
                           @Valid @ModelAttribute("form") ItemForm form, BindingResult result, Model model) {
        checkModelName(modelName);
        service.getModule(moduleId, user.id());
        Item item = itemId == null ? null : service.getItem(moduleId, modelName, itemId, user.id());
        validateByType(modelName, item, form, result);
        if (result.hasErrors()) {
            return itemPage(model, modelName, item, form);
        }
        service.saveItem(moduleId, modelName, itemId, user.id(), form);
        return "redirect:/course/module/" + moduleId + "/";
    }

    @PostMapping("/content/{id}/delete/")
    public String deleteContent(@PathVariable Long id, @AuthenticationPrincipal CurrentUser user) {
        Long moduleId = service.deleteContent(id, user.id());
        return "redirect:/course/module/" + moduleId + "/";
    }

    // ---------------------------------------------------------------- 拖拽排序（JSON）

    /**
     * ≈ ModuleOrderView(CsrfExemptMixin, JsonRequestResponseMixin)。
     * 书中为了省事直接关闭了 CSRF 校验；这里保留 CSRF，前端 fetch 时把令牌放进请求头。
     */
    @PostMapping("/module/order/")
    @ResponseBody
    public Map<String, String> moduleOrder(@RequestBody Map<Long, Integer> order,
                                           @AuthenticationPrincipal CurrentUser user) {
        service.reorderModules(order, user.id());
        return Map.of("saved", "OK");
    }

    @PostMapping("/content/order/")
    @ResponseBody
    public Map<String, String> contentOrder(@RequestBody Map<Long, Integer> order,
                                            @AuthenticationPrincipal CurrentUser user) {
        service.reorderContents(order, user.id());
        return Map.of("saved", "OK");
    }

    // ----------------------------------------------------------------

    private String itemPage(Model model, String modelName, Item item, ItemForm form) {
        model.addAttribute("modelName", modelName);
        model.addAttribute("object", item);
        model.addAttribute("currentFile", item == null ? null : ItemForm.currentFile(item));
        model.addAttribute("form", form);
        return "courses/manage/content/form";
    }

    private static void checkModelName(String modelName) {
        if (!CourseManageService.ITEM_TYPES.contains(modelName)) {
            throw new NotFoundException();
        }
    }

    /**
     * modelform_factory 会根据模型字段生成不同的校验规则；这里按类型手动补上：
     * text 要求 content，video 要求合法 URL，image/file 新建时要求上传文件，image 还要确认真的是图片。
     */
    private static void validateByType(String modelName, Item item, ItemForm form, BindingResult result) {
        switch (modelName) {
            case "text" -> {
                if (form.getContent().isBlank()) {
                    result.rejectValue("content", "required", "This field is required.");
                }
            }
            case "video" -> {
                if (form.getUrl().isBlank()) {
                    result.rejectValue("url", "required", "This field is required.");
                } else if (!form.getUrl().matches("(?i)https?://[^\\s/$.?#][^\\s]*")) {
                    result.rejectValue("url", "invalid", "Enter a valid URL.");
                }
            }
            default -> {
                if (!form.hasFile()) {
                    if (item == null) {
                        result.rejectValue("file", "required", "This field is required.");
                    }
                } else if (modelName.equals("image")) {
                    try {
                        if (!MediaStorage.isImage(form.getFile().getBytes())) {
                            result.rejectValue("file", "invalid_image", "Upload a valid image. "
                                    + "The file you uploaded was either not an image or a corrupted image.");
                        }
                    } catch (IOException e) {
                        result.rejectValue("file", "invalid", "The submitted file is empty.");
                    }
                }
            }
        }
    }
}
