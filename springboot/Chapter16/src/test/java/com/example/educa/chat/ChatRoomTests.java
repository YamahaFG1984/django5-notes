package com.example.educa.chat;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.stringContainsInOrder;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.example.educa.EducaTest;
import com.example.educa.account.CurrentUser;
import com.example.educa.account.User;
import com.example.educa.account.UserRepository;
import com.example.educa.courses.Course;
import com.example.educa.courses.CourseRepository;
import com.example.educa.courses.Subject;
import com.example.educa.courses.SubjectRepository;

@EducaTest
@Transactional
class ChatRoomTests {

    @Autowired
    MockMvc mvc;

    @Autowired
    UserRepository users;

    @Autowired
    SubjectRepository subjects;

    @Autowired
    CourseRepository courses;

    @Autowired
    MessageRepository messages;

    User alice;
    User bob;
    Course course;

    @BeforeEach
    void setUp() {
        alice = users.save(new User("alice", "{noop}pw", ""));
        bob = users.save(new User("bob", "{noop}pw", ""));
        Subject music = subjects.save(new Subject("Music", "music-chat"));
        course = courses.save(new Course(alice, music, "Jazz", "jazz-chat", "o"));
        course.getStudents().add(alice);
    }

    @Test
    void onlyEnrolledStudentsMayEnter() throws Exception {
        mvc.perform(get("/chat/room/{id}/", course.getId())).andExpect(redirectedUrl("/accounts/login/"));
        mvc.perform(get("/chat/room/{id}/", course.getId()).with(user(CurrentUser.from(bob))))
                .andExpect(status().isForbidden());
        mvc.perform(get("/chat/room/999999/").with(user(CurrentUser.from(alice))))
                .andExpect(status().isForbidden());
    }

    @Test
    void showsTheLatestFiveMessagesOldestFirst() throws Exception {
        course.getStudents().add(bob);
        for (int i = 1; i <= 7; i++) {
            messages.save(new Message(i % 2 == 0 ? bob : alice, course, "message " + i));
        }
        messages.save(new Message(bob, course, "<script>alert(1)</script>"));

        mvc.perform(get("/chat/room/{id}/", course.getId()).with(user(CurrentUser.from(alice))))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("message 3<"))))
                .andExpect(content().string(stringContainsInOrder(List.of(
                        "message 4", "message 5", "message 6", "message 7", "&lt;script&gt;"))))
                .andExpect(content().string(containsString("class=\"message me\"")))
                .andExpect(content().string(containsString("class=\"message other\"")))
                // 页面里的 JS 变量由内联模式安全输出
                .andExpect(content().string(containsString("const courseId = " + course.getId() + ";")))
                .andExpect(content().string(containsString("const requestUser = \"alice\";")));
    }

    @Test
    void studentCoursePageLinksToTheChatRoom() throws Exception {
        mvc.perform(get("/students/course/{id}/", course.getId()).with(user(CurrentUser.from(alice))))
                .andExpect(content().string(containsString("/chat/room/" + course.getId() + "/")));
    }
}
