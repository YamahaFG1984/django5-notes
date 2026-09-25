package com.example.mysite.blog;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** 访问站点根路径时跳到博客首页（原书的项目根路径没有配置视图，会返回 404）。 */
@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "redirect:/blog/";
    }
}
