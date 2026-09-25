package com.example.bookmarks.actions;

/**
 * 可以作为“动态目标”的实体（比如“alice 关注了 bob”里的 bob，“alice 收藏了图片”里的图片）。
 * Django 用 contenttypes 框架的 GenericForeignKey 指向任意模型；这里用“类型名 + id”两列来表达，
 * 类型名由实体自己提供。
 */
public interface ActionTarget {

    Long getId();

    /** 存进 actions_action.target_type 的值，比如 "image"、"user" */
    String targetType();
}
