package com.example.educa.courses;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

import com.example.educa.account.User;

/** 文字，≈ class Text(ItemBase)。表 courses_text 的主键同时是指向 courses_item 的外键。 */
@Entity
@Table(name = "courses_text")
@DiscriminatorValue("text")
@PrimaryKeyJoinColumn(name = "item_id")
public class TextItem extends Item {

    @Column(name = "content", nullable = false, columnDefinition = "text")
    private String content;

    protected TextItem() {
    }

    public TextItem(User owner, String title, String content) {
        super(owner, title);
        this.content = content;
    }

    @Override
    public String getModelName() {
        return "text";
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
