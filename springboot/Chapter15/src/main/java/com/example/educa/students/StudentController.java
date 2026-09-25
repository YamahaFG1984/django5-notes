package com.example.educa.students;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.educa.account.CurrentUser;
import com.example.educa.account.User;
import com.example.educa.courses.catalog.CatalogService;

/** students/urls.py 里的五个视图。除注册外都要求登录（在 SecurityConfig 里按 URL 配置）。 */
@Controller
@RequestMapping("/students")
public class StudentController {

    private final StudentService service;
    private final CatalogService catalog;
    private final SecurityContextRepository securityContextRepository;

    public StudentController(StudentService service, CatalogService catalog,
                             SecurityContextRepository securityContextRepository) {
        this.service = service;
        this.catalog = catalog;
        this.securityContextRepository = securityContextRepository;
    }

    // ---------------------------------------------------------------- 注册

    @GetMapping("/register/")
    public String registrationForm(Model model) {
        model.addAttribute("form", new RegistrationForm());
        return "students/student/registration";
    }

    @PostMapping("/register/")
    public String register(@Valid @ModelAttribute("form") RegistrationForm form, BindingResult result,
                           HttpServletRequest request, HttpServletResponse response) {
        if (!result.hasFieldErrors("username") && service.usernameTaken(form.getUsername())) {
            result.rejectValue("username", "unique", "A user with that username already exists.");
        }
        if (!result.hasFieldErrors("password2") && !form.passwordsMatch()) {
            result.rejectValue("password2", "password_mismatch", "The two password fields didn’t match.");
        }
        if (result.hasErrors()) {
            return "students/student/registration";
        }
        User user = service.register(form);
        login(CurrentUser.from(user), request, response);
        return "redirect:/students/courses/";   // success_url = reverse_lazy('student_course_list')
    }

    /**
     * ≈ django.contrib.auth.login(request, user)。
     * 自己写登录逻辑时有三件事要做：把认证结果放进 SecurityContext、把它保存到会话里（否则下一个请求就丢了）、
     * 更换会话 id 防止会话固定攻击（Django 的 login() 内部会 cycle_key()）。
     */
    private void login(CurrentUser principal, HttpServletRequest request, HttpServletResponse response) {
        var authentication = UsernamePasswordAuthenticationToken.authenticated(
                principal, null, principal.getAuthorities());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        if (request.getSession(false) != null) {
            request.changeSessionId();
        }
        securityContextRepository.saveContext(context, request, response);
    }

    // ---------------------------------------------------------------- 选课

    /** StudentEnrollCourseView：表单只有一个隐藏字段 course */
    @PostMapping("/enroll-course/")
    public String enroll(@RequestParam("course") Long courseId, @AuthenticationPrincipal CurrentUser user) {
        service.enroll(courseId, user.id());
        return "redirect:/students/course/" + courseId + "/";
    }

    @GetMapping("/courses/")
    public String myCourses(@AuthenticationPrincipal CurrentUser user, Model model) {
        model.addAttribute("courses", service.myCourses(user.id()));
        return "students/course/list";
    }

    // ---------------------------------------------------------------- 学习页面

    @GetMapping({"/course/{id}/", "/course/{id}/{moduleId}/"})
    public String course(@PathVariable Long id, @PathVariable(required = false) Long moduleId,
                         @AuthenticationPrincipal CurrentUser user, Model model) {
        StudentService.CoursePage page = service.coursePage(id, moduleId, user.id());
        model.addAttribute("object", page.course());
        model.addAttribute("modules", page.modules());
        model.addAttribute("module", page.module());
        // 模块内容来自缓存（≈ {% cache 600 module_contents module %}），讲师修改内容时会被清除
        model.addAttribute("contents", page.module() == null ? List.of()
                : catalog.moduleContents(page.module().getId()));
        return "students/course/detail";
    }
}
