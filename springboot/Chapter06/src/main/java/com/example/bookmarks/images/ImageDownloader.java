package com.example.bookmarks.images;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * 下载用户提交的图片地址，对应书中 ImageCreateForm.save() 里的 requests.get(image_url)。
 * 书中直接请求任意 URL，存在 SSRF（服务端请求伪造）风险：攻击者可以让服务器去访问内网地址。
 * 这里补上几道防线：只允许 http/https、拒绝内网和本机地址、不跟随重定向、设置超时和大小上限。
 */
@Component
public class ImageDownloader {

    static final int MAX_BYTES = 5 * 1024 * 1024;

    private final RestClient restClient;
    private final boolean blockPrivateNetworks;

    public ImageDownloader(RestClient.Builder builder,
                           @Value("${bookmarks.download.block-private-networks:true}") boolean blockPrivateNetworks) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NEVER)   // 重定向可能把请求“拐”到内网地址
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(10));
        this.restClient = builder.requestFactory(requestFactory).build();
        this.blockPrivateNetworks = blockPrivateNetworks;
    }

    public byte[] download(String url) {
        URI uri = checkUrl(url);
        try {
            return restClient.get().uri(uri).exchange((request, response) -> {
                if (!response.getStatusCode().is2xxSuccessful()) {
                    throw new DownloadException("The image could not be downloaded (HTTP " + response.getStatusCode().value() + ").");
                }
                try (InputStream body = response.getBody()) {
                    byte[] bytes = body.readNBytes(MAX_BYTES + 1);
                    if (bytes.length > MAX_BYTES) {
                        throw new DownloadException("The image is too large.");
                    }
                    return bytes;
                }
            });
        } catch (RestClientException e) {
            throw new DownloadException("The image could not be downloaded.");
        }
    }

    URI checkUrl(String url) {
        URI uri;
        try {
            uri = URI.create(url);
        } catch (IllegalArgumentException e) {
            throw new DownloadException("Enter a valid URL.");
        }
        if (!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme())
                || uri.getHost() == null) {
            throw new DownloadException("Only http and https URLs are allowed.");
        }
        if (blockPrivateNetworks) {
            try {
                for (InetAddress address : InetAddress.getAllByName(uri.getHost())) {
                    if (address.isLoopbackAddress() || address.isSiteLocalAddress() || address.isLinkLocalAddress()
                            || address.isAnyLocalAddress() || address.isMulticastAddress()) {
                        throw new DownloadException("Downloading from this address is not allowed.");
                    }
                }
            } catch (UnknownHostException e) {
                throw new DownloadException("Unknown host.");
            }
        }
        return uri;
    }

    public static class DownloadException extends RuntimeException {
        public DownloadException(String message) {
            super(message);
        }
    }
}
