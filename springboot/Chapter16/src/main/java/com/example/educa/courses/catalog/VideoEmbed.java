package com.example.educa.courses.catalog;

import java.net.URI;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * 把视频页面地址转换成可嵌入 iframe 的播放器地址，替代 django-embed-video。
 * 只认识 YouTube 和 Vimeo；其他地址返回 null，模板里退化为一个普通链接。
 */
public final class VideoEmbed {

    private static final Pattern YOUTUBE_ID = Pattern.compile("[A-Za-z0-9_-]{11}");
    private static final Pattern VIMEO_ID = Pattern.compile("\\d+");

    private VideoEmbed() {
    }

    public static String embedUrl(String url) {
        URI uri;
        try {
            uri = URI.create(url.strip());
        } catch (IllegalArgumentException | NullPointerException e) {
            return null;
        }
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT).replaceFirst("^(www|m)\\.", "");
        String path = uri.getPath() == null ? "" : uri.getPath();
        String id = switch (host) {
            case "youtube.com" -> {
                if (path.equals("/watch")) {
                    yield queryParam(uri.getRawQuery(), "v");
                }
                if (path.startsWith("/embed/") || path.startsWith("/shorts/")) {
                    yield path.substring(path.indexOf('/', 1) + 1);
                }
                yield null;
            }
            case "youtu.be" -> path.length() > 1 ? path.substring(1) : null;
            case "vimeo.com" -> {
                String vimeoId = path.length() > 1 ? path.substring(1) : "";
                yield VIMEO_ID.matcher(vimeoId).matches() ? "vimeo:" + vimeoId : null;
            }
            default -> null;
        };
        if (id == null) {
            return null;
        }
        if (id.startsWith("vimeo:")) {
            return "https://player.vimeo.com/video/" + id.substring(6);
        }
        return YOUTUBE_ID.matcher(id).matches() ? "https://www.youtube.com/embed/" + id : null;
    }

    private static String queryParam(String query, String name) {
        if (query == null) {
            return null;
        }
        for (String pair : query.split("&")) {
            int eq = pair.indexOf('=');
            if (eq > 0 && pair.substring(0, eq).equals(name)) {
                return pair.substring(eq + 1);
            }
        }
        return null;
    }
}
