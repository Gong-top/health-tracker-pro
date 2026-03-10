# 第一阶段：编译打包
FROM maven:3.8.4-openjdk-17-slim AS build
WORKDIR /app
# 将整个后端代码复制进去
COPY . .
# 运行 Maven 打包 (跳过测试)
RUN mvn clean package -DskipTests

# 第二阶段：运行
FROM openjdk:17-jdk-alpine
WORKDIR /app
# 从编译阶段复制生成的 JAR 包
COPY --from=build /app/target/health-tracker-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
