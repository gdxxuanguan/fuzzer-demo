FROM ubuntu:22.04

# 更新系统并安装必要工具
RUN echo "更新系统和安装工具..." && \
    apt update && apt upgrade -y && \
    apt install -y --no-install-recommends \
        build-essential \
        wget \
        curl \
        git \
        maven \
        vim \
        libjpeg62 \
        libpcap0.8 && \
        openjdk-11-jdk && \
    echo "系统更新完成。"

# djpeg需要的库
RUN wget http://ftp.de.debian.org/debian/pool/main/libj/libjpeg-turbo/libjpeg62-turbo_2.1.5-2_amd64.deb && \
    dpkg -i libjpeg62-turbo_2.1.5-2_amd64.deb && \
    rm -f libjpeg62-turbo_2.1.5-2_amd64.deb

# 确保 Maven 配置文件存在并添加阿里云镜像
RUN mkdir -p /root/.m2 && \
    if [ ! -f /root/.m2/settings.xml ]; then \
        cp /usr/share/maven/conf/settings.xml /root/.m2/settings.xml; \
    fi && \
    sed -i '/<mirrors>/a \
    <mirror>\n\
        <id>aliyun-maven</id>\n\
        <mirrorOf>*</mirrorOf>\n\
        <name>aliyun maven</name>\n\
        <url>https://maven.aliyun.com/repository/public</url>\n\
    </mirror>' /root/.m2/settings.xml

# 克隆项目仓库
RUN git clone https://github.com/gdxxuanguan/fuzzer-demo.git /opt/project/dev

# 设置工作目录
WORKDIR /opt/project/dev

# 编译项目
RUN mvn clean compile

RUN chmod +x target/classes/targets/*

# 设置容器启动时进入交互模式
CMD ["/bin/bash"]



