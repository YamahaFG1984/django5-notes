package com.example.bookmarks.images;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import com.example.bookmarks.images.ImageDownloader.DownloadException;

/** SSRF 防护的单元测试：不发任何网络请求（只做 URL 和地址检查）。 */
class ImageDownloaderTests {

    private final ImageDownloader downloader = new ImageDownloader(RestClient.builder(), true);

    @Test
    void rejectsNonHttpSchemes() {
        assertThatThrownBy(() -> downloader.checkUrl("file:///etc/passwd.png")).isInstanceOf(DownloadException.class);
        assertThatThrownBy(() -> downloader.checkUrl("ftp://example.com/a.png")).isInstanceOf(DownloadException.class);
        assertThatThrownBy(() -> downloader.checkUrl("not a url")).isInstanceOf(DownloadException.class);
    }

    @Test
    void rejectsLoopbackAndPrivateAddresses() {
        for (String url : new String[] {"http://127.0.0.1/a.png", "http://localhost:8080/a.png",
                "http://10.0.0.5/a.png", "http://192.168.1.1/a.png", "http://169.254.169.254/latest/meta-data.png"}) {
            assertThatThrownBy(() -> downloader.checkUrl(url)).as(url).isInstanceOf(DownloadException.class);
        }
    }

    @Test
    void canBeDisabledForLocalDevelopment() {
        ImageDownloader permissive = new ImageDownloader(RestClient.builder(), false);
        assertThat(permissive.checkUrl("http://127.0.0.1/a.png").getHost()).isEqualTo("127.0.0.1");
    }
}
