package com.example.mysite.blog;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 对应 Django 的 {@code class CommentForm(forms.ModelForm)}，fields = ['name', 'email', 'body']。
 * Spring 没有 ModelForm：字段和校验规则要手写一遍，再由 {@link #toComment(Post)} 转成实体
 * （≈ form.save(commit=False) 后再补上 comment.post）。
 */
public class CommentForm {

    @NotBlank
    @Size(max = 80)
    private String name;

    @NotBlank
    @Email
    @Size(max = 254)
    private String email;

    @NotBlank
    private String body;

    public Comment toComment(Post post) {
        return new Comment(post, name, email, body);
    }

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

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }
}
