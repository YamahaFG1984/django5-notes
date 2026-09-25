package com.example.educa.courses.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import com.example.educa.EducaTest;
import com.example.educa.account.CurrentUser;
import com.example.educa.account.GroupRepository;
import com.example.educa.account.User;
import com.example.educa.account.UserRepository;
import com.example.educa.courses.Content;
import com.example.educa.courses.ContentRepository;
import com.example.educa.courses.Course;
import com.example.educa.courses.CourseRepository;
import com.example.educa.courses.Module;
import com.example.educa.courses.ModuleRepository;
import com.example.educa.courses.Subject;
import com.example.educa.courses.SubjectRepository;
import com.example.educa.courses.TextItem;

/**
 * 缓存行为要在<b>真实提交的事务</b>里测试（缓存写入推迟到提交之后），所以这个类不加 @Transactional，
 * 每个测试结束后手动清理数据库和 Redis。
 */
@EducaTest
class CatalogCacheTests {

    @Autowired
    MockMvc mvc;

    @Autowired
    CatalogService catalog;

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
    StringRedisTemplate redis;

    @Autowired
    JdbcTemplate jdbc;

    User teacher;
    Subject music;

    @BeforeEach
    void setUp() {
        redis.getConnectionFactory().getConnection().serverCommands().flushDb();
        User t = new User("cache-teacher", "{noop}pw", "");
        t.getGroups().add(groups.findByName("Instructors").orElseThrow());
        teacher = users.save(t);
        music = subjects.save(new Subject("Music", "music-cache"));
    }

    @AfterEach
    void tearDown() {
        courses.deleteAll(courses.findAll());
        jdbc.update("delete from courses_item");
        jdbc.update("delete from courses_subject where slug = 'music-cache'");
        jdbc.update("delete from auth_user where username = 'cache-teacher'");
    }

    @Test
    void listsAreCachedInRedis() {
        courses.save(new Course(teacher, music, "Jazz", "jazz-cache", "x"));

        assertThat(catalog.courses(null)).extracting(CourseSummary::title).contains("Jazz");
        assertThat(redis.hasKey("educa::courses::all")).isTrue();

        // 绕过应用直接改数据库：缓存没有被清除，读到的还是旧数据 —— 这正是书中代码的状态
        jdbc.update("update courses_course set title = 'Changed' where slug = 'jazz-cache'");
        assertThat(catalog.courses(null)).extracting(CourseSummary::title).contains("Jazz");
    }

    @Test
    void editingThroughTheCmsEvictsTheCatalog() throws Exception {
        mvc.perform(get("/")).andExpect(content().string(containsString("0 courses")));
        assertThat(redis.hasKey("educa::subjects::all")).isTrue();

        var asTeacher = user(CurrentUser.from(users.findWithGroupsByUsername("cache-teacher").orElseThrow()));
        mvc.perform(post("/course/create/").with(asTeacher).with(csrf())
                        .param("subjectId", music.getId().toString()).param("title", "Blues")
                        .param("slug", "blues-cache").param("overview", "o"))
                .andExpect(status().is3xxRedirection());

        // @EvictCatalog 在事务提交后清掉了两个缓存，首页马上看到新课程
        assertThat(redis.hasKey("educa::subjects::all")).isFalse();
        mvc.perform(get("/"))
                .andExpect(content().string(containsString("Blues")))
                .andExpect(content().string(containsString("1 course<")));
    }

    @Test
    void moduleContentsAreEvictedWhenAnInstructorEditsThem() throws Exception {
        Course course = courses.save(new Course(teacher, music, "Jazz", "jazz-cache", "x"));
        Module module = modules.save(new Module(course, "Intro", ""));
        Content content = contents.save(new Content(module, new TextItem(teacher, "Welcome", "old text")));
        String key = "educa::moduleContents::" + module.getId();

        assertThat(catalog.moduleContents(module.getId())).extracting(ContentView::text).containsExactly("old text");
        assertThat(redis.hasKey(key)).isTrue();

        var asTeacher = user(CurrentUser.from(users.findWithGroupsByUsername("cache-teacher").orElseThrow()));
        mvc.perform(post("/course/module/{m}/content/text/{id}/", module.getId(), content.getItem().getId())
                        .with(asTeacher).with(csrf()).param("title", "Welcome").param("content", "new text"))
                .andExpect(status().is3xxRedirection());
        assertThat(redis.hasKey(key)).isFalse();
        assertThat(catalog.moduleContents(module.getId())).extracting(ContentView::text).containsExactly("new text");

        mvc.perform(post("/course/content/{id}/delete/", content.getId()).with(asTeacher).with(csrf()))
                .andExpect(status().is3xxRedirection());
        assertThat(redis.hasKey(key)).isFalse();
        assertThat(catalog.moduleContents(module.getId())).isEmpty();
    }
}
