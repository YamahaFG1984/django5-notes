package com.example.bookmarks;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/** 项目二：图片书签社交网站（≈ Django 的 bookmarks 项目）。 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class BookmarksApplication {

    public static void main(String[] args) {
        SpringApplication.run(BookmarksApplication.class, args);
    }
}
