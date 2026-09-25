package com.example.educa.students;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import com.example.educa.EducaTest;
import com.example.educa.account.CurrentUser;
import com.example.educa.account.User;
import com.example.educa.account.UserRepository;
import com.example.educa.courses.Content;
import com.example.educa.courses.ContentRepository;
import com.example.educa.courses.Course;
import com.example.educa.courses.CourseRepository;
import com.example.educa.courses.FileItem;
import com.example.educa.courses.ImageItem;
import com.example.educa.courses.Module;
import com.example.educa.courses.ModuleRepository;
import com.example.educa.courses.Subject;
import com.example.educa.courses.SubjectRepository;
import com.example.educa.courses.TextItem;
import com.example.educa.courses.VideoItem;

/** 学生端：注册、选课、学习页面。缓存在测试事务里不会真正写入（transactionAware），这里不涉及缓存行为。 */
@EducaTest
@Transactional
class StudentFlowTests {

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
    Course course;
    RequestPostProcessor asStudent;

    @BeforeEach
    void setUp() {
        teacher = new User("teacher", "{noop}pw", "t@example.com");
        teacher.setFirstName("Ada");
        teacher.setLastName("Lovelace");
        teacher = users.save(teacher);
        student = users.save(new User("student", "{noop}pw", "s@example.com"));
        music = subjects.save(new Subject("Music", "music-test"));
        course = courses.save(new Course(teacher, music, "Jazz", "jazz", "Line one\n\nLine <two>"));
        asStudent = user(CurrentUser.from(student));
    }

    private void flushAndClear() {
        em.flush();
        em.clear();
    }

    @Test
    void publicCourseListAndSubjectFilter() throws Exception {
        Subject maths = subjects.save(new Subject("Maths", "maths-test"));
        courses.save(new Course(teacher, maths, "Algebra", "algebra", "x"));
        modules.save(new Module(course, "M1", ""));
        modules.save(new Module(course, "M2", ""));
        flushAndClear();

        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("All courses")))
                .andExpect(content().string(containsString("Jazz")))
                .andExpect(content().string(containsString("Algebra")))
                .andExpect(content().string(containsString("2 modules.")))
                .andExpect(content().string(containsString("Instructor: Ada Lovelace")))
                .andExpect(content().string(containsString("1 course<")));

        mvc.perform(get("/course/subject/maths-test/"))
                .andExpect(content().string(containsString("Maths courses")))
                .andExpect(content().string(containsString("Algebra")))
                .andExpect(content().string(not(containsString(">Jazz<"))));
        mvc.perform(get("/course/subject/nope/")).andExpect(status().isNotFound());
    }

    @Test
    void courseDetailOffersEnrollOrRegistration() throws Exception {
        mvc.perform(get("/course/jazz/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<p>Line one</p>")))
                .andExpect(content().string(containsString("Line &lt;two&gt;")))
                .andExpect(content().string(containsString("Register to enroll")));
        mvc.perform(get("/course/jazz/").with(asStudent))
                .andExpect(content().string(containsString("Enroll now")));
        mvc.perform(get("/course/unknown/")).andExpect(status().isNotFound());
    }

    @Test
    void registrationLogsTheUserIn() throws Exception {
        MvcResult result = mvc.perform(post("/students/register/").with(csrf())
                        .param("username", "newbie").param("password1", "s3cret-pass").param("password2", "s3cret-pass"))
                .andExpect(redirectedUrl("/students/courses/"))
                .andReturn();
        assertThat(users.findByUsername("newbie")).isPresent();
        assertThat(users.findByUsername("newbie").orElseThrow().getPassword()).startsWith("{bcrypt}");

        // 登录状态保存在会话里：带着同一个会话访问“我的课程”不会被重定向到登录页
        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        mvc.perform(get("/students/courses/").session(session))
                .andExpect(status().isOk())
                .andExpect(authenticated().withUsername("newbie"))
                .andExpect(content().string(containsString("You are not enrolled in any courses yet.")));
    }

    @Test
    void registrationValidation() throws Exception {
        mvc.perform(post("/students/register/").with(csrf())
                        .param("username", "student").param("password1", "12345678").param("password2", "123456789"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("form", "username", "password1", "password2"))
                .andExpect(content().string(containsString("A user with that username already exists.")))
                .andExpect(content().string(containsString("This password is entirely numeric.")))
                .andExpect(content().string(containsString("The two password fields didn’t match.")));
        mvc.perform(post("/students/register/").with(csrf())
                        .param("username", "bad name!").param("password1", "short").param("password2", "short"))
                .andExpect(model().attributeHasFieldErrors("form", "username", "password1"));
    }

    @Test
    void enrollAndStudy() throws Exception {
        Module m1 = modules.save(new Module(course, "Intro", ""));
        Module m2 = modules.save(new Module(course, "Swing", ""));
        contents.save(new Content(m1, new TextItem(teacher, "Welcome", "Hello <b>jazz</b>\nfans")));
        contents.save(new Content(m1, new VideoItem(teacher, "Talk", "https://www.youtube.com/watch?v=bgV39DlmZ2U")));
        contents.save(new Content(m2, new ImageItem(teacher, "Poster", "images/poster.png")));
        contents.save(new Content(m2, new FileItem(teacher, "Sheet", "files/sheet.pdf")));
        flushAndClear();

        // 还没选课：学习页面 404
        mvc.perform(get("/students/course/{id}/", course.getId()).with(asStudent)).andExpect(status().isNotFound());

        mvc.perform(post("/students/enroll-course/").with(asStudent).with(csrf()).param("course", course.getId().toString()))
                .andExpect(redirectedUrl("/students/course/" + course.getId() + "/"));
        // 再选一次：幂等
        mvc.perform(post("/students/enroll-course/").with(asStudent).with(csrf()).param("course", course.getId().toString()))
                .andExpect(status().is3xxRedirection());
        flushAndClear();
        assertThat(courses.findByStudentsIdOrderByCreatedDesc(student.getId())).hasSize(1);

        mvc.perform(get("/students/courses/").with(asStudent))
                .andExpect(content().string(containsString("/students/course/" + course.getId() + "/")));
        mvc.perform(get("/course/jazz/").with(asStudent))
                .andExpect(content().string(containsString("Access contents")))
                .andExpect(content().string(not(containsString("Enroll now"))));

        // 默认显示第一个模块
        mvc.perform(get("/students/course/{id}/", course.getId()).with(asStudent))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<h1>Intro</h1>")))
                .andExpect(content().string(containsString("<p>Hello &lt;b&gt;jazz&lt;/b&gt;<br>fans</p>")))
                .andExpect(content().string(containsString("src=\"https://www.youtube.com/embed/bgV39DlmZ2U\"")));

        mvc.perform(get("/students/course/{id}/{m}/", course.getId(), m2.getId()).with(asStudent))
                .andExpect(content().string(containsString("<h1>Swing</h1>")))
                .andExpect(content().string(containsString("src=\"/media/images/poster.png\"")))
                .andExpect(content().string(containsString("href=\"/media/files/sheet.pdf\"")));
    }

    @Test
    void moduleOfAnotherCourseIs404AndEmptyCourseRenders() throws Exception {
        Course other = courses.save(new Course(teacher, music, "Other", "other", "x"));
        Module foreign = modules.save(new Module(other, "Foreign", ""));
        course.getStudents().add(student);
        flushAndClear();

        // 书中：还没有模块时 course.modules.all()[0] 抛 IndexError
        mvc.perform(get("/students/course/{id}/", course.getId()).with(asStudent))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No modules yet.")));
        mvc.perform(get("/students/course/{id}/{m}/", course.getId(), foreign.getId()).with(asStudent))
                .andExpect(status().isNotFound());
    }

    @Test
    void studentPagesRequireLogin() throws Exception {
        mvc.perform(get("/students/courses/")).andExpect(redirectedUrl("/accounts/login/"));
        mvc.perform(post("/students/enroll-course/").with(csrf()).param("course", course.getId().toString()))
                .andExpect(redirectedUrl("/accounts/login/"));
        mvc.perform(get("/course/mine/")).andExpect(redirectedUrl("/accounts/login/"));
        mvc.perform(get("/students/register/")).andExpect(status().isOk());
    }
}
