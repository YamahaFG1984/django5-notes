package com.example.educa.courses.catalog;

import java.util.List;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.educa.courses.ContentRepository;
import com.example.educa.courses.CourseRepository;
import com.example.educa.courses.SubjectRepository;

/**
 * 公开目录的读操作，结果放进 Redis。
 * <p>
 * 书中的写法是“先 cache.get，没有再查库并 cache.set”（缓存旁路），代码散落在视图里；
 * Spring 用 @Cacheable 把同样的逻辑声明在方法上：方法参数算出 key，缓存命中就不执行方法体。
 * <p>
 * 注意 @Cacheable 靠代理实现：只有<b>从别的 Bean 调用</b>才会经过缓存，同一个类里的 this.xxx() 调用不会。
 */
@Service
@Transactional(readOnly = true)
public class CatalogService {

    private final SubjectRepository subjects;
    private final CourseRepository courses;
    private final ContentRepository contents;

    public CatalogService(SubjectRepository subjects, CourseRepository courses, ContentRepository contents) {
        this.subjects = subjects;
        this.courses = courses;
        this.contents = contents;
    }

    @Cacheable(cacheNames = CacheNames.SUBJECTS, key = "'all'")
    public List<SubjectSummary> subjects() {
        return subjects.summaries();
    }

    /** subjectSlug 为 null 表示全部课程 */
    @Cacheable(cacheNames = CacheNames.COURSES, key = "#subjectSlug ?: 'all'")
    public List<CourseSummary> courses(String subjectSlug) {
        return courses.summaries(subjectSlug);
    }

    @Cacheable(cacheNames = CacheNames.MODULE_CONTENTS, key = "#moduleId")
    public List<ContentView> moduleContents(Long moduleId) {
        return contents.findByModuleIdOrderByOrderAsc(moduleId).stream()
                .map(content -> ContentView.of(content.getItem()))
                .toList();
    }
}
