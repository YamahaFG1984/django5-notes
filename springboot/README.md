# Django 5 By Example —— Spring Boot 重构版

把《Django 5 By Example》配套仓库（`../Django-5-By-Example-main/`）的 17 章代码，逐章用 **Spring Boot 4.1 + Java 21** 重写。
每个 `ChapterNN` 都是一个独立的 Maven 项目，是该章结束时的完整快照，可以单独运行、单独测试。

配套教学页面在 `../docs/`：从 `docs/spring.html`（导读）开始，`docs/sb01.html` … `docs/sb17.html` 逐章讲解，并与 Django 5.2 的写法逐段对比。

## 准备

- JDK 21、Maven 3.9+
- Docker：第 3 章起用来运行 PostgreSQL / Redis / RabbitMQ；测试用 Testcontainers 临时启动这些服务

## 常用命令

```bash
cd springboot/Chapter01
mvn test                  # 运行测试（≈ python manage.py test）
mvn spring-boot:run       # 启动开发服务器 http://localhost:8080（≈ runserver）
                          # 有 compose.yaml 的章节会自动 docker compose up

# “管理命令”：打包后用 jar 执行
mvn package -DskipTests
# 第 1–7 章：加上 --spring.main.web-application-type=none，不启动 Web 服务器
java -jar target/mysite-1.0.0.jar --spring.main.web-application-type=none \
     --createsuperuser --username=admin --email=admin@example.com --password=change-me
# 第 8 章起：命令执行完会自动退出，不需要额外参数
java -jar target/educa-1.0.0.jar --loaddata=subjects          # 第 12 章起
java -jar target/educa-1.0.0.jar --enroll-reminder --days=20  # 第 17 章
```

## 各章一览

| 章 | 项目 | 主要内容 | 开发时需要的服务 |
|----|------|----------|------------------|
| 01 | mysite | JPA 实体与仓库、Flyway、Thymeleaf、Spring Security、手写后台 | — （H2 文件库） |
| 02 | mysite | 分页、表单绑定与校验、发送邮件、评论 | — |
| 03 | mysite | 标签（多对多）、自定义方言、Markdown、RSS、Sitemap、pg_trgm 搜索 | PostgreSQL |
| 04 | bookmarks | 登录登出、注册、密码重置、用户资料、上传头像 | — |
| 05 | bookmarks | 闪存消息、邮箱登录、Google OAuth2 登录、本地 HTTPS | — |
| 06 | bookmarks | 图片书签、书签小工具、缩略图、fetch + CSRF、无限滚动 | — |
| 07 | bookmarks | 关注、活动流、领域事件、Redis 计数与排行 | Redis |
| 08 | myshop | 商品目录、会话购物车、订单、RabbitMQ 异步任务 | RabbitMQ、Mailpit |
| 09 | myshop | Stripe 支付与 Webhook、CSV 导出、PDF 发票 | RabbitMQ、Mailpit |
| 10 | myshop | 优惠券、Stripe 折扣、Redis 推荐引擎 | RabbitMQ、Mailpit、Redis |
| 11 | myshop | 国际化：MessageSource、URL 语言前缀、翻译表、本地化格式 | RabbitMQ、Mailpit、Redis |
| 12 | educa | fixture 导入、JPA 继承、自定义排序字段、用户组与权限 | — |
| 13 | educa | 讲师后台：方法级权限、列表绑定（formset）、多态内容、拖拽排序 | — |
| 14 | educa | 学生注册与选课、按类型渲染内容、Spring Cache + Redis | Redis |
| 15 | educa | REST API、DRF 式分页、HTTP Basic、OpenAPI、API 客户端 | Redis |
| 16 | educa | WebSocket 聊天室、Redis 发布/订阅、握手鉴权 | Redis |
| 17 | educa | 生产部署：Profile、PostgreSQL、Dockerfile、Compose、Nginx、HTTPS | Redis（生产：见 `compose.prod.yaml`） |

第 5、9 章需要第三方账号（Google OAuth2、Stripe 测试密钥），配置方法见各章的 `.env.example` 和对应教学页面；
没有配置时应用照常启动，只是相应功能不可用，测试不依赖这些账号。

## 第 17 章：在 Docker 里运行生产配置

```bash
cd springboot/Chapter17
scripts/dev-cert.sh                          # 生成自签名证书（覆盖 educaproject.com 和 *.educaproject.com）
cp .env.example .env                         # 修改 POSTGRES_PASSWORD
docker compose -f compose.prod.yaml up -d --build
docker compose -f compose.prod.yaml run --rm web --loaddata=subjects
# /etc/hosts：127.0.0.1 educaproject.com django.educaproject.com
```
