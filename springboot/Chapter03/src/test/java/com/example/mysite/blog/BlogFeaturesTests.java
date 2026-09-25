package com.example.mysite.blog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.xpath;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.mail.MailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.example.mysite.TestcontainersConfiguration;
import static com.example.mysite.HtmlPage.texts;
import com.example.mysite.account.User;
import com.example.mysite.account.UserRepository;

/** 第 3 章新增功能：标签、相似文章、侧边栏模板标签、Markdown、RSS、Sitemap、trigram 搜索。 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class BlogFeaturesTests {

    @Autowired
    MockMvc mvc;

    @Autowired
    UserRepository users;

    @Autowired
    PostRepository posts;

    @Autowired
    TagRepository tags;

    @Autowired
    CommentRepository comments;

    @Autowired
    PostService postService;

    @Autowired
    EntityManager entityManager;

    @MockitoBean
    MailSender mailSender;

    Post jazz;
    Post django;
    Post music;

    @BeforeEach
    void setUp() {
        User author = users.save(new User("alice", "{noop}secret", "alice@example.com"));
        jazz = publish(author, "Who was Django Reinhardt?", "who-was-django-reinhardt", 1,
                "A **jazz** guitarist from Belgium.", "music", "jazz");
        django = publish(author, "Django templates", "django-templates", 2,
                "About the template engine.", "django", "python");
        music = publish(author, "Gypsy jazz today", "gypsy-jazz-today", 3,
                "Still alive.", "music", "jazz", "guitar");
        comments.save(new Comment(django, "Bob", "bob@example.com", "Nice"));
        comments.save(new Comment(django, "Eve", "eve@example.com", "Great"));
        // 测试方法和 setUp 在同一个事务里：清空一级缓存，让后面的请求真正从数据库读取
        entityManager.flush();
        entityManager.clear();
    }

    private Post publish(User author, String title, String slug, int day, String body, String... tagNames) {
        Post post = new Post(title, slug, author, body);
        post.setPublish(OffsetDateTime.parse("2025-05-0%dT10:00:00Z".formatted(day)));
        post.setStatus(PostStatus.PUBLISHED);
        for (String name : tagNames) {
            post.getTags().add(tags.getOrCreate(name));
        }
        return posts.save(post);
    }

    @Test
    void listFiltersByTagAndShowsTags() throws Exception {
        var page = mvc.perform(get("/blog/tag/jazz/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Posts tagged with \"<span>jazz</span>\"")));
        assertThat(texts(page, "#content h2 > a")).containsExactly("Gypsy jazz today", "Who was Django Reinhardt?");
        assertThat(texts(page, "#content p.tags").getFirst()).isEqualTo("Tags: guitar, jazz, music");
        mvc.perform(get("/blog/tag/unknown/")).andExpect(status().isNotFound());
    }

    @Test
    void similarPostsAreRankedBySharedTags() {
        List<Post> similar = postService.similarPosts(jazz, 4);
        assertThat(similar).extracting(Post::getTitle).containsExactly("Gypsy jazz today");
    }

    @Test
    void detailRendersMarkdownAndSidebarTags() throws Exception {
        mvc.perform(get(jazz.getAbsoluteUrl()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("A <strong>jazz</strong> guitarist")))
                .andExpect(content().string(containsString("I've written <span>3</span> posts so far.")))
                .andExpect(content().string(containsString("Similar posts")));
        // 侧边栏：最新 3 篇、评论最多的排第一
        var home = mvc.perform(get("/blog/"));
        assertThat(texts(home, "#sidebar h3:contains(Latest posts) + ul a"))
                .containsExactly("Gypsy jazz today", "Django templates", "Who was Django Reinhardt?");
        assertThat(texts(home, "#sidebar h3:contains(Most commented posts) + ul a").getFirst())
                .isEqualTo("Django templates");
    }

    @Test
    void feedAndSitemap() throws Exception {
        mvc.perform(get("/blog/feed/"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/rss+xml"))
                .andExpect(xpath("/rss/channel/title").string("My blog"))
                .andExpect(xpath("/rss/channel/item[1]/title").string("Gypsy jazz today"))
                .andExpect(xpath("count(/rss/channel/item)").number(3.0));

        Map<String, String> ns = Map.of("s", "http://www.sitemaps.org/schemas/sitemap/0.9");
        mvc.perform(get("/sitemap.xml"))
                .andExpect(status().isOk())
                .andExpect(xpath("count(//s:url)", ns).number(3.0 + 5.0))
                .andExpect(xpath("//s:url[s:loc='http://localhost/blog/2025/5/1/who-was-django-reinhardt/']/s:priority", ns)
                        .string("0.9"));
    }

    @Test
    void trigramSearchToleratesTypos() throws Exception {
        mvc.perform(get("/blog/search/"))
                .andExpect(content().string(containsString("Search for posts")))
                .andExpect(content().string(not(containsString("errorlist"))));
        // 拼错了也能搜到，按相似度排序；和标题毫不相干的文章不会出现
        var results = mvc.perform(get("/blog/search/").param("query", "Djnago Reinhart"));
        assertThat(texts(results, "#content h4 a").getFirst()).isEqualTo("Who was Django Reinhardt?");
        assertThat(texts(results, "#content h4 a")).doesNotContain("Gypsy jazz today");
        mvc.perform(get("/blog/search/").param("query", ""))
                .andExpect(content().string(containsString("errorlist")));
    }
}
