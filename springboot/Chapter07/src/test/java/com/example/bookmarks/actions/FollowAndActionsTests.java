package com.example.bookmarks.actions;

import static com.example.bookmarks.TestImages.png;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import com.example.bookmarks.TestcontainersConfiguration;
import com.example.bookmarks.account.ContactRepository;
import com.example.bookmarks.account.CurrentUser;
import com.example.bookmarks.account.Profile;
import com.example.bookmarks.account.ProfileRepository;
import com.example.bookmarks.account.User;
import com.example.bookmarks.account.UserRepository;
import com.example.bookmarks.common.MediaStorage;
import com.example.bookmarks.images.Image;
import com.example.bookmarks.images.ImageRepository;
import com.example.bookmarks.images.ImageService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Transactional
class FollowAndActionsTests {

    @Autowired
    MockMvc mvc;

    @Autowired
    UserRepository users;

    @Autowired
    ProfileRepository profiles;

    @Autowired
    ContactRepository contacts;

    @Autowired
    ActionRepository actionRepository;

    @Autowired
    ActionService actions;

    @Autowired
    ImageRepository images;

    @Autowired
    ImageService imageService;

    @Autowired
    MediaStorage media;

    @Autowired
    StringRedisTemplate redis;

    @Autowired
    EntityManager entityManager;

    User alice;
    User bob;
    User carol;

    @BeforeEach
    void setUp() {
        redis.getConnectionFactory().getConnection().serverCommands().flushDb();
        alice = person("alice", "Alice");
        bob = person("bob", "Bob");
        carol = person("carol", "Carol");
    }

    private User person(String username, String firstName) {
        User user = new User(username, "{noop}pw", username + "@example.com");
        user.setFirstName(firstName);
        users.save(user);
        profiles.save(new Profile(user));
        return user;
    }

    private RequestPostProcessor as(User user) {
        return user(CurrentUser.from(user));
    }

    private Image imageOf(User owner, String title) {
        return images.save(new Image(owner, title, "https://example.com/x.png", media.saveImage("images", "x.png", png()), ""));
    }

    @Test
    void followAndUnfollowWithAjax() throws Exception {
        mvc.perform(post("/account/users/follow/").with(as(bob)).with(csrf())
                        .param("id", alice.getId().toString()).param("action", "follow"))
                .andExpect(jsonPath("$.status").value("ok"));
        assertThat(contacts.existsByUserFromIdAndUserToId(bob.getId(), alice.getId())).isTrue();

        mvc.perform(get("/account/users/alice/").with(as(bob)))
                .andExpect(content().string(containsString("<span class=\"total\">1</span>")))
                .andExpect(content().string(containsString("data-action=\"unfollow\"")));

        mvc.perform(post("/account/users/follow/").with(as(bob)).with(csrf())
                        .param("id", alice.getId().toString()).param("action", "unfollow"))
                .andExpect(jsonPath("$.status").value("ok"));
        assertThat(contacts.countByUserToId(alice.getId())).isZero();

        // 不能关注自己，也不能关注不存在的人
        mvc.perform(post("/account/users/follow/").with(as(bob)).with(csrf())
                        .param("id", bob.getId().toString()).param("action", "follow"))
                .andExpect(jsonPath("$.status").value("error"));
        mvc.perform(post("/account/users/follow/").with(as(bob)).with(csrf()).param("id", "999").param("action", "follow"))
                .andExpect(jsonPath("$.status").value("error"));
    }

    @Test
    void peoplePagesListActiveUsers() throws Exception {
        carol.setActive(false);
        mvc.perform(get("/account/users/").with(as(bob)))
                .andExpect(content().string(containsString("/account/users/alice/")))
                .andExpect(content().string(not(containsString("/account/users/carol/"))));
        mvc.perform(get("/account/users/carol/").with(as(bob))).andExpect(status().isNotFound());
    }

    @Test
    void similarActionsWithinAMinuteAreNotDuplicated() {
        Image image = imageOf(alice, "Sunset");
        assertThat(actions.create(bob.getId(), "likes", image)).isTrue();
        assertThat(actions.create(bob.getId(), "likes", image)).isFalse();
        assertThat(actions.create(bob.getId(), "likes", imageOf(alice, "Sunrise"))).isTrue();
        assertThat(actions.create(bob.getId(), "has created an account", null)).isTrue();
        assertThat(actions.create(bob.getId(), "has created an account", null)).isFalse();
    }

    @Test
    void dashboardShowsActionsOfFollowedUsersOnly() throws Exception {
        Image sunset = imageOf(alice, "Sunset");
        actions.create(alice.getId(), "bookmarked image", sunset);
        actions.create(carol.getId(), "is following", alice);

        // 还没关注任何人：看到所有人（除了自己）的动态
        List<ActionService.ActionView> everyone = actions.feedFor(bob.getId(), 10);
        assertThat(everyone).extracting(v -> v.actor().getUsername()).containsExactlyInAnyOrder("alice", "carol");
        assertThat(everyone).anySatisfy(v -> {
            assertThat(v.targetLabel()).isEqualTo("Sunset");
            assertThat(v.targetUrl()).isEqualTo(sunset.getAbsoluteUrl());
        });

        mvc.perform(post("/account/users/follow/").with(as(bob)).with(csrf())
                        .param("id", alice.getId().toString()).param("action", "follow"))
                .andExpect(jsonPath("$.status").value("ok"));

        // 关注 alice 之后，只看 alice 的动态
        mvc.perform(get("/account/").with(as(bob)))
                .andExpect(content().string(containsString("bookmarked image")))
                .andExpect(content().string(containsString("0 minutes ago")))
                .andExpect(content().string(not(containsString(">Carol<"))));
    }

    @Test
    void likesUpdateTotalLikesThroughEvent() {
        Image image = imageOf(alice, "Sunset");
        imageService.like(image.getId(), bob.getId(), true);
        imageService.like(image.getId(), carol.getId(), true);
        entityManager.flush();
        entityManager.clear();
        assertThat(images.findById(image.getId()).orElseThrow().getTotalLikes()).isEqualTo(2);

        imageService.like(image.getId(), carol.getId(), false);
        entityManager.flush();
        entityManager.clear();
        assertThat(images.findById(image.getId()).orElseThrow().getTotalLikes()).isEqualTo(1);
        assertThat(actionRepository.findAll()).extracting(Action::getVerb).contains("likes");
    }

    @Test
    void viewsAreCountedInRedisAndRanked() throws Exception {
        Image a = imageOf(alice, "A");
        Image b = imageOf(alice, "B");
        mvc.perform(get(a.getAbsoluteUrl()).with(as(bob))).andExpect(model().attribute("totalViews", 1L));
        mvc.perform(get(b.getAbsoluteUrl()).with(as(bob))).andExpect(model().attribute("totalViews", 1L));
        mvc.perform(get(b.getAbsoluteUrl()).with(as(bob)))
                .andExpect(model().attribute("totalViews", 2L))
                .andExpect(content().string(containsString("2 views")));
        assertThat(redis.opsForValue().get("image:" + b.getId() + ":views")).isEqualTo("2");

        mvc.perform(get("/images/ranking/").with(as(bob)))
                .andExpect(model().attribute("mostViewed", List.of(b, a)));
    }

    @Test
    void actuatorEndpointsAreRestricted() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
        mvc.perform(get("/actuator/health").with(as(bob))).andExpect(status().isOk());
        mvc.perform(get("/actuator/beans").with(as(bob))).andExpect(status().isForbidden());
    }
}
