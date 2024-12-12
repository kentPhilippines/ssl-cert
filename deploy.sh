#!/bin/bash

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# 配置信息
APP_NAME="certificate-easy-apply"
APP_PORT=80
APP_USER="certapp"
JAVA_VERSION="15"
APP_PATH="/opt/${APP_NAME}"
JAR_PATH="${APP_PATH}/${APP_NAME}.jar"
LOG_PATH="/var/log/${APP_NAME}"

# 检查root权限
check_root() {
    if [ "$EUID" -ne 0 ]; then
        echo -e "${RED}请使用root权限运行此脚本${NC}"
        exit 1
    fi
}

# 检查并安装Java
install_java() {
    echo -e "${YELLOW}检查Java环境...${NC}"
    if ! command -v java &> /dev/null; then
        echo -e "${YELLOW}安装OpenJDK ${JAVA_VERSION}...${NC}"
        apt-get update
        apt-get install -y openjdk-${JAVA_VERSION}-jdk
    fi
    java -version
}

# 创建应用用户
create_user() {
    echo -e "${YELLOW}创建应用用户...${NC}"
    if ! id -u ${APP_USER} &>/dev/null; then
        useradd -r -s /bin/false ${APP_USER}
    fi
}

# 创建必要的目录
create_directories() {
    echo -e "${YELLOW}创建应用目录...${NC}"
    mkdir -p ${APP_PATH}
    mkdir -p ${LOG_PATH}
    mkdir -p ${APP_PATH}/certificates
    
    # 设置目录权限
    chown -R ${APP_USER}:${APP_USER} ${APP_PATH}
    chown -R ${APP_USER}:${APP_USER} ${LOG_PATH}
    chmod 755 ${APP_PATH}
    chmod 755 ${LOG_PATH}
}

# 配置系统服务
create_service() {
    echo -e "${YELLOW}配置系统服务...${NC}"
    cat > /etc/systemd/system/${APP_NAME}.service << EOF
[Unit]
Description=Certificate Easy Apply Service
After=network.target

[Service]
User=${APP_USER}
Type=simple
ExecStart=/usr/bin/java -jar ${JAR_PATH}
SuccessExitStatus=143
TimeoutStopSec=10
Restart=on-failure
RestartSec=5

[Install]
WantedBy=multi-user.target
EOF

    systemctl daemon-reload
    systemctl enable ${APP_NAME}
}

# 部署应用
deploy_application() {
    echo -e "${YELLOW}部署应用...${NC}"
    
    # 检查构建文件是否存在
    if [ ! -f "build/libs/${APP_NAME}-*.jar" ]; then
        echo -e "${RED}找不到构建文件，请先构建应用${NC}"
        exit 1
    fi
    
    # 停止现有服务
    if systemctl is-active --quiet ${APP_NAME}; then
        echo -e "${YELLOW}停止现有服务...${NC}"
        systemctl stop ${APP_NAME}
    fi
    
    # 复制新的jar文件
    cp build/libs/${APP_NAME}-*.jar ${JAR_PATH}
    chown ${APP_USER}:${APP_USER} ${JAR_PATH}
    chmod 500 ${JAR_PATH}
    
    # 启动服务
    echo -e "${YELLOW}启动服务...${NC}"
    systemctl start ${APP_NAME}
    
    # 检查服务状态
    if systemctl is-active --quiet ${APP_NAME}; then
        echo -e "${GREEN}应用部署成功！${NC}"
    else
        echo -e "${RED}应用启动失败，请检查日志${NC}"
        journalctl -u ${APP_NAME} -n 50
        exit 1
    fi
}

# 配置防火墙
configure_firewall() {
    echo -e "${YELLOW}配置防火墙...${NC}"
    if command -v ufw &> /dev/null; then
        ufw allow ${APP_PORT}/tcp
        ufw allow 443/tcp
    elif command -v firewall-cmd &> /dev/null; then
        firewall-cmd --permanent --add-port=${APP_PORT}/tcp
        firewall-cmd --permanent --add-port=443/tcp
        firewall-cmd --reload
    fi
}

# 主函数
main() {
    echo -e "${GREEN}开始部署 ${APP_NAME}...${NC}"
    
    check_root
    install_java
    create_user
    create_directories
    create_service
    configure_firewall
    deploy_application
    
    echo -e "${GREEN}部署完成！${NC}"
    echo -e "${GREEN}服务状态：${NC}"
    systemctl status ${APP_NAME}
    
    echo -e "\n${YELLOW}应用访问地址：${NC}"
    echo -e "HTTP: http://$(hostname -I | cut -d' ' -f1):${APP_PORT}"
    echo -e "HTTPS: https://$(hostname -I | cut -d' ' -f1)"
}

# 执行主函数
main 