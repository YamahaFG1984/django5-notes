package com.example.bookmarks.common;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.geometry.Positions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 缩略图，对应 easy-thumbnails 的 {% thumbnail image.image 300x300 crop="smart" %}。
 * 和 easy-thumbnails 一样：第一次需要时生成文件并保存到 media/thumbs/ 下，之后直接复用。
 * height 为 0 表示“只限定宽度、按比例缩放”（≈ 300x0）。
 */
@Component
public class ThumbnailService {

    private static final Logger log = LoggerFactory.getLogger(ThumbnailService.class);

    private final MediaStorage media;

    public ThumbnailService(MediaStorage media) {
        this.media = media;
    }

    /** 返回缩略图的 URL；原图不存在或无法处理时返回 null（模板里据此不显示 img）。 */
    public String url(String relativePath, int width, int height, boolean crop) {
        if (relativePath == null || relativePath.isBlank()) {
            return null;
        }
        String thumbPath = thumbPath(relativePath, width, height, crop);
        Path source = media.resolve(relativePath);
        Path target = media.resolve(thumbPath);
        try {
            if (!Files.exists(target) && Files.exists(source)) {
                Files.createDirectories(target.getParent());
                var builder = Thumbnails.of(source.toFile());
                if (height <= 0) {
                    builder.width(width);
                } else if (crop) {
                    builder.size(width, height).crop(Positions.CENTER);   // 居中裁剪成正好 width x height
                } else {
                    builder.size(width, height);                          // 等比缩放到不超过这个尺寸
                }
                builder.toFile(target.toFile());
            }
        } catch (IOException e) {
            log.warn("无法生成缩略图 {}: {}", relativePath, e.getMessage());
            return null;
        }
        return Files.exists(target) ? "/media/" + thumbPath : null;
    }

    static String thumbPath(String relativePath, int width, int height, boolean crop) {
        int dot = relativePath.lastIndexOf('.');
        String base = dot > 0 ? relativePath.substring(0, dot) : relativePath;
        String ext = dot > 0 ? relativePath.substring(dot) : ".jpg";
        return "thumbs/" + base + "_" + width + "x" + height + (crop ? "_crop" : "") + ext;
    }
}
