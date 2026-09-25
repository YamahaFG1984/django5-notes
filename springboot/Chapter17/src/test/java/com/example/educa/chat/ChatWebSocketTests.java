package com.example.educa.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.example.educa.TestcontainersConfiguration;
import com.example.educa.account.User;
import com.example.educa.account.UserRepository;
import com.example.educa.courses.Course;
import com.example.educa.courses.CourseRepository;
import com.example.educa.courses.Subject;
import com.example.educa.courses.SubjectRepository;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * 启动真正的服务器（随机端口），用 WebSocket 客户端连接聊天室。
 * ≈ Channels 文档里用 WebsocketCommunicator 写的测试。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class ChatWebSocketTests {

    @LocalServerPort
    int port;

    @Autowired
    UserRepository users;

    @Autowired
    SubjectRepository subjects;

    @Autowired
    CourseRepository courses;

    @Autowired
    MessageRepository messages;

    @Autowired
    ChatRooms rooms;

    @Autowired
    ObjectMapper json;

    @Autowired
    JdbcTemplate jdbc;

    Course course;

    @BeforeEach
    void setUp() {
        User alice = users.save(new User("ws-alice", "{noop}pw", ""));
        User bob = users.save(new User("ws-bob", "{noop}pw", ""));
        users.save(new User("ws-carol", "{noop}pw", ""));
        Subject music = subjects.save(new Subject("Music", "music-ws"));
        Course c = new Course(alice, music, "Jazz", "jazz-ws", "o");
        c.getStudents().add(alice);
        c.getStudents().add(bob);
        course = courses.save(c);
    }

    @AfterEach
    void tearDown() {
        jdbc.update("delete from chat_message");
        jdbc.update("delete from courses_course where slug = 'jazz-ws'");
        jdbc.update("delete from courses_subject where slug = 'music-ws'");
        jdbc.update("delete from auth_user where username like 'ws-%'");
    }

    /** 走一遍表单登录，拿到会话 cookie（浏览器打开聊天室页面时已经登录过了） */
    private String login(String username) throws Exception {
        HttpClient http = HttpClient.newBuilder().cookieHandler(new CookieManager()).build();
        String page = http.send(HttpRequest.newBuilder(URI.create(base("http") + "/accounts/login/")).build(),
                HttpResponse.BodyHandlers.ofString()).body();
        Matcher csrf = Pattern.compile("name=\"_csrf\" value=\"([^\"]+)\"").matcher(page);
        assertThat(csrf.find()).isTrue();
        http.send(HttpRequest.newBuilder(URI.create(base("http") + "/accounts/login/"))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(
                                "username=" + username + "&password=pw&_csrf=" + csrf.group(1)))
                        .build(),
                HttpResponse.BodyHandlers.discarding());
        return ((CookieManager) http.cookieHandler().orElseThrow()).getCookieStore().getCookies().stream()
                .filter(c -> c.getName().equals("JSESSIONID"))
                .map(c -> "JSESSIONID=" + c.getValue())
                .findFirst().orElseThrow();
    }

    private String base(String scheme) {
        return scheme + "://localhost:" + port;
    }

    record Client(WebSocketSession session, BlockingQueue<String> inbox, BlockingQueue<CloseStatus> closed) {
    }

    private Client connect(String cookie, String origin) throws Exception {
        BlockingQueue<String> inbox = new LinkedBlockingQueue<>();
        BlockingQueue<CloseStatus> closed = new LinkedBlockingQueue<>();
        WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
        if (cookie != null) {
            headers.add("Cookie", cookie);
        }
        if (origin != null) {
            headers.add("Origin", origin);
        }
        WebSocketSession session = new StandardWebSocketClient().execute(new TextWebSocketHandler() {
            @Override
            protected void handleTextMessage(WebSocketSession s, TextMessage message) {
                inbox.add(message.getPayload());
            }

            @Override
            public void afterConnectionClosed(WebSocketSession s, CloseStatus status) {
                closed.add(status);
            }
        }, headers, URI.create(base("ws") + "/ws/chat/room/" + course.getId() + "/")).get(5, TimeUnit.SECONDS);
        return new Client(session, inbox, closed);
    }

    @Test
    void messagesAreBroadcastToEveryoneInTheRoomAndPersisted() throws Exception {
        Client alice = connect(login("ws-alice"), base("http"));
        Client bob = connect(login("ws-bob"), null);
        await().atMost(Duration.ofSeconds(5)).until(() -> rooms.size(course.getId()) == 2);

        alice.session().sendMessage(new TextMessage("{\"message\": \"<b>hi</b> bob\"}"));

        for (Client client : new Client[] {alice, bob}) {
            JsonNode event = json.readTree(client.inbox().poll(5, TimeUnit.SECONDS));
            assertThat(event.get("type").asString()).isEqualTo("chat_message");
            assertThat(event.get("message").asString()).isEqualTo("<b>hi</b> bob");   // 原样传递，由前端当纯文本显示
            assertThat(event.get("user").asString()).isEqualTo("ws-alice");
            assertThat(event.get("datetime").asString()).isNotBlank();
        }
        assertThat(messages.findAll()).extracting(Message::getContent).containsExactly("<b>hi</b> bob");

        // 空消息被忽略
        bob.session().sendMessage(new TextMessage("{\"message\": \"   \"}"));
        assertThat(alice.inbox().poll(500, TimeUnit.MILLISECONDS)).isNull();

        alice.session().close();
        await().atMost(Duration.ofSeconds(5)).until(() -> rooms.size(course.getId()) == 1);
        bob.session().close();
    }

    @Test
    void invalidJsonClosesTheConnection() throws Exception {
        Client alice = connect(login("ws-alice"), null);
        alice.session().sendMessage(new TextMessage("not json"));
        assertThat(alice.closed().poll(5, TimeUnit.SECONDS)).isEqualTo(CloseStatus.BAD_DATA);
    }

    @Test
    void handshakeIsRejectedForStrangersAnonymousUsersAndForeignOrigins() throws Exception {
        // 登录了但没选这门课（书中的实现允许连接）
        String carol = login("ws-carol");
        assertThatThrownBy(() -> connect(carol, null)).hasStackTraceContaining("403");
        // 未登录：401
        assertThatThrownBy(() -> connect(null, null)).hasStackTraceContaining("401");
        // 别的网站的页面发起的连接（跨站 WebSocket 劫持）
        String alice = login("ws-alice");
        assertThatThrownBy(() -> connect(alice, "http://evil.example")).hasStackTraceContaining("403");
    }
}
