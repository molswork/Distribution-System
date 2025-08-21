# 多阶段构建：使用官方 Maven + Temurin JDK17，稳定且无需手工安装
FROM maven:3.9.6-eclipse-temurin-17 AS builder

WORKDIR /app

# 复制 POM 以利用缓存
COPY pom.xml .
COPY common/pom.xml common/
COPY data-access/pom.xml data-access/
COPY lead-service/pom.xml lead-service/

# 预下载依赖（先安装父POM）
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -f pom.xml -N install

# 复制源码并构建
COPY common/src common/src
COPY data-access/src data-access/src
COPY lead-service/src lead-service/src

# 按正确顺序构建：父POM -> common -> data-access -> lead-service
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -Dmaven.test.skip=true -f common/pom.xml clean install -Dmaven.wagon.http.retryHandler.count=5 && \
    mvn -B -Dmaven.test.skip=true -f data-access/pom.xml clean install -Dmaven.wagon.http.retryHandler.count=5 && \
    mvn -B -Dmaven.test.skip=true -f lead-service/pom.xml clean package -Dmaven.wagon.http.retryHandler.count=5

# 运行阶段使用精简 JRE 镜像
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# 拷贝可执行 JAR
COPY --from=builder /app/lead-service/target/lead-service-1.0-SNAPSHOT.jar app.jar

# 创建非 root 用户
RUN addgroup --system --gid 1000 spring && adduser --system --uid 1000 --ingroup spring spring && \
    chown -R spring:spring /app
USER spring:spring

EXPOSE 8082

# 健康检查（若未接入 Actuator 可临时关闭或改为业务健康端点）
# HEALTHCHECK --interval=30s --timeout=5s --retries=3 CMD curl -f http://localhost:8082/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]

