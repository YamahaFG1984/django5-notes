package com.example.bookmarks.account;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/**
 * 用户资料，和 User 一对一（≈ OneToOneField(settings.AUTH_USER_MODEL)）。
 * “扩展用户模型”的这种做法在两个框架里一模一样：另建一张表，用唯一外键指回用户。
 */
@Entity
@Table(name = "account_profile")
public class Profile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    /** ImageField 在数据库里其实只存相对路径，比如 users/2025/01/31/me_1a2b3c4d.jpg。 */
    @Column(nullable = false, length = 100)
    private String photo = "";

    protected Profile() {
    }

    public Profile(User user) {
        this.user = user;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getPhoto() {
        return photo;
    }

    public void setPhoto(String photo) {
        this.photo = photo == null ? "" : photo;
    }

    /** {{ profile.photo.url }} */
    public String getPhotoUrl() {
        return photo.isEmpty() ? null : "/media/" + photo;
    }

    @Override
    public String toString() {
        return "Profile of " + user.getUsername();
    }
}
