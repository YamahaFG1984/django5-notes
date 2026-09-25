package com.example.educa.chat;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.educa.account.UserRepository;
import com.example.educa.courses.Course;
import com.example.educa.courses.CourseRepository;

@Service
@Transactional
public class ChatService {

    /** 单条消息的长度上限（书中没有限制） */
    public static final int MAX_LENGTH = 2000;

    private final CourseRepository courses;
    private final MessageRepository messages;
    private final UserRepository users;

    public ChatService(CourseRepository courses, MessageRepository messages, UserRepository users) {
        this.courses = courses;
        this.messages = messages;
        this.users = users;
    }

    /** request.user.courses_joined.get(id=course_id)：只有选了这门课的学生才能进聊天室 */
    @Transactional(readOnly = true)
    public Optional<Course> joinedCourse(Long courseId, Long userId) {
        return courses.findById(courseId).filter(c -> courses.existsByIdAndStudentsId(courseId, userId));
    }

    @Transactional(readOnly = true)
    public boolean isEnrolled(Long courseId, Long userId) {
        return courses.existsByIdAndStudentsId(courseId, userId);
    }

    /** 最近 5 条消息，按时间正序（书中先倒序取 5 条，再 reversed()） */
    @Transactional(readOnly = true)
    public List<Message> latestMessages(Long courseId) {
        return messages.findByCourseIdOrderByIdDesc(courseId, Limit.of(5)).reversed();
    }

    /** persist_message()；返回保存后的消息（带上数据库生成的时间） */
    public Message save(Long courseId, Long userId, String content) {
        return messages.save(new Message(users.getReferenceById(userId), courses.getReferenceById(courseId), content));
    }
}
