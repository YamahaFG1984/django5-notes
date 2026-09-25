package com.example.educa.courses.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class VideoEmbedTests {

    @Test
    void recognisesYoutubeAndVimeo() {
        assertThat(VideoEmbed.embedUrl("https://www.youtube.com/watch?v=bgV39DlmZ2U&t=10"))
                .isEqualTo("https://www.youtube.com/embed/bgV39DlmZ2U");
        assertThat(VideoEmbed.embedUrl("https://youtu.be/bgV39DlmZ2U"))
                .isEqualTo("https://www.youtube.com/embed/bgV39DlmZ2U");
        assertThat(VideoEmbed.embedUrl("https://m.youtube.com/shorts/bgV39DlmZ2U"))
                .isEqualTo("https://www.youtube.com/embed/bgV39DlmZ2U");
        assertThat(VideoEmbed.embedUrl("https://vimeo.com/76979871"))
                .isEqualTo("https://player.vimeo.com/video/76979871");
    }

    @Test
    void unknownOrMalformedUrlsAreNotEmbedded() {
        assertThat(VideoEmbed.embedUrl("https://example.com/watch?v=bgV39DlmZ2U")).isNull();
        assertThat(VideoEmbed.embedUrl("https://www.youtube.com/watch?v=\"><script>")).isNull();
        assertThat(VideoEmbed.embedUrl("https://vimeo.com/channels/staffpicks")).isNull();
        assertThat(VideoEmbed.embedUrl("not a url")).isNull();
    }
}
