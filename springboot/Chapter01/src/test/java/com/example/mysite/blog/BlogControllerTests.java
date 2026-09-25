package com.example.mysite.blog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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

/**
 * 相当于 Django 的 TestCase + Client：@Transactional 让每个测试结束后回滚数据。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class BlogControllerTests {

    @Autowired
    MockMvc mvc;

    @Autowired
    UserRepository users;

    @Autowired
    PostRepository posts;

    @Autowired
    FavouritePostRepository favourites;

    User author;
    Post published;
    Post draft;

    @BeforeEach
    void setUp() {
        author = users.save(new User("alice", "{noop}secret", "alice@example.com"));
        published = new Post("Hello Spring", "hello-spring", author, "First paragraph.\n\nSecond paragraph.");
        published.setStatus(PostStatus.PUBLISHED);
        posts.save(published);
        draft = posts.save(new Post("Secret draft", "secret-draft", author, "Not yet."));
    }

    @Test
    void listShowsOnlyPublishedPosts() throws Exception {
        mvc.perform(get("/blog/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Hello Spring")))
                .andExpect(content().string(not(containsString("Secret draft"))));
    }

    @Test
    void detailRendersLinebreaksAndHidesDrafts() throws Exception {
        mvc.perform(get(published.getAbsoluteUrl()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<p>First paragraph.</p>")));
        mvc.perform(get(draft.getAbsoluteUrl()))
                .andExpect(status().isNotFound());
    }

    @Test
    void anonymousUserMustLogInBeforeAddingFavourite() throws Exception {
        mvc.perform(post("/blog/favourite/add/{id}/", published.getId()).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
        assertThat(favourites.count()).isZero();
    }

    @Test
    void favouriteRequiresCsrfToken() throws Exception {
        mvc.perform(post("/blog/favourite/add/{id}/", published.getId()).with(user(CurrentUser.from(author))))
                .andExpect(status().isForbidden());
    }

    @Test
    void loggedInUserCanFavouriteOnlyOnce() throws Exception {
        var me = user(CurrentUser.from(author));
        for (int i = 0; i < 2; i++) {
            mvc.perform(post("/blog/favourite/add/{id}/", published.getId()).with(me).with(csrf()))
                    .andExpect(redirectedUrl(published.getAbsoluteUrl()));
        }
        assertThat(favourites.count()).isEqualTo(1);

        mvc.perform(get("/blog/favourites/").with(me))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Hello Spring")));
        mvc.perform(get(published.getAbsoluteUrl()).with(me))
                .andExpect(content().string(containsString("❤️")));
    }
}
