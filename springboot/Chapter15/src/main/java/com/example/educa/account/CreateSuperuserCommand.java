package com.example.educa.account;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * python manage.py createsuperuser 的替代品。用法：
 * <pre>
 * java -jar target/educa-1.0.0.jar --spring.main.web-application-type=none \
 *      --createsuperuser --username=admin --email=admin@example.com --password=change-me
 * </pre>
 * 不带 --createsuperuser 参数时什么也不做。
 */
@Component
public class CreateSuperuserCommand implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CreateSuperuserCommand.class);

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationContext context;

    public CreateSuperuserCommand(UserRepository users, PasswordEncoder passwordEncoder, ApplicationContext context) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.context = context;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!args.containsOption("createsuperuser")) {
            return;
        }
        try {
            createSuperuser(args);
        } finally {
            // 有后台线程（比如后面章节的 Redis 订阅）时，即使不启动 Web 服务器进程也不会自己结束，
            // 所以命令执行完要显式关闭应用（≈ manage.py 命令执行完进程就退出）
            new Thread(() -> System.exit(SpringApplication.exit(context, () -> 0))).start();
        }
    }

    /** 只有一次 save，仓库方法自带事务；注意同一个类内部调用 @Transactional 方法是不会生效的（不经过代理） */
    private void createSuperuser(ApplicationArguments args) {
        String username = single(args, "username");
        String email = args.containsOption("email") ? single(args, "email") : "";
        String password = single(args, "password");
        if (users.existsByUsername(username)) {
            log.warn("用户 {} 已存在，跳过", username);
            return;
        }
        User user = new User(username, passwordEncoder.encode(password), email);
        user.setStaff(true);
        user.setSuperuser(true);
        users.save(user);
        log.info("已创建超级用户 {}", username);
    }

    private static String single(ApplicationArguments args, String name) {
        var values = args.getOptionValues(name);
        if (values == null || values.isEmpty() || values.getFirst().isBlank()) {
            throw new IllegalArgumentException("缺少参数 --" + name + "=...");
        }
        return values.getFirst();
    }
}
