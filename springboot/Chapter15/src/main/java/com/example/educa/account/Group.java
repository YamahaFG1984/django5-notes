package com.example.educa.account;

import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

/**
 * 用户组，≈ django.contrib.auth.models.Group。
 * Django 的权限是一张 auth_permission 表（每个模型自动生成 add/change/delete/view 四个权限）；
 * 这里简化为直接保存权限代码字符串，比如 "courses.add_course" —— 它会原样成为 Spring Security 的一个 authority。
 */
@Entity
@Table(name = "auth_group")
public class Group {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 150)
    private String name;

    @ElementCollection
    @CollectionTable(name = "auth_group_permissions", joinColumns = @JoinColumn(name = "group_id"))
    @Column(name = "codename", nullable = false, length = 100)
    private Set<String> permissions = new LinkedHashSet<>();

    protected Group() {
    }

    public Group(String name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Set<String> getPermissions() {
        return permissions;
    }
}
