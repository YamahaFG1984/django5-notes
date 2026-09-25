package com.example.educa.courses;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

import com.example.educa.account.User;

/** 视频链接，≈ class Video(ItemBase)。表 courses_video 的主键同时是指向 courses_item 的外键。 */
@Entity
@Table(name = "courses_video")
@DiscriminatorValue("video")
@PrimaryKeyJoinColumn(name = "item_id")
public class VideoItem extends Item {

    @Column(name = "url", nullable = false, length = 200)
    private String url;

    protected VideoItem() {
    }

    public VideoItem(User owner, String title, String url) {
        super(owner, title);
        this.url = url;
    }

    @Override
    public String getModelName() {
        return "video";
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
