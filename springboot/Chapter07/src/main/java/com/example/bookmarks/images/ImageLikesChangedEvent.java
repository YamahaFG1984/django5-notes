package com.example.bookmarks.images;

/**
 * “某张图片的点赞发生了变化”这一事件。
 * Django 在 users_like 变化时自动发出 m2m_changed 信号；Spring 没有自动信号，
 * 由业务代码显式发布事件，监听者（ImageLikesListener）负责后续处理。
 */
public record ImageLikesChangedEvent(Long imageId) {
}
