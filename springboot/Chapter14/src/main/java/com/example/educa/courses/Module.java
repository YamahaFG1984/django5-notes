package com.example.educa.courses;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

/** 课程里的一个模块（章节）。 */
@Entity
@Table(name = "courses_module")
@EntityListeners(OrderFieldListener.class)
public class Module {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id")
    private Course course;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String description = "";

    /**
     * order = OrderField(blank=True, for_fields=['course'])。
     * “order” 是 SQL 关键字：Django 生成 SQL 时会自动给列名加引号，JPA 需要我们自己写成 "\"order\""。
     */
    @OrderField(scope = "course")
    @Column(name = "\"order\"", nullable = false)
    private Integer order;

    /** related_name='contents' */
    @OneToMany(mappedBy = "module", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("order")
    private List<Content> contents = new ArrayList<>();

    protected Module() {
    }

    public Module(Course course, String title, String description) {
        this.course = course;
        this.title = title;
        this.description = description == null ? "" : description;
    }

    public Long getId() {
        return id;
    }

    public Course getCourse() {
        return course;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description == null ? "" : description;
    }

    public Integer getOrder() {
        return order;
    }

    public void setOrder(Integer order) {
        this.order = order;
    }

    public List<Content> getContents() {
        return contents;
    }

    /** __str__：f'{self.order}. {self.title}' */
    @Override
    public String toString() {
        return order + ". " + title;
    }
}
