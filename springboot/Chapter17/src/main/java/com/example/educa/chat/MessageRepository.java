package com.example.educa.chat;

import java.util.List;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageRepository extends JpaRepository<Message, Long> {

    /** course.chat_messages.select_related('user').order_by('-id')[:5] */
    @EntityGraph(attributePaths = "user")
    List<Message> findByCourseIdOrderByIdDesc(Long courseId, Limit limit);
}
