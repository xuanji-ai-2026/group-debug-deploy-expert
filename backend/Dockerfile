# 北极星AI - 单体应用 Dockerfile
# 构建: docker build -t beijixing/beijixing-ai-app:latest .
# 最后更新: 2026-05-28

# ---- 构建阶段 ----
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /build

# 先复制POM文件，利用Docker缓存加速依赖下载
COPY pom.xml .
COPY beijixing-app/pom.xml beijixing-app/
COPY bx-common/pom.xml bx-common/
COPY bx-user/pom.xml bx-user/
COPY bx-tenant/pom.xml bx-tenant/
COPY bx-lead/pom.xml bx-lead/
COPY bx-ai/pom.xml bx-ai/
COPY bx-content/pom.xml bx-content/
COPY bx-social/pom.xml bx-social/
COPY bx-billing/pom.xml bx-billing/
COPY bx-risk/pom.xml bx-risk/
COPY bx-message/pom.xml bx-message/
COPY bx-storage/pom.xml bx-storage/
COPY bx-monitor/pom.xml bx-monitor/
COPY bx-schedule/pom.xml bx-schedule/
COPY bx-system/pom.xml bx-system/
COPY bx-data/pom.xml bx-data/
COPY bx-task/pom.xml bx-task/

# 复制Maven wrapper
COPY .mvn/ .mvn/
COPY mvnw .

# 下载依赖（利用Docker缓存层）
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B 2>/dev/null || true

# 复制全部源码
COPY . .

# 构建（跳过测试）
RUN ./mvnw package -pl beijixing-app -am -DskipTests -B

# ---- 运行阶段 ----
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# 安装curl用于健康检查
RUN apk add --no-cache curl tzdata && \
    cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone && \
    apk del tzdata

# 从构建阶段复制JAR
COPY --from=builder /build/beijixing-app/target/*.jar app.jar

# 创建日志和上传目录
RUN mkdir -p /opt/beijixing-ai/logs /opt/beijixing-ai/uploads

# JVM参数
ENV JAVA_OPTS="-Xms1g -Xmx2g -XX:+UseG1GC -XX:+HeapDumpOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom"

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --retries=5 --start-period=90s \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar app.jar"]
