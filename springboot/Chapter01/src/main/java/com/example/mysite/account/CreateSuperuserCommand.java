package com.example.mysite.account;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * python manage.py createsuperuser 的替代品。用法：
 * <pre>
 * java -jar target/mysite-1.0.0.jar --spring.main.web-application-type=none \
 *      --createsuperuser --username=admin --email=admin@example.com --password=change-me
 * </pre>
 * 不带 --createsuperuser 参数时什么也不做。
 */
@Component
public class CreateSuperuserCommand implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CreateSuperuserCommand.class);

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    public CreateSuperuserCommand(UserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!args.containsOption("createsuperuser")) {
            return;
        }
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
