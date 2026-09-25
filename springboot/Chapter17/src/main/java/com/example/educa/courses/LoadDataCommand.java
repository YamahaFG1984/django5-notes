package com.example.educa.courses;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import tools.jackson.databind.ObjectMapper;

/**
 * python manage.py loaddata subjects.json 的替代品：
 * <pre>
 * java -jar target/educa-1.0.0.jar --spring.main.web-application-type=none --loaddata=subjects
 * </pre>
 * 读取 classpath:fixtures/&lt;名字&gt;.json（文件格式与 Django 的 fixture 相同），按主键“有则更新、无则插入”。
 * 这里只支持 courses.subject 一种模型 —— Django 的 loaddata 能处理任意模型，是因为它能反射出所有模型的字段。
 */
@Component
public class LoadDataCommand implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(LoadDataCommand.class);

    /** fixture 文件里的一条记录：{"model": "courses.subject", "pk": 1, "fields": {...}} */
    public record FixtureObject(String model, Long pk, SubjectFields fields) {
    }

    public record SubjectFields(String title, String slug) {
    }

    private final SubjectRepository subjects;
    private final TransactionTemplate transactions;
    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbc;
    private final ApplicationContext context;

    public LoadDataCommand(SubjectRepository subjects, TransactionTemplate transactions,
                           ObjectMapper objectMapper, JdbcTemplate jdbc, ApplicationContext context) {
        this.subjects = subjects;
        this.transactions = transactions;
        this.objectMapper = objectMapper;
        this.jdbc = jdbc;
        this.context = context;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!args.containsOption("loaddata")) {
            return;
        }
        try {
            for (String name : args.getOptionValues("loaddata")) {
                log.info("Installed {} object(s) from 1 fixture(s)", load(name));
            }
        } finally {
            new Thread(() -> System.exit(SpringApplication.exit(context, () -> 0))).start();
        }
    }

    /** 整个文件在一个事务里导入：要么全部成功，要么什么都不改（loaddata 也是这样） */
    public int load(String name) {
        List<FixtureObject> objects;
        try (InputStream in = new ClassPathResource("fixtures/" + name + ".json").getInputStream()) {
            objects = List.of(objectMapper.readValue(in, FixtureObject[].class));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return transactions.execute(status -> {
            int count = 0;
            for (FixtureObject object : objects) {
                if (!"courses.subject".equals(object.model())) {
                    throw new IllegalArgumentException("不支持的模型: " + object.model());
                }
                subjects.upsert(object.pk(), object.fields().title(), object.fields().slug());
                count++;
            }
            resetIdentity("courses_subject");
            return count;
        });
    }

    /**
     * 显式插入了主键之后，自增序列并不会跟着前进，下一次普通插入就会主键冲突。
     * Django 的 loaddata 会自动执行 sqlsequencereset；这里手动把序列推到 max(id) + 1。
     * 不同数据库的语法不同（这正是 ORM 平时替我们屏蔽掉的差异）。
     */
    private void resetIdentity(String table) {
        Long max = jdbc.queryForObject("SELECT COALESCE(MAX(id), 0) FROM " + table, Long.class);
        String database = jdbc.execute((java.sql.Connection c) -> c.getMetaData().getDatabaseProductName());
        if ("PostgreSQL".equals(database)) {
            jdbc.queryForObject("SELECT setval(pg_get_serial_sequence('" + table + "', 'id'), ?)", Long.class, Math.max(max, 1));
        } else {
            jdbc.execute("ALTER TABLE " + table + " ALTER COLUMN id RESTART WITH " + (max + 1));
        }
    }
}
