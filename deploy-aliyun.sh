#!/bin/bash

# 阿里云部署脚本
# 在阿里云ECS上执行此脚本

echo "========================================="
echo "阿里云ECS部署脚本"
echo "========================================="

# 1. 更新系统
echo "1. 更新系统..."
sudo yum update -y

# 2. 安装Java 17
echo "2. 安装Java 17..."
sudo yum install -y java-17-openjdk java-17-openjdk-devel

# 3. 安装MySQL 8.0
echo "3. 安装MySQL 8.0..."
sudo yum install -y mysql-server
sudo systemctl start mysqld
sudo systemctl enable mysqld

# 4. 获取MySQL临时密码
echo "4. 获取MySQL临时密码..."
TEMP_PASSWORD=$(sudo grep 'temporary password' /var/log/mysqld.log | tail -1 | awk '{print $NF}')
echo "MySQL临时密码: $TEMP_PASSWORD"

# 5. 配置MySQL安全设置
echo "5. 配置MySQL..."
sudo mysql_secure_installation <<EOF

y
YourNewPassword123!
YourNewPassword123!
y
y
y
y
EOF

# 6. 创建数据库
echo "6. 创建数据库..."
sudo mysql -u root -p'YourNewPassword123!' -e "CREATE DATABASE IF NOT EXISTS aichat CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# 7. 安装Maven
echo "7. 安装Maven..."
sudo yum install -y maven

# 8. 创建应用目录
echo "8. 创建应用目录..."
mkdir -p /opt/aichat
cd /opt/aichat

# 9. 提示用户上传项目
echo "========================================="
echo "请将项目文件上传到 /opt/aichat 目录"
echo "可以使用scp命令："
echo "scp -r D:\\06\\six_springboot root@YOUR_ALIYUN_IP:/opt/aichat"
echo "========================================="

# 10. 构建项目
echo "10. 构建项目..."
cd /opt/aichat
mvn clean package -DskipTests

# 11. 创建systemd服务
echo "11. 创建systemd服务..."
sudo tee /etc/systemd/system/aichat.service > /dev/null <<EOF
[Unit]
Description=AI Chat Application
After=syslog.target network.target mysql.service

[Service]
Type=simple
User=root
WorkingDirectory=/opt/aichat
Environment=DB_HOST=localhost
Environment=DB_PORT=3306
Environment=DB_NAME=aichat
Environment=DB_USERNAME=root
Environment=DB_PASSWORD=YourNewPassword123!
Environment=ZHIPU_API_KEY=your_zhipu_api_key_here
Environment=SPRING_PROFILES_ACTIVE=prod
ExecStart=/usr/bin/java -jar target/six_springboot-0.0.1-SNAPSHOT.jar
SuccessExitStatus=143
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF

# 12. 启动服务
echo "12. 启动服务..."
sudo systemctl daemon-reload
sudo systemctl enable aichat
sudo systemctl start aichat

# 13. 查看状态
echo "13. 查看服务状态..."
sudo systemctl status aichat

# 14. 配置防火墙
echo "14. 配置防火墙..."
sudo firewall-cmd --permanent --add-port=8080/tcp
sudo firewall-cmd --reload

echo "========================================="
echo "部署完成！"
echo "访问地址: http://YOUR_ALIYUN_IP:8080/bai-e.html"
echo "========================================="
echo ""
echo "常用命令："
echo "查看日志: sudo journalctl -u aichat -f"
echo "重启服务: sudo systemctl restart aichat"
echo "停止服务: sudo systemctl stop aichat"
echo "========================================="