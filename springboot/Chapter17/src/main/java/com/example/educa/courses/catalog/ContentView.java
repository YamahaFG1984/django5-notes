package com.example.educa.courses.catalog;

import java.io.Serializable;

import org.hibernate.Hibernate;

import com.example.educa.courses.FileItem;
import com.example.educa.courses.ImageItem;
import com.example.educa.courses.Item;
import com.example.educa.courses.TextItem;
import com.example.educa.courses.VideoItem;

/**
 * 学生看到的一项内容。书中给每个条目加了 render() 方法，用 render_to_string 渲染 courses/content/&lt;model_name&gt;.html；
 * 这里先把条目转换成可缓存的 DTO，模板再按 modelName 选择对应的片段来渲染。
 *
 * @param embedUrl 视频的嵌入地址（能识别 YouTube / Vimeo 时才有），≈ django-embed-video 的 {% video %}
 */
public record ContentView(String title, String modelName, String text, String url, String file, String embedUrl)
        implements Serializable {

    public static ContentView of(Item item) {
        // 懒加载的 item 可能是 Hibernate 代理（Item 的子类，而不是 TextItem 等），先取出真实对象再做类型匹配
        return switch (Hibernate.unproxy(item)) {
            case TextItem t -> new ContentView(t.getTitle(), "text", t.getContent(), null, null, null);
            case VideoItem v -> new ContentView(v.getTitle(), "video", null, v.getUrl(), null, VideoEmbed.embedUrl(v.getUrl()));
            case ImageItem i -> new ContentView(i.getTitle(), "image", null, null, i.getFile(), null);
            case FileItem f -> new ContentView(f.getTitle(), "file", null, null, f.getFile(), null);
            case Object other -> throw new IllegalArgumentException("Unknown item " + other);
        };
    }
}
