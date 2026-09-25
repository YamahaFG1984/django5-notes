package com.example.bookmarks.images;

import static com.example.bookmarks.TestImages.png;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import com.example.bookmarks.TestcontainersConfiguration;
import com.example.bookmarks.account.CurrentUser;
import com.example.bookmarks.account.Profile;
import com.example.bookmarks.account.ProfileRepository;
import com.example.bookmarks.account.User;
import com.example.bookmarks.account.UserRepository;
import com.example.bookmarks.common.MediaStorage;
import com.example.bookmarks.images.ImageDownloader.DownloadException;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Transactional
class ImageFlowTests {

    @Autowired
    MockMvc mvc;

    @Autowired
    UserRepository users;

    @Autowired
    ProfileRepository profiles;

    @Autowired
    ImageRepository images;

    @Autowired
    MediaStorage media;

    @Autowired
    EntityManager entityManager;

    /** 不真的上网下载：替换成返回一张 PNG 的假下载器 */
    @MockitoBean
    ImageDownloader downloader;

    User alice;
    RequestPostProcessor asAlice;

    @BeforeEach
    void setUp() throws Exception {
        alice = users.save(new User("alice", "{noop}pw", "alice@example.com"));
        alice.setFirstName("Alice");
        profiles.save(new Profile(alice));
        asAlice = user(CurrentUser.from(alice));
        when(downloader.download(anyString())).thenReturn(png());
    }

    private Image bookmark(String title) {
        Image image = new Image(alice, title, "https://example.com/" + title + ".png",
                media.saveImage("images", title + ".png", png()), "");
        return images.save(image);
    }

    @Test
    void bookmarkletPrefillsFormFromQueryString() throws Exception {
        mvc.perform(get("/images/create/").param("url", "https://example.com/cat.png").param("title", "A cat").with(asAlice))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("value=\"A cat\"")))
                .andExpect(content().string(containsString("src=\"https://example.com/cat.png\"")));
    }

    @Test
    void createDownloadsImageAndRedirectsToDetail() throws Exception {
        mvc.perform(post("/images/create/").with(asAlice).with(csrf())
                        .param("title", "Django Reinhardt").param("url", "https://example.com/django.png")
                        .param("description", "Jazz"))
                .andExpect(redirectedUrlPattern("/images/detail/*/django-reinhardt/"));

        Image saved = images.findAll().getFirst();
        assertThat(saved.getSlug()).isEqualTo("django-reinhardt");
        assertThat(saved.getImage()).startsWith("images/").endsWith(".png");
        assertThat(Files.exists(media.resolve(saved.getImage()))).isTrue();

        // 详情页会生成 300 宽的缩略图
        mvc.perform(get(saved.getAbsoluteUrl()).with(asAlice))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/media/thumbs/images/")))
                .andExpect(content().string(containsString("Nobody likes this image yet.")));
    }

    @Test
    void invalidExtensionAndDownloadErrorsAreShownOnTheForm() throws Exception {
        mvc.perform(post("/images/create/").with(asAlice).with(csrf())
                        .param("title", "Doc").param("url", "https://example.com/file.pdf"))
                .andExpect(model().attributeHasFieldErrors("form", "urlExtensionValid"))
                .andExpect(content().string(containsString("does not match valid image extensions")));

        when(downloader.download(anyString())).thenThrow(new DownloadException("Downloading from this address is not allowed."));
        mvc.perform(post("/images/create/").with(asAlice).with(csrf())
                        .param("title", "Internal").param("url", "http://127.0.0.1/secret.png"))
                .andExpect(model().attributeHasFieldErrorCode("form", "url", "download"));
    }

    @Test
    void likeAndUnlikeWithAjax() throws Exception {
        Image image = bookmark("sunset");
        mvc.perform(post("/images/like/").with(asAlice).with(csrf())
                        .param("id", image.getId().toString()).param("action", "like"))
                .andExpect(jsonPath("$.status").value("ok"));
        // 重复点赞不会报错，也不会重复计数
        mvc.perform(post("/images/like/").with(asAlice).with(csrf())
                        .param("id", image.getId().toString()).param("action", "like"))
                .andExpect(jsonPath("$.status").value("ok"));
        entityManager.flush();
        assertThat(images.isLikedBy(image.getId(), alice.getId())).isTrue();

        entityManager.clear();
        mvc.perform(get(image.getAbsoluteUrl()).with(asAlice))
                .andExpect(content().string(containsString("<span class=\"total\">1</span>")))
                .andExpect(content().string(containsString("data-action=\"unlike\"")));

        mvc.perform(post("/images/like/").with(asAlice).with(csrf())
                        .param("id", image.getId().toString()).param("action", "unlike"))
                .andExpect(jsonPath("$.status").value("ok"));
        entityManager.flush();
        assertThat(images.isLikedBy(image.getId(), alice.getId())).isFalse();

        mvc.perform(post("/images/like/").with(asAlice).with(csrf()).param("id", "999").param("action", "like"))
                .andExpect(jsonPath("$.status").value("error"));
    }

    @Test
    void ajaxPostWithoutCsrfTokenIsRejected() throws Exception {
        Image image = bookmark("sunset");
        mvc.perform(post("/images/like/").with(asAlice).param("id", image.getId().toString()).param("action", "like"))
                .andExpect(status().isForbidden());
        // CSRF 令牌放在请求头里也可以（前端 fetch 就是这么做的）
        mvc.perform(post("/images/like/").with(asAlice).with(csrf().asHeader())
                        .param("id", image.getId().toString()).param("action", "like"))
                .andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    void listSupportsInfiniteScroll() throws Exception {
        for (int i = 1; i <= 10; i++) {
            bookmark("img" + i);
        }
        mvc.perform(get("/images/").with(asAlice))
                .andExpect(content().string(containsString("Images bookmarked")))
                .andExpect(model().attribute("images", org.hamcrest.Matchers.hasSize(8)));
        mvc.perform(get("/images/").param("images_only", "1").param("page", "2").with(asAlice))
                .andExpect(model().attribute("images", org.hamcrest.Matchers.hasSize(2)))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("<html"))));
        mvc.perform(get("/images/").param("images_only", "1").param("page", "3").with(asAlice))
                .andExpect(content().string(org.hamcrest.Matchers.blankString()));
    }

    @Test
    void dashboardShowsCountAndBookmarklet() throws Exception {
        bookmark("one");
        mvc.perform(get("/account/").with(asAlice))
                .andExpect(content().string(containsString("<span>1 image</span>")))
                .andExpect(model().attribute("bookmarklet", startsWith("javascript:(function(){")))
                .andExpect(model().attribute("bookmarklet", containsString("//localhost/static/js/bookmarklet.js")))
                .andExpect(content().string(containsString("name=\"_csrf\"")));
    }
}
