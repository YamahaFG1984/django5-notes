package com.example.mysite.blog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.example.mysite.TestcontainersConfiguration;
import static com.example.mysite.HtmlPage.texts;
import com.example.mysite.account.User;
import com.example.mysite.account.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class BlogControllerTests {

    @Autowired
    MockMvc mvc;

    @Autowired
    UserRepository users;

    @Autowired
    PostRepository posts;

    @Autowired
    CommentRepository comments;

    /** 用 Mockito 替换掉邮件发送器（≈ Django 测试里的 mail.outbox） */
    @MockitoBean
    MailSender mailSender;

    User author;
    Post published;

    @BeforeEach
    void setUp() {
        author = users.save(new User("alice", "{noop}secret", "alice@example.com"));
        published = publish("Hello Spring", "hello-spring", OffsetDateTime.of(2025, 1, 31, 23, 30, 0, 0, ZoneOffset.UTC));
        posts.save(new Post("Secret draft", "secret-draft", author, "Not yet."));
    }

    private Post publish(String title, String slug, OffsetDateTime when) {
        Post post = new Post(title, slug, author, "First paragraph.\n\nSecond paragraph.");
        post.setPublish(when);
        post.setStatus(PostStatus.PUBLISHED);
        return posts.save(post);
    }

    @Test
    void canonicalUrlUsesPublishDateAndSlug() throws Exception {
        assertThat(published.getAbsoluteUrl()).isEqualTo("/blog/2025/1/31/hello-spring/");
        mvc.perform(get("/blog/2025/1/31/hello-spring/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("0 comments")));
        mvc.perform(get("/blog/2025/2/1/hello-spring/")).andExpect(status().isNotFound());
        mvc.perform(get("/blog/2025/13/1/hello-spring/")).andExpect(status().isNotFound());
    }

    @Test
    void listIsPaginatedThreePerPage() throws Exception {
        for (int i = 1; i <= 4; i++) {
            publish("Extra " + i, "extra-" + i, OffsetDateTime.of(2025, 2, i, 9, 0, 0, 0, ZoneOffset.UTC));
        }
        assertThat(texts(mvc.perform(get("/blog/")), "#content h2 a"))
                .containsExactly("Extra 4", "Extra 3", "Extra 2");
        mvc.perform(get("/blog/")).andExpect(content().string(containsString("Page 1 of 2.")));
        // 非整数 → 第一页；超出范围 → 最后一页（和书中函数视图的行为一致）
        mvc.perform(get("/blog/").param("page", "abc"))
                .andExpect(content().string(containsString("Page 1 of 2.")));
        assertThat(texts(mvc.perform(get("/blog/").param("page", "99")), "#content h2 a"))
                .containsExactly("Extra 1", "Hello Spring");
    }

    @Test
    void shareSendsEmailWithAbsoluteUrl() throws Exception {
        mvc.perform(post("/blog/{id}/share/", published.getId()).with(csrf())
                        .param("name", "Alice")
                        .param("email", "alice@example.com")
                        .param("to", "bob@example.com")
                        .param("comments", "Worth reading"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("E-mail successfully sent")));

        ArgumentCaptor<SimpleMailMessage> sent = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(sent.capture());
        assertThat(sent.getValue().getTo()).containsExactly("bob@example.com");
        assertThat(sent.getValue().getSubject()).isEqualTo("Alice (alice@example.com) recommends you read Hello Spring");
        assertThat(sent.getValue().getText()).contains("http://localhost/blog/2025/1/31/hello-spring/");
    }

    @Test
    void invalidShareFormShowsErrorsAndSendsNothing() throws Exception {
        mvc.perform(post("/blog/{id}/share/", published.getId()).with(csrf())
                        .param("name", "Alice")
                        .param("email", "not-an-email")
                        .param("to", ""))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("form", "email", "to"))
                .andExpect(content().string(containsString("errorlist")));
        verify(mailSender, never()).send(org.mockito.ArgumentMatchers.any(SimpleMailMessage.class));
    }

    @Test
    void commentIsSavedAndShownOnDetailPage() throws Exception {
        mvc.perform(post("/blog/{id}/comment/", published.getId()).with(csrf())
                        .param("name", "Bob")
                        .param("email", "bob@example.com")
                        .param("body", "Nice post!"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Your comment has been added.")));
        assertThat(comments.count()).isEqualTo(1);

        mvc.perform(get(published.getAbsoluteUrl()))
                .andExpect(content().string(containsString("1 comment<")))
                .andExpect(content().string(containsString("Comment <span>1</span> by <span>Bob</span>")));
    }

    @Test
    void commentEndpointOnlyAcceptsPost() throws Exception {
        mvc.perform(get("/blog/{id}/comment/", published.getId()))
                .andExpect(status().isMethodNotAllowed());
    }
}
