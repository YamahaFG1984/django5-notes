package com.example.educa.courses.api;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.example.educa.EducaTest;
import com.example.educa.account.User;
import com.example.educa.account.UserRepository;
import com.example.educa.courses.Content;
import com.example.educa.courses.ContentRepository;
import com.example.educa.courses.Course;
import com.example.educa.courses.CourseRepository;
import com.example.educa.courses.ImageItem;
import com.example.educa.courses.Module;
import com.example.educa.courses.ModuleRepository;
import com.example.educa.courses.Subject;
import com.example.educa.courses.SubjectRepository;
import com.example.educa.courses.TextItem;

@EducaTest
@Transactional
class CourseApiTests {

    @Autowired
    MockMvc mvc;

    @Autowired
    UserRepository users;

    @Autowired
    SubjectRepository subjects;

    @Autowired
    CourseRepository courses;

    @Autowired
    ModuleRepository modules;

    @Autowired
    ContentRepository contents;

    @Autowired
    EntityManager em;

    User teacher;
    User student;
    Subject music;
    Course jazz;

    @BeforeEach
    void setUp() {
        teacher = users.save(new User("teacher", "{noop}pw", ""));
        student = users.save(new User("student", "{noop}secret", ""));
        music = subjects.save(new Subject("AAA Music", "music-api"));
        jazz = courses.save(new Course(teacher, music, "Jazz", "jazz", "Overview"));
        modules.save(new Module(jazz, "Intro", "Start"));
    }

    private void flushAndClear() {
        em.flush();
        em.clear();
    }

    @Test
    void apiRootListsResources() throws Exception {
        mvc.perform(get("/api/"))
                .andExpect(jsonPath("$.courses").value("http://localhost/api/courses/"))
                .andExpect(jsonPath("$.subjects").value("http://localhost/api/subjects/"));
    }

    @Test
    void subjectsArePaginatedWithTotalsAndPopularCourses() throws Exception {
        Course blues = courses.save(new Course(teacher, music, "Blues", "blues", "o"));
        courses.save(new Course(teacher, music, "Swing", "swing", "o"));
        courses.save(new Course(teacher, music, "Bebop", "bebop", "o"));
        blues.getStudents().add(student);
        blues.getStudents().add(teacher);
        jazz.getStudents().add(student);
        flushAndClear();

        mvc.perform(get("/api/subjects/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1))
                .andExpect(jsonPath("$.next").value(nullValue()))
                .andExpect(jsonPath("$.results[0].title").value("AAA Music"))
                .andExpect(jsonPath("$.results[0].total_courses").value(4))
                .andExpect(jsonPath("$.results[0].popular_courses", contains(
                        "Blues (2 students)", "Jazz (1 students)", "Bebop (0 students)")));

        mvc.perform(get("/api/subjects/{id}/", music.getId()))
                .andExpect(jsonPath("$.slug").value("music-api"));
        mvc.perform(get("/api/subjects/999999/"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").exists());
    }

    @Test
    void coursesPaginationFollowsDrfFormat() throws Exception {
        for (int i = 0; i < 4; i++) {
            courses.save(new Course(teacher, music, "Course " + i, "course-" + i, "o"));
        }
        flushAndClear();

        mvc.perform(get("/api/courses/?page_size=2"))
                .andExpect(jsonPath("$.count").value(5))
                .andExpect(jsonPath("$.results", hasSize(2)))
                .andExpect(jsonPath("$.previous").value(nullValue()))
                .andExpect(jsonPath("$.next").value("http://localhost/api/courses/?page_size=2&page=2"));
        mvc.perform(get("/api/courses/?page_size=2&page=2"))
                .andExpect(jsonPath("$.previous").value("http://localhost/api/courses/?page_size=2"))
                .andExpect(jsonPath("$.next").value("http://localhost/api/courses/?page_size=2&page=3"));
        mvc.perform(get("/api/courses/?page=9"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Invalid page."));
        mvc.perform(get("/api/courses/?page_size=500"))
                .andExpect(jsonPath("$.results", hasSize(5)));
    }

    @Test
    void courseDetailIncludesModules() throws Exception {
        flushAndClear();
        mvc.perform(get("/api/courses/{id}/", jazz.getId()))
                .andExpect(jsonPath("$.subject").value(music.getId()))
                .andExpect(jsonPath("$.owner").value(teacher.getId()))
                .andExpect(jsonPath("$.created").isString())
                .andExpect(jsonPath("$.modules[0].order").value(0))
                .andExpect(jsonPath("$.modules[0].title").value("Intro"));
        mvc.perform(get("/api/courses/abc/")).andExpect(status().isNotFound());
    }

    @Test
    void enrollRequiresBasicAuthAndNoCsrf() throws Exception {
        mvc.perform(post("/api/courses/{id}/enroll/", jazz.getId()))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Basic realm=\"api\""))
                .andExpect(jsonPath("$.detail").value("Authentication credentials were not provided."));
        mvc.perform(post("/api/courses/{id}/enroll/", jazz.getId()).with(httpBasic("student", "wrong")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid username/password."));

        // 不需要 CSRF 令牌；无状态：不会创建会话
        mvc.perform(post("/api/courses/{id}/enroll/", jazz.getId()).with(httpBasic("student", "secret")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enrolled").value(true))
                .andExpect(request().sessionAttributeDoesNotExist("SPRING_SECURITY_CONTEXT"));
        flushAndClear();
        org.assertj.core.api.Assertions.assertThat(courses.existsByIdAndStudentsId(jazz.getId(), student.getId())).isTrue();
    }

    @Test
    void contentsRequireEnrollment() throws Exception {
        Module intro = modules.findByCourseIdOrderByOrderAsc(jazz.getId()).getFirst();
        contents.save(new Content(intro, new TextItem(teacher, "Welcome", "Hello")));
        contents.save(new Content(intro, new ImageItem(teacher, "Poster", "images/p.png")));
        flushAndClear();

        mvc.perform(get("/api/courses/{id}/contents/", jazz.getId())).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/courses/{id}/contents/", jazz.getId()).with(httpBasic("student", "secret")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("You do not have permission to perform this action."));

        mvc.perform(post("/api/courses/{id}/enroll/", jazz.getId()).with(httpBasic("student", "secret")));
        mvc.perform(get("/api/courses/{id}/contents/", jazz.getId()).with(httpBasic("student", "secret")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.modules[0].contents[0].item.type").value("text"))
                .andExpect(jsonPath("$.modules[0].contents[0].item.content").value("Hello"))
                .andExpect(jsonPath("$.modules[0].contents[1].item.file", endsWith("/media/images/p.png")));
    }

    @Test
    void readOnlyEndpointsRejectWrites() throws Exception {
        mvc.perform(post("/api/courses/").with(httpBasic("student", "secret")))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void openApiDocumentIsPublished() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/courses/{id}/enroll/'].post").exists());
    }
}
