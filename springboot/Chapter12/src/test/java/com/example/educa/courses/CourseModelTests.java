package com.example.educa.courses;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.example.educa.account.User;
import com.example.educa.account.UserRepository;

/** 模型层测试：OrderField、继承与多态、级联删除。（≈ 书中在 shell 里做的实验） */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CourseModelTests {

    @Autowired
    UserRepository users;

    @Autowired
    SubjectRepository subjects;

    @Autowired
    CourseRepository courses;

    @Autowired
    ModuleRepository modules;

    @Autowired
    ContentRepository contents;

    @Autowired
    EntityManager em;

    User instructor;
    Subject music;

    @BeforeEach
    void setUp() {
        instructor = users.save(new User("teacher", "{noop}pw", "t@example.com"));
        music = subjects.save(new Subject("Music", "music-test"));
    }

    private Course course(String slug) {
        return courses.save(new Course(instructor, music, "Course " + slug, slug, "Overview"));
    }

    @Test
    void orderFieldNumbersModulesWithinEachCourse() {
        Course c1 = course("c1");
        Course c2 = course("c2");
        Module m1 = modules.save(new Module(c1, "Module 1", ""));
        Module m2 = modules.save(new Module(c1, "Module 2", ""));
        Module other = modules.save(new Module(c2, "Module 1", ""));
        assertThat(m1.getOrder()).isZero();
        assertThat(m2.getOrder()).isEqualTo(1);
        assertThat(other.getOrder()).isZero();   // 每门课程单独编号（for_fields=['course']）

        Module explicit = new Module(c1, "Module 5", "");
        explicit.setOrder(5);
        assertThat(modules.save(explicit).getOrder()).isEqualTo(5);   // 指定了就不改
        assertThat(modules.save(new Module(c1, "Module 6", "")).getOrder()).isEqualTo(6);
        assertThat(m2.toString()).isEqualTo("1. Module 2");
    }

    @Test
    void contentsArePolymorphicAndOrderedPerModule() {
        Module module = modules.save(new Module(course("c3"), "Module", ""));
        contents.save(new Content(module, new TextItem(instructor, "Intro", "Hello")));
        contents.save(new Content(module, new VideoItem(instructor, "Lecture", "https://www.youtube.com/watch?v=abc")));
        contents.save(new Content(module, new ImageItem(instructor, "Diagram", "images/d.png")));
        em.flush();
        em.clear();

        List<Content> loaded = contents.findByModuleIdOrderByOrderAsc(module.getId());
        assertThat(loaded).extracting(Content::getOrder).containsExactly(0, 1, 2);
        // 一次查询就得到了正确的子类（Hibernate 用鉴别列 item_type 决定实例化哪个类）
        assertThat(loaded).extracting(c -> c.getItem().getModelName()).containsExactly("text", "video", "image");
        assertThat(loaded.getFirst().getItem()).isInstanceOf(TextItem.class);
        assertThat(((TextItem) loaded.getFirst().getItem()).getContent()).isEqualTo("Hello");

        // 可以直接对父类做多态查询：所有条目，不管是什么类型
        List<Item> all = em.createQuery("select i from Item i where i.owner = :owner", Item.class)
                .setParameter("owner", instructor).getResultList();
        assertThat(all).hasSize(3);
    }

    @Test
    void deletingContentDeletesItsItem() {
        Module module = modules.save(new Module(course("c4"), "Module", ""));
        Content content = contents.save(new Content(module, new TextItem(instructor, "Intro", "Hello")));
        Long itemId = content.getItem().getId();
        contents.delete(content);
        em.flush();
        assertThat(em.find(Item.class, itemId)).isNull();   // orphanRemoval：书中要手动 content.item.delete()
    }
}
