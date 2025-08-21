
# 使用多阶段构建减小镜像体积
FROM public.ecr.aws/debian/debian:bullseye-slim AS builder

# 设置工作目录
WORKDIR /app
# 安装 OpenJDK17 与 Maven 3.9.6（只依赖 Debian 官方仓库与 Apache 源）
RUN apt-get update && apt-get install -y openjdk-17-jdk-headless curl ca-certificates tar git && \
    rm -rf /var/lib/apt/lists/* && \
    curl -fsSL https://archive.apache.org/dist/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.tar.gz -o /tmp/maven.tgz && \
    tar -xzf /tmp/maven.tgz -C /opt && rm /tmp/maven.tgz && \
    ln -s /opt/apache-maven-3.9.6 /opt/maven && \
    ln -s /opt/maven/bin/mvn /usr/local/bin/mvn && \
    java -version && mvn -v


# 复制pom文件
COPY pom.xml .
COPY common/pom.xml common/
COPY auth-service/pom.xml auth-service/

# 下载依赖（利用BuildKit缓存挂载）仅针对 auth-service 模块，避免扫描父POM的所有子模块
# 延后到安装 common 之后再执行 go-offline，以避免缺少 com.example:common

# 先安装父 POM，避免子模块 go-offline 阶段找不到 com.example:distribution-system
RUN --mount=type=cache,target=/root/.m2 \
    --mount=type=cache,target=/root/.m2/wrapper \
    mvn -f pom.xml -B -N install

# 复制源代码
COPY common/src common/src

# 先安装 common 模块（供下游 auth-service 依赖）
RUN --mount=type=cache,target=/root/.m2 \
    --mount=type=cache,target=/root/.m2/wrapper \
    mvn -f common/pom.xml -B clean install -Dmaven.test.skip=true
# 现在 com.example:common 已安装，执行 auth-service 的 go-offline 和构建
RUN --mount=type=cache,target=/root/.m2 \
    --mount=type=cache,target=/root/.m2/wrapper \
    mvn -f auth-service/pom.xml -B dependency:go-offline -Dmaven.test.skip=true \
    -Dmaven.wagon.http.retryHandler.count=5

# 再复制 auth-service 源码以最大化缓存命中
COPY auth-service/src auth-service/src

# 构建 auth-service 模块
RUN --mount=type=cache,target=/root/.m2 \
    --mount=type=cache,target=/root/.m2/wrapper \
    rm -rf /root/.m2/repository/org/glassfish/jaxb/jaxb-runtime/2.3.8 && \
    find /root/.m2/repository -name "*.lastUpdated" -delete && \
    mvn -f auth-service/pom.xml -B clean package -Dmaven.test.skip=true \
    -Dmaven.wagon.http.retryHandler.count=5

# 运行阶段
FROM public.ecr.aws/debian/debian:bullseye-slim

# 安装运行所需的 OpenJDK17 JRE 与基础工具
RUN apt-get update && apt-get install -y openjdk-17-jre-headless curl ca-certificates && rm -rf /var/lib/apt/lists/*


# 创建用户（Debian/Ubuntu 系列用 groupadd/useradd）
RUN groupadd -g 1000 spring && \
    useradd -m -u 1000 -g spring -s /bin/sh spring

# 设置工作目录
WORKDIR /app

# 从构建阶段复制jar文件
COPY --from=builder /app/auth-service/target/auth-service-1.0-SNAPSHOT.jar app.jar

# 修改文件权限
RUN chown -R spring:spring /app

# 切换到非root用户
USER spring:spring

# 暴露端口
EXPOSE 8081

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s --retries=3 \
    CMD curl -f http://localhost:8081/auth/actuator/health || exit 1

# 启动应用
ENTRYPOINT ["java", "-jar", "/app/app.jar"]