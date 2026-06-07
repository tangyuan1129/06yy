#!/bin/bash

# 自动部署脚本
echo "========================================="
echo "开始自动部署..."
echo "========================================="

# 1. 进入项目目录
cd /opt/aichat

# 2. 拉取最新代码
echo "拉取最新代码..."
git pull origin main

# 3. 停止旧服务
echo "停止旧服务..."
systemctl stop aichat

# 4. 构建项目
echo "构建项目..."
mvn clean package -DskipTests

# 5. 启动新服务
echo "启动新服务..."
systemctl start aichat

# 6. 查看状态
echo "查看服务状态..."
systemctl status aichat --no-pager

echo "========================================="
echo "部署完成！"
echo "========================================="