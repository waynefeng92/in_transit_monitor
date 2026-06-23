# 在途车辆监控系统 (In-Transit Vehicle Monitor)

滚装船在途车辆运输监控系统，用于跟踪和管理车辆从出发到到达的全程运输状态。

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端 | Spring Boot 2.7.18 + Java 17 + MyBatis-Plus + MySQL 8.0 |
| 前端 | Vue 3 + Vite + Element Plus + ECharts + Pinia |
| 部署 | Docker Compose（nginx + backend + mysql 三容器）|

## 快速开始

```bash
# 1. 确保已安装 Docker + Docker Compose

# 2. 克隆项目
git clone <repo-url>
cd in_transit_monitor

# 3. 配置环境变量
cp .env.example .env
# 编辑 .env，至少修改：
#   DB_PASSWORD=你的数据库密码
#   ADMIN_DEFAULT_PASSWORD=管理员初始密码

# 4. 启动（首次约 5-10 分钟）
docker compose up -d

# 5. 访问
#   前端: http://localhost
#   登录: admin / 你设置的 ADMIN_DEFAULT_PASSWORD
```

> 后续迭代升级：`git pull && bash deploy.sh`（自动版本化构建+部署）

## 架构

```
用户 → [80] → nginx（前端静态文件）
                 ↓ /api/*
            Spring Boot [8080]
                 ↓
            MySQL 8.0 [3306]
```

- 仅 nginx 80 端口对外暴露
- 后端和 MySQL 位于 Docker 内部网络，不直接暴露
- 认证方式：Session Cookie（httpOnly + SameSite Lax）

## 配置说明

通过项目根目录的 `.env` 文件配置：

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `DB_PASSWORD` | (必填) | MySQL root 密码 |
| `DB_NAME` | `ro_ro_monitor` | 数据库名 |
| `ADMIN_DEFAULT_PASSWORD` | `admin123` | 管理员初始密码 |
| `CORS_ALLOWED_ORIGINS` | `http://localhost` | 允许跨域访问的来源 |
| `FRONTEND_PORT` | `80` | 前端 nginx 监听端口 |
| `BACKEND_TAG` | `latest` | 后端镜像版本标签（git SHA） |
| `FRONTEND_TAG` | `latest` | 前端镜像版本标签（git SHA） |
| `DB_USERNAME` | `roro_app` | 数据库连接用户名（专用用户，非 root） |

## 项目结构

```
in_transit_monitor/
├── ro-ro-monitor/                # Spring Boot 后端
│   ├── src/main/java/            # Java 源码
│   │   ├── config/               # 配置类（CORS、Security、Session 等）
│   │   ├── controller/           # REST API 控制器
│   │   ├── service/              # 业务逻辑层
│   │   ├── repository/           # MyBatis-Plus 数据访问
│   │   ├── entity/               # 实体类
│   │   └── handler/              # 全局异常处理
│   ├── src/main/resources/       # 配置文件和 SQL 脚本
│   └── Dockerfile                # 多阶段 Docker 构建
├── ro-ro-monitor-web/            # Vue 3 前端
│   ├── src/views/                # 页面组件
│   ├── src/stores/               # Pinia 状态管理
│   ├── src/api/                  # API 请求模块
│   ├── src/router/               # 路由 + 守卫
│   ├── nginx.conf                # nginx 反向代理配置
│   └── Dockerfile                # 多阶段 Docker 构建
├── docker-compose.yml            # 三容器编排
└── .env.example                  # 环境变量模板
├── deploy.sh                    # 一键构建+部署（版本化镜像标签）
├── rollback.sh                  # 一键回滚到上一版本
├── run-migration.sh            # 增量数据库迁移
├── backup.sh                   # 每日数据库备份（7天保留）
├── backups/                    # 备份文件输出目录
├── docs/
│   └── deployment-guide.md     # 生产部署指南
```

## 本地开发

```bash
# 后端启动（需要本地 MySQL）
cd ro-ro-monitor
mvn spring-boot:run

# 前端启动
cd ro-ro-monitor-web
npm run dev

# 后端测试
cd ro-ro-monitor && mvn test

# 前端测试
cd ro-ro-monitor-web && npm test
```

## 生产部署

### 日常迭代部署

```bash
# 拉取最新代码
git pull

# 如有数据库变更，先执行迁移
bash run-migration.sh

# 一键构建+部署（自动版本化标签 + 健康检查）
bash deploy.sh
```

### 回滚

```bash
# 回滚到上一版本（5 秒完成，无需重新构建）
bash rollback.sh
```

### 数据库迁移

新增迁移文件放在 `ro-ro-monitor/src/main/resources/sql/` 目录下，编号从 `5_` 开始：
```
5_add_new_feature.sql
```

执行迁移：
```bash
bash run-migration.sh
```
自动追踪已应用的迁移（`.last_migration`），幂等执行。

### 数据库备份

```bash
bash backup.sh
```
备份文件保存在 `backups/ro_ro_monitor_YYYYMMDD.sql`，自动保留最近 7 天。

设置定时备份（crontab）：
```bash
crontab -e
# 添加：
0 3 * * * cd /path/to/in_transit_monitor && bash backup.sh
```

### 安全说明

- **数据库用户**：应用使用专用 `roro_app` 用户连接（仅 SELECT/INSERT/UPDATE/DELETE），不直接使用 root
- **凭证轮换**：首次部署后应在系统内修改管理员默认密码
- **Actuator**：健康检查端点仅返回 `{"status":"UP"}`，不泄露内部详情
- **凭证管理**：`.env` 和 `application-prod.yml` 由 `.gitignore` 排除，不提交到仓库

## API 接口

| 方法 | 路径 | 说明 | 是否需要登录 |
|------|------|------|------------|
| POST | `/api/auth/login` | 登录 | 否 |
| POST | `/api/auth/logout` | 登出 | 否 |
| GET | `/api/auth/me` | 获取当前用户 | 是 |
| GET | `/actuator/health` | 健康检查 | 否 |
| GET | `/api/transit/summary` | 在途汇总数据 | 是 |
| GET | `/api/chart/*` | 图表数据 | 是 |
| GET | `/api/brand/*` | 品牌管理 | 是 |
| GET | `/api/port/*` | 港口管理 | 是 |
| GET | `/api/route/*` | 线路管理 | 是 |
| POST | `/api/upload/excel` | 导入 Excel | 是 |

## 默认管理员

- 用户名：`admin`
- 密码：由 `.env` 中 `ADMIN_DEFAULT_PASSWORD` 指定（默认 `admin123`）

首次启动会自动创建管理员账号。

## 注意事项

- 首次启动会自动执行 SQL 初始化脚本创建表结构（位于 `ro-ro-monitor/src/main/resources/sql/`）
- MySQL 数据存储在 Docker 命名卷 `mysql-data` 中，`docker compose down` 不丢失
- 所有 API（除 `/actuator/health` 和 `/api/auth/*`）均需登录后访问
- 更新代码后执行 `bash deploy.sh` 构建并部署（自动版本化镜像标签）
