# 使用多阶段构建减小镜像体积
FROM public.ecr.aws/debian/debian:bullseye-slim AS builder

# 设置工作目录
WORKDIR /app

# 复制pom文件
COPY pom.xml .
COPY common/pom.xml common/
COPY data-access/pom.xml data-access/
COPY user-service/pom.xml user-service/

# 下载依赖（利用Docker缓存）
RUN apt-get update && apt-get install -y openjdk-17-jdk-headless curl ca-certificates tar git && rm -rf /var/lib/apt/lists/* && \
    curl -fsSL https://archive.apache.org/dist/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.tar.gz -o /tmp/maven.tgz && \
    tar -xzf /tmp/maven.tgz -C /opt && rm /tmp/maven.tgz && \
    ln -s /opt/apache-maven-3.9.6 /opt/maven && \
    ln -s /opt/maven/bin/mvn /usr/local/bin/mvn && \
    mvn -v

# 为编译 common/data-access 提前复制源码
COPY common/src common/src
COPY data-access/src data-access/src

# 预安装父POM + 构建 common/data-access 供下游使用
RUN --mount=type=cache,target=/root/.m2 \
    mvn -f pom.xml -B -N install && \
    mvn -f common/pom.xml -B clean install -Dmaven.test.skip=true && \
    mvn -f data-access/pom.xml -B clean install -Dmaven.test.skip=true && \
    mvn -f user-service/pom.xml -B dependency:go-offline -Dmaven.test.skip=true

# 复制源代码
COPY user-service/src user-service/src

# 构建项目
RUN --mount=type=cache,target=/root/.m2 mvn -f user-service/pom.xml clean package -Dmaven.test.skip=true

# 运行阶段
FROM public.ecr.aws/debian/debian:bullseye-slim

# 安装必要的工具
RUN apt-get update && apt-get install -y openjdk-17-jre-headless curl ca-certificates && rm -rf /var/lib/apt/lists/*

# 创建用户（Debian）
RUN groupadd -g 1000 spring && \
    useradd -m -u 1000 -g spring -s /bin/bash spring

# 设置工作目录
WORKDIR /app

# 从构建阶段复制jar文件
COPY --from=builder /app/user-service/target/user-service-1.0-SNAPSHOT.jar app.jar

# 修改文件权限
RUN chown -R spring:spring /app

# 切换到非root用户
USER spring:spring

# 暴露端口
EXPOSE 8086

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s --retries=3 \
    CMD curl -f http://localhost:8086/actuator/health || exit 1

# 启动应用
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
