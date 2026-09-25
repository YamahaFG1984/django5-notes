package com.example.educa.deploy;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.example.educa.TestcontainersConfiguration;
import com.example.educa.account.GroupRepository;
import com.example.educa.account.User;
import com.example.educa.account.UserRepository;
import com.example.educa.courses.Course;
import com.example.educa.courses.CourseRepository;
import com.example.educa.courses.LoadDataCommand;
import com.example.educa.courses.Subject;
import com.example.educa.courses.SubjectRepository;
import com.example.educa.courses.catalog.SubjectSummary;

/**
 * 本地开发和其他测试用 H2，生产环境用 PostgreSQL：这里在真正的 PostgreSQL 上跑一遍迁移脚本和几个
 * 手写的查询（MERGE、group by 分页、序列重置），确保换数据库不会出问题。
 * ≈ 书中把 SQLite 换成 PostgreSQL 后重新 migrate、loaddata。
 */
@SpringBootTest
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, PostgresCompatibilityTests.Postgres.class})
class PostgresCompatibilityTests {

    @TestConfiguration(proxyBeanMethods = false)
    static class Postgres {

        static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16");

        @Bean
        @ServiceConnection
        PostgreSQLContainer postgres() {
            return POSTGRES;
        }
    }

    @Autowired
    LoadDataCommand loadData;

    @Autowired
    SubjectRepository subjects;

    @Autowired
    CourseRepository courses;

    @Autowired
    UserRepository users;

    @Autowired
    GroupRepository groups;

    @Test
    void migrationsFixturesAndQueriesWorkOnPostgres() {
        assertThat(groups.findByName("Instructors")).isPresent();

        assertThat(loadData.load("subjects")).isEqualTo(4);
        assertThat(loadData.load("subjects")).isEqualTo(4);
        Subject chemistry = subjects.save(new Subject("Chemistry", "chemistry"));
        assertThat(chemistry.getId()).isGreaterThan(4L);   // setval() 之后新主键不冲突

        User owner = users.save(new User("pg-owner", "{noop}pw", ""));
        Course course = courses.save(new Course(owner, chemistry, "Organic", "organic", "o"));

        List<SubjectSummary> summaries = subjects.summaries(PageRequest.of(0, 10)).getContent();
        assertThat(summaries).extracting(SubjectSummary::title).contains("Chemistry", "Programming");
        assertThat(summaries).filteredOn(s -> s.slug().equals("chemistry")).singleElement()
                .extracting(SubjectSummary::totalCourses).isEqualTo(1L);
        assertThat(courses.summaries(null)).hasSize(1);
        assertThat(courses.popularCourses(List.of(chemistry.getId()))).singleElement()
                .satisfies(p -> assertThat(p.label()).isEqualTo("Organic (0 students)"));

        courses.delete(course);
        users.delete(owner);
        subjects.delete(chemistry);
    }
}
