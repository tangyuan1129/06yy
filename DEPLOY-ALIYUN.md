# 阿里云ECS部署教程

## 第一步：领取阿里云免费试用

1. 访问 https://free.aliyun.com/
2. 登录阿里云账号（没有就注册一个）
3. 找到"云服务器ECS"，点击"立即试用"
4. 选择配置：
   - **地域**：选择离你近的（如华东1-杭州）
   - **实例规格**：ecs.t6-c1m1.large（2核2G，免费3个月）
   - **操作系统**：CentOS 7.9 或 Alibaba Cloud Linux 3
   - **公网IP**：分配公网IPv4地址
5. 设置root密码（记住这个密码）
6. 完成领取

## 第二步：配置安全组（开放端口）

1. 进入阿里云控制台：https://ecs.console.aliyun.com/
2. 找到你的ECS实例，点击进入详情
3. 点击"安全组"标签
4. 点击安全组ID进入配置
5. 点击"手动添加"入站规则：
   - **授权策略**：允许
   - **优先级**：1
   - **协议类型**：自定义TCP
   - **端口范围**：8080/8080
   - **授权对象**：0.0.0.0/0
   - **描述**：AI聊天应用
6. 点击"保存"

## 第三步：上传项目到服务器

### 方式1：使用PowerShell SCP（推荐）

```powershell
# 打开PowerShell，进入项目目录
cd D:\06

# 上传整个项目（替换YOUR_ALIYUN_IP为你的ECS公网IP）
scp -r six_springboot root@YOUR_ALIYUN_IP:/opt/aichat
```

输入root密码后开始上传。

### 方式2：使用WinSCP工具

1. 下载WinSCP：https://winscp.net/eng/download.php
2. 打开WinSCP，填写：
   - **文件协议**：SFTP
   - **主机名**：你的ECS公网IP
   - **端口号**：22
   - **用户名**：root
   - **密码**：你设置的root密码
3. 点击"登录"
4. 左边是本地文件，右边是服务器文件
5. 将 `D:\06\six_springboot` 拖拽到服务器的 `/opt/aichat` 目录

## 第四步：连接服务器并部署

### 使用PowerShell SSH连接：

```powershell
# 连接服务器（替换YOUR_ALIYUN_IP）
ssh root@YOUR_ALIYUN_IP
```

输入root密码后登录。

### 在服务器上执行部署：

```bash
# 1. 进入项目目录
cd /opt/aichat

# 2. 给脚本执行权限
chmod +x deploy-aliyun.sh

# 3. 编辑脚本，修改密码和API密钥
nano deploy-aliyun.sh
```

**需要修改的地方**：
- 找到 `YourNewPassword123!` 改为你想要的MySQL密码（出现3处）
- 找到 `your_zhipu_api_key_here` 改为你的智谱AI API密钥

修改完成后，按 `Ctrl+O` 保存，`Ctrl+X` 退出。

```bash
# 4. 执行部署脚本（需要10-15分钟）
sudo ./deploy-aliyun.sh
```

## 第五步：验证部署

1. 等待脚本执行完成
2. 在浏览器访问：`http://你的ECS公网IP:8080/bai-e.html`
3. 如果能看到聊天界面，说明部署成功！

## 常用运维命令

```bash
# 查看应用日志（实时）
sudo journalctl -u aichat -f

# 查看最近100行日志
sudo journalctl -u aichat -n 100

# 重启应用
sudo systemctl restart aichat

# 停止应用
sudo systemctl stop aichat

# 查看应用状态
sudo systemctl status aichat

# 查看MySQL状态
sudo systemctl status mysqld

# 进入MySQL
mysql -u root -p
```

## 修改配置

如果需要修改配置（如API密钥）：

```bash
# 编辑服务文件
sudo nano /etc/systemd/system/aichat.service

# 修改Environment行中的配置，例如：
Environment=ZHIPU_API_KEY=新的API密钥

# 重新加载配置并重启
sudo systemctl daemon-reload
sudo systemctl restart aichat
```

## 故障排查

### 应用无法访问？

```bash
# 1. 检查服务状态
sudo systemctl status aichat

# 2. 查看日志
sudo journalctl -u aichat -f

# 3. 检查防火墙
sudo firewall-cmd --list-all

# 4. 检查端口是否监听
sudo netstat -tlnp | grep 8080

# 5. 检查阿里云安全组是否开放8080端口
# 登录阿里云控制台 -> ECS -> 安全组 -> 入站规则
```

### 数据库连接失败？

```bash
# 1. 检查MySQL状态
sudo systemctl status mysqld

# 2. 启动MySQL（如果没运行）
sudo systemctl start mysqld

# 3. 测试数据库连接
mysql -u root -p -e "USE aichat; SHOW TABLES;"
```

### 端口被占用？

```bash
# 查看8080端口被谁占用
sudo lsof -i :8080

# 或者
sudo netstat -tlnp | grep 8080
```

## 备份数据

```bash
# 备份数据库
mysqldump -u root -p aichat > /opt/aichat_backup_$(date +%Y%m%d).sql

# 恢复数据库
mysql -u root -p aichat < /opt/aichat_backup_20250101.sql
```

## 免费额度说明

阿里云新用户免费试用：
- **ECS实例**：2核2G，免费3个月
- **带宽**：1-5Mbps
- **存储**：40GB系统盘

3个月后需要付费，但费用很低（约50元/月）。

## 域名绑定（可选）

如果你想用域名访问：

1. 购买域名（阿里云域名注册）
2. 在阿里云控制台添加A记录，指向ECS公网IP
3. 安装Nginx反向代理：

```bash
# 安装Nginx
sudo yum install -y nginx

# 配置Nginx
sudo nano /etc/nginx/conf.d/aichat.conf
```

添加以下内容：

```nginx
server {
    listen 80;
    server_name your-domain.com;  # 改为你的域名

    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
}
```

```bash
# 启动Nginx
sudo systemctl start nginx
sudo systemctl enable nginx
```

现在可以通过 `http://your-domain.com/bai-e.html` 访问了！