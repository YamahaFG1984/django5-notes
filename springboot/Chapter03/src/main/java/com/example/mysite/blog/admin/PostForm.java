package com.example.mysite.blog.admin;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import org.springframework.format.annotation.DateTimeFormat;

import com.example.mysite.blog.Post;
import com.example.mysite.blog.PostStatus;

/**
 * 后台“编辑文章”表单的数据对象（form backing object）。
 * Django admin 会根据模型自动生成表单；Spring 里要自己写一个类，用 Bean Validation 注解声明校验规则。
 */
public class PostForm {

    @NotBlank
    @Size(max = 250)
    private String title;

    /** SlugField：只允许字母、数字、下划线和连字符。 */
    @NotBlank
    @Size(max = 250)
    @Pattern(regexp = "[-a-zA-Z0-9_]+", message = "只能包含字母、数字、下划线或连字符")
    private String slug;

    @NotNull
    private Long authorId;

    @NotBlank
    private String body;

    /** HTML 的 datetime-local 控件提交 2025-01-31T09:30 这样的格式；这里约定按 UTC 解释。 */
    @NotNull
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime publish = LocalDateTime.now(ZoneOffset.UTC).withSecond(0).withNano(0);

    @NotNull
    private PostStatus status = PostStatus.DRAFT;

    /** 逗号分隔的标签，和 django-taggit 在 admin 里的输入方式一样：“music, jazz” */
    @Size(max = 500)
    private String tags = "";

    public static PostForm of(Post post) {
        PostForm form = new PostForm();
        form.title = post.getTitle();
        form.slug = post.getSlug();
        form.authorId = post.getAuthor().getId();
        form.body = post.getBody();
        form.publish = post.getPublish().withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
        form.status = post.getStatus();
        form.tags = post.getTags().stream().map(t -> t.getName()).collect(Collectors.joining(", "));
        return form;
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

    public Long getAuthorId() {
        return authorId;
    }

    public void setAuthorId(Long authorId) {
        this.authorId = authorId;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public LocalDateTime getPublish() {
        return publish;
    }

    public void setPublish(LocalDateTime publish) {
        this.publish = publish;
    }

    /** "music, Jazz ,, music" → [music, Jazz]（去空白、去重，忽略大小写） */
    public List<String> tagNames() {
        return Arrays.stream(tags == null ? new String[0] : tags.split(","))
                .map(String::strip)
                .filter(name -> !name.isEmpty())
                .collect(Collectors.toMap(String::toLowerCase, name -> name, (a, b) -> a, java.util.LinkedHashMap::new))
                .values().stream().toList();
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public PostStatus getStatus() {
        return status;
    }

    public void setStatus(PostStatus status) {
        this.status = status;
    }
}
