package com.example.educa.courses.manage;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.springframework.web.multipart.MultipartFile;

import com.example.educa.courses.FileItem;
import com.example.educa.courses.ImageItem;
import com.example.educa.courses.Item;
import com.example.educa.courses.TextItem;
import com.example.educa.courses.VideoItem;

/**
 * 内容条目的表单。Django 用 modelform_factory(model, exclude=[...]) 在运行时为 Text/Video/Image/File 动态生成表单；
 * Java 是静态类型语言，这里用一个包含所有可能字段的类，再按 modelName 决定哪些字段必填、模板显示哪些字段。
 */
public class ItemForm {

    @NotBlank
    @Size(max = 250)
    private String title;

    /** text */
    private String content = "";

    /** video */
    @Size(max = 200)
    private String url = "";

    /** image / file */
    private MultipartFile file;

    public static ItemForm of(Item item) {
        ItemForm form = new ItemForm();
        form.title = item.getTitle();
        if (item instanceof TextItem text) {
            form.content = text.getContent();
        } else if (item instanceof VideoItem video) {
            form.url = video.getUrl();
        }
        return form;
    }

    public boolean hasFile() {
        return file != null && !file.isEmpty();
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content == null ? "" : content;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url == null ? "" : url;
    }

    public MultipartFile getFile() {
        return file;
    }

    public void setFile(MultipartFile file) {
        this.file = file;
    }

    /** 用于模板：编辑已有图片/文件时显示当前文件 */
    public static String currentFile(Item item) {
        if (item instanceof FileItem f) {
            return f.getFile();
        }
        if (item instanceof ImageItem i) {
            return i.getFile();
        }
        return null;
    }
}
