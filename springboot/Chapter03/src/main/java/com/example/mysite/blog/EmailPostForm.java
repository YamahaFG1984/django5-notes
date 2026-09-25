package com.example.mysite.blog;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 对应 Django 的 {@code class EmailPostForm(forms.Form)}。
 * Django 的 Form 同时负责“字段定义 + 校验 + 渲染”；Spring 里它只是一个带校验注解的普通类，
 * 渲染交给模板里的 th:field，校验交给控制器参数上的 @Valid。
 */
public class EmailPostForm {

    @NotBlank
    @Size(max = 25)
    private String name;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Email
    private String to;

    /** required=False, widget=forms.Textarea */
    @Size(max = 2000)
    private String comments = "";

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }
}
