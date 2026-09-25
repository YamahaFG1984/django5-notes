package com.example.educa.courses.api;

import java.util.Map;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.example.educa.account.CurrentUser;
import com.example.educa.courses.api.ApiDtos.CourseDto;
import com.example.educa.courses.api.ApiDtos.CourseWithContentsDto;
import com.example.educa.courses.api.ApiDtos.SubjectDto;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * ≈ SubjectViewSet 和 CourseViewSet（ReadOnlyModelViewSet + 两个 @action）。
 * DRF 的 router 根据 ViewSet 自动生成 URL；Spring 没有 ViewSet，每个端点就是一个显式映射的方法。
 * {@code @RestController} = {@code @Controller} + 每个方法都加 {@code @ResponseBody}：返回值直接写成 JSON。
 */
@RestController
@RequestMapping("/api")
public class CourseApiController {

    private final CourseApiService service;

    public CourseApiController(CourseApiService service) {
        this.service = service;
    }

    /** DefaultRouter 自带的 API 根视图：列出各资源的地址 */
    @GetMapping("/")
    public Map<String, String> root() {
        String base = ServletUriComponentsBuilder.fromCurrentContextPath().path("/api/").toUriString();
        return Map.of("courses", base + "courses/", "subjects", base + "subjects/");
    }

    @Tag(name = "subjects")
    @GetMapping("/subjects/")
    public PageResponse<SubjectDto> subjects(@RequestParam(required = false) String page,
                                             @RequestParam(name = "page_size", required = false) String pageSize) {
        return PageResponse.of(service.subjects(StandardPagination.pageable(page, pageSize)));
    }

    @Tag(name = "subjects")
    @GetMapping("/subjects/{id}/")
    public SubjectDto subject(@PathVariable Long id) {
        return service.subject(id);
    }

    @Tag(name = "courses")
    @GetMapping("/courses/")
    public PageResponse<CourseDto> courses(@RequestParam(required = false) String page,
                                           @RequestParam(name = "page_size", required = false) String pageSize) {
        return PageResponse.of(service.courses(StandardPagination.pageable(page, pageSize)));
    }

    @Tag(name = "courses")
    @GetMapping("/courses/{id}/")
    public CourseDto course(@PathVariable Long id) {
        return service.course(id);
    }

    @Tag(name = "courses")
    @Operation(summary = "选课", security = @SecurityRequirement(name = "basicAuth"))
    @PostMapping("/courses/{id}/enroll/")
    public Map<String, Boolean> enroll(@PathVariable Long id, @AuthenticationPrincipal CurrentUser user) {
        service.enroll(id, user.id());
        return Map.of("enrolled", true);
    }

    @Tag(name = "courses")
    @Operation(summary = "课程内容（仅限已选课的学生）", security = @SecurityRequirement(name = "basicAuth"))
    @GetMapping("/courses/{id}/contents/")
    public CourseWithContentsDto contents(@PathVariable Long id, @AuthenticationPrincipal CurrentUser user) {
        String mediaUrl = ServletUriComponentsBuilder.fromCurrentContextPath().path("/media/").toUriString();
        return service.contents(id, user.id(), mediaUrl);
    }
}
