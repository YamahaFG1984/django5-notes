package com.example.educa.courses.manage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.util.List;

import javax.imageio.ImageIO;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import com.example.educa.EducaTest;
import com.example.educa.account.CurrentUser;
import com.example.educa.account.GroupRepository;
import com.example.educa.account.User;
import com.example.educa.account.UserRepository;
import com.example.educa.common.MediaStorage;
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

/** 讲师 CMS 的端到端测试（≈ 书中在浏览器里走一遍的流程）。 */
@EducaTest
@Transactional
class CourseManageTests {

    @Autowired
    MockMvc mvc;

    @Autowired
    UserRepository users;

    @Autowired
    GroupRepository groups;

    @Autowired
    SubjectRepository subjects;

    @Autowired
    CourseRepository courses;

    @Autowired
    ModuleRepository modules;

    @Autowired
    ContentRepository contents;

    @Autowired
    MediaStorage media;

    @Autowired
    EntityManager em;

    User teacher;
    User other;
    Subject music;
    RequestPostProcessor asTeacher;
    RequestPostProcessor asOther;

    @BeforeEach
    void setUp() {
        var instructors = groups.findByName("Instructors").orElseThrow();   // V3 迁移创建的组
        teacher = new User("teacher", "{noop}pw", "t@example.com");
        teacher.getGroups().add(instructors);
        teacher = users.save(teacher);
        other = new User("other", "{noop}pw", "o@example.com");
        other.getGroups().add(instructors);
        other = users.save(other);
        music = subjects.save(new Subject("Music", "music-test"));
        asTeacher = user(CurrentUser.from(teacher));
        asOther = user(CurrentUser.from(other));
    }

    private Course course(User owner, String slug) {
        return courses.save(new Course(owner, music, "Course " + slug, slug, "Overview"));
    }

    private void flushAndClear() {
        em.flush();
        em.clear();
    }

    @Test
    void anonymousIsRedirectedToLogin() throws Exception {
        mvc.perform(get("/course/mine/"))
                .andExpect(redirectedUrl("/accounts/login/"));
    }

    @Test
    void userWithoutPermissionGets403() throws Exception {
        User student = users.save(new User("student", "{noop}pw", "s@example.com"));
        mvc.perform(get("/course/mine/").with(user(CurrentUser.from(student))))
                .andExpect(status().isForbidden());
        mvc.perform(post("/course/create/").with(user(CurrentUser.from(student))).with(csrf())
                        .param("subjectId", music.getId().toString()).param("title", "T")
                        .param("slug", "t").param("overview", "O"))
                .andExpect(status().isForbidden());
        assertThat(courses.findBySlug("t")).isEmpty();
    }

    @Test
    void listShowsOnlyOwnCourses() throws Exception {
        course(teacher, "mine");
        course(other, "theirs");
        mvc.perform(get("/course/mine/").with(asTeacher))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Course mine")))
                .andExpect(content().string(not(containsString("Course theirs"))));
    }

    @Test
    void createEditAndDeleteCourse() throws Exception {
        mvc.perform(post("/course/create/").with(asTeacher).with(csrf())
                        .param("subjectId", music.getId().toString()).param("title", "Django")
                        .param("slug", "django").param("overview", "Learn Django"))
                .andExpect(redirectedUrl("/course/mine/"));
        Course created = courses.findBySlug("django").orElseThrow();
        assertThat(created.getOwner().getId()).isEqualTo(teacher.getId());

        mvc.perform(post("/course/{id}/edit/", created.getId()).with(asTeacher).with(csrf())
                        .param("subjectId", music.getId().toString()).param("title", "Django 5")
                        .param("slug", "django").param("overview", "Learn Django 5"))
                .andExpect(redirectedUrl("/course/mine/"));
        flushAndClear();
        assertThat(courses.findBySlug("django").orElseThrow().getTitle()).isEqualTo("Django 5");

        mvc.perform(get("/course/{id}/delete/", created.getId()).with(asTeacher))
                .andExpect(content().string(containsString("Are you sure you want to delete &quot;Django 5&quot;?")));
        mvc.perform(post("/course/{id}/delete/", created.getId()).with(asTeacher).with(csrf()))
                .andExpect(redirectedUrl("/course/mine/"));
        assertThat(courses.findBySlug("django")).isEmpty();
    }

    @Test
    void duplicateSlugIsAFormError() throws Exception {
        course(other, "taken");
        mvc.perform(post("/course/create/").with(asTeacher).with(csrf())
                        .param("subjectId", music.getId().toString()).param("title", "X")
                        .param("slug", "taken").param("overview", "O"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("form", "slug"))
                .andExpect(content().string(containsString("Course with this Slug already exists.")));
    }

    @Test
    void otherInstructorsCoursesAre404() throws Exception {
        Course theirs = course(other, "theirs");
        mvc.perform(get("/course/{id}/edit/", theirs.getId()).with(asTeacher)).andExpect(status().isNotFound());
        mvc.perform(post("/course/{id}/delete/", theirs.getId()).with(asTeacher).with(csrf()))
                .andExpect(status().isNotFound());
        mvc.perform(get("/course/{id}/module/", theirs.getId()).with(asTeacher)).andExpect(status().isNotFound());
        Module module = modules.save(new Module(theirs, "M", ""));
        mvc.perform(get("/course/module/{id}/", module.getId()).with(asTeacher)).andExpect(status().isNotFound());
        mvc.perform(get("/course/module/{id}/content/text/create/", module.getId()).with(asTeacher))
                .andExpect(status().isNotFound());
    }

    @Test
    void moduleFormsetAddsUpdatesAndDeletes() throws Exception {
        Course c = course(teacher, "c");
        Module keep = modules.save(new Module(c, "Intro", ""));
        Module drop = modules.save(new Module(c, "Old", ""));
        flushAndClear();

        // GET：已有的两行 + extra=2 的两行空表单
        mvc.perform(get("/course/{id}/module/", c.getId()).with(asTeacher))
                .andExpect(content().string(containsString("name=\"rows[3].title\"")))
                .andExpect(content().string(not(containsString("name=\"rows[4].title\""))));

        mvc.perform(post("/course/{id}/module/", c.getId()).with(asTeacher).with(csrf())
                        .param("rows[0].id", keep.getId().toString()).param("rows[0].title", "Introduction")
                        .param("rows[0].description", "Start here")
                        .param("rows[1].id", drop.getId().toString()).param("rows[1].title", "Old")
                        .param("rows[1].delete", "true")
                        .param("rows[2].id", "").param("rows[2].title", "Models").param("rows[2].description", "")
                        .param("rows[3].id", "").param("rows[3].title", "").param("rows[3].description", ""))
                .andExpect(redirectedUrl("/course/mine/"));
        flushAndClear();

        List<Module> result = modules.findByCourseIdOrderByOrderAsc(c.getId());
        assertThat(result).extracting(Module::getTitle).containsExactly("Introduction", "Models");
        assertThat(result.getFirst().getDescription()).isEqualTo("Start here");
    }

    @Test
    void moduleFormsetRequiresTitleForNonEmptyRows() throws Exception {
        Course c = course(teacher, "c");
        mvc.perform(post("/course/{id}/module/", c.getId()).with(asTeacher).with(csrf())
                        .param("rows[0].title", "").param("rows[0].description", "Only a description"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("formset", "rows[0].title"))
                .andExpect(content().string(containsString("This field is required.")));
        assertThat(modules.findByCourseIdOrderByOrderAsc(c.getId())).isEmpty();
    }

    @Test
    void createAndEditTextContent() throws Exception {
        Course c = course(teacher, "c");
        Module m = modules.save(new Module(c, "M", ""));

        mvc.perform(post("/course/module/{id}/content/text/create/", m.getId()).with(asTeacher).with(csrf())
                        .param("title", "Hello").param("content", ""))
                .andExpect(model().attributeHasFieldErrors("form", "content"));

        mvc.perform(post("/course/module/{id}/content/text/create/", m.getId()).with(asTeacher).with(csrf())
                        .param("title", "Hello").param("content", "World"))
                .andExpect(redirectedUrl("/course/module/" + m.getId() + "/"));
        flushAndClear();

        Content content = contents.findByModuleIdOrderByOrderAsc(m.getId()).getFirst();
        TextItem item = (TextItem) content.getItem();
        assertThat(item.getContent()).isEqualTo("World");
        assertThat(item.getOwner().getId()).isEqualTo(teacher.getId());

        mvc.perform(get("/course/module/{id}/", m.getId()).with(asTeacher))
                .andExpect(content().string(containsString("Hello (text)")))
                .andExpect(content().string(containsString(
                        "/course/module/" + m.getId() + "/content/text/" + item.getId() + "/")));

        // 编辑时类型必须与 URL 一致
        mvc.perform(get("/course/module/{m}/content/video/{id}/", m.getId(), item.getId()).with(asTeacher))
                .andExpect(status().isNotFound());
        mvc.perform(post("/course/module/{m}/content/text/{id}/", m.getId(), item.getId()).with(asTeacher).with(csrf())
                        .param("title", "Hello!").param("content", "World!"))
                .andExpect(redirectedUrl("/course/module/" + m.getId() + "/"));
        flushAndClear();
        TextItem edited = (TextItem) contents.findByModuleIdOrderByOrderAsc(m.getId()).getFirst().getItem();
        assertThat(edited.getTitle()).isEqualTo("Hello!");
        assertThat(edited.getContent()).isEqualTo("World!");
    }

    @Test
    void unknownModelNameIs404() throws Exception {
        Module m = modules.save(new Module(course(teacher, "c"), "M", ""));
        mvc.perform(get("/course/module/{id}/content/user/create/", m.getId()).with(asTeacher))
                .andExpect(status().isNotFound());
    }

    @Test
    void videoUrlIsValidated() throws Exception {
        Module m = modules.save(new Module(course(teacher, "c"), "M", ""));
        mvc.perform(post("/course/module/{id}/content/video/create/", m.getId()).with(asTeacher).with(csrf())
                        .param("title", "Talk").param("url", "javascript:alert(1)"))
                .andExpect(model().attributeHasFieldErrors("form", "url"))
                .andExpect(content().string(containsString("Enter a valid URL.")));
    }

    @Test
    void uploadImageValidatesContent() throws Exception {
        Module m = modules.save(new Module(course(teacher, "c"), "M", ""));
        MockMultipartFile fake = new MockMultipartFile("file", "x.png", "image/png", "not an image".getBytes());
        mvc.perform(multipart("/course/module/{id}/content/image/create/", m.getId()).file(fake)
                        .param("title", "Pic").with(asTeacher).with(csrf()))
                .andExpect(model().attributeHasFieldErrors("form", "file"));

        MockMultipartFile real = new MockMultipartFile("file", "pic.png", "image/png", png());
        mvc.perform(multipart("/course/module/{id}/content/image/create/", m.getId()).file(real)
                        .param("title", "Pic").with(asTeacher).with(csrf()))
                .andExpect(redirectedUrl("/course/module/" + m.getId() + "/"));
        flushAndClear();
        ImageItem image = (ImageItem) contents.findByModuleIdOrderByOrderAsc(m.getId()).getFirst().getItem();
        assertThat(image.getFile()).startsWith("images/").endsWith(".png");
        assertThat(Files.exists(media.resolve(image.getFile()))).isTrue();

        // 编辑时不上传新文件：保留原来的
        mvc.perform(multipart("/course/module/{m}/content/image/{id}/", m.getId(), image.getId())
                        .param("title", "Picture").with(asTeacher).with(csrf()))
                .andExpect(redirectedUrl("/course/module/" + m.getId() + "/"));
        flushAndClear();
        ImageItem edited = (ImageItem) contents.findByModuleIdOrderByOrderAsc(m.getId()).getFirst().getItem();
        assertThat(edited.getTitle()).isEqualTo("Picture");
        assertThat(edited.getFile()).isEqualTo(image.getFile());
    }

    @Test
    void uploadFileRequiresFileOnCreate() throws Exception {
        Module m = modules.save(new Module(course(teacher, "c"), "M", ""));
        mvc.perform(multipart("/course/module/{id}/content/file/create/", m.getId())
                        .param("title", "Slides").with(asTeacher).with(csrf()))
                .andExpect(model().attributeHasFieldErrors("form", "file"));
        MockMultipartFile pdf = new MockMultipartFile("file", "../../slides.pdf", "application/pdf", "%PDF-1.4".getBytes());
        mvc.perform(multipart("/course/module/{id}/content/file/create/", m.getId()).file(pdf)
                        .param("title", "Slides").with(asTeacher).with(csrf()))
                .andExpect(redirectedUrl("/course/module/" + m.getId() + "/"));
        flushAndClear();
        var item = (com.example.educa.courses.FileItem) contents.findByModuleIdOrderByOrderAsc(m.getId()).getFirst().getItem();
        assertThat(item.getFile()).startsWith("files/slides_").endsWith(".pdf");
    }

    @Test
    void deleteContentRemovesItemToo() throws Exception {
        Module m = modules.save(new Module(course(teacher, "c"), "M", ""));
        Content content = contents.save(new Content(m, new TextItem(teacher, "T", "body")));
        Long itemId = content.getItem().getId();
        flushAndClear();

        mvc.perform(post("/course/content/{id}/delete/", content.getId()).with(asOther).with(csrf()))
                .andExpect(status().isNotFound());
        mvc.perform(post("/course/content/{id}/delete/", content.getId()).with(asTeacher).with(csrf()))
                .andExpect(redirectedUrl("/course/module/" + m.getId() + "/"));
        flushAndClear();
        assertThat(contents.findById(content.getId())).isEmpty();
        assertThat(em.find(TextItem.class, itemId)).isNull();
    }

    @Test
    void reorderModulesViaJsonOnlyAffectsOwnCourses() throws Exception {
        Course c = course(teacher, "c");
        Module a = modules.save(new Module(c, "A", ""));
        Module b = modules.save(new Module(c, "B", ""));
        Module foreign = modules.save(new Module(course(other, "o"), "F", ""));
        flushAndClear();

        String json = "{\"%d\": 1, \"%d\": 0, \"%d\": 9}".formatted(a.getId(), b.getId(), foreign.getId());
        // 没带 CSRF 令牌：拒绝（书中用 CsrfExemptMixin 关掉了这层保护）
        mvc.perform(post("/course/module/order/").with(asTeacher)
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isForbidden());
        mvc.perform(post("/course/module/order/").with(asTeacher).with(csrf().asHeader())
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saved").value("OK"));
        flushAndClear();

        assertThat(modules.findByCourseIdOrderByOrderAsc(c.getId())).extracting(Module::getTitle)
                .containsExactly("B", "A");
        assertThat(modules.findById(foreign.getId()).orElseThrow().getOrder()).isZero();
    }

    @Test
    void reorderContents() throws Exception {
        Module m = modules.save(new Module(course(teacher, "c"), "M", ""));
        Content first = contents.save(new Content(m, new TextItem(teacher, "first", "1")));
        Content second = contents.save(new Content(m, new TextItem(teacher, "second", "2")));
        flushAndClear();
        mvc.perform(post("/course/content/order/").with(asTeacher).with(csrf().asHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"%d\": 1, \"%d\": 0}".formatted(first.getId(), second.getId())))
                .andExpect(status().isOk());
        flushAndClear();
        assertThat(contents.findByModuleIdOrderByOrderAsc(m.getId()))
                .extracting(c -> c.getItem().getTitle()).containsExactly("second", "first");
    }

    static byte[] png() {
        try {
            BufferedImage image = new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "png", out);
            return out.toByteArray();
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
