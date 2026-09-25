package com.example.bookmarks.account;

import java.time.LocalDate;

import jakarta.validation.constraints.Past;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

/**
 * class ProfileEditForm(forms.ModelForm): fields = ['date_of_birth', 'photo']
 * 上传的文件绑定为 MultipartFile（≈ request.FILES['photo']）；不选文件时它是空的，表示“保持原图”。
 */
public class ProfileEditForm {

    @Past
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateOfBirth;

    private MultipartFile photo;

    public static ProfileEditForm of(Profile profile) {
        ProfileEditForm form = new ProfileEditForm();
        form.dateOfBirth = profile.getDateOfBirth();
        return form;
    }

    public boolean hasNewPhoto() {
        return photo != null && !photo.isEmpty();
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public MultipartFile getPhoto() {
        return photo;
    }

    public void setPhoto(MultipartFile photo) {
        this.photo = photo;
    }
}
