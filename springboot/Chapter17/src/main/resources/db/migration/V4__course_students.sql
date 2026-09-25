-- Course.students = ManyToManyField(User, related_name='courses_joined', blank=True)
CREATE TABLE courses_course_students (
    course_id  BIGINT NOT NULL REFERENCES courses_course (id) ON DELETE CASCADE,
    user_id    BIGINT NOT NULL REFERENCES auth_user (id) ON DELETE CASCADE,
    PRIMARY KEY (course_id, user_id)
);
CREATE INDEX courses_course_students_user_idx ON courses_course_students (user_id);
