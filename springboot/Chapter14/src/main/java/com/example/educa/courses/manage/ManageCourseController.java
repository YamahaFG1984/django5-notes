package com.example.educa.courses.manage;

import jakarta.validation.Valid;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.educa.account.CurrentUser;
import com.example.educa.account.Permissions;
import com.example.educa.courses.Course;
import com.example.educa.courses.SubjectRepository;

/**
 * 课程的增删改查，对应书中的 ManageCourseListView / CourseCreateView / CourseUpdateView / CourseDeleteView。
 * Django 用 mixin 组合出这些类视图；这里是普通控制器方法，
 * “只看自己的课程”交给 service 的 ownerId 查询，“需要什么权限”写在 @PreAuthorize 上。
 */
@Controller
@RequestMapping("/course")
public class ManageCourseController {

    private final CourseManageService service;
    private final SubjectRepository subjects;

    public ManageCourseController(CourseManageService service, SubjectRepository subjects) {
        this.service = service;
        this.subjects = subjects;
    }

    @GetMapping("/mine/")
    @PreAuthorize("hasAuthority('" + Permissions.VIEW_COURSE + "')")
    public String list(@AuthenticationPrincipal CurrentUser user, Model model) {
        model.addAttribute("courses", service.myCourses(user.id()));
        return "courses/manage/course/list";
    }

    @GetMapping("/create/")
    @PreAuthorize("hasAuthority('" + Permissions.ADD_COURSE + "')")
    public String createForm(Model model) {
        return form(model, new CourseForm(), null);
    }

    @PostMapping("/create/")
    @PreAuthorize("hasAuthority('" + Permissions.ADD_COURSE + "')")
    public String create(@AuthenticationPrincipal CurrentUser user,
                         @Valid @ModelAttribute("form") CourseForm form, BindingResult result, Model model) {
        checkSlug(form, result, null);
        if (result.hasErrors()) {
            return form(model, form, null);
        }
        service.createCourse(user.id(), form);
        return "redirect:/course/mine/";   // success_url = reverse_lazy('manage_course_list')
    }

    @GetMapping("/{id}/edit/")
    @PreAuthorize("hasAuthority('" + Permissions.CHANGE_COURSE + "')")
    public String editForm(@PathVariable Long id, @AuthenticationPrincipal CurrentUser user, Model model) {
        Course course = service.getCourse(id, user.id());
        return form(model, CourseForm.of(course), course);
    }

    @PostMapping("/{id}/edit/")
    @PreAuthorize("hasAuthority('" + Permissions.CHANGE_COURSE + "')")
    public String edit(@PathVariable Long id, @AuthenticationPrincipal CurrentUser user,
                       @Valid @ModelAttribute("form") CourseForm form, BindingResult result, Model model) {
        Course course = service.getCourse(id, user.id());
        checkSlug(form, result, id);
        if (result.hasErrors()) {
            return form(model, form, course);
        }
        service.updateCourse(id, user.id(), form);
        return "redirect:/course/mine/";
    }

    @GetMapping("/{id}/delete/")
    @PreAuthorize("hasAuthority('" + Permissions.DELETE_COURSE + "')")
    public String confirmDelete(@PathVariable Long id, @AuthenticationPrincipal CurrentUser user, Model model) {
        model.addAttribute("object", service.getCourse(id, user.id()));
        return "courses/manage/course/delete";
    }

    @PostMapping("/{id}/delete/")
    @PreAuthorize("hasAuthority('" + Permissions.DELETE_COURSE + "')")
    public String delete(@PathVariable Long id, @AuthenticationPrincipal CurrentUser user) {
        service.deleteCourse(id, user.id());
        return "redirect:/course/mine/";
    }

    private String form(Model model, CourseForm form, Course course) {
        model.addAttribute("form", form);
        model.addAttribute("object", course);
        model.addAttribute("subjects", subjects.findAllByOrderByTitleAsc());
        return "courses/manage/course/form";
    }

    /** slug 的 unique=True：ModelForm 会自动做唯一性校验，这里手动查一次 */
    private void checkSlug(CourseForm form, BindingResult result, Long courseId) {
        if (!result.hasFieldErrors("slug") && service.slugTaken(form.getSlug(), courseId)) {
            result.rejectValue("slug", "unique", "Course with this Slug already exists.");
        }
    }
}
