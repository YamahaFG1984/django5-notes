# syntax=docker/dockerfile:1
# 多阶段构建：第一阶段编译打包，最后的镜像里只有 JRE 和应用，没有 Maven 和源码。
# （书中的 Dockerfile 是单阶段：python 镜像里装依赖、复制整个项目。）

# ---- 1. 编译打包 ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
# 缓存挂载：依赖只下载一次，之后重新构建镜像不会再下载
RUN --mount=type=cache,target=/root/.m2 mvn -B -q dependency:go-offline
COPY src src
RUN --mount=type=cache,target=/root/.m2 mvn -B -q package -DskipTests

# ---- 2. 把 jar 按“变化频率”拆成几层 ----
# 依赖很少变、应用代码经常变：分层后改代码只需要重建最上面那层，镜像推送/拉取更快
FROM eclipse-temurin:21-jre AS extract
WORKDIR /extract
COPY --from=build /src/target/educa-*.jar app.jar
RUN java -Djarmode=tools -jar app.jar extract --layers --launcher --destination extracted

# ---- 3. 运行镜像 ----
FROM eclipse-temurin:21-jre
# 不用 root 运行应用
RUN useradd --system --uid 1001 educa && mkdir -p /app/media && chown educa /app/media
WORKDIR /app
COPY --from=extract /extract/extracted/dependencies/ ./
COPY --from=extract /extract/extracted/spring-boot-loader/ ./
COPY --from=extract /extract/extracted/snapshot-dependencies/ ./
COPY --from=extract /extract/extracted/application/ ./
USER educa
EXPOSE 8080
# 管理命令同样通过这个入口执行，例如：docker compose run --rm web --loaddata=subjects
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
