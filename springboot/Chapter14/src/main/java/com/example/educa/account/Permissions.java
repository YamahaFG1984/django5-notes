package com.example.educa.account;

import java.util.List;

/**
 * 权限代码。Django 为每个模型自动生成 add_、change_、delete_、view_ 四个权限；
 * 这里把 courses 应用用到的列出来（迁移脚本 V1 里也插入了同样的数据）。
 */
public final class Permissions {

    public static final String ADD_COURSE = "courses.add_course";
    public static final String CHANGE_COURSE = "courses.change_course";
    public static final String DELETE_COURSE = "courses.delete_course";
    public static final String VIEW_COURSE = "courses.view_course";

    /** 超级用户隐式拥有全部权限（≈ user.is_superuser 时 has_perm 总是 True） */
    public static final List<String> ALL = List.of(
            ADD_COURSE, CHANGE_COURSE, DELETE_COURSE, VIEW_COURSE,
            "courses.add_module", "courses.change_module", "courses.delete_module", "courses.view_module",
            "courses.add_content", "courses.change_content", "courses.delete_content", "courses.view_content");

    private Permissions() {
    }
}
