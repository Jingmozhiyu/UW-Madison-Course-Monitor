# 1. 基础镜像：我们要一个装好了 Java 17 的 Linux 环境
FROM eclipse-temurin:21-jdk-jammy

# 2. 设定工作目录
WORKDIR /app

# 3. 把你刚才编译好的 JAR 包拷进去，并改名为 app.jar
# 注意：确保 target/*.jar 只有一个 jar 包，或者写死具体名字
COPY target/*.jar app.jar

# 4. 暴露端口（告诉 Docker 这个程序用 8080）
EXPOSE 8080

# 5. 启动命令
ENTRYPOINT ["java", "-jar", "app.jar"]