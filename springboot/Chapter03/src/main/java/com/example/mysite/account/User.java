package com.example.mysite.account;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.CreationTimestamp;

/**
 * 用户表。Django 自带 auth.User，Spring Security 只定义“怎么认证”，
 * 用户存在哪张表、有哪些字段要自己建模 —— 这里刻意沿用 Django 的表名和字段名，方便对照。
 */
@Entity
@Table(name = "auth_user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 150)
    private String username;

    /** 存的是 {bcrypt}$2a$10$... 这样带算法前缀的哈希，而不是明文。 */
    @Column(nullable = false, length = 128)
    private String password;

    @Column(nullable = false, length = 254)
    private String email = "";

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    /** 能否进入管理后台，对应 Django 的 is_staff。 */
    @Column(name = "is_staff", nullable = false)
    private boolean staff;

    @Column(name = "is_superuser", nullable = false)
    private boolean superuser;

    @CreationTimestamp
    @Column(name = "date_joined", nullable = false, updatable = false)
    private OffsetDateTime dateJoined;

    protected User() {
        // JPA 需要一个无参构造器
    }

    public User(String username, String password, String email) {
        this.username = username;
        this.password = password;
        this.email = email;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public boolean isActive() {
        return active;
    }

    public boolean isStaff() {
        return staff;
    }

    public void setStaff(boolean staff) {
        this.staff = staff;
    }

    public boolean isSuperuser() {
        return superuser;
    }

    public void setSuperuser(boolean superuser) {
        this.superuser = superuser;
    }

    public OffsetDateTime getDateJoined() {
        return dateJoined;
    }

    /** 模板里直接输出 ${post.author} 时显示用户名，相当于 Django 的 __str__。 */
    @Override
    public String toString() {
        return username;
    }
}
