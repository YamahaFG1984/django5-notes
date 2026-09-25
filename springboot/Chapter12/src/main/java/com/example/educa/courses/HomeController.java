package com.example.educa.courses;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** 本章还没有前台页面（第 14 章才有课程列表）；首页暂时只列出学科，方便确认 fixtures 已经导入。 */
@Controller
public class HomeController {

    private final SubjectRepository subjects;

    public HomeController(SubjectRepository subjects) {
        this.subjects = subjects;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("subjects", subjects.findAllByOrderByTitleAsc());
        return "home";
    }
}
