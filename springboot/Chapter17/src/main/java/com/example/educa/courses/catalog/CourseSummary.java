package com.example.educa.courses.catalog;

import java.io.Serializable;

/** 课程列表里的一行：课程、学科、讲师和模块数。 */
public record CourseSummary(Long id, String title, String slug, String subjectTitle, String subjectSlug,
                            String ownerUsername, String ownerFirstName, String ownerLastName,
                            long totalModules) implements Serializable {

    /** ≈ owner.get_full_name；名字为空时退回用户名 */
    public String instructor() {
        String full = (ownerFirstName + " " + ownerLastName).strip();
        return full.isEmpty() ? ownerUsername : full;
    }
}
