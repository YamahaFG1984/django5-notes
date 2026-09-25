package com.example.bookmarks.images;

import java.util.Locale;
import java.util.Set;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * class ImageCreateForm(forms.ModelForm): fields = ['title', 'url', 'description']，url 是隐藏字段。
 * clean_url 的“扩展名必须是 jpg/jpeg/png”写成一个 @AssertTrue 方法：
 * 方法名 isUrlExtensionValid → 错误挂在属性 urlExtensionValid 上。
 */
public class ImageCreateForm {

    static final Set<String> VALID_EXTENSIONS = Set.of("jpg", "jpeg", "png");

    @NotBlank
    @Size(max = 200)
    private String title;

    @NotBlank
    @Size(max = 2000)
    private String url;

    private String description = "";

    @AssertTrue(message = "The given URL does not match valid image extensions.")
    public boolean isUrlExtensionValid() {
        return url == null || url.isBlank() || VALID_EXTENSIONS.contains(extension());
    }

    /** url.rsplit('.', 1)[1].lower()，并去掉查询参数 */
    public String extension() {
        String path = url.split("[?#]", 2)[0];
        int dot = path.lastIndexOf('.');
        return dot < 0 ? "" : path.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
