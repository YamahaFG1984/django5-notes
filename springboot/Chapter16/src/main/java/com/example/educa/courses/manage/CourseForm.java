package com.example.educa.courses.manage;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.example.educa.courses.Course;

/** OwnerCourseMixin.fields = ['subject', 'title', 'slug', 'overview'] */
public class CourseForm {

    @NotNull
    private Long subjectId;

    @NotBlank @Size(max = 200)
    private String title;

    @NotBlank @Size(max = 200) @Pattern(regexp = "[-a-zA-Z0-9_]+")
    private String slug;

    @NotBlank
    private String overview;

    public static CourseForm of(Course course) {
        CourseForm form = new CourseForm();
        form.subjectId = course.getSubject().getId();
        form.title = course.getTitle();
        form.slug = course.getSlug();
        form.overview = course.getOverview();
        return form;
    }

    public Long getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(Long subjectId) {
        this.subjectId = subjectId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getOverview() {
        return overview;
    }

    public void setOverview(String overview) {
        this.overview = overview;
    }
}
