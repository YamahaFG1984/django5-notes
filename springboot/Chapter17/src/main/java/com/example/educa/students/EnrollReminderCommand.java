package com.example.educa.students;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import com.example.educa.account.User;
import com.example.educa.account.UserRepository;
import com.example.educa.config.EducaProperties;

/**
 * 管理命令：提醒注册了 N 天还没选课的用户，≈ students/management/commands/enroll_reminder.py。
 * <pre>
 *   java -jar educa.jar --enroll-reminder --days=20
 *   docker compose -f compose.prod.yaml run --rm web --enroll-reminder --days=20
 * </pre>
 * 定时执行：书中用 cron 调用 manage.py；这里同样可以用 cron / Kubernetes CronJob 调用上面的命令，
 * 也可以在应用内用 @Scheduled(cron = "0 0 9 * * *") 定时执行 {@link #sendReminders(int)}。
 */
@Component
public class EnrollReminderCommand implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(EnrollReminderCommand.class);

    private final UserRepository users;
    private final JavaMailSender mailSender;
    private final EducaProperties properties;
    private final ApplicationContext context;
    private final Clock clock;

    public EnrollReminderCommand(UserRepository users, JavaMailSender mailSender, EducaProperties properties,
                                 ApplicationContext context) {
        this.users = users;
        this.mailSender = mailSender;
        this.properties = properties;
        this.context = context;
        this.clock = Clock.systemUTC();
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!args.containsOption("enroll-reminder")) {
            return;
        }
        int days = args.containsOption("days") ? Integer.parseInt(args.getOptionValues("days").getFirst()) : 0;
        int sent = sendReminders(days);
        System.out.println("Sent " + sent + " reminders");   // ≈ self.stdout.write(...)
        new Thread(() -> System.exit(SpringApplication.exit(context, () -> 0))).start();
    }

    /**
     * 发送提醒，返回发送数量。date_joined__date__lte=今天-N天：注册日期不晚于那一天，
     * 即注册时间早于“那一天的第二天零点”（UTC）。
     * 与书中的区别：跳过没有邮箱的用户（书中会给空地址发信）；一次调用发送全部邮件（≈ send_mass_mail，复用同一个 SMTP 连接）。
     */
    public int sendReminders(int days) {
        LocalDate lastDay = LocalDate.now(clock).minusDays(days);
        OffsetDateTime cutoff = lastDay.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);
        List<User> recipients = users.findNotEnrolledJoinedBefore(cutoff);
        SimpleMailMessage[] messages = recipients.stream().map(user -> {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(properties.defaultFromEmail());
            message.setTo(user.getEmail());
            message.setSubject("Enroll in a course");
            message.setText("""
                    Dear %s,
                    We noticed that you didn't enroll in any courses yet.
                    What are you waiting for?""".formatted(user.getDisplayName()));
            return message;
        }).toArray(SimpleMailMessage[]::new);
        if (messages.length > 0) {
            mailSender.send(messages);
        }
        log.info("Sent {} enroll reminders", messages.length);
        return messages.length;
    }
}
