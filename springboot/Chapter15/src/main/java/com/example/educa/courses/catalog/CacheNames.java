package com.example.educa.courses.catalog;

/** 缓存名集中定义，避免在注解里到处写字符串。 */
public final class CacheNames {

    /** 学科列表 + 每个学科的课程数（书中的 'all_subjects'） */
    public static final String SUBJECTS = "subjects";

    /** 课程列表；key 为学科 slug 或 'all'（书中的 'all_courses' / f'subject_{id}_courses'） */
    public static final String COURSES = "courses";

    /** 某个模块的内容；key 为模块 id（书中的 {% cache 600 module_contents module %}） */
    public static final String MODULE_CONTENTS = "moduleContents";

    private CacheNames() {
    }
}
