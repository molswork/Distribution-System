# =========================================================================
#  运行阶段 (Runtime)
#  使用轻量级的 JRE 镜像。eclipse-temurin:17-jre-jammy 基于 Ubuntu 22.04 LTS，
#  解决了 Debian bullseye 的 GPG 密钥问题，并且非常稳定。
# =========================================================================
FROM eclipse-temurin:17-jre-jammy

# 安装 wget (用于健康检查) 和 tzdata (用于时区设置)
RUN apt-get update && \
    apt-get install -y --no-install-recommends wget tzdata && \
    rm -rf /var/lib/apt/lists/*

# 设置工作目录
WORKDIR /app

# 设置时区为上海
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 直接复制已经构建好的JAR文件
COPY gateway/target/gateway-*.jar app.jar

# 创建非 root 用户以增强安全性
RUN addgroup --system --gid 1000 spring && \
    adduser --system --uid 1000 --ingroup spring spring && \
    chown -R spring:spring /app

# 切换到非 root 用户
USER spring:spring

# 暴露网关服务端口
EXPOSE 8080

# 保留原有的灵活配置方式
VOLUME /tmp
ENV JAVA_OPTS="-Djava.security.egd=file:/dev/./urandom"
ENV APP_OPTS=""

# 保留原有的健康检查配置
HEALTHCHECK --interval=30s --timeout=3s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# 保留原有的启动命令，它能正确解析环境变量，非常灵活
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar $APP_OPTS"]
