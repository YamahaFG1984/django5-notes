package com.example.educa.common;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.springframework.stereotype.Component;

import com.example.educa.config.EducaProperties;

/**
 * 把上传的文件保存到 MEDIA_ROOT 下，返回相对路径（存进数据库），相当于 Django 的 FileSystemStorage。
 * upload_to='users/%Y/%m/%d/' → saveImage("users", ...) 会保存到 users/2025/01/31/xxx.jpg。
 */
@Component
public class MediaStorage {

    private static final DateTimeFormatter DATE_DIRS = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private final Path root;

    public MediaStorage(EducaProperties properties) {
        this.root = properties.mediaRoot().toAbsolutePath().normalize();
    }

    /**
     * 校验内容确实是图片（≈ ImageField 用 Pillow 做的检查），再按日期目录保存。
     * 文件名只保留安全字符，并加上随机后缀避免覆盖同名文件。
     */
    public String saveImage(String uploadTo, String filename, byte[] content) {
        if (!isImage(content)) {
            throw new InvalidImageException("Upload a valid image. The file you uploaded was either not an image or a corrupted image.");
        }
        String safeName = sanitize(filename);
        String relative = uploadTo + "/" + LocalDate.now().format(DATE_DIRS) + "/" + safeName;
        Path target = root.resolve(relative).normalize();
        if (!target.startsWith(root)) {   // 防止 ../ 之类的路径穿越
            throw new InvalidImageException("Invalid file name.");
        }
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, content);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return relative;
    }

    /** 普通文件（≈ FileField）：不校验内容，只清洗文件名并防止路径穿越 */
    public String saveFile(String uploadTo, String filename, byte[] content) {
        String relative = uploadTo + "/" + sanitize(filename);
        Path target = root.resolve(relative).normalize();
        if (!target.startsWith(root)) {
            throw new InvalidImageException("Invalid file name.");
        }
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, content);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return relative;
    }

    public Path resolve(String relative) {
        return root.resolve(relative).normalize();
    }

    public static boolean isImage(byte[] content) {
        try {
            return content != null && content.length > 0
                    && ImageIO.read(new ByteArrayInputStream(content)) != null;
        } catch (IOException e) {
            return false;
        }
    }

    private static String sanitize(String filename) {
        String name = filename == null ? "upload" : Path.of(filename).getFileName().toString();
        int dot = name.lastIndexOf('.');
        String base = dot > 0 ? name.substring(0, dot) : name;
        String ext = dot > 0 ? name.substring(dot + 1).toLowerCase(Locale.ROOT) : "jpg";
        base = base.replaceAll("[^A-Za-z0-9_-]", "_");
        if (base.isBlank()) {
            base = "upload";
        }
        return base + "_" + UUID.randomUUID().toString().substring(0, 8) + "." + ext.replaceAll("[^a-z0-9]", "");
    }

    public static class InvalidImageException extends RuntimeException {
        public InvalidImageException(String message) {
            super(message);
        }
    }
}
