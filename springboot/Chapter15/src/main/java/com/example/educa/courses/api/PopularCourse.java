package com.example.educa.courses.api;

/** 查询结果的一行：某学科下的一门课程及其学生人数 */
public record PopularCourse(Long subjectId, String title, long totalStudents) {

    /** f'{c.title} ({c.total_students} students)' */
    public String label() {
        return title + " (" + totalStudents + " students)";
    }
}
