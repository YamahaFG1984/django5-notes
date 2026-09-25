package com.example.bookmarks.account;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.CreationTimestamp;

import com.example.bookmarks.actions.ActionTarget;

/** 用户，字段与 Django 的 auth.User 对应。 */
@Entity
@Table(name = "auth_user")
public class User implements ActionTarget {

    public static final String TARGET_TYPE = "user";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 150)
    private String username;

    @Column(nullable = false, length = 128)
    private String password;

    @Column(name = "first_name", nullable = false, length = 150)
    private String firstName = "";

    @Column(name = "last_name", nullable = false, length = 150)
    private String lastName = "";

    @Column(nullable = false, length = 254)
    private String email = "";

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "is_staff", nullable = false)
    private boolean staff;

    @Column(name = "is_superuser", nullable = false)
    private boolean superuser;

    /** 每次登录成功时更新（见 LastLoginListener）；密码重置令牌也用到它。 */
    @Column(name = "last_login")
    private OffsetDateTime lastLogin;

    @CreationTimestamp
    @Column(name = "date_joined", nullable = false, updatable = false)
    private OffsetDateTime dateJoined;

    protected User() {
    }

    public User(String username, String password, String email) {
        this.username = username;
        this.password = password;
        this.email = email == null ? "" : email;
    }

    /** ABSOLUTE_URL_OVERRIDES = {'auth.user': lambda u: reverse_lazy('user_detail', args=[u.username])} */
    public String getAbsoluteUrl() {
        return "/account/users/" + username + "/";
    }

    @Override
    public String targetType() {
        return TARGET_TYPE;
    }

    /** get_full_name() */
    public String getFullName() {
        return (firstName + " " + lastName).strip();
    }

    /** {{ user.first_name|default:user.username }} */
    public String getDisplayName() {
        return firstName.isBlank() ? username : firstName;
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

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName == null ? "" : firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName == null ? "" : lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email == null ? "" : email;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
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

    public OffsetDateTime getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(OffsetDateTime lastLogin) {
        this.lastLogin = lastLogin;
    }

    public OffsetDateTime getDateJoined() {
        return dateJoined;
    }

    @Override
    public String toString() {
        return username;
    }
}
