package com.example.educa.config;

import java.nio.file.Path;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** @param mediaRoot 上传文件目录，≈ MEDIA_ROOT */
@ConfigurationProperties(prefix = "educa")
public record EducaProperties(@DefaultValue("media") Path mediaRoot) {
}
