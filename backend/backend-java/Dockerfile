# 使用轻量级的 JDK 17 基础镜像
FROM openjdk:17-jdk-alpine

# 设置工作目录
WORKDIR /app

# 将本地的 JAR 包复制到镜像中
# 注意：确保这里的 JAR 包名字和你刚才生成的一致
COPY target/health-tracker-0.0.1-SNAPSHOT.jar app.jar

# 暴露端口 (与 Spring Boot 配置一致)
EXPOSE 8080

# 运行 JAR 包
ENTRYPOINT ["java", "-jar", "app.jar"]
