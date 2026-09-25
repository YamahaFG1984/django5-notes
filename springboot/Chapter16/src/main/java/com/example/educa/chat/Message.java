package com.example.educa.chat;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.annotations.CreationTimestamp;

import com.example.educa.account.User;
import com.example.educa.courses.Course;

/** 聊天消息，≈ chat.models.Message */
@Entity
@Table(name = "chat_message")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id")
    private Course course;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    /** auto_now_add=True */
    @CreationTimestamp
    @Column(name = "sent_on", nullable = false, updatable = false)
    private OffsetDateTime sentOn;

    protected Message() {
    }

    public Message(User user, Course course, String content) {
        this.user = user;
        this.course = course;
        this.content = content;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public Course getCourse() {
        return course;
    }

    public String getContent() {
        return content;
    }

    public OffsetDateTime getSentOn() {
        return sentOn;
    }

    @Override
    public String toString() {
        return user + " on " + course + " at " + sentOn;
    }
}
