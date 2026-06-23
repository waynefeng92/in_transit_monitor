# 在途车辆监控系统 — 生产部署指南

> **适用版本**: Docker 24+, Docker Compose v2
> **最后更新**: 2026-06-23

---

## 目录

1. [首次部署](#1-首次部署)
2. [日常迭代部署](#2-日常迭代部署)
3. [回滚](#3-回滚)
4. [备份与恢复](#4-备份与恢复)
5. [镜像管理](#5-镜像管理)
6. [安全配置](#6-安全配置)
7. [故障排查](#7-故障排查)
8. [环境变量参考](#8-环境变量参考)

---

## 1. 首次部署

### 1.1 前置条件

| 依赖 | 最低版本 | 验证命令 |
|------|----------|----------|
| Docker | 24+ | `docker --version` |
| Docker Compose | v2 | `docker compose version` |
| Git | 2.x | `git --version` |

确保已配置 Docker 镜像加速器（国内环境），否则首次构建拉取基础镜像可能较慢。

### 1.2 克隆项目

```bash
git clone <仓库地址>
cd in_transit_monitor
```

### 1.3 配置环境变量

从模板创建 `.env` 文件，并修改敏感信息：

```bash
cp .env.example .env
```

**必须修改的变量：**

| 变量 | 说明 | 建议 |
|------|------|------|
| `DB_PASSWORD` | MySQL root 密码 | 至少 16 位，包含大小写字母、数字、特殊字符 |
| `ADMIN_DEFAULT_PASSWORD` | 管理员初始密码 | 首次登录后立即修改 |
| `CORS_ALLOWED_ORIGINS` | 允许跨域来源 | 替换为你的服务器 IP 或域名，如 `http://192.168.1.100` |
| `FRONTEND_PORT` | 前端对外端口 | 默认 `80`，可根据需要修改，如 `8080` |

完整环境变量列表见[第 8 节](#8-环境变量参考)。

### 1.4 首次部署

执行构建与启动脚本（首次构建约需 5-10 分钟）：

```bash
bash deploy.sh
```

`deploy.sh` 执行流程：

1. 取当前 Git 短 SHA（如 `a3f2b01`）作为版本标签
2. 分别构建后端镜像 `roro-backend:$TAG` 和前端镜像 `roro-frontend:$TAG`
3. 如果存在 `.current_tag`，将其内容复制到 `.previous_tag`（首次不存在，跳过）
4. 导出 `BACKEND_TAG` 和 `FRONTEND_TAG` 环境变量
5. 执行 `docker compose up -d` 启动 MySQL、后端、前端三个容器
6. 轮询 `/actuator/health` 端点（每 5 秒一次，最多 24 次 = 120 秒）
7. 健康检查通过后，将当前标签写入 `.current_tag`
8. 超时则退出码 1，提示服务未就绪

```text
执行流程示意：

docker build → roro-backend:$TAG
docker build → roro-frontend:$TAG
.previous_tag ← .current_tag (如果存在)
docker compose up -d
health check 轮询 120s
.current_tag ← $TAG
```

### 1.5 执行数据库迁移（创建应用账号）

首次部署后，MySQL 初始化脚本会自动创建表结构。但还需要创建应用运行时的数据库用户：

```bash
bash run-migration.sh
```

`run-migration.sh` 会读取 `.env` 中的数据库凭据，通过 `docker exec` 执行 `sql/` 目录下的迁移 SQL 文件。`0_` 至 `4_` 前缀的文件为 MySQL 容器首次初始化脚本（由 `docker-entrypoint-initdb.d` 自动执行），脚本会自动跳过。其余 SQL 文件按文件名排序依次执行，并在 `.last_migration` 文件中记录进度，避免重复执行。

> **注意**：`roro_app` 用户的创建 SQL 需要手动放入 `sql/` 目录或以迁移脚本形式提供。首次部署请确认该用户已创建，否则后端启动时会报错 `Access denied for user 'roro_app'`。

### 1.6 验证部署

**命令行验证：**

```bash
curl -s http://localhost/actuator/health
# 预期输出: {"status":"UP"}
```

**浏览器验证：**

打开 `http://localhost`（或配置的 `FRONTEND_PORT` 端口），使用以下凭据登录：

- 用户名：`admin`
- 密码：在 `.env` 中配置的 `ADMIN_DEFAULT_PASSWORD` 值

登录成功即可正常使用系统。

---

## 2. 日常迭代部署

### 2.1 标准流程

```bash
git pull                           # 拉取最新代码
bash run-migration.sh              # 如有数据库变更，执行迁移
bash deploy.sh                     # 构建镜像并部署
```

**注意**：并非每次更新都需要运行 `run-migration.sh`。仅在拉取的代码包含新的 SQL 迁移文件时才需要执行。可先执行 `git diff --name-only HEAD@{1}` 查看变更文件列表判断是否需要。

### 2.2 deploy.sh 行为详解

| 步骤 | 操作 | 说明 |
|------|------|------|
| 1 | `git rev-parse --short HEAD` | 获取当前 Git 提交的 7 位短 SHA |
| 2 | `docker build -t roro-backend:$TAG` | 构建后端镜像 |
| 3 | `docker build -t roro-frontend:$TAG` | 构建前端镜像 |
| 4 | 备份 `.current_tag` → `.previous_tag` | 保留回滚锚点 |
| 5 | `export BACKEND_TAG=$TAG FRONTEND_TAG=$TAG` | 设置镜像标签 |
| 6 | `docker compose up -d` | 重新创建并启动容器 |
| 7 | `echo "$TAG" > .current_tag` | 记录当前版本 |
| 8 | 健康检查轮询（最多 120 秒） | 等待 `/actuator/health` 返回 200 |

### 2.3 版本追踪文件

部署脚本会在项目根目录生成两个标签文件：

| 文件 | 内容 | 用途 |
|------|------|------|
| `.current_tag` | 当前运行版本的 Git SHA | 查看当前版本 |
| `.previous_tag` | 上一次部署的 Git SHA | 回滚锚点 |

--- 查看当前版本：

```bash
cat .current_tag
# 输出示例: a3f2b01
```

---

## 3. 回滚

### 3.1 回滚操作

```bash
bash rollback.sh
```

### 3.2 回滚流程

1. 检查 `.previous_tag` 是否存在，不存在则报错退出
2. 读取上一个版本的标签
3. 将 `.current_tag` 写入 `.previous_tag`（当前版本变为"上一个版本"）
4. 设置 `BACKEND_TAG` 和 `FRONTEND_TAG` 为上一个版本的标签
5. 执行 `docker compose up -d`，使用旧镜像重建容器
6. 将标签写入 `.current_tag`
7. 健康检查轮询（最多 120 秒）

### 3.3 重要限制

> ⚠️ **`rollback.sh` 仅回滚代码/镜像，不会回滚数据库迁移。**

Docker Compose 以旧标签启动容器时，使用的是之前构建的旧镜像。但如果新部署执行了 `run-migration.sh` 对数据库进行了 DDL 变更（如新增字段、修改表结构），回滚到旧代码可能导致业务异常。

**建议**：每次编写数据库迁移 SQL 文件时，同时编写对应的**逆向 SQL**（回滚脚本），以便在需要时手动执行回滚。将逆向脚本命名为 `{迁移文件名}_rollback.sql` 存放于 `sql/rollback/` 目录。

### 3.4 手动回滚到指定版本

如果 `.previous_tag` 不可用，也可以直接指定标签回滚：

```bash
# 查看本地已有的镜像
docker images | grep roro

# 指定标签重新部署
export BACKEND_TAG=<目标标签> FRONTEND_TAG=<目标标签>
docker compose up -d

# 更新版本文件
echo "<目标标签>" > .current_tag
```

---

## 4. 备份与恢复

### 4.1 执行备份

```bash
bash backup.sh
```

备份脚本行为：

1. 在项目根目录创建 `backups/` 目录
2. 从 `.env` 读取数据库凭据
3. 使用 `mysqldump` 导出 `ro_ro_monitor` 数据库（`--single-transaction --routines --triggers`）
4. 生成文件名为 `backups/ro_ro_monitor_YYYYMMDD.sql`
5. 自动清理 7 天前的备份文件

**备份包含的内容**：表结构、存储过程（routines）、触发器（triggers）、全部数据。

### 4.2 定时备份（crontab）

```bash
# 每天凌晨 3 点执行备份
0 3 * * * cd /path/to/in_transit_monitor && bash backup.sh >> /var/log/roro-backup.log 2>&1
```

推荐同时将备份文件同步到远程存储（如对象存储、NAS）以防磁盘故障。

### 4.3 恢复数据

```bash
# 从备份文件恢复
cat backups/ro_ro_monitor_20260622.sql | docker exec -i roro-mysql mysql -uroot -p"${DB_PASSWORD}" ro_ro_monitor
```

**恢复注意事项**：

- 确保 MySQL 容器正在运行
- 恢复前建议先对当前数据库做一次备份
- 如果备份文件较大，恢复可能需要几分钟
- 恢复完成后建议重启后端容器使连接池重建：

```bash
docker compose restart backend
```

### 4.4 只恢复特定表

```bash
# 先提取单表数据
grep -A 999999 "Table structure for table \`your_table\`" backups/ro_ro_monitor_20260622.sql | \
  grep -B 999999 "Table structure for table \`" | head -n -2 | \
  docker exec -i roro-mysql mysql -uroot -p"${DB_PASSWORD}" ro_ro_monitor
```

---

## 5. 镜像管理

### 5.1 查看当前版本

```bash
# 当前运行版本
cat .current_tag

# 上一次部署版本
cat .previous_tag

# 查看正在运行的容器使用的镜像
docker ps --filter "name=roro" --format "table {{.Names}}\t{{.Image}}"
```

### 5.2 列出本地镜像

```bash
# 列出所有 roro 相关镜像
docker images | grep roro

# 包含镜像大小
docker images --format "table {{.Repository}}\t{{.Tag}}\t{{.Size}}\t{{.CreatedAt}}" | grep roro
```

### 5.3 清理旧镜像

随着持续部署，本地会积累大量旧版本镜像。建议保留最近 5 个版本的镜像，删除更早的。

```bash
# 查看 roro-backend 镜像及标签
docker images roro-backend --format "{{.Tag}}" | sort -r

# 保留最近 5 个镜像，删除其余
docker images roro-backend --format "{{.Repository}}:{{.Tag}}" | \
  tail -n +6 | xargs -r docker rmi

docker images roro-frontend --format "{{.Repository}}:{{.Tag}}" | \
  tail -n +6 | xargs -r docker rmi

# 清理悬空镜像（dangling images）
docker image prune -f
```

> ⚠️ **警告**：请勿删除 `.previous_tag` 对应的镜像！该镜像是 rollback.sh 的回滚目标。建议在清理时排除 `.previous_tag` 和 `.current_tag` 中记录的标签：

```bash
# 安全清理：保留当前版本和上一版本的镜像
KEEP_TAGS=$(cat .current_tag)
KEEP_TAGS="$KEEP_TAGS $(cat .previous_tag 2>/dev/null)"

for repo in roro-backend roro-frontend; do
  docker images $repo --format "{{.Tag}}" | while read tag; do
    if ! echo "$KEEP_TAGS" | grep -qw "$tag"; then
      docker rmi "${repo}:${tag}" 2>/dev/null || true
    fi
  done
done
```

### 5.4 每日部署产生的镜像数量

每次 `deploy.sh` 会生成 2 个新镜像（`roro-backend` 和 `roro-frontend`）。以每日部署一次计算，保留 5 个版本约占用磁盘 5-10 GB（取决于项目规模）。建议定期运行清理脚本或配置 crontab 自动清理。

---

## 6. 安全配置

### 6.1 数据库用户权限

应用运行时使用的是**最小权限账号**，而非 root：

- **`root`**：仅通过 MySQL 容器的初始化脚本（`docker-entrypoint-initdb.d`）和运维脚本（`run-migration.sh`、`backup.sh`）使用
- **`roro_app`**：后端应用连接数据库的账号，仅授予以下权限：

```sql
GRANT SELECT, INSERT, UPDATE, DELETE ON ro_ro_monitor.* TO 'roro_app'@'%';
-- roro_app 没有 DDL 权限（CREATE/ALTER/DROP 等），无法修改表结构
```

创建 `roro_app` 用户的 SQL 示例（需以迁移脚本形式放入 `sql/` 目录）：

```sql
CREATE USER IF NOT EXISTS 'roro_app'@'%' IDENTIFIED BY '你的应用数据库密码';
GRANT SELECT, INSERT, UPDATE, DELETE ON ro_ro_monitor.* TO 'roro_app'@'%';
FLUSH PRIVILEGES;
```

### 6.2 凭据轮换

#### 更换 MySQL root 密码

```bash
# 进入 MySQL 容器
docker exec -it roro-mysql mysql -uroot -p

# 在 MySQL 中执行
ALTER USER 'root'@'%' IDENTIFIED BY '新密码';
ALTER USER 'root'@'localhost' IDENTIFIED BY '新密码';
FLUSH PRIVILEGES;
```

然后更新 `.env` 中的 `DB_PASSWORD`：

```bash
# 编辑 .env
vim .env
# 修改: DB_PASSWORD=新密码
```

#### 更换 roro_app 密码

```bash
docker exec -it roro-mysql mysql -uroot -p

# 在 MySQL 中执行
ALTER USER 'roro_app'@'%' IDENTIFIED BY '新密码';
FLUSH PRIVILEGES;
```

更新 `.env` 中的 `DB_USERNAME`…不对，`roro_app` 的密码在 `docker-compose.yml` 中直接使用的是 `DB_PASSWORD`（与 root 共用）。如果希望 `roro_app` 使用独立密码，需要：

1. 在 `.env` 中新增 `APP_DB_PASSWORD` 变量
2. 修改 `docker-compose.yml` 中 backend 服务的 `DB_PASSWORD` 环境变量引用为新变量

### 6.3 应用安全配置

**Actuator 端点安全**：

生产环境的 Actuator 配置（位于 `ro-ro-monitor/src/main/resources/application-prod.properties`）已将 `management.endpoint.health.show-details` 设置为 `never`，外部无法通过 `/actuator/health` 获取详细健康信息。

**CORS 配置**：

`.env` 中的 `CORS_ALLOWED_ORIGINS` 变量控制允许跨域访问的来源。生产环境应设置为具体的服务器 IP 或域名，**不要使用通配符 `*`**：

```bash
# 正确写法（单域名）
CORS_ALLOWED_ORIGINS=http://your-domain.com

# 正确写法（多个来源，逗号分隔）
CORS_ALLOWED_ORIGINS=http://192.168.1.100,http://backup-server.local
```

**Session 安全**：

- Session Cookie 标记为 `httpOnly`（JavaScript 不可访问）
- `SameSite` 设置为 `Lax`（防止 CSRF 攻击）
- 详见 `ro-ro-monitor/src/main/java/com/company/roro/config/SessionConfig.java`

### 6.4 .env 文件保护

```bash
# 限制 .env 文件访问权限
chmod 600 .env

# 确保 .env 已加入 .gitignore
echo ".env" >> .gitignore
```

---

## 7. 故障排查

### 7.1 容器状态检查

```bash
# 查看所有容器状态
docker compose ps

# 查看指定容器日志
docker compose logs --tail=50 backend
docker compose logs --tail=50 mysql
docker compose logs --tail=50 frontend

# 实时跟踪日志
docker compose logs -f backend
```

### 7.2 后端启动失败

**症状**：`/actuator/health` 返回非 200，或容器反复重启。

**常见原因及排查**：

| 症状 | 可能原因 | 排查方法 |
|------|----------|----------|
| `Access denied for user 'roro_app'` | 数据库用户不存在或密码错误 | 检查是否已通过迁移脚本创建 `roro_app` 用户 |
| `CannotGetJdbcConnection` | MySQL 容器未就绪或网络不通 | `docker compose logs mysql` 查看 MySQL 是否启动成功 |
| `CommunicationsException` | MySQL 拒绝连接 | 检查 `docker-compose.yml` 中 backend 的 `DB_HOST` 是否为 `mysql`（容器网络内应使用服务名） |
| `Table 'xxx' doesn't exist` | 数据库表未创建 | 检查 `sql/` 目录中的初始化脚本是否完整，重新运行 `run-migration.sh` |

### 7.3 前端健康检查失败

**已知问题**：前端 nginx 容器的 `/actuator/health` 端点透传可能存在问题，这是 nginx 反代健康检查的已知行为，**不影响前端页面的正常访问和功能**。

如果 `deploy.sh` 的健康检查报超时错误，请先手动验证：

```bash
# 1. 确认后端直接可达（内部端口 8080）
docker exec roro-backend curl -s http://localhost:8080/actuator/health

# 2. 确认前端端口映射
docker port roro-frontend
# 预期输出: 80/tcp -> 0.0.0.0:8080 (或你配置的 FRONTEND_PORT)

# 3. 确认前端页面可以访问
curl -s http://localhost/ | head -5
```

### 7.4 访问返回 000 或连接被拒绝

```bash
# 检查端口映射是否正确
docker port roro-frontend

# 检查防火墙是否放行对应端口
sudo ufw status           # Ubuntu/Debian
sudo firewall-cmd --list-all  # CentOS/RHEL

# 确认 FRONTEND_PORT 配置
grep FRONTEND_PORT .env
```

### 7.5 验证数据库密码

```bash
# 加载环境变量
source .env

# 使用 mysql 客户端测试连接
docker exec -it roro-mysql mysql -uroot -p"${DB_PASSWORD}" -e "SELECT 1;"

# 验证 roro_app 用户
docker exec -it roro-mysql mysql -uroot -p"${DB_PASSWORD}" -e \
  "SELECT User, Host FROM mysql.user WHERE User='roro_app';"

# 验证数据库是否存在
docker exec -it roro-mysql mysql -uroot -p"${DB_PASSWORD}" -e "SHOW DATABASES;" | grep ro_ro_monitor
```

### 7.6 磁盘空间不足

长期运行后，Docker 可能积累大量未使用的镜像、容器和数据卷：

```bash
# 检查磁盘使用
df -h

# 检查 Docker 磁盘占用
docker system df

# 清理未使用的资源（谨慎操作）
docker system prune -a --volumes
```

### 7.7 端口冲突

如果部署时提示端口已被占用：

```bash
# 查看端口占用
sudo lsof -i :80           # 或你的 FRONTEND_PORT
sudo ss -tlnp | grep :80

# 方案 1: 修改 FRONTEND_PORT
echo "FRONTEND_PORT=8080" >> .env

# 方案 2: 停止占用进程
sudo systemctl stop nginx  # 如果被系统 nginx 占用
```

---

## 8. 环境变量参考

所有环境变量在项目根目录的 `.env` 文件中配置。以下为完整变量列表：

| 变量 | 默认值 | 必填 | 说明 |
|------|--------|------|------|
| `DB_HOST` | `mysql` | 是 | MySQL 服务主机名（Docker 内部网络使用服务名） |
| `DB_PORT` | `3306` | 是 | MySQL 端口（同时作用于外部暴露端口） |
| `DB_NAME` | `ro_ro_monitor` | 是 | 数据库名称 |
| `DB_USERNAME` | `roro_app` | 是 | 后端连接数据库的用户名 |
| `DB_PASSWORD` | — | **是** | MySQL root 密码与应用密码（当前共用） |
| `ADMIN_DEFAULT_PASSWORD` | `admin123` | 是 | 管理员初始登录密码（首次登录后修改） |
| `CORS_ALLOWED_ORIGINS` | `http://localhost` | 是 | 允许跨域访问的来源地址，多个用逗号分隔 |
| `FRONTEND_PORT` | `80` | 否 | 前端 nginx 容器对外暴露的端口 |
| `BACKEND_TAG` | `latest` | 否 | 后端镜像标签（`deploy.sh` 会自动覆写） |
| `FRONTEND_TAG` | `latest` | 否 | 前端镜像标签（`deploy.sh` 会自动覆写） |

### 8.1 开发环境 vs 生产环境

| 场景 | `FRONTEND_PORT` | `CORS_ALLOWED_ORIGINS` | `ADMIN_DEFAULT_PASSWORD` |
|------|-----------------|------------------------|--------------------------|
| 本地开发 | `80` / `8080` | `http://localhost` | 可简单设置 |
| 内网部署 | `80` | `http://192.168.x.x` | 建议使用强密码 |
| 公网部署 | `80` / `443`（需反向代理） | `https://your-domain.com` | **必须使用强密码且立即更换** |

### 8.2 变量作用域

环境变量在不同组件中的使用范围：

```
.env
├── docker-compose.yml
│   ├── mysql 容器:    DB_PASSWORD, DB_NAME, DB_PORT
│   ├── backend 容器:  DB_HOST, DB_PORT, DB_NAME, DB_USERNAME,
│   │                  DB_PASSWORD, ADMIN_DEFAULT_PASSWORD,
│   │                  CORS_ALLOWED_ORIGINS
│   └── frontend 容器: FRONTEND_PORT
│
├── deploy.sh:         BACKEND_TAG, FRONTEND_TAG (自动设置)
├── rollback.sh:       BACKEND_TAG, FRONTEND_TAG (自动设置)
├── run-migration.sh:  DB_PASSWORD, DB_NAME
├── backup.sh:         DB_PASSWORD, DB_NAME
└── Spring Boot 应用:  ADMIN_DEFAULT_PASSWORD, CORS_ALLOWED_ORIGINS,
                        DB_HOST, DB_PORT, DB_NAME, DB_USERNAME, DB_PASSWORD
```

---

## 附录 A：常用命令速查

```bash
# ===== 部署与回滚 =====
bash deploy.sh                        # 部署当前代码
bash rollback.sh                      # 回滚到上一个版本
bash run-migration.sh                 # 执行数据库迁移

# ===== 容器管理 =====
docker compose ps                     # 查看容器状态
docker compose logs -f backend        # 跟踪后端日志
docker compose restart backend        # 重启后端
docker compose down                   # 停止所有容器（保留数据卷）
docker compose down -v                # 停止并删除数据卷（⚠️ 数据丢失）

# ===== 健康检查 =====
curl -s http://localhost/actuator/health     # 健康检查
curl -s http://localhost/api/auth/me         # 需要登录态测试

# ===== 备份与恢复 =====
bash backup.sh                                  # 备份数据库
cat backups/ro_ro_monitor_*.sql | \
  docker exec -i roro-mysql mysql -uroot -p"${DB_PASSWORD}" ro_ro_monitor  # 恢复

# ===== 版本管理 =====
cat .current_tag                      # 查看当前版本
cat .previous_tag                     # 查看上一版本
docker images | grep roro             # 列出镜像
```

## 附录 B：目录结构（部署相关）

```
in_transit_monitor/
├── .env                      # 环境变量（不纳入版本控制）
├── .env.example              # 环境变量模板
├── .current_tag              # 当前部署的 Git SHA（自动生成）
├── .previous_tag             # 上一次部署的 Git SHA（自动生成）
├── .last_migration           # 最后执行的迁移文件名（自动生成）
├── backups/                  # 数据库备份目录（自动创建）
│   └── ro_ro_monitor_YYYYMMDD.sql
├── docker-compose.yml        # Docker Compose 编排文件
├── deploy.sh                 # 部署脚本
├── rollback.sh               # 回滚脚本
├── run-migration.sh          # 数据库迁移脚本
├── backup.sh                 # 数据库备份脚本
├── ro-ro-monitor/            # 后端源码 + Dockerfile
├── ro-ro-monitor-web/        # 前端源码 + Dockerfile + nginx.conf
└── docs/
    └── deployment-guide.md   # 本文档
```

---

> **如有问题，请检查对应章节的故障排查步骤。欢迎提交 Issue 或补充文档。**
