package com.example.educa.students;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import com.example.educa.EducaTest;
import com.example.educa.account.User;
import com.example.educa.account.UserRepository;
import com.example.educa.courses.Course;
import com.example.educa.courses.CourseRepository;
import com.example.educa.courses.Subject;
import com.example.educa.courses.SubjectRepository;

@EducaTest
@Transactional
class EnrollReminderCommandTests {

    @Autowired
    EnrollReminderCommand command;

    @Autowired
    UserRepository users;

    @Autowired
    SubjectRepository subjects;

    @Autowired
    CourseRepository courses;

    @Autowired
    JdbcTemplate jdbc;

    @MockitoBean
    JavaMailSender mailSender;

    private User user(String username, String email, int daysAgo) {
        User user = users.saveAndFlush(new User(username, "{noop}pw", email));
        // date_joined 由 @CreationTimestamp 生成，直接改数据库模拟“N 天前注册”
        jdbc.update("update auth_user set date_joined = ? where id = ?",
                OffsetDateTime.now().minusDays(daysAgo), user.getId());
        return user;
    }

    @Test
    void remindsOnlyOldUsersWithoutCoursesAndWithAnEmail() {
        User lazy = user("lazy", "lazy@example.com", 30);
        lazy.setFirstName("Lazy");
        User busy = user("busy", "busy@example.com", 30);
        user("fresh", "fresh@example.com", 2);
        user("noemail", "", 30);
        Course course = courses.save(new Course(busy, subjects.save(new Subject("S", "s-reminder")), "C", "c-reminder", "o"));
        course.getStudents().add(busy);
        users.flush();

        assertThat(command.sendReminders(20)).isEqualTo(1);

        // 一次调用发送全部邮件（send(SimpleMailMessage...)）
        ArgumentCaptor<SimpleMailMessage[]> sent = ArgumentCaptor.forClass(SimpleMailMessage[].class);
        verify(mailSender).send(sent.capture());
        assertThat(sent.getValue()).singleElement().satisfies(m -> {
            assertThat(m.getTo()).containsExactly("lazy@example.com");
            assertThat(m.getSubject()).isEqualTo("Enroll in a course");
            assertThat(m.getText()).startsWith("Dear Lazy,");
        });
    }

    @Test
    void nothingToSend() {
        user("fresh", "fresh@example.com", 1);
        assertThat(command.sendReminders(20)).isZero();
        verify(mailSender, never()).send(org.mockito.ArgumentMatchers.<SimpleMailMessage[]>any());
    }
}
