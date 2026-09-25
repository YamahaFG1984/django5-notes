///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 21+
//DEPS tools.jackson.core:jackson-databind:3.1.5

// ≈ 书中的 api_examples/enroll_all.py：先翻页读取全部课程，再用 HTTP Basic 认证逐个选课。
//
// 运行方式（任选其一）：
//   jbang api-examples/EnrollAll.java <username> <password>
//   java --class-path <jackson-databind、jackson-core、jackson-annotations 的 jar> api-examples/EnrollAll.java <username> <password>
//
// 与书中代码的区别：书中第二个循环遍历的是 courses（只剩最后一页），这里遍历全部页收集到的课程。

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

public class EnrollAll {

    record Course(long id, String title) {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("usage: EnrollAll <username> <password> [base-url]");
            System.exit(2);
        }
        String username = args[0];
        String password = args[1];
        String baseUrl = args.length > 2 ? args[2] : "http://127.0.0.1:8080/api/";

        HttpClient http = HttpClient.newHttpClient();
        ObjectMapper json = JsonMapper.builder().build();

        // 1. 跟着 next 链接翻页，直到 next 为 null
        List<Course> available = new ArrayList<>();
        String url = baseUrl + "courses/";
        while (url != null) {
            System.out.println("Loading courses from " + url);
            HttpResponse<String> response = http.send(HttpRequest.newBuilder(URI.create(url)).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            JsonNode page = json.readTree(response.body());
            for (JsonNode course : page.get("results")) {
                available.add(new Course(course.get("id").asLong(), course.get("title").asString()));
            }
            url = page.get("next").isNull() ? null : page.get("next").asString();
        }
        System.out.println("Available courses: "
                + String.join(", ", available.stream().map(Course::title).toList()));

        // 2. 逐个选课：Authorization: Basic base64(username:password)
        String credentials = Base64.getEncoder()
                .encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
        for (Course course : available) {
            HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "courses/" + course.id() + "/enroll/"))
                    .header("Authorization", "Basic " + credentials)
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                System.out.println("Successfully enrolled in " + course.title());
            } else {
                System.out.println("Could not enroll in " + course.title() + ": "
                        + response.statusCode() + " " + response.body());
            }
        }
    }
}
