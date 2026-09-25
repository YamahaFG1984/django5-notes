package com.example.bookmarks.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

class TextFiltersTests {

    private final TextFilters filters = new TextFilters();
    private final Clock now = Clock.fixed(Instant.parse("2025-06-01T12:00:00Z"), ZoneOffset.UTC);

    private String since(String instant) {
        return filters.timesince(OffsetDateTime.parse(instant), now);
    }

    @Test
    void timesinceShowsAtMostTwoAdjacentUnits() {
        assertThat(since("2025-06-01T11:59:30Z")).isEqualTo("0 minutes");
        assertThat(since("2025-06-01T11:59:00Z")).isEqualTo("1 minute");
        assertThat(since("2025-06-01T09:55:00Z")).isEqualTo("2 hours, 5 minutes");
        assertThat(since("2025-05-31T12:00:00Z")).isEqualTo("1 day");
        assertThat(since("2025-05-18T11:00:00Z")).isEqualTo("2 weeks");
        assertThat(since("2024-05-01T12:00:00Z")).isEqualTo("1 year, 1 month");
    }
}
