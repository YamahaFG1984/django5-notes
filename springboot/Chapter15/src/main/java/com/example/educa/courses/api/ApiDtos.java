package com.example.educa.courses.api;

import java.time.OffsetDateTime;
import java.util.List;

import org.hibernate.Hibernate;

import com.example.educa.courses.Content;
import com.example.educa.courses.Course;
import com.example.educa.courses.FileItem;
import com.example.educa.courses.ImageItem;
import com.example.educa.courses.Item;
import com.example.educa.courses.Module;
import com.example.educa.courses.TextItem;
import com.example.educa.courses.VideoItem;

/**
 * API 返回的数据结构，≈ courses/api/serializers.py。
 * DRF 的 ModelSerializer 根据模型自动生成字段；这里用 record 显式声明，每个 record 对应一个 Serializer。
 * 字段名是驼峰，输出 JSON 时由 spring.jackson.property-naming-strategy=SNAKE_CASE 转成 total_courses 这样的下划线风格。
 */
public final class ApiDtos {

    private ApiDtos() {
    }

    /** SubjectSerializer */
    public record SubjectDto(Long id, String title, String slug, long totalCourses, List<String> popularCourses) {
    }

    /** ModuleSerializer */
    public record ModuleDto(int order, String title, String description) {

        static ModuleDto of(Module m) {
            return new ModuleDto(m.getOrder(), m.getTitle(), m.getDescription());
        }
    }

    /** CourseSerializer：subject、owner 默认序列化成主键（PrimaryKeyRelatedField） */
    public record CourseDto(Long id, Long subject, String title, String slug, String overview,
                            OffsetDateTime created, Long owner, List<ModuleDto> modules) {

        public static CourseDto of(Course c) {
            return new CourseDto(c.getId(), c.getSubject().getId(), c.getTitle(), c.getSlug(), c.getOverview(),
                    c.getCreated(), c.getOwner().getId(), c.getModules().stream().map(ModuleDto::of).toList());
        }
    }

    /**
     * 条目。书中的 ItemRelatedField 调用 item.render()，把渲染好的 HTML 字符串放进 JSON；
     * 这里返回结构化数据（类型 + 各自的字段），客户端想怎么显示由它自己决定。
     */
    public record ItemDto(String type, String title, String content, String url, String file) {

        static ItemDto of(Item item, String mediaUrl) {
            // item 是懒加载的一对一关联，拿到的是 Hibernate 代理；unproxy 后才能按具体子类匹配
            return switch (Hibernate.unproxy(item)) {
                case TextItem t -> new ItemDto("text", t.getTitle(), t.getContent(), null, null);
                case VideoItem v -> new ItemDto("video", v.getTitle(), null, v.getUrl(), null);
                case ImageItem i -> new ItemDto("image", i.getTitle(), null, null, mediaUrl + i.getFile());
                case FileItem f -> new ItemDto("file", f.getTitle(), null, null, mediaUrl + f.getFile());
                case Object other -> throw new IllegalArgumentException("Unknown item " + other);
            };
        }
    }

    /** ContentSerializer */
    public record ContentDto(int order, ItemDto item) {

        static ContentDto of(Content c, String mediaUrl) {
            return new ContentDto(c.getOrder(), ItemDto.of(c.getItem(), mediaUrl));
        }
    }

    /** ModuleWithContentsSerializer */
    public record ModuleWithContentsDto(int order, String title, String description, List<ContentDto> contents) {

        static ModuleWithContentsDto of(Module m, String mediaUrl) {
            return new ModuleWithContentsDto(m.getOrder(), m.getTitle(), m.getDescription(),
                    m.getContents().stream().map(c -> ContentDto.of(c, mediaUrl)).toList());
        }
    }

    /** CourseWithContentsSerializer */
    public record CourseWithContentsDto(Long id, Long subject, String title, String slug, String overview,
                                        OffsetDateTime created, Long owner, List<ModuleWithContentsDto> modules) {

        static CourseWithContentsDto of(Course c, String mediaUrl) {
            return new CourseWithContentsDto(c.getId(), c.getSubject().getId(), c.getTitle(), c.getSlug(),
                    c.getOverview(), c.getCreated(), c.getOwner().getId(),
                    c.getModules().stream().map(m -> ModuleWithContentsDto.of(m, mediaUrl)).toList());
        }
    }
}
