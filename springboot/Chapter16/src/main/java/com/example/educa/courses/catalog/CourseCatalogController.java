package com.example.educa.courses.catalog;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.example.educa.account.CurrentUser;
import com.example.educa.common.NotFoundException;
import com.example.educa.courses.Course;
import com.example.educa.courses.CourseRepository;
import com.example.educa.courses.Subject;
import com.example.educa.courses.SubjectRepository;

/** 公开的课程目录：CourseListView 与 CourseDetailView。 */
@Controller
public class CourseCatalogController {

    private final CatalogService catalog;
    private final SubjectRepository subjects;
    private final CourseRepository courses;

    public CourseCatalogController(CatalogService catalog, SubjectRepository subjects, CourseRepository courses) {
        this.catalog = catalog;
        this.subjects = subjects;
        this.courses = courses;
    }

    /** path('', CourseListView.as_view(), name='course_list') */
    @GetMapping("/")
    public String list(Model model) {
        return render(model, null);
    }

    /** path('subject/<slug:subject>/', ..., name='course_list_subject') */
    @GetMapping("/course/subject/{slug}/")
    public String bySubject(@PathVariable String slug, Model model) {
        Subject subject = subjects.findBySlug(slug).orElseThrow(NotFoundException::new);
        return render(model, subject);
    }

    /**
     * path('<slug:slug>/', CourseDetailView, name='course_detail')。
     * 和 /course/mine/、/course/create/ 共享前缀：Spring MVC 会优先匹配字面量更多的模式，所以不会冲突
     * （但 slug 恰好叫 mine 或 create 的课程就访问不到了 —— Django 版按顺序匹配，情况相同）。
     */
    @GetMapping("/course/{slug}/")
    public String detail(@PathVariable String slug, @AuthenticationPrincipal CurrentUser user, Model model) {
        Course course = courses.findWithDetailsBySlug(slug).orElseThrow(NotFoundException::new);
        model.addAttribute("object", course);
        // 书中总是显示“Enroll now”；这里已经选过课的学生直接给出“进入课程”的链接
        model.addAttribute("enrolled", user != null && courses.existsByIdAndStudentsId(course.getId(), user.id()));
        return "courses/course/detail";
    }

    private String render(Model model, Subject subject) {
        model.addAttribute("subjects", catalog.subjects());
        model.addAttribute("subject", subject);
        model.addAttribute("courses", catalog.courses(subject == null ? null : subject.getSlug()));
        return "courses/course/list";
    }
}
