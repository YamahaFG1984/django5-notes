package com.example.educa.courses;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

import com.example.educa.account.User;

/** 文件（MEDIA_ROOT 下的相对路径），≈ class File(ItemBase)。表 courses_file 的主键同时是指向 courses_item 的外键。 */
@Entity
@Table(name = "courses_file")
@DiscriminatorValue("file")
@PrimaryKeyJoinColumn(name = "item_id")
public class FileItem extends Item {

    @Column(name = "file", nullable = false, length = 200)
    private String file;

    protected FileItem() {
    }

    public FileItem(User owner, String title, String file) {
        super(owner, title);
        this.file = file;
    }

    @Override
    public String getModelName() {
        return "file";
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }
}
