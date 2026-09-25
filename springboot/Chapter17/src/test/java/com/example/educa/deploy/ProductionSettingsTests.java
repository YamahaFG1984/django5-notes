package com.example.educa.deploy;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import com.example.educa.EducaTest;
import com.example.educa.account.User;
import com.example.educa.account.UserRepository;
import com.example.educa.courses.Course;
import com.example.educa.courses.CourseRepository;
import com.example.educa.courses.Subject;
import com.example.educa.courses.SubjectRepository;

/**
 * 打开生产环境的几个安全开关后，检查它们的效果（≈ python manage.py check --deploy 关心的那些设置）。
 * 数据库仍然是 H2，只是把 educa.* 设置成 application-prod.yaml 里的值。
 */
@EducaTest
@Transactional
@TestPropertySource(properties = {
        "educa.require-https=true",
        "educa.site-domain=educaproject.com",
        "educa.allowed-hosts=.educaproject.com,localhost"
})
class ProductionSettingsTests {

    @Autowired
    MockMvc mvc;

    @Autowired
    UserRepository users;

    @Autowired
    SubjectRepository subjects;

    @Autowired
    CourseRepository courses;

    private static RequestPostProcessor host(String host) {
        return request -> {
            request.setServerName(host);
            return request;
        };
    }

    /** 模拟经过 Nginx 转发的 HTTPS 请求（应用看到的协议是 https） */
    private static RequestPostProcessor https(String host) {
        return request -> {
            request.setServerName(host);
            request.setScheme("https");
            request.setServerPort(443);
            request.setSecure(true);
            return request;
        };
    }

    @Test
    void httpIsRedirectedToHttpsAndHstsIsSent() throws Exception {
        mvc.perform(get("/").with(host("educaproject.com")))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("https://educaproject.com/"));
        mvc.perform(get("/").with(https("educaproject.com")))
                .andExpect(status().isOk())
                .andExpect(header().string("Strict-Transport-Security", startsWith("max-age=")));
        // 容器内部的健康检查不重定向
        mvc.perform(get("/actuator/health/liveness")).andExpect(status().isOk());
    }

    @Test
    void unknownHostsAreRejected() throws Exception {
        mvc.perform(get("/").with(https("evil.example"))).andExpect(status().isBadRequest());
    }

    @Test
    void courseSubdomainsRedirectToTheCoursePage() throws Exception {
        User owner = users.save(new User("owner", "{noop}pw", ""));
        Subject s = subjects.save(new Subject("Programming", "prog-deploy"));
        courses.save(new Course(owner, s, "Django", "django", "o"));

        mvc.perform(get("/anything/").with(https("django.educaproject.com")))
                .andExpect(redirectedUrl("https://educaproject.com/course/django/"));
        mvc.perform(get("/").with(https("nope.educaproject.com")))
                .andExpect(status().isNotFound());
        mvc.perform(get("/").with(https("www.educaproject.com")))
                .andExpect(status().isOk());
    }
}
