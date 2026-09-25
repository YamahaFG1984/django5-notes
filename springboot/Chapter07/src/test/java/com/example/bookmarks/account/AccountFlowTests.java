package com.example.bookmarks.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.logout;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.example.bookmarks.TestcontainersConfiguration;
import com.example.bookmarks.common.Messages;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Transactional
class AccountFlowTests {

    @Autowired
    MockMvc mvc;

    @Autowired
    UserRepository users;

    @Autowired
    ProfileRepository profiles;

    @Autowired
    PasswordEncoder passwordEncoder;

    @MockitoBean
    MailSender mailSender;

    private User createUser(String username, String password, String email) {
        User user = users.save(new User(username, passwordEncoder.encode(password), email));
        profiles.save(new Profile(user));
        return user;
    }

    @Test
    void anonymousUsersAreSentToLoginPage() throws Exception {
        mvc.perform(get("/account/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/account/login/"));
        mvc.perform(get("/account/login/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("register here")));
    }

    @Test
    void registerCreatesUserAndProfile() throws Exception {
        mvc.perform(post("/account/register/").with(csrf())
                        .param("username", "paloma")
                        .param("firstName", "Paloma")
                        .param("email", "paloma@example.com")
                        .param("password", "s3cret-pass")
                        .param("password2", "s3cret-pass"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Welcome <span>Paloma</span>!")));

        User paloma = users.findByUsername("paloma").orElseThrow();
        assertThat(passwordEncoder.matches("s3cret-pass", paloma.getPassword())).isTrue();
        assertThat(profiles.findByUserId(paloma.getId())).isPresent();

        // 注册后可以用表单登录；登录成功会更新 last_login
        mvc.perform(formLogin("/account/login/").user("paloma").password("s3cret-pass"))
                .andExpect(authenticated().withUsername("paloma"))
                .andExpect(redirectedUrl("/account/"));
        assertThat(users.findByUsername("paloma").orElseThrow().getLastLogin()).isNotNull();
    }

    @Test
    void registerRejectsMismatchedPasswordsAndDuplicateUsername() throws Exception {
        createUser("paloma", "whatever-123", "p@example.com");
        mvc.perform(post("/account/register/").with(csrf())
                        .param("username", "paloma")
                        .param("password", "s3cret-pass")
                        .param("password2", "different"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrorCode("userForm", "username", "unique"))
                .andExpect(model().attributeHasFieldErrorCode("userForm", "password2", "mismatch"))
                // 密码框不回填
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("s3cret-pass"))));
    }

    @Test
    void wrongPasswordShowsErrorAndLogoutWorks() throws Exception {
        createUser("alice", "correct-horse", "alice@example.com");
        mvc.perform(formLogin("/account/login/").user("alice").password("nope"))
                .andExpect(unauthenticated())
                .andExpect(redirectedUrl("/account/login/?error"));
        mvc.perform(get("/account/login/").param("error", ""))
                .andExpect(content().string(containsString("didn't match")));
        mvc.perform(logout("/account/logout/"))
                .andExpect(redirectedUrl("/account/logged-out/"));
    }

    @Test
    void editUpdatesUserAndUploadsPhoto() throws Exception {
        User alice = createUser("alice", "correct-horse", "alice@example.com");
        var me = user(CurrentUser.from(alice));

        mvc.perform(multipart("/account/edit/").file(new MockMultipartFile("photo", "me.png", "image/png", png()))
                        .param("firstName", "Alice").param("lastName", "Liddell").param("email", "alice@example.com")
                        .param("dateOfBirth", "1990-05-04")
                        .with(me).with(csrf()))
                .andExpect(redirectedUrl("/account/edit/"))
                .andExpect(flash().attribute("messages",
                        List.of(new Messages.Message("success", "Profile updated successfully"))));

        User updated = users.findById(alice.getId()).orElseThrow();
        assertThat(updated.getFullName()).isEqualTo("Alice Liddell");
        Profile profile = profiles.findByUserId(alice.getId()).orElseThrow();
        assertThat(profile.getDateOfBirth()).hasToString("1990-05-04");
        assertThat(profile.getPhoto()).startsWith("users/").endsWith(".png");

        // 不是图片的文件会被拒绝（≈ ImageField 的校验）
        mvc.perform(multipart("/account/edit/").file(new MockMultipartFile("photo", "evil.png", "image/png", "not an image".getBytes()))
                        .param("firstName", "Alice").with(me).with(csrf()))
                .andExpect(model().attributeHasFieldErrorCode("profileForm", "photo", "invalid_image"))
                .andExpect(content().string(containsString("Error updating your profile")));
    }

    @Test
    void canLogInWithEmailInsteadOfUsername() throws Exception {
        createUser("alice", "correct-horse", "alice@example.com");
        mvc.perform(formLogin("/account/login/").user("alice@example.com").password("correct-horse"))
                .andExpect(authenticated().withUsername("alice"));
        // 两个账号共用一个邮箱时，邮箱登录失败（≈ MultipleObjectsReturned）
        createUser("alice2", "correct-horse", "alice@example.com");
        mvc.perform(formLogin("/account/login/").user("alice@example.com").password("correct-horse"))
                .andExpect(unauthenticated());
    }

    @Test
    void emailMustBeUniqueOnRegistrationAndEdit() throws Exception {
        createUser("alice", "correct-horse", "alice@example.com");
        User bob = createUser("bob", "correct-horse", "bob@example.com");
        mvc.perform(post("/account/register/").with(csrf())
                        .param("username", "carol").param("email", "ALICE@example.com")
                        .param("password", "s3cret-pass").param("password2", "s3cret-pass"))
                .andExpect(model().attributeHasFieldErrorCode("userForm", "email", "unique"));
        mvc.perform(multipart("/account/edit/").param("email", "alice@example.com")
                        .with(user(CurrentUser.from(bob))).with(csrf()))
                .andExpect(model().attributeHasFieldErrorCode("userForm", "email", "unique"));
        // 保留自己原来的邮箱不算重复
        mvc.perform(multipart("/account/edit/").param("email", "bob@example.com")
                        .with(user(CurrentUser.from(bob))).with(csrf()))
                .andExpect(redirectedUrl("/account/edit/"));
    }

    @Test
    void googleLoginStartsOAuth2Redirect() throws Exception {
        mvc.perform(get("/oauth2/authorization/google"))
                .andExpect(status().is3xxRedirection())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .redirectedUrlPattern("https://accounts.google.com/**"));
        mvc.perform(get("/account/login/"))
                .andExpect(content().string(containsString("/oauth2/authorization/google")));
    }

    @Test
    void passwordChangeRequiresCorrectOldPassword() throws Exception {
        User alice = createUser("alice", "correct-horse", "alice@example.com");
        var me = user(CurrentUser.from(alice));
        mvc.perform(post("/account/password-change/").with(me).with(csrf())
                        .param("oldPassword", "wrong").param("newPassword1", "new-password-1").param("newPassword2", "new-password-1"))
                .andExpect(model().attributeHasFieldErrorCode("form", "oldPassword", "password_incorrect"));
        mvc.perform(post("/account/password-change/").with(me).with(csrf())
                        .param("oldPassword", "correct-horse").param("newPassword1", "new-password-1").param("newPassword2", "new-password-1"))
                .andExpect(redirectedUrl("/account/password-change/done/"));
        assertThat(passwordEncoder.matches("new-password-1", users.findById(alice.getId()).orElseThrow().getPassword())).isTrue();
    }

    @Test
    void passwordResetFlowWorksOnlyOnce() throws Exception {
        createUser("alice", "correct-horse", "alice@example.com");
        mvc.perform(post("/account/password-reset/").with(csrf()).param("email", "ALICE@example.com"))
                .andExpect(redirectedUrl("/account/password-reset/done/"));

        ArgumentCaptor<SimpleMailMessage> mail = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(mail.capture());
        Matcher link = Pattern.compile("http://localhost(/account/password-reset/[^/]+/[^/]+/)").matcher(mail.getValue().getText());
        assertThat(link.find()).isTrue();
        String path = link.group(1);

        mvc.perform(get(path)).andExpect(model().attribute("validlink", true));
        mvc.perform(post(path).with(csrf()).param("newPassword1", "brand-new-pass").param("newPassword2", "brand-new-pass"))
                .andExpect(redirectedUrl("/account/password-reset/complete/"));
        // 密码变了，同一个链接不能再用
        mvc.perform(get(path)).andExpect(model().attribute("validlink", false));
        mvc.perform(formLogin("/account/login/").user("alice").password("brand-new-pass"))
                .andExpect(authenticated());
    }

    @Test
    void passwordResetDoesNotRevealUnknownEmails() throws Exception {
        mvc.perform(post("/account/password-reset/").with(csrf()).param("email", "nobody@example.com"))
                .andExpect(redirectedUrl("/account/password-reset/done/"));
    }

    static byte[] png() throws Exception {
        BufferedImage image = new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }
}
