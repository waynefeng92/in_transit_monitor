# 在途车辆监控系统 — Windows Server 部署指南

> **适用版本**: Windows Server 2012 R2
> **部署架构**: Nginx(80) → Spring Boot(8080) → MySQL 8.0(3307)
> **最后更新**: 2026-06-29

---

## 目录

1. [概述](#1-概述)
2. [服务器环境准备](#2-服务器环境准备)
3. [基础数据迁移](#3-基础数据迁移)
4. [应用首次部署](#4-应用首次部署)
5. [日常迭代升级](#5-日常迭代升级)
6. [数据库运维](#6-数据库运维)
7. [服务管理](#7-服务管理)
8. [故障排查](#8-故障排查)
9. [附录：目录结构](#9-附录目录结构)

---

## 1. 概述

### 部署架构

```
浏览器用户
    │
    ▼  [80 端口]
┌─────────────────────────────────────────────┐
│  Nginx for Windows                           │
│  ├── 静态文件托管（Vue 前端 dist）              │
│  └── 反向代理 /api/* → localhost:8080         │
└─────────────────────────────────────────────┘
    │  [8080 端口]
┌─────────────────────────────────────────────┐
│  Spring Boot JAR（NSSM 服务）                 │
│  └── 业务逻辑、认证、监控                      │
└─────────────────────────────────────────────┘
    │  [3307 端口]
┌─────────────────────────────────────────────┐
│  MySQL 8.0                                   │
│  └── 数据库：ro_ro_monitor                    │
└─────────────────────────────────────────────┘
```

### 与 Docker 版的关键差异

| 项目 | Docker 版 | Windows 版 |
|------|-----------|------------|
| 后端运行 | Docker 容器 | NSSM Windows 服务 |
| Nginx 配置 | 只有 server 块 | 完整配置（worker_processes + events + http） |
| proxy_pass | `http://backend:8080`（Docker 网络） | `http://localhost:8080` |
| 数据库端口 | 3306 | **3307**（与现有数据库隔离） |
| 前端构建 | Docker 多阶段构建 | 开发机构建后拷贝 dist |
| 部署方式 | `bash deploy.sh` | `deploy-win.bat` |

---

## 2. 服务器环境准备

### 2.1 安装 JDK 17

> Eclipse Temurin JDK 17 **官方支持** Windows Server 2012 R2（Red Hat 企业支持至 2027 年 12 月）。

1. 下载 JDK 17：https://adoptium.net/temurin/releases/?version=17
   - 选择 Windows x64 版本（.msi 安装包）
2. 安装到 `C:\Program Files\Java\jdk-17\`
3. **不设置系统环境变量**——本项目通过脚本和 NSSM 服务配置直接引用 JDK 17 的完整路径（`C:\Program Files\Java\jdk-17\bin\java.exe`），不影响服务器上已有的 JDK 8 和其他应用
4. 验证安装（使用完整路径）：
   ```cmd
   "C:\Program Files\Java\jdk-17\bin\java" -version
   ```
   预期输出包含 `openjdk version "17"`

**与现有 JDK 8 共存**：服务器已有 JDK 8 且 `JAVA_HOME`/`Path` 指向 JDK 8——保持不变。本项目所有脚本和服务配置均硬编码 JDK 17 的完整路径，互不干扰。

### 2.2 安装 MySQL 8.0

> **前置条件**：MySQL 8.0 安装器需要 .NET Framework 4.5.2+。Windows Server 2012 R2 默认仅带 4.5.1，需先安装 [.NET Framework 4.5.2 Runtime](https://dotnet.microsoft.com/en-us/download/dotnet-framework/net452)，重启后再继续。

1. 下载 MySQL 8.0：https://dev.mysql.com/downloads/mysql/
   - 选择 Windows (x86, 64-bit), MSI Installer
2. 安装时选择端口 **3307**（服务器已有其他数据库占用了 3306）
3. 设置 root 密码（请使用强密码，至少 16 位）
4. 字符集选择 **utf8mb4**
5. 安装完成后验证：
   ```cmd
   mysql -u root -p -P 3307 -e "SELECT VERSION();"
   ```

### 2.3 安装 Nginx for Windows

1. 下载 Nginx for Windows：https://nginx.org/en/download.html
   - 选择 `nginx/Windows-*` 稳定版（zip 包）
2. 解压到 `D:\in_transit_monitor\nginx\`
3. 将项目仓库中的 `ro-ro-monitor-web/nginx-windows.conf` 复制到 `D:\in_transit_monitor\nginx\conf\` 并**重命名为 `nginx.conf`**（nginx 默认读这个文件名），或直接在该路径下手动创建 `nginx.conf`
4. 验证配置：
   ```cmd
   cd D:\in_transit_monitor\nginx
   .\nginx -t
   ```
   预期输出：`test is successful`

### 2.4 安装 NSSM

1. 下载 NSSM：https://nssm.cc/download
   - 选择最新的稳定版（zip 包）
2. 解压到 `D:\in_transit_monitor\nssm\`
3. 将 NSSM 添加到系统 PATH：添加 `D:\in_transit_monitor\nssm\win64` 到系统变量 `Path`
4. 验证安装：打开新的命令提示符窗口，运行：
   ```cmd
   nssm --version
   ```

### 2.5 防火墙配置

以管理员身份运行以下命令：

```cmd
REM 放行 80 端口（前端访问）
netsh advfirewall firewall add rule name="RoRo-Nginx(80)" dir=in action=allow protocol=TCP localport=80

REM 阻止 3307 端口外部访问（仅允许本机连接 MySQL）
REM Windows 默认会阻止入站端口，但如果之前放过宽，可添加阻止规则：
netsh advfirewall firewall add rule name="RoRo-MySQL(3307-block)" dir=in action=block protocol=TCP localport=3307
```

---

## 3. 基础数据迁移

> 从本地开发 MySQL 导出 10 张基础信息表，导入到 Windows Server 的 MySQL 8.0。
> **注意**：业务数据（订单、在途记录、上传批次、监控快照）不迁移，在 Windows 上通过 Excel 重新导入。

### 3.1 在开发机上导出基础数据

`build-for-win.sh` 会自动调用导出，**无需单独运行**。执行时弹出一次密码输入，10 张表一次性导出。

```bash
cd /path/to/in_transit_monitor
bash scripts/build-for-win.sh
```

如需单独导出（不构建），可手动运行：
```bash
bash scripts/export-master-data.sh -h localhost -u root -P 3306 -o master-data.sql
```

导出内容（10 张表）：
| 序号 | 表名 | 内容 |
|------|------|------|
| 1 | `brand_dict` | 品牌字典（上汽大众、特斯拉等） |
| 2 | `port_dict` | 港口字典（上海南港、大连港等） |
| 3 | `monitor_status_dict` | 监控状态（正常/预警/超期） |
| 4 | `transport_status_dict` | 在途状态（未出库→已到达） |
| 5 | `users` | 用户账号（含 admin） |
| 6 | `location_alias` | 地点别名 |
| 7 | `excel_field_mapping` | Excel 字段映射（按品牌） |
| 8 | `excel_parse_config` | Excel 解析配置 |
| 9 | `route_dict` | 线路表（品牌+港口+城市） |
| 10 | `route_otd_config` | OTD 时效配置 |

### 3.2 检查导出文件

```bash
# 开发机
head -50 master-data.sql
wc -l master-data.sql
grep "INSERT INTO" master-data.sql | wc -l
# 预期：10 个 INSERT INTO（10 张表）
```

### 3.3 传输到 Windows Server

将 `master-data.sql` 拷贝到 Windows Server（可使用 U 盘、网络共享、scp 等方式），放到 `D:\in_transit_monitor\data\`。

**如果使用了 `build-for-win.sh` 打包**，`master-data.sql` 会自动包含在 `roro-windows-deploy.zip` 的 `data/` 目录中。

### 3.4 建表 DDL（首次）

**以 root 用户执行**（⚠️ 该 SQL 含 `DROP DATABASE IF EXISTS`，仅首次运行！）：

```cmd
REM Windows Server
mysql -u root -p -P 3307 < D:\in_transit_monitor\sql\0_ro_ro_monitor_full.sql
```

验证表已创建：
```cmd
mysql -u root -p -P 3307 -e "USE ro_ro_monitor; SHOW TABLES;"
```

### 3.5 导入基础数据

```cmd
REM Windows Server
mysql -u root -p -P 3307 ro_ro_monitor < D:\in_transit_monitor\data\master-data.sql
```

### 3.6 创建应用用户 roro_app

```cmd
REM Windows Server — 以 root 连接 MySQL
mysql -u root -p -P 3307
```

在 MySQL 提示符下执行：

```sql
CREATE USER 'roro_app'@'localhost' IDENTIFIED BY '设置一个强密码';
GRANT SELECT, INSERT, UPDATE, DELETE ON ro_ro_monitor.* TO 'roro_app'@'localhost';
FLUSH PRIVILEGES;
```

> **注意**：`roro_app` 仅有 CRUD 权限（无 DDL 权限）。创建表、修改表结构等操作必须用 `root`。

### 3.7 验证导入

```cmd
mysql -u root -p -P 3307 -e "USE ro_ro_monitor; SELECT COUNT(*) AS brands FROM brand_dict; SELECT COUNT(*) AS ports FROM port_dict; SELECT COUNT(*) AS routes FROM route_dict;"
```

---

## 4. 应用首次部署

### 4.1 在开发机构建部署包

```bash
# 开发机（Linux/macOS）
cd /path/to/in_transit_monitor
bash scripts/build-for-win.sh
```

成功执行后，在 `scripts/` 目录下生成 `roro-windows-deploy.zip`。

### 4.2 拷贝到 Windows Server

将 `roro-windows-deploy.zip` 拷贝到 Windows Server。

### 4.3 解压

```cmd
REM Windows Server
REM 将 zip 解压到 D:\in_transit_monitor\
REM 可以使用 7-Zip 或其他工具解压，确保目录结构如下：
REM
REM D:\in_transit_monitor\
REM ├── backend\ro-ro-monitor.jar
REM ├── frontend\ (dist 文件)
REM ├── data\master-data.sql
REM ├── sql\0_ro_ro_monitor_full.sql
REM └── env.bat
```

### 4.4 修改 env.bat 配置

编辑 `D:\in_transit_monitor\env.bat`，将以下变量改为实际值：

```bat
set DB_PASSWORD=你设置的数据库密码
set ADMIN_DEFAULT_PASSWORD=管理员登录密码（首次登录后修改）
set CORS_ALLOWED_ORIGINS=http://服务器IP地址
```

### 4.5 注册 Windows 服务

**以管理员身份运行**命令提示符：

```cmd
REM Windows Server
cd D:\in_transit_monitor
scripts\register-service.bat
```

该脚本会：
1. 检查 NSSM 和 JDK 17 是否就绪
2. 注册 `roro-backend` 服务（开机自启）
3. 设置所有环境变量
4. 启动服务
5. 设置崩溃自动重启（5 秒后，10 秒间隔）

### 4.6 执行首次部署

```cmd
REM Windows Server
cd D:\in_transit_monitor
deploy-win.bat
```

`deploy-win.bat` 会自动判断是首次部署：
1. 创建目录结构
2. 初始化数据库（DDL + 基础数据）
3. 部署后端 JAR + 前端文件
4. 启动服务
5. 轮询健康检查

### 4.7 启动 Nginx

如果 Nginx 未自动启动：

```cmd
REM Windows Server
cd D:\in_transit_monitor\nginx
start nginx
```

### 4.8 验证部署

```cmd
REM 验证健康检查
curl http://localhost:8080/actuator/health

REM 验证前端可访问
curl http://localhost/ | head -10

REM 验证 API 可访问
curl http://localhost/api/auth/me
```

在浏览器中访问 `http://服务器IP`，使用管理员账号登录。

---

## 5. 日常迭代升级

### 5.1 开发机重新构建

```bash
cd /path/to/in_transit_monitor
git pull                        # 拉取最新代码
bash scripts/build-for-win.sh   # 重新构建
```

### 5.2 拷贝部署包到服务器

将最新的 `roro-windows-deploy.zip` 拷贝到 Windows Server。

### 5.3 服务器执行更新

将 zip 解压到临时目录，然后运行：

```cmd
REM Windows Server — 迭代部署会自动跳过数据库初始化
REM 将解压后的 deploy-win.bat + backend/ + frontend/ 放到 D:\in_transit_monitor\ 同级
D:\in_transit_monitor\deploy-win.bat
```

`deploy-win.bat` 的迭代模式会：
1. 备份当前 JAR（带时间戳）
2. 停止后端服务（NSSM stop）
3. 停止 Nginx（taskkill）
4. 替换 JAR 和前端文件
5. 启动后端服务
6. 健康检查（最多 120 秒）
7. 启动 Nginx

**不涉及数据库变更时**，无需执行数据库脚本。如本次更新包含数据库迁移，请在部署前手动执行：

```cmd
REM 以 root 执行迁移 SQL（如有）
mysql -u root -p -P 3307 ro_ro_monitor < D:\path\to\migration.sql
```

---

## 6. 数据库运维

### 6.1 手动备份

```cmd
REM Windows Server
mysqldump -u root -p -P 3307 --single-transaction --routines --triggers ro_ro_monitor > D:\in_transit_monitor\backups\ro_ro_monitor_%DATE:~0,4%%DATE:~5,2%%DATE:~8,2%.sql
```

### 6.2 恢复数据

```cmd
REM Windows Server
mysql -u root -p -P 3307 ro_ro_monitor < D:\in_transit_monitor\backups\ro_ro_monitor_20260629.sql
```

### 6.3 基础信息更新

品牌、港口、线路、OTD 时效、Excel 映射等基础信息，建议通过系统管理页面进行增删改操作，避免手动修改数据库。

---

## 7. 服务管理

### 7.1 后端服务（NSSM）

```cmd
REM 查看状态
nssm status roro-backend

REM 停止服务
nssm stop roro-backend

REM 启动服务
nssm start roro-backend

REM 重启服务
nssm restart roro-backend
```

### 7.2 查看日志

```cmd
REM 后端日志
type D:\in_transit_monitor\logs\roro-monitor.log

REM Nginx 访问日志
type D:\in_transit_monitor\nginx\logs\access.log

REM Nginx 错误日志
type D:\in_transit_monitor\nginx\logs\error.log
```

### 7.3 Nginx 管理

```cmd
REM 启动 Nginx
cd D:\in_transit_monitor\nginx && start nginx

REM 停止 Nginx
taskkill /f /im nginx.exe

REM 重新加载配置（不中断服务）
cd D:\in_transit_monitor\nginx && .\nginx -s reload
```

> ⚠️ **Nginx 不会自动开机自启！**
> 如需 Nginx 随系统启动，可用 NSSM 将 nginx 注册为服务：
> ```cmd
> nssm install roro-nginx "D:\in_transit_monitor\nginx\nginx.exe"
> nssm set roro-nginx AppDirectory D:\in_transit_monitor\nginx
> nssm set roro-nginx Start SERVICE_AUTO_START
> ```

---

## 8. 故障排查

### 8.1 端口冲突

```cmd
REM 查看端口占用
netstat -ano | findstr :80
netstat -ano | findstr :8080
netstat -ano | findstr :3307

REM 查看对应进程
tasklist /FI "PID eq <PID>"
```

**常见冲突**：
- 80 端口：被系统 IIS 占用 → 停止 IIS 服务或在防火墙中禁用
- 8080 端口：被其他 Java 程序占用 → 修改 SERVER_PORT 环境变量
- 3307 端口：被其他 MySQL 实例占用 → 确认安装时选择的端口

### 8.2 后端服务启动失败

**症状**：NSSM 服务状态显示已停止，或健康检查不通过

**排查步骤**：

```cmd
REM 1. 查看 NSSM 状态
nssm status roro-backend

REM 2. 查看 Windows 事件查看器 → Windows 日志 → 应用程序
eventvwr.msc

REM 3. 直接运行 JAR 测试（手动启动看报错）
java -jar D:\in_transit_monitor\backend\ro-ro-monitor.jar --spring.profiles.active=prod
```

**常见原因**：

| 症状 | 可能原因 | 解决方法 |
|------|----------|----------|
| `Access denied for user 'roro_app'` | 数据库用户未创建或密码错误 | 检查 env.bat 中的 DB_PASSWORD，用 root 验证 roro_app 是否存在 |
| `CannotGetJdbcConnection` | MySQL 未启动或端口错误 | `mysql -u root -p -P 3307 -e "SELECT 1;"` 测试连接 |
| `Table 'xxx' doesn't exist` | 数据库表未创建 | 重新执行 `0_ro_ro_monitor_full.sql` |
| `Failed to bind to 8080` | 端口被占用 | 修改 SERVER_PORT 或停止占用进程 |

### 8.3 数据库连接失败

```cmd
REM 测试 MySQL 是否运行
mysql -u root -p -P 3307 -e "SELECT 1;"

REM 测试 roro_app 用户
mysql -u roro_app -p -P 3307 -h localhost -e "SELECT COUNT(*) FROM ro_ro_monitor.brand_dict;"

REM 检查 MySQL 服务状态
net start | findstr MySQL
```

### 8.4 Nginx 返回 404

| 原因 | 解决方法 |
|------|----------|
| root 路径不对 | 检查 nginx.conf 中 `root` 指向 `D:/in_transit_monitor/frontend` |
| 前端文件未部署 | 检查 `D:\in_transit_monitor\frontend\` 下是否有文件 |
| SPA 路由未配置 | 确认 nginx.conf 中有 `try_files $uri $uri/ /index.html` |
| Nginx 配置未加载 | `.\nginx -t` 检查语法 → `.\nginx -s reload` 重新加载 |

### 8.5 健康检查失败

```cmd
REM 直接访问后端（绕过 Nginx）
curl http://localhost:8080/actuator/health

REM 如后端不可达，检查服务状态
nssm status roro-backend
nssm restart roro-backend
```

---

## 9. 附录：目录结构

### Windows Server 目录结构

```
D:\in_transit_monitor\
├── backend\                     # Spring Boot JAR
│   └── ro-ro-monitor.jar
├── frontend\                    # Vue 前端静态文件
│   ├── index.html
│   ├── assets/
│   └── ...
├── data\                        # 基础数据 SQL
│   └── master-data.sql
├── sql\                         # 建表 DDL
│   └── 0_ro_ro_monitor_full.sql
├── nginx\                       # Nginx for Windows
│   ├── nginx.exe
│   ├── conf\
│   │   └── nginx.conf           ← 由 nginx-windows.conf 重命名而来
│   ├── logs\
│   │   ├── access.log
│   │   └── error.log
│   └── ...
├── nssm\                        # NSSM
│   └── win64\
│       └── nssm.exe
├── logs\                        # 应用日志
│   └── roro-monitor.log
├── backups\                     # 部署备份（JAR 历史版本）
│   └── ro-ro-monitor_20260629_143022.jar
├── env.bat                      # 环境变量配置（不纳入版本控制）
├── deploy-win.bat               # Windows 部署/更新脚本
└── register-service.bat         # NSSM 服务注册脚本
```

### 项目仓库目录

```
in_transit_monitor/
├── .omo/                         # 工作计划
│   ├── drafts/windows-deploy.md  # 本次部署计划决策记录
│   └── plans/windows-deploy.md   # 本次部署计划文件
├── scripts/
│   ├── build-for-win.sh          # 开发机构建脚本
│   └── export-master-data.sh     # 基础数据导出脚本
├── ro-ro-monitor-web/
│   └── nginx-windows.conf        # Nginx Windows 配置文件
├── docs/
│   └── windows-deploy.md         # 本文档
├── deploy-win.bat                # Windows 部署脚本（拷贝到服务器）
└── scripts/
    └── register-service.bat      # NSSM 服务注册脚本（拷贝到服务器）
```

---

> **安全提醒**：
> - 首次登录后请立即修改管理员密码
> - `env.bat` 包含数据库密码，请限制文件访问权限（`icacls env.bat /inheritance:r /grant "%USERNAME%:F"`）
> - 防火墙仅放行 80 端口，3307（MySQL）不应对外开放
> - 定期备份数据库（建议每日）

> **如有问题**，请先查阅[故障排查](#8-故障排查)章节，或查看后端日志 `D:\in_transit_monitor\logs\roro-monitor.log`。
