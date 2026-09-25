package com.example.bookmarks.images;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.example.bookmarks.common.MediaStorage;
import com.example.bookmarks.common.ThumbnailService;
import com.example.bookmarks.config.BookmarksProperties;

class ThumbnailServiceTests {

    @TempDir
    Path mediaRoot;

    @Test
    void cropAndWidthOnlyThumbnails() throws Exception {
        MediaStorage media = new MediaStorage(new BookmarksProperties("x", "k", mediaRoot, Duration.ofDays(1)));
        BufferedImage wide = new BufferedImage(800, 400, BufferedImage.TYPE_INT_RGB);
        Path original = mediaRoot.resolve("images/wide.png");
        Files.createDirectories(original.getParent());
        ImageIO.write(wide, "png", original.toFile());

        ThumbnailService thumbnails = new ThumbnailService(media);
        String cropped = thumbnails.url("images/wide.png", 300, 300, true);
        assertThat(cropped).isEqualTo("/media/thumbs/images/wide_300x300_crop.png");
        BufferedImage square = ImageIO.read(mediaRoot.resolve("thumbs/images/wide_300x300_crop.png").toFile());
        assertThat(square.getWidth()).isEqualTo(300);
        assertThat(square.getHeight()).isEqualTo(300);

        thumbnails.url("images/wide.png", 300, 0, false);
        BufferedImage scaled = ImageIO.read(mediaRoot.resolve("thumbs/images/wide_300x0.png").toFile());
        assertThat(scaled.getWidth()).isEqualTo(300);
        assertThat(scaled.getHeight()).isEqualTo(150);

        assertThat(thumbnails.url("images/missing.png", 100, 100, true)).isNull();
        assertThat(thumbnails.url("", 100, 100, true)).isNull();
    }
}
