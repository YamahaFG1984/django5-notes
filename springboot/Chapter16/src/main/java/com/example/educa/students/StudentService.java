package com.example.educa.students;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.educa.account.User;
import com.example.educa.account.UserRepository;
import com.example.educa.common.NotFoundException;
import com.example.educa.courses.Course;
import com.example.educa.courses.CourseRepository;
import com.example.educa.courses.Module;

@Service
@Transactional
public class StudentService {

    private final UserRepository users;
    private final CourseRepository courses;
    private final PasswordEncoder passwordEncoder;

    public StudentService(UserRepository users, CourseRepository courses, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.courses = courses;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public boolean usernameTaken(String username) {
        return users.findByUsername(username).isPresent();
    }

    /** UserCreationForm.save()：密码用 PasswordEncoder 做哈希后保存 */
    public User register(RegistrationForm form) {
        return users.save(new User(form.getUsername(), passwordEncoder.encode(form.getPassword1()), ""));
    }

    /** self.course.students.add(self.request.user)；已经选过就什么都不做（和 ManyToMany.add 一样幂等） */
    public void enroll(Long courseId, Long studentId) {
        Course course = courses.findById(courseId).orElseThrow(NotFoundException::new);
        if (!courses.existsByIdAndStudentsId(courseId, studentId)) {
            course.getStudents().add(users.getReferenceById(studentId));
        }
    }

    @Transactional(readOnly = true)
    public List<Course> myCourses(Long studentId) {
        return courses.findByStudentsIdOrderByCreatedDesc(studentId);
    }

    /** 学生只能打开自己选了的课程（书中用 qs.filter(students__in=[user])） */
    @Transactional(readOnly = true)
    public CoursePage coursePage(Long courseId, Long moduleId, Long studentId) {
        Course course = courses.findByIdAndStudentsId(courseId, studentId).orElseThrow(NotFoundException::new);
        List<Module> modules = course.getModules();
        Module module;
        if (moduleId == null) {
            // 书中写的是 course.modules.all()[0]：课程还没有模块时会抛 IndexError（500）
            module = modules.isEmpty() ? null : modules.getFirst();
        } else {
            // 书中是 course.modules.get(id=...)：id 不属于这门课时抛 DoesNotExist（500）；这里返回 404
            module = modules.stream().filter(m -> m.getId().equals(moduleId)).findFirst()
                    .orElseThrow(NotFoundException::new);
        }
        return new CoursePage(course, modules, module);
    }

    public record CoursePage(Course course, List<Module> modules, Module module) {
    }
}
