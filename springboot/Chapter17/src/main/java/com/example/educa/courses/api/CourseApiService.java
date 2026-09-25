package com.example.educa.courses.api;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.educa.account.UserRepository;
import com.example.educa.common.NotFoundException;
import com.example.educa.courses.Course;
import com.example.educa.courses.CourseRepository;
import com.example.educa.courses.SubjectRepository;
import com.example.educa.courses.api.ApiDtos.CourseDto;
import com.example.educa.courses.api.ApiDtos.CourseWithContentsDto;
import com.example.educa.courses.api.ApiDtos.SubjectDto;
import com.example.educa.courses.catalog.SubjectSummary;

/** 查询并转换成 DTO 都在事务里完成：返回给控制器的是普通 record，不再有懒加载问题。 */
@Service
@Transactional(readOnly = true)
public class CourseApiService {

    private final SubjectRepository subjects;
    private final CourseRepository courses;
    private final UserRepository users;

    public CourseApiService(SubjectRepository subjects, CourseRepository courses, UserRepository users) {
        this.subjects = subjects;
        this.courses = courses;
        this.users = users;
    }

    public Page<SubjectDto> subjects(Pageable pageable) {
        Page<SubjectSummary> page = subjects.summaries(pageable);
        StandardPagination.check(page);
        Map<Long, List<String>> popular = popularCourses(page.getContent());
        return page.map(s -> toDto(s, popular));
    }

    public SubjectDto subject(Long id) {
        SubjectSummary summary = subjects.summary(id).orElseThrow(NotFoundException::new);
        return toDto(summary, popularCourses(List.of(summary)));
    }

    public Page<CourseDto> courses(Pageable pageable) {
        Page<Course> page = courses.findAllByOrderByCreatedDesc(pageable);
        StandardPagination.check(page);
        return page.map(CourseDto::of);   // 访问 modules 时按批加载，不是每门课一条 SQL
    }

    public CourseDto course(Long id) {
        return CourseDto.of(courses.findById(id).orElseThrow(NotFoundException::new));
    }

    /** @action(detail=True, methods=['post']) def enroll：course.students.add(request.user) */
    @Transactional
    public void enroll(Long courseId, Long studentId) {
        Course course = courses.findById(courseId).orElseThrow(NotFoundException::new);
        if (!courses.existsByIdAndStudentsId(courseId, studentId)) {
            course.getStudents().add(users.getReferenceById(studentId));
        }
    }

    /**
     * @action def contents，permission_classes=[IsAuthenticated, IsEnrolled]。
     * 课程不存在 → 404；存在但没选 → {@link NotEnrolledException}（403）。
     */
    public CourseWithContentsDto contents(Long courseId, Long studentId, String mediaUrl) {
        Course course = courses.findById(courseId).orElseThrow(NotFoundException::new);
        if (!courses.existsByIdAndStudentsId(courseId, studentId)) {
            throw new NotEnrolledException();
        }
        // 课程 → 模块 → 内容 → 条目：在事务内转换成 DTO，各层集合由批量抓取加载
        return CourseWithContentsDto.of(course, mediaUrl);
    }

    private Map<Long, List<String>> popularCourses(List<SubjectSummary> page) {
        if (page.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = page.stream().map(SubjectSummary::id).toList();
        return courses.popularCourses(ids).stream()
                .collect(Collectors.groupingBy(PopularCourse::subjectId,
                        Collectors.collectingAndThen(Collectors.toList(),
                                list -> list.stream().limit(3).map(PopularCourse::label).toList())));
    }

    private static SubjectDto toDto(SubjectSummary s, Map<Long, List<String>> popular) {
        return new SubjectDto(s.id(), s.title(), s.slug(), s.totalCourses(), popular.getOrDefault(s.id(), List.of()));
    }

    public static class NotEnrolledException extends RuntimeException {
        public NotEnrolledException() {
            super("You do not have permission to perform this action.");
        }
    }
}
