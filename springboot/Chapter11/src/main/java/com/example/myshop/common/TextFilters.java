package com.example.myshop.common;

import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

/**
 * Django 内置模板过滤器 linebreaks / truncatewords 的替代品。
 * Thymeleaf 没有“过滤器”概念，这里写成普通方法，再由 {@link TextDialect} 暴露为 {@code #text}。
 */
@Component
public class TextFilters {

    /** {{ value|linebreaks }}：先转义 HTML，空行分段成 &lt;p&gt;，单个换行变 &lt;br&gt;。 */
    public String linebreaks(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String normalized = value.replace("\r\n", "\n").replace('\r', '\n').strip();
        return Arrays.stream(normalized.split("\n{2,}"))
                .map(paragraph -> "<p>" + HtmlUtils.htmlEscape(paragraph).replace("\n", "<br>") + "</p>")
                .collect(Collectors.joining("\n\n"));
    }

    /** {{ value|truncatewords:n }}：保留前 n 个单词，超出部分用 … 结尾。 */
    public String truncatewords(String value, int count) {
        if (value == null) {
            return "";
        }
        String[] words = value.strip().split("\\s+");
        if (words.length <= count) {
            return value.strip();
        }
        return String.join(" ", Arrays.copyOfRange(words, 0, count)) + " …";
    }

    private static final long[] UNIT_SECONDS = {365L * 24 * 3600, 30L * 24 * 3600, 7L * 24 * 3600, 24L * 3600, 3600, 60};
    private static final String[] UNIT_NAMES = {"year", "month", "week", "day", "hour", "minute"};

    /**
     * {{ value|timesince }}：“3 hours, 5 minutes”这样的相对时间，最多显示两个相邻的单位（和 Django 一样）。
     */
    public String timesince(OffsetDateTime value) {
        return timesince(value, Clock.systemUTC());
    }

    String timesince(OffsetDateTime value, Clock clock) {
        if (value == null) {
            return "";
        }
        long seconds = Math.max(0, Duration.between(value.toInstant(), clock.instant()).getSeconds());
        for (int i = 0; i < UNIT_SECONDS.length; i++) {
            long count = seconds / UNIT_SECONDS[i];
            if (count > 0) {
                List<String> parts = new ArrayList<>();
                parts.add(plural(count, UNIT_NAMES[i]));
                if (i + 1 < UNIT_SECONDS.length) {
                    long next = (seconds - count * UNIT_SECONDS[i]) / UNIT_SECONDS[i + 1];
                    if (next > 0) {
                        parts.add(plural(next, UNIT_NAMES[i + 1]));
                    }
                }
                return String.join(", ", parts);
            }
        }
        return "0 minutes";
    }

    private static String plural(long count, String unit) {
        return count + " " + unit + (count == 1 ? "" : "s");
    }
}
