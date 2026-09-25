package com.example.mysite.blog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.example.mysite.account.CurrentUser;
import com.example.mysite.account.User;
import com.example.mysite.account.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PostAdminControllerTests {

    @Autowired
    MockMvc mvc;

    @Autowired
    UserRepository users;

    @Autowired
    PostRepository posts;

    User staff;
    User regular;

    @BeforeEach
    void setUp() {
        staff = new User("admin", "{noop}secret", "admin@example.com");
        staff.setStaff(true);
        users.save(staff);
        regular = users.save(new User("bob", "{noop}secret", "bob@example.com"));
    }

    @Test
    void adminIsOnlyForStaff() throws Exception {
        mvc.perform(get("/admin/blog/post/"))
                .andExpect(status().is3xxRedirection());
        mvc.perform(get("/admin/blog/post/").with(user(CurrentUser.from(regular))))
                .andExpect(status().isForbidden());
        mvc.perform(get("/admin/blog/post/").with(user(CurrentUser.from(staff))))
                .andExpect(status().isOk());
    }

    @Test
    void staffCanAddSearchAndFilterPosts() throws Exception {
        var me = user(CurrentUser.from(staff));
        mvc.perform(post("/admin/blog/post/add/").with(me).with(csrf())
                        .param("title", "Who was Django Reinhardt?")
                        .param("slug", "who-was-django-reinhardt")
                        .param("authorId", staff.getId().toString())
                        .param("body", "A jazz guitarist.")
                        .param("publish", "2025-01-01T10:00")
                        .param("status", "PUBLISHED"))
                .andExpect(redirectedUrl("/admin/blog/post/"));
        assertThat(posts.countByStatus(PostStatus.PUBLISHED)).isEqualTo(1);

        mvc.perform(get("/admin/blog/post/").param("q", "jazz").with(me))
                .andExpect(content().string(containsString("Who was Django Reinhardt?")))
                .andExpect(content().string(containsString("Published (1)")));
        mvc.perform(get("/admin/blog/post/").param("status", "DRAFT").with(me))
                .andExpect(content().string(containsString("0 posts")));
    }

    @Test
    void invalidFormIsRedisplayedWithErrors() throws Exception {
        mvc.perform(post("/admin/blog/post/add/").with(user(CurrentUser.from(staff))).with(csrf())
                        .param("title", "")
                        .param("slug", "not a slug!")
                        .param("publish", "2025-01-01T10:00")
                        .param("status", "DRAFT"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("form", "title", "slug", "authorId", "body"));
    }
}
