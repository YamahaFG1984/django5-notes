package com.example.educa.common.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

/** 两个过滤器的纯逻辑测试（不需要启动 Spring） */
class DeploymentFiltersTests {

    @Test
    void allowedHostsFollowDjangoSemantics() {
        AllowedHostsFilter filter = new AllowedHostsFilter(List.of(".educaproject.com", "localhost"));
        assertThat(filter.isAllowed("educaproject.com")).isTrue();
        assertThat(filter.isAllowed("www.EducaProject.com")).isTrue();
        assertThat(filter.isAllowed("django.educaproject.com")).isTrue();
        assertThat(filter.isAllowed("localhost")).isTrue();
        assertThat(filter.isAllowed("evil.com")).isFalse();
        assertThat(filter.isAllowed("educaproject.com.evil.com")).isFalse();
        assertThat(filter.isAllowed("notreallyeducaproject.com")).isFalse();
    }

    @Test
    void subdomainDetectionIgnoresIpAddressesAndWww() {
        SubdomainCourseFilter filter = new SubdomainCourseFilter("educaproject.com", null);
        assertThat(filter.subdomainOf("django.educaproject.com")).isEqualTo("django");
        assertThat(filter.subdomainOf("educaproject.com")).isNull();
        assertThat(filter.subdomainOf("www.educaproject.com")).isNull();
        assertThat(filter.subdomainOf("a.b.educaproject.com")).isNull();
        // 书中的中间件会把 127.0.0.1 当成子域名 '127'
        assertThat(filter.subdomainOf("127.0.0.1")).isNull();
        assertThat(filter.subdomainOf("192.168.1.10")).isNull();
        assertThat(new SubdomainCourseFilter("", null).subdomainOf("django.educaproject.com")).isNull();
    }
}
