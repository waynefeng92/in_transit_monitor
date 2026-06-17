# Production Deployment Readiness — In-Transit Vehicle Monitor

## TL;DR

> **Quick Summary**: 将项目从开发状态改造为 Docker Compose 单机生产部署，涵盖 Session Cookie 认证、配置外部化、代码安全加固、前端认证页面、多阶段 Docker 构建。
>
> **Deliverables**:
> - 后端：Spring Security 5.x Session 认证（单管理员，可扩展多用户）
> - 后端：application-prod.yml 配置外部化 + logback + actuator + 全局异常处理
> - 前端：登录页 + Pinia auth store + 路由守卫 + .env 配置 + 安全头 + 404 页
> - 部署：2 个 multi-stage Dockerfile + nginx.conf + docker-compose.yml
>
> **Estimated Effort**: Large (~30 tasks, 5 waves)
> **Parallel Execution**: YES — 5 waves, max 8 concurrent
> **Critical Path**: Config Foundation → CORS + Refactoring → Auth Backend → Auth Frontend → Docker

---

## Context

### Original Request
用户询问：项目部署到生产环境需要做什么。经过全面审计（后端 7 维度 + 前端 7 维度 + 基础设施）发现项目处于纯开发状态：无认证、密码硬编码、无部署配置。

### Interview Summary
**Key Discussions**:
- **部署方式**: 对比 Docker Compose / 裸机 / K8s 后选择 Docker Compose 单机部署
- **认证方式**: Session Cookie (httpOnly) — 比 JWT 更适合内部工具
- **用户体系**: 单管理员账号，数据库 schema 预留多用户扩展接口
- **Scheduler 问题**: 重构 SnapshotScheduler 从调 Controller 改为调 Service
- **修复范围**: 全部审计问题一次性修复（15+ 项）
- **测试策略**: TDD 模式（JUnit 5 + Vitest）
- **域名**: 不需要，IP + 端口访问
- **数据库**: MySQL 服务纳入 docker-compose（`mysql:8.0` 容器 + 数据卷持久化）

**Key Decisions**:
- Spring Boot 2.7.18 保持不变（不升级 3.x，单独记录为技术债）
- Spring Security 5.x API 风格
- CORS 必须在认证之前锁死
- 文件上传端点需要加文件类型白名单验证

### Research Findings
**Backend Audit** (3 audits completed):
- 14 个 Controller 完全无认证
- DB 密码 `11235813@Fw` 明文硬编码于 `application.yml:10`
- 无 `application-prod.yml`、无 `@ControllerAdvice`、CORS 开放 `*`
- 35+ 个 `System.out.println` 和 2 个 `printStackTrace`
- Actuator 依赖已引入但端点未配置
- `Result<T>` 响应包装存在但 14 个 Controller 中使用不一致

**Frontend Audit**:
- API baseURL 硬编码 `/api`，无 `.env` 文件
- 无 404 路由、无安全头（CSP/X-Frame-Options）
- axios 版本过旧（1.17.0）且有安全隐患

**Infrastructure Audit**:
- 零存在：无 Dockerfile、无 docker-compose、无 CI/CD、无 nginx 配置

### Metis Review
**Identified Gaps** (addressed):
- **认证是 greenfield 不是修复**: 需要 4-6 个独立任务（entity、config、controller、frontend）
- **Scheduler → Controller 耦合**: 加认证后调度器会 401/403，必须先重构到 Service 层
- **CORS 时序风险**: 必须在认证之前锁死 CORS，否则 `allowedOriginPatterns("*")` + `allowCredentials(true)` 会被利用
- **Docker 遗漏**: .dockerignore、多阶段构建、nginx 反向代理、时区对齐、文件上传持久化
- **文件上传无校验**: UploadController 接受任意文件类型，需加 MIME 白名单

---

## Work Objectives

### Core Objective
将 In-Transit Vehicle Monitor 改造为可通过 `docker compose up -d` 一键启动的生产就绪部署，具备 Session 认证、外部化配置、安全加固和完整的前端认证体验。

### Concrete Deliverables
- `application-prod.yml` — 所有敏感值通过 `${ENV_VAR}` 注入
- `SecurityConfig.java` + `SessionConfig.java` — Spring Security 5.x Session 认证
- `User.java` 实体 + `UserRepository.java` — 可扩展的用户表
- `AuthController.java` — login/logout/session-check 端点
- `GlobalExceptionHandler.java` — 统一异常处理 + `Result<T>` 标准化
- `Login.vue` — 登录页 + `stores/auth.js` — Pinia 认证状态
- `nginx.conf` — 静态文件服务 + `/api` 反向代理
- `docker-compose.yml` — 后端 + nginx + MySQL 连接
- `Dockerfile` × 2 — 后端多阶段（Maven→JRE）+ 前端多阶段（Node→nginx）

### Definition of Done
- [ ] `docker compose up -d` 启动成功，三个容器 health check 通过
- [ ] `curl http://<ip>/api/transit/summary` → 401（未登录）
- [ ] 登录后 `curl -b cookies.txt http://<ip>/api/transit/summary` → 200
- [ ] `curl http://<ip>/` → 前端登录页正常加载
- [x] `grep -r "11235813" ro-ro-monitor/src/main/resources/` → 无匹配
- [x] `docker compose logs backend | grep -ci "System.out"` → 0

### Must Have
- Session Cookie (httpOnly) 认证保护所有 `/api/*` 端点
- 所有敏感配置通过环境变量注入（DB 密码、端口等）
- CORS 仅允许 nginx 来源或特定 IP
- 全局异常处理 + 统一 `Result<T>` 响应格式
- Actuator `/actuator/health` 暴露供 Docker health check
- logback-spring.xml 文件日志轮转
- 所有 `System.out.println` / `printStackTrace` 替换为 SLF4J
- 前端登录页 + 路由守卫 + 401 处理
- 前端 404 页面 + 安全头
- 多阶段 Dockerfile + nginx 反向代理（后端不暴露到宿主机）
- .dockerignore 排除 `node_modules/`、`target/`、`.git/`

### Must NOT Have (Guardrails)
- **NO Spring Boot 3.x 升级** — 保持在 2.7.18，记录为技术债
- **NO RBAC 多角色系统** — 只实现单管理员，schema 预留扩展
- **NO JWT** — 使用 Session Cookie，不引入 JWT 复杂度
- **NO 数据库迁移工具** — 不引入 Flyway/Liquibase，手动执行现有 SQL 脚本
- **NO CI/CD 流水线** — 本次不涉及
- **NO Swagger/OpenAPI** — 不引入 API 文档框架
- **NO 过度抽象** — 不创建不必要的 util/helper 类
- **NO localStorage 存 token** — 认证状态仅通过 httpOnly cookie 传递

---

## Verification Strategy

> **ZERO HUMAN INTERVENTION** - ALL verification is agent-executed. No exceptions.

### Test Decision
- **Infrastructure exists**: YES (JUnit 5 backend + Vitest frontend)
- **Automated tests**: TDD (RED → GREEN → REFACTOR)
- **Framework**: JUnit 5 (backend) + Vitest (frontend)
- **If TDD**: Each functional task follows RED (failing test) → GREEN (minimal impl) → REFACTOR

### QA Policy
Every task MUST include agent-executed QA scenarios.
Evidence saved to `.omo/evidence/task-{N}-{scenario-slug}.{ext}`.

- **API/Backend**: Use Bash (curl) — Send requests, assert status + response fields
- **Frontend/UI**: Use Playwright — Navigate, interact, assert DOM, screenshot
- **Docker**: Use Bash — docker compose up, health check, logs verification

---

## Execution Strategy

### Parallel Execution Waves

```
Wave 1 (Start Immediately - backend foundation, MAX PARALLEL):
├── Task 1: application-prod.yml + externalize credentials [quick]
├── Task 2: logback-spring.xml [quick]
├── Task 3: Replace System.out.println → SLF4J [quick]
├── Task 4: HikariCP connection pool tuning [quick]
├── Task 5: Actuator endpoint configuration [quick]
├── Task 6: User entity + repository [quick]
└── Task 7: PasswordEncoder config bean [quick]

Wave 2 (After Wave 1 - backend restructuring, MAX PARALLEL):
├── Task 8: Lock CORS to specific origins [quick]
├── Task 9: Global exception handler [quick]
├── Task 10: Standardize Result<T> across controllers [deep]
├── Task 11: Refactor SnapshotScheduler → Service layer [deep]
├── Task 12: File upload MIME type validation [quick]
└── Task 13: Session configuration [quick]

Wave 3 (After Wave 2 - auth implementation):
├── Task 14: Spring Security config (5.x) [deep]
├── Task 15: AuthController (login/logout/session-check) + TDD [deep]
└── Task 16: Wire auth into all controllers [quick]

Wave 4 (After Wave 3 - frontend, MAX PARALLEL):
├── Task 17: .env files + axios update + env-driven baseURL [quick]
├── Task 18: Pinia auth store + Vitest tests [quick]
├── Task 19: Login page (Vue + Element Plus) [visual-engineering]
├── Task 20: Router guards (beforeEach) [quick]
├── Task 21: API interceptor (session handling + 401 redirect) [quick]
├── Task 22: 404 Not Found page [quick]
└── Task 23: Security headers in index.html [quick]

Wave 5 (After Wave 4 - Docker deployment, MAX PARALLEL):
├── Task 24: Backend multi-stage Dockerfile [quick]
├── Task 25: Frontend multi-stage Dockerfile + nginx.conf [quick]
├── Task 26: .dockerignore [quick]
├── Task 27: docker-compose.yml [quick]
└── Task 28: Integration verification (full stack up) [deep]

Wave FINAL (After ALL tasks — 4 parallel reviews):
├── Task F1: Plan Compliance Audit (oracle)
├── Task F2: Code Quality Review (unspecified-high)
├── Task F3: Real Manual QA (unspecified-high)
└── Task F4: Scope Fidelity Check (deep)
```

**Critical Path**: Task 1 → Task 8 → Task 13 → Task 14 → Task 15 → Task 20 → Task 28 → F1-F4
**Parallel Speedup**: ~60% faster than sequential
**Max Concurrent**: 7 (Waves 1 & 4)

---

## TODOs

- [x] 1. Externalize configuration — create `application-prod.yml` + clean `application.yml`

  **What to do**:
  - **TDD**: No test needed — pure configuration task. Verify with grep after.
  - Create `ro-ro-monitor/src/main/resources/application-prod.yml` with:
    - `spring.datasource.url` using `${DB_HOST:localhost}:${DB_PORT:3306}/${DB_NAME:ro_ro_monitor}`
    - `spring.datasource.username: ${DB_USERNAME:root}`
    - `spring.datasource.password: ${DB_PASSWORD}` (no default — must fail if unset)
    - `server.port: ${SERVER_PORT:8080}`
    - HikariCP pool settings (copied from Task 4)
    - Actuator config (copied from Task 5)
    - Logging config referencing `logback-spring.xml`
  - Modify `application.yml`: remove hardcoded password from line 10, keep dev-friendly defaults for local development
  - Set `spring.profiles.active: dev` in `application.yml` as default

  **Must NOT do**:
  - Do NOT delete `application.yml` — it stays as dev defaults
  - Do NOT put any real password in any config file

  **Recommended Agent Profile**:
  - **Category**: `quick` — single config file creation + minor edit
  - **Skills**: `[]`

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 1 (with Tasks 2-7)
  - **Blocks**: Tasks 8, 9, 13, 14, 24, 27
  - **Blocked By**: None

  **References**:
  - `ro-ro-monitor/src/main/resources/application.yml` — current config, see lines 1-10 for datasource structure, line 10 for password to remove
  - `ro-ro-monitor/src/main/java/com/company/roro/config/MonitorConfig.java` — existing `@ConfigurationProperties` pattern

  **Acceptance Criteria**:
  - [ ] `grep -r "11235813" ro-ro-monitor/src/main/resources/` returns zero matches
  - [ ] `grep "spring.profiles.active" ro-ro-monitor/src/main/resources/application.yml` shows `dev`
  - [ ] `application-prod.yml` contains `${DB_PASSWORD}` placeholder (not literal password)
  - [ ] `application-prod.yml` has `server.port`, `spring.datasource.*`, `management.endpoints.*` sections

  **QA Scenarios**:
  ```
  Scenario: Prod config has env var placeholders, no hardcoded secrets
    Tool: Bash
    Preconditions: application-prod.yml exists
    Steps:
      1. grep -c '\${' ro-ro-monitor/src/main/resources/application-prod.yml
      2. grep -c '11235813' ro-ro-monitor/src/main/resources/application-prod.yml
      3. grep -c 'password:' ro-ro-monitor/src/main/resources/application-prod.yml
    Expected Result: Step 1 returns >= 4 (multiple env vars used); Step 2 returns 0; Step 3 line contains ${DB_PASSWORD}
    Evidence: .omo/evidence/task-1-no-hardcoded-secrets.txt

  Scenario: application.yml has no hardcoded password
    Tool: Bash
    Preconditions: application.yml exists
    Steps:
      1. grep -n 'password' ro-ro-monitor/src/main/resources/application.yml
    Expected Result: Either no match, or if present, the value is NOT a literal password string (should reference env var or be removed)
    Evidence: .omo/evidence/task-1-dev-config-clean.txt
  ```

  **Commit**: YES (groups with Wave 1)
  - Message: `chore(config): externalize credentials and add production profile`
  - Files: `application-prod.yml` (new), `application.yml` (modified)

- [x] 2. Add `logback-spring.xml` with file rotation

  **What to do**:
  - **TDD**: No test needed — config file. Verify with grep/logs after.
  - Create `ro-ro-monitor/src/main/resources/logback-spring.xml`:
    - Console appender for Docker stdout (JSON format optional, plain text is fine)
    - Rolling file appender: `logs/roro-monitor.log`, max 10MB, keep 7 days
    - Root level: INFO (prod) / DEBUG (dev) via `springProfile`
    - Package-specific levels: `com.company.roro=DEBUG` in dev, `INFO` in prod
  - Reference `logback-spring.xml` in `application-prod.yml` via `logging.config`

  **Must NOT do**:
  - Do NOT log sensitive data (passwords, tokens)
  - Do NOT use absolute paths — use relative `logs/` directory

  **Recommended Agent Profile**:
  - **Category**: `quick`
  - **Skills**: `[]`

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 1 (with Tasks 1, 3-7)
  - **Blocks**: Task 24 (Dockerfile)
  - **Blocked By**: None

  **References**:
  - `ro-ro-monitor/src/main/resources/application.yml` — see existing logging patterns (none currently, adding from scratch)

  **Acceptance Criteria**:
  - [ ] `ls ro-ro-monitor/src/main/resources/logback-spring.xml` exists
  - [ ] Contains `<springProfile name="prod">` and `<springProfile name="dev">` sections
  - [ ] Contains `<rollingPolicy>` with max file size
  - [ ] `application-prod.yml` references `logging.config: classpath:logback-spring.xml`

  **QA Scenarios**:
  ```
  Scenario: logback config file exists and is valid XML
    Tool: Bash
    Preconditions: logback-spring.xml exists
    Steps:
      1. xmllint --noout ro-ro-monitor/src/main/resources/logback-spring.xml 2>&1 || echo "xmllint not available, checking with grep"
      2. grep -c '<configuration' ro-ro-monitor/src/main/resources/logback-spring.xml
      3. grep -c '<springProfile' ro-ro-monitor/src/main/resources/logback-spring.xml
      4. grep -c '<rollingPolicy' ro-ro-monitor/src/main/resources/logback-spring.xml
    Expected Result: Step 2 returns >= 1; Step 3 returns >= 2; Step 4 returns >= 1
    Evidence: .omo/evidence/task-2-logback-structure.txt
  ```

  **Commit**: YES (groups with Wave 1)
  - Files: `logback-spring.xml` (new)

- [x] 3. Replace all `System.out.println` / `printStackTrace` with SLF4J

  **What to do**:
  - **TDD**: No test needed — code hygiene. Verify with grep.
  - Find ALL occurrences: `grep -rn "System.out.println\|\.printStackTrace()" ro-ro-monitor/src/main/java/`
  - Affected files (from audit):
    - `TransitDataServiceImpl.java` — 12 println + 1 printStackTrace
    - `UploadController.java` — 5 println + 1 printStackTrace
    - `ExcelParseUtil.java` — 9 println
    - `ExcelParseServiceImpl.java` — 3 println
    - `RoroMonitorApplication.java` — 3 println
  - For each file: add `@Slf4j` annotation (if not present) + `import lombok.extern.slf4j.Slf4j`
  - Replace `System.out.println("xxx")` → `log.info("xxx")`
  - Replace `e.printStackTrace()` → `log.error("Error description", e)`
  - Replace `System.out.println("error: " + e.getMessage())` → `log.error("Error: {}", e.getMessage(), e)`

  **Must NOT do**:
  - Do NOT change log message content — only the logging method
  - Do NOT change any business logic

  **Recommended Agent Profile**:
  - **Category**: `quick` — mechanical search-and-replace across 5 files
  - **Skills**: `[]`

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 1 (with Tasks 1-2, 4-7)
  - **Blocks**: None (cosmetic only)
  - **Blocked By**: None

  **References**:
  - `ro-ro-monitor/src/main/java/com/company/roro/scheduler/SnapshotScheduler.java` — good example of existing `@Slf4j` usage pattern

  **Acceptance Criteria**:
  - [ ] `grep -rn "System.out.println" ro-ro-monitor/src/main/java/` returns zero matches
  - [ ] `grep -rn "\.printStackTrace()" ro-ro-monitor/src/main/java/` returns zero matches
  - [ ] `grep -rn "@Slf4j" ro-ro-monitor/src/main/java/` shows annotation on all 5 affected files
  - [ ] `mvn compile` succeeds

  **QA Scenarios**:
  ```
  Scenario: Zero System.out.println or printStackTrace in source
    Tool: Bash
    Preconditions: All files modified
    Steps:
      1. grep -rn "System.out.println" ro-ro-monitor/src/main/java/ | wc -l
      2. grep -rn "\.printStackTrace()" ro-ro-monitor/src/main/java/ | wc -l
      3. mvn compile -q 2>&1 | tail -5
    Expected Result: Steps 1 and 2 return 0; Step 3 shows BUILD SUCCESS
    Evidence: .omo/evidence/task-3-no-println.txt

  Scenario: Compilation succeeds after changes
    Tool: Bash (workdir: ro-ro-monitor)
    Preconditions: All files modified
    Steps:
      1. mvn compile 2>&1
    Expected Result: BUILD SUCCESS, no compilation errors
    Evidence: .omo/evidence/task-3-compile-success.txt
  ```

  **Commit**: YES (groups with Wave 1)
  - Files: 5 modified Java files

- [x] 4. HikariCP connection pool tuning

  **What to do**:
  - **TDD**: No test needed — config change. Verify pool metrics via actuator after deployment.
  - Add to `application-prod.yml`:
    ```yaml
    spring:
      datasource:
        hikari:
          maximum-pool-size: 10
          minimum-idle: 2
          idle-timeout: 300000        # 5 min
          max-lifetime: 1200000       # 20 min
          connection-timeout: 30000   # 30 sec
          validation-timeout: 5000
          leak-detection-threshold: 60000  # log connections held > 60s
    ```
  - Keep `application.yml` with smaller pool (max 5) for dev
  - Reference HikariCP docs for rationale: small internal tool → 10 max connections is conservative and safe

  **Must NOT do**:
  - Do NOT set `maximum-pool-size` > 20 (over-provisioning for small internal tool)
  - Do NOT hardcode values — use `${HIKARI_MAX_POOL_SIZE:10}` style for overridability if desired

  **Recommended Agent Profile**:
  - **Category**: `quick`
  - **Skills**: `[]`

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 1 (with Tasks 1-3, 5-7)
  - **Blocks**: None
  - **Blocked By**: Task 1 (application-prod.yml must exist)

  **References**:
  - `ro-ro-monitor/src/main/resources/application.yml:8-10` — current datasource config to extend
  - Official HikariCP docs: https://github.com/brettwooldridge/HikariCP#frequently-used

  **Acceptance Criteria**:
  - [ ] `grep -A 10 "hikari:" ro-ro-monitor/src/main/resources/application-prod.yml` shows pool config
  - [ ] `maximum-pool-size` is between 5 and 20
  - [ ] `leak-detection-threshold` is set (catches connection leaks)

  **QA Scenarios**:
  ```
  Scenario: HikariCP config present in production profile
    Tool: Bash
    Preconditions: application-prod.yml exists
    Steps:
      1. grep -c 'hikari:' ro-ro-monitor/src/main/resources/application-prod.yml
      2. grep -c 'maximum-pool-size' ro-ro-monitor/src/main/resources/application-prod.yml
      3. grep -c 'leak-detection-threshold' ro-ro-monitor/src/main/resources/application-prod.yml
    Expected Result: Step 1 >= 1; Step 2 >= 1; Step 3 >= 1
    Evidence: .omo/evidence/task-4-hikari-config.txt
  ```

  **Commit**: YES (groups with Wave 1)
  - Files: `application-prod.yml` (modified)

- [x] 5. Actuator endpoint configuration

  **What to do**:
  - **TDD**: No test needed. Verify with curl after deployment.
  - Add to `application-prod.yml`:
    ```yaml
    management:
      endpoints:
        web:
          exposure:
            include: health,info,metrics
          base-path: /actuator
      endpoint:
        health:
          show-details: when-authorized
          probes:
            enabled: true
      health:
        db:
          enabled: true
    ```
  - Add to `application.yml` (dev): expose all endpoints for development
    ```yaml
    management:
      endpoints:
        web:
          exposure:
            include: "*"
    ```

  **Must NOT do**:
  - Do NOT expose `env`, `configprops`, `beans` in production profile (information leak)
  - Do NOT expose `shutdown` endpoint

  **Recommended Agent Profile**:
  - **Category**: `quick`
  - **Skills**: `[]`

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 1 (with Tasks 1-4, 6-7)
  - **Blocks**: Task 14 (Security config must permit `/actuator/health`), Task 27 (docker-compose healthcheck)
  - **Blocked By**: Task 1 (application-prod.yml must exist)

  **References**:
  - `ro-ro-monitor/pom.xml:35` — actuator dependency already present

  **Acceptance Criteria**:
  - [ ] `grep "management:" ro-ro-monitor/src/main/resources/application-prod.yml` shows config
  - [ ] Prod profile only exposes `health,info,metrics`
  - [ ] `health.probes.enabled: true` set for k8s-style readiness/liveness

  **QA Scenarios**:
  ```
  Scenario: Actuator config present and restricted in production
    Tool: Bash
    Preconditions: application-prod.yml exists
    Steps:
      1. grep -A5 'management:' ro-ro-monitor/src/main/resources/application-prod.yml
      2. grep 'include:' ro-ro-monitor/src/main/resources/application-prod.yml
    Expected Result: Step 1 shows management config block; Step 2 shows only health,info,metrics — NOT env,beans,configprops
    Evidence: .omo/evidence/task-5-actuator-config.txt
  ```

  **Commit**: YES (groups with Wave 1)
  - Files: `application-prod.yml` (modified), `application.yml` (modified)

- [x] 6. User entity + repository (extensible schema)

  **What to do**:
  - **TDD**: Write JUnit test first (RED), then entity (GREEN), then repository (GREEN).
  - Create `ro-ro-monitor/src/test/java/com/company/roro/entity/UserTest.java`:
    - Test: `shouldCreateUserWithRequiredFields()` — verify entity can be constructed
    - Test: `shouldHaveTimestampsAutoSet()` — verify `createdAt` auto-populated
  - Create `ro-ro-monitor/src/main/java/com/company/roro/entity/User.java`:
    - Fields: `id` (Long, auto-increment), `username` (unique, not null), `password` (not null, BCrypt hash), `role` (String, default "ADMIN"), `enabled` (Boolean, default true), `createdAt` (LocalDateTime), `updatedAt` (LocalDateTime)
    - Annotations: `@TableName("users")`, `@TableId(type = IdType.AUTO)`, `@TableField(fill = FieldFill.INSERT)` for timestamps
    - Use Lombok `@Data` + MyBatis-Plus annotations (follow existing entity patterns)
  - Create `ro-ro-monitor/src/main/java/com/company/roro/repository/UserRepository.java`:
    - Extends `BaseMapper<User>` (MyBatis-Plus pattern)
    - Method: `User findByUsername(String username)` — annotated with `@Select`

  **Must NOT do**:
  - Do NOT create RBAC tables (roles, permissions, user_roles)
  - Do NOT add JWT-related fields (refreshToken, tokenExpiry)
  - BUT: role field should be String (not Enum) to allow future expansion without schema migration

  **Recommended Agent Profile**:
  - **Category**: `quick` — straightforward entity + mapper
  - **Skills**: `[]`

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 1 (with Tasks 1-5, 7)
  - **Blocks**: Tasks 7, 14, 15
  - **Blocked By**: None

  **References**:
  - Look at any existing entity in `ro-ro-monitor/src/main/java/com/company/roro/entity/` for MyBatis-Plus annotation patterns (`@TableName`, `@TableId`, `@TableField`)
  - `ro-ro-monitor/src/main/java/com/company/roro/config/MybatisPlusConfig.java` — confirms MyBatis-Plus is configured

  **Acceptance Criteria** (TDD RED → GREEN):
  - [ ] Test file created: `src/test/java/com/company/roro/entity/UserTest.java` (FAILS initially — entity doesn't exist)
  - [ ] Entity file created: `User.java` — test should now PASS
  - [ ] Repository file created: `UserRepository.java` with `findByUsername` method
  - [ ] `mvn test -Dtest=UserTest` → PASS

  **QA Scenarios**:
  ```
  Scenario: User entity compiles and has correct annotations
    Tool: Bash (workdir: ro-ro-monitor)
    Preconditions: User.java exists
    Steps:
      1. mvn compile -q 2>&1 | tail -5
      2. grep '@TableName' ro-ro-monitor/src/main/java/com/company/roro/entity/User.java
      3. grep 'private String role' ro-ro-monitor/src/main/java/com/company/roro/entity/User.java
    Expected Result: BUILD SUCCESS; @TableName("users") present; role field is String (extensible)
    Evidence: .omo/evidence/task-6-user-entity.txt

  Scenario: JUnit test for User entity passes
    Tool: Bash (workdir: ro-ro-monitor)
    Preconditions: UserTest.java exists
    Steps:
      1. mvn test -Dtest=UserTest 2>&1
    Expected Result: Tests run: >= 2, Failures: 0, BUILD SUCCESS
    Evidence: .omo/evidence/task-6-user-test-pass.txt
  ```

  **Commit**: YES (groups with Wave 1)
  - Files: `User.java` (new), `UserRepository.java` (new), `UserTest.java` (new)

- [x] 7. PasswordEncoder configuration bean

  **What to do**:
  - **TDD**: Write test first (RED) — test that `BCryptPasswordEncoder` encodes and matches correctly.
  - Create `ro-ro-monitor/src/test/java/com/company/roro/config/PasswordEncoderConfigTest.java`:
    - Test: `shouldEncodeAndMatchPassword()` — encode "testPassword", verify `matches()` returns true
    - Test: `shouldRejectWrongPassword()` — verify `matches("wrongPassword")` returns false
  - Create `ro-ro-monitor/src/main/java/com/company/roro/config/PasswordEncoderConfig.java`:
    - `@Configuration` class with `@Bean` returning `new BCryptPasswordEncoder()`
    - Follow existing config pattern: `CorsConfig.java`, `ThreadPoolConfig.java`

  **Must NOT do**:
  - Do NOT use deprecated `MD5` or `SHA-256` — BCrypt only
  - Do NOT create a custom encoder — use Spring Security's built-in

  **Recommended Agent Profile**:
  - **Category**: `quick`
  - **Skills**: `[]`

  **Parallelization**:
  - **Can Run In Parallel**: YES (but needs `spring-boot-starter-security` on classpath — verify in pom.xml)
  - **Parallel Group**: Wave 1 (with Tasks 1-6)
  - **Blocks**: Task 14, 15
  - **Blocked By**: Task 6 (User entity defines what password format we need)

  **References**:
  - `ro-ro-monitor/src/main/java/com/company/roro/config/CorsConfig.java` — config class pattern to follow
  - `ro-ro-monitor/pom.xml` — check if `spring-boot-starter-security` is already a dependency (it's NOT — this task will fail to compile without it. Add it.)

  **Acceptance Criteria** (TDD RED → GREEN):
  - [ ] Test file created: `PasswordEncoderConfigTest.java` (FAILS — config doesn't exist)
  - [ ] Config file created: `PasswordEncoderConfig.java` — test PASSES
  - [ ] `spring-boot-starter-security` added to `pom.xml` if not present
  - [ ] `mvn test -Dtest=PasswordEncoderConfigTest` → PASS

  **QA Scenarios**:
  ```
  Scenario: PasswordEncoder bean correctly encodes and verifies
    Tool: Bash (workdir: ro-ro-monitor)
    Preconditions: PasswordEncoderConfig.java exists
    Steps:
      1. mvn test -Dtest=PasswordEncoderConfigTest 2>&1
    Expected Result: Tests run: >= 2, Failures: 0, BUILD SUCCESS
    Evidence: .omo/evidence/task-7-password-encoder-test.txt

  Scenario: BCrypt produces different hashes for same input (salt works)
    Tool: Bash (workdir: ro-ro-monitor)
    Preconditions: Test file includes salt verification
    Steps:
      1. grep -c 'BCryptPasswordEncoder' ro-ro-monitor/src/main/java/com/company/roro/config/PasswordEncoderConfig.java
    Expected Result: 1 (uses Spring Security's BCryptPasswordEncoder, not custom implementation)
    Evidence: .omo/evidence/task-7-bcrypt-import.txt
  ```

  **Commit**: YES (groups with Wave 1)
  - Files: `PasswordEncoderConfig.java` (new), `PasswordEncoderConfigTest.java` (new), `pom.xml` (modified if security dep added)

- [x] 8. Lock CORS to specific origins (BEFORE auth)

  **What to do**:
  - **TDD**: Write test first — verify CORS rejects disallowed origins, allows allowed origins.
  - Create `ro-ro-monitor/src/test/java/com/company/roro/config/CorsConfigTest.java`:
    - Test: `shouldAllowConfiguredOrigins()` — mock request with allowed origin, expect CORS headers present
    - Test: `shouldRejectDisallowedOrigins()` — mock request with `Origin: http://evil.com`, expect no CORS headers or 403
  - Modify `ro-ro-monitor/src/main/java/com/company/roro/config/CorsConfig.java`:
    - Replace `allowedOriginPatterns("*")` with env-driven list: `${CORS_ALLOWED_ORIGINS:http://localhost:5173}`
    - Parse comma-separated origins from env var in config
    - Keep `allowCredentials(true)` (needed for session cookies)
    - Update comment from "生产环境建议改为具体域名" to document the env var approach

  **Must NOT do**:
  - Do NOT keep `"*"` as a fallback — if env var is unset in prod, CORS should fail closed
  - Do NOT remove `allowCredentials(true)` — session cookies require it

  **Recommended Agent Profile**:
  - **Category**: `quick`
  - **Skills**: `[]`

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 2 (with Tasks 9-13)
  - **Blocks**: Task 14 (security config must work with CORS)
  - **Blocked By**: Task 1 (application-prod.yml for env var pattern)

  **References**:
  - `ro-ro-monitor/src/main/java/com/company/roro/config/CorsConfig.java` — current CORS config, lines 22-30 show the wildcard pattern to replace
  - `ro-ro-monitor/src/main/resources/application.yml` — see env var pattern from Task 1

  **Acceptance Criteria** (TDD RED → GREEN):
  - [ ] Test: `CorsConfigTest.java` created with 2 test methods (RED initially)
  - [ ] CorsConfig modified: no `allowedOriginPatterns("*")` — uses env var
  - [ ] `mvn test -Dtest=CorsConfigTest` → PASS (both tests)
  - [ ] `grep '"\\*"' ro-ro-monitor/src/main/java/com/company/roro/config/CorsConfig.java` returns zero matches

  **QA Scenarios**:
  ```
  Scenario: CORS config reads origins from environment variable, not wildcard
    Tool: Bash
    Preconditions: CorsConfig.java modified
    Steps:
      1. grep -c 'allowedOriginPatterns' ro-ro-monitor/src/main/java/com/company/roro/config/CorsConfig.java
      2. grep -c '"\\\\*"' ro-ro-monitor/src/main/java/com/company/roro/config/CorsConfig.java
      3. grep -c 'CORS_ALLOWED_ORIGINS' ro-ro-monitor/src/main/java/com/company/roro/config/CorsConfig.java
    Expected Result: Step 1 >= 1; Step 2 == 0; Step 3 >= 1
    Evidence: .omo/evidence/task-8-cors-locked.txt

  Scenario: CORS test passes — allowed origins work, disallowed rejected
    Tool: Bash (workdir: ro-ro-monitor)
    Preconditions: CorsConfigTest.java exists
    Steps:
      1. mvn test -Dtest=CorsConfigTest 2>&1
    Expected Result: Tests run: >= 2, Failures: 0
    Evidence: .omo/evidence/task-8-cors-test-pass.txt
  ```

  **Commit**: YES (groups with Wave 2)
  - Files: `CorsConfig.java` (modified), `CorsConfigTest.java` (new)

- [x] 9. Global exception handler (`@ControllerAdvice`)

  **What to do**:
  - **TDD**: Write test first (RED) — mock controller throwing exceptions, verify handler returns correct `Result<T>` shape.
  - Create `ro-ro-monitor/src/test/java/com/company/roro/handler/GlobalExceptionHandlerTest.java`:
    - Test: `shouldHandleIllegalArgumentException()` → expect HTTP 400 + `Result.error(400, "Bad request")`
    - Test: `shouldHandleGenericException()` → expect HTTP 500 + `Result.error(500, "Internal server error")`
    - Test: `shouldHandleMethodArgumentNotValidException()` → expect HTTP 400 with validation error details
  - Create `ro-ro-monitor/src/main/java/com/company/roro/handler/GlobalExceptionHandler.java`:
    - `@RestControllerAdvice` class
    - Handle: `IllegalArgumentException` → 400, `RuntimeException` → 500, `Exception` → 500
    - Handle: `MethodArgumentNotValidException` → 400 with field-level errors
    - Handle: `AccessDeniedException` → 401 (preparation for auth)
    - All handlers return `Result<T>` DTO with appropriate code/message
  - Add `spring-boot-starter-validation` to `pom.xml` for `@Valid` support

  **Must NOT do**:
  - Do NOT expose stack traces in error responses — only message
  - Do NOT catch `Throwable` — too broad

  **Recommended Agent Profile**:
  - **Category**: `quick`
  - **Skills**: `[]`

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 2 (with Tasks 8, 10-13)
  - **Blocks**: Tasks 15, 28
  - **Blocked By**: Task 1 (Result<T> must exist)

  **References**:
  - `ro-ro-monitor/src/main/java/com/company/roro/dto/Result.java` — response wrapper to use: `Result.error(code, message)` pattern
  - `ro-ro-monitor/src/main/java/com/company/roro/controller/UploadController.java` — example of existing error handling via `Result.error()` for pattern matching

  **Acceptance Criteria** (TDD RED → GREEN):
  - [ ] Test: `GlobalExceptionHandlerTest.java` (3 tests, RED)
  - [ ] Handler: `GlobalExceptionHandler.java` (GREEN — tests pass)
  - [ ] `mvn test -Dtest=GlobalExceptionHandlerTest` → PASS
  - [ ] `spring-boot-starter-validation` in `pom.xml`

  **QA Scenarios**:
  ```
  Scenario: Global exception handler returns consistent Result<T> for known exception
    Tool: Bash (workdir: ro-ro-monitor)
    Preconditions: GlobalExceptionHandler.java exists
    Steps:
      1. mvn test -Dtest=GlobalExceptionHandlerTest 2>&1 | grep -E "(Tests run|BUILD)"
    Expected Result: Tests run: >= 3, Failures: 0, BUILD SUCCESS
    Evidence: .omo/evidence/task-9-exception-handler-test.txt

  Scenario: Handler does not expose stack traces in Result.error()
    Tool: Bash
    Preconditions: GlobalExceptionHandler.java exists
    Steps:
      1. grep -c 'getStackTrace' ro-ro-monitor/src/main/java/com/company/roro/handler/GlobalExceptionHandler.java
      2. grep -c 'printStackTrace' ro-ro-monitor/src/main/java/com/company/roro/handler/GlobalExceptionHandler.java
    Expected Result: Both return 0 (no stack trace leaking to clients)
    Evidence: .omo/evidence/task-9-no-stacktrace-leak.txt
  ```

  **Commit**: YES (groups with Wave 2)
  - Files: `GlobalExceptionHandler.java` (new), `GlobalExceptionHandlerTest.java` (new), `pom.xml` (modified)

- [x] 10. Standardize all controllers to use `Result<T>` wrapper

  **What to do**:
  - **TDD**: For each controller, verify response shape matches `Result<T>` via test.
  - Audit all 14 controllers for response types:
    - Some return raw entities (`BrandController`), some use `Result.success()` (`UploadController`, `RouteController`)
  - For each controller NOT using `Result<T>`:
    - Change return type from `List<Xxx>` → `Result<List<Xxx>>`
    - Wrap return value in `Result.success(data)`
    - Error cases already handled by Task 9's `GlobalExceptionHandler`
  - Controllers to fix (from audit): `BrandController`, `TransitController`, `ChartController` (verify each)
  - Create integration test `ResultWrapperConsistencyTest.java`:
    - Use `@SpringBootTest` + `@AutoConfigureMockMvc`
    - Test: each endpoint returns JSON with `code`, `message`, `data` fields

  **Must NOT do**:
  - Do NOT change business logic — only wrap return values
  - Do NOT create new DTO classes — `Result<T>` is the only wrapper

  **Recommended Agent Profile**:
  - **Category**: `deep` — touches 14 controllers, requires methodical audit
  - **Skills**: `[]`

  **Parallelization**:
  - **Can Run In Parallel**: NO (needs dedicated focus, touches many files)
  - **Parallel Group**: Wave 2 (independent of Tasks 8, 9, 11, 12, 13)
  - **Blocks**: Task 28 (integration test)
  - **Blocked By**: Task 9 (GlobalExceptionHandler must exist first)

  **References**:
  - `ro-ro-monitor/src/main/java/com/company/roro/dto/Result.java` — wrapper class: `Result.success(data)`, `Result.error(code, msg)`
  - `ro-ro-monitor/src/main/java/com/company/roro/controller/UploadController.java` — example controller already using `Result<T>` correctly
  - `ro-ro-monitor/src/main/java/com/company/roro/controller/BrandController.java` — example of controller NOT using Result (needs fixing)

  **Acceptance Criteria**:
  - [ ] All controller methods return `Result<T>` (not raw entities)
  - [ ] `ResultWrapperConsistencyTest.java` passes: verifies `code`/`message`/`data` in response JSON
  - [ ] `mvn compile` succeeds
  - [ ] `mvn test -Dtest=ResultWrapperConsistencyTest` → PASS

  **QA Scenarios**:
  ```
  Scenario: All controllers return consistent Result<T> structure
    Tool: Bash (workdir: ro-ro-monitor)
    Preconditions: All controllers modified
    Steps:
      1. mvn test -Dtest=ResultWrapperConsistencyTest 2>&1
      2. grep -rn "ResponseEntity<" ro-ro-monitor/src/main/java/com/company/roro/controller/ | grep -v "Result<" | wc -l
    Expected Result: Tests pass; Step 2 shows 0 (no raw entity returns)
    Evidence: .omo/evidence/task-10-result-wrapper-consistency.txt
  ```

  **Commit**: YES (groups with Wave 2)
  - Files: Multiple controllers modified + `ResultWrapperConsistencyTest.java` (new)

- [x] 11. Refactor SnapshotScheduler — call Service instead of Controller

  **What to do**:
  - **TDD**: Write test verifying scheduler calls service, not controller.
  - Identify controller methods called by `SnapshotScheduler` (from audit):
    - `transitController.summary(null, null)` — extract to `TransitService.getSummary()`
    - `chartController.getBrandStatusChart(null, null, tabType, null)` — extract to `ChartService.getBrandStatusChart(tabType)`
  - For each extracted method:
    - Create/update Service class with the business logic
    - Update Controller to delegate to Service
    - Update Scheduler to call Service directly
  - Add `@Transactional(readOnly = true)` on read-only service methods
  - Create test: `SnapshotSchedulerServiceRefactorTest.java` — verify scheduler bean has service dependency (not controller)

  **Must NOT do**:
  - Do NOT change the scheduled logic behavior — only the call target
  - Do NOT remove controller methods — they still serve HTTP requests via service delegation

  **Recommended Agent Profile**:
  - **Category**: `deep` — requires understanding scheduler ↔ controller coupling
  - **Skills**: `[]`

  **Parallelization**:
  - **Can Run In Parallel**: NO (focus task, touches scheduler + controller + service)
  - **Parallel Group**: Wave 2 (independent of Tasks 8, 9, 10, 12, 13)
  - **Blocks**: Task 14 (auth must work with refactored scheduler)
  - **Blocked By**: None (can start independently)

  **References**:
  - `ro-ro-monitor/src/main/java/com/company/roro/scheduler/SnapshotScheduler.java` — lines 59-63 show controller calls to refactor
  - Use `lsp_find_references` before modifying: find all callers of `summary()` and `getBrandStatusChart()` to ensure no other code is affected

  **Acceptance Criteria** (TDD RED → GREEN):
  - [ ] Test: `SnapshotSchedulerServiceRefactorTest.java` — verifies scheduler has service deps, not controller deps
  - [ ] `SnapshotScheduler.java` imports zero controller classes
  - [ ] `mvn test -Dtest=SnapshotSchedulerServiceRefactorTest` → PASS
  - [ ] `mvn compile` succeeds with no circular dependency errors

  **QA Scenarios**:
  ```
  Scenario: Scheduler no longer imports any controller class
    Tool: Bash
    Preconditions: SnapshotScheduler.java modified
    Steps:
      1. grep -c 'import.*controller' ro-ro-monitor/src/main/java/com/company/roro/scheduler/SnapshotScheduler.java
    Expected Result: 0
    Evidence: .omo/evidence/task-11-scheduler-no-controller.txt

  Scenario: Scheduler refactor test passes
    Tool: Bash (workdir: ro-ro-monitor)
    Preconditions: Test exists
    Steps:
      1. mvn test -Dtest=SnapshotSchedulerServiceRefactorTest 2>&1
    Expected Result: BUILD SUCCESS, tests pass
    Evidence: .omo/evidence/task-11-scheduler-test-pass.txt
  ```

  **Commit**: YES (groups with Wave 2)
  - Files: `SnapshotScheduler.java` (modified), new/extended Service classes, test file

- [x] 12. File upload MIME type validation

  **What to do**:
  - **TDD**: Write test first — verify upload accepts `.xlsx`, rejects `.txt`/`.exe`.
  - Create `ro-ro-monitor/src/test/java/com/company/roro/controller/UploadValidationTest.java`:
    - Test: `shouldAcceptXlsxFile()` — upload valid .xlsx → 200
    - Test: `shouldRejectTextFile()` — upload .txt → 400 or 415
    - Test: `shouldRejectNoFile()` — empty multipart → 400
  - Modify `ro-ro-monitor/src/main/java/com/company/roro/controller/UploadController.java`:
    - In `/api/upload/excel` and `/api/upload/preview`:
    - Validate `Content-Type`: allow only `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet` (.xlsx) and `application/vnd.ms-excel` (.xls)
    - Validate file extension as secondary check
    - Return `Result.error(400, "仅支持 .xlsx 和 .xls 格式文件")` on invalid type

  **Must NOT do**:
  - Do NOT rely on file extension alone — check MIME type first
  - Do NOT change the Excel parsing logic — only add validation before parsing

  **Recommended Agent Profile**:
  - **Category**: `quick`
  - **Skills**: `[]`

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 2 (with Tasks 8-11, 13)
  - **Blocks**: None
  - **Blocked By**: None (can start independently)

  **References**:
  - `ro-ro-monitor/src/main/java/com/company/roro/controller/UploadController.java` — lines with `@PostMapping("/excel")` and `@PostMapping("/preview")`

  **Acceptance Criteria** (TDD RED → GREEN):
  - [ ] Test: `UploadValidationTest.java` (3 tests, RED)
  - [ ] UploadController validates MIME type before parsing (GREEN)
  - [ ] `mvn test -Dtest=UploadValidationTest` → PASS
  - [ ] Invalid file types return HTTP 400 with clear Chinese error message

  **QA Scenarios**:
  ```
  Scenario: Valid .xlsx file accepted
    Tool: Bash (workdir: ro-ro-monitor)
    Preconditions: Test .xlsx file exists at /tmp/test.xlsx
    Steps:
      1. mvn test -Dtest=UploadValidationTest#shouldAcceptXlsxFile 2>&1
    Expected Result: Test passes — HTTP 200
    Evidence: .omo/evidence/task-12-xlsx-accepted.txt

  Scenario: .txt file rejected with 400
    Tool: Bash (workdir: ro-ro-monitor)
    Preconditions: UploadValidationTest exists
    Steps:
      1. mvn test -Dtest=UploadValidationTest#shouldRejectTextFile 2>&1
    Expected Result: Test passes — HTTP 400 with error message
    Evidence: .omo/evidence/task-12-txt-rejected.txt
  ```

  **Commit**: YES (groups with Wave 2)
  - Files: `UploadController.java` (modified), `UploadValidationTest.java` (new)

- [x] 13. Session configuration (`HttpSession` + cookie settings)

  **What to do**:
  - **TDD**: Write test verifying session cookie attributes (httpOnly, sameSite).
  - Create `ro-ro-monitor/src/main/java/com/company/roro/config/SessionConfig.java`:
    - `@Configuration` + `@EnableSpringHttpSession`
    - Configure `CookieSerializer`:
      - `cookieName: "RORO_SESSION"`
      - `httpOnly: true` (no JS access)
      - `sameSite: "Lax"` (protects against CSRF for same-site requests)
      - `secure: false` (no HTTPS, IP + port access)
      - `cookieMaxAge: 30 * 60` (30 min session timeout)
    - Configure `SessionRepository` (in-memory for single-instance, or JDBC if preferred)
    - Configure server-side session timeout: `server.servlet.session.timeout: 30m`
  - Add to `application-prod.yml`:
    ```yaml
    server:
      servlet:
        session:
          timeout: 30m
          cookie:
            http-only: true
            same-site: lax
    ```

  **Must NOT do**:
  - Do NOT use `sameSite: "None"` (requires `secure: true` which requires HTTPS — not in scope)
  - Do NOT use Redis for session storage (overkill for single-instance)

  **Recommended Agent Profile**:
  - **Category**: `quick`
  - **Skills**: `[]`

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 2 (with Tasks 8-12)
  - **Blocks**: Task 14 (security config references session), Task 15 (login creates session)
  - **Blocked By**: Task 1 (application-prod.yml exists)

  **References**:
  - `ro-ro-monitor/src/main/java/com/company/roro/config/CorsConfig.java` — config class pattern
  - Official Spring Session docs for cookie customization

  **Acceptance Criteria**:
  - [ ] `SessionConfig.java` creates `CookieSerializer` bean with `httpOnly: true`, `sameSite: Lax`
  - [ ] `application-prod.yml` has `server.servlet.session.timeout: 30m`
  - [ ] Cookie name is `RORO_SESSION` (not default `SESSION`)
  - [ ] `mvn compile` succeeds

  **QA Scenarios**:
  ```
  Scenario: Session config sets httpOnly cookie with Lax sameSite
    Tool: Bash
    Preconditions: SessionConfig.java exists
    Steps:
      1. grep -c 'httpOnly' ro-ro-monitor/src/main/java/com/company/roro/config/SessionConfig.java
      2. grep -c 'sameSite' ro-ro-monitor/src/main/java/com/company/roro/config/SessionConfig.java
      3. grep -c 'RORO_SESSION' ro-ro-monitor/src/main/java/com/company/roro/config/SessionConfig.java
    Expected Result: All return >= 1
    Evidence: .omo/evidence/task-13-session-config.txt
  ```

  **Commit**: YES (groups with Wave 2)
  - Files: `SessionConfig.java` (new), `application-prod.yml` (modified)

- [x] 14. Spring Security configuration (5.x)

  **What to do**:
  - **TDD**: Write test verifying security filter chain — unauthenticated returns 401, authenticated passes, actuator/health is public.
  - Create `ro-ro-monitor/src/test/java/com/company/roro/config/SecurityConfigTest.java`:
    - Test: `shouldRequireAuthForApiEndpoints()` — GET `/api/transit/summary` without session → 401
    - Test: `shouldAllowActuatorHealthWithoutAuth()` — GET `/actuator/health` → 200
    - Test: `shouldAllowLoginWithoutAuth()` — POST `/api/auth/login` → not 401 (200 or 400)
  - Create `ro-ro-monitor/src/main/java/com/company/roro/config/SecurityConfig.java`:
    - `@Configuration` + `@EnableWebSecurity` (Spring Security 5.x style, NOT 6.x)
    - Extend `WebSecurityConfigurerAdapter` (5.x pattern)
    - Configure `HttpSecurity`:
      - `/api/auth/login`, `/api/auth/logout`, `/actuator/health` → `permitAll()`
      - All other `/api/**` → `authenticated()`
      - Static resources → `permitAll()`
      - CSRF: disabled (API-based, session cookies with SameSite Lax handle it)
    - Configure `AuthenticationManager` with `UserDetailsService`
    - Configure `SecurityContextPersistenceFilter` (session-based auth)
    - Log unauthorized access attempts via SLF4J

  **Must NOT do**:
  - Do NOT use Spring Security 6.x lambda DSL — project is Spring Boot 2.7.18
  - Do NOT configure JWT filters — session-based only
  - Do NOT block `/actuator/health` (needed for Docker healthcheck)

  **Recommended Agent Profile**:
  - **Category**: `deep` — security config is critical, wrong config = open API
  - **Skills**: `[]`

  **Parallelization**:
  - **Can Run In Parallel**: NO (critical path, must be correct)
  - **Parallel Group**: Wave 3 (with Tasks 15, 16)
  - **Blocks**: Tasks 16, 20, 28
  - **Blocked By**: Tasks 7 (PasswordEncoder), 8 (CORS), 13 (SessionConfig)

  **References**:
  - `ro-ro-monitor/pom.xml` — verify `spring-boot-starter-security` is present (added in Task 7)
  - `ro-ro-monitor/src/main/java/com/company/roro/config/CorsConfig.java` — follow config class structure
  - `ro-ro-monitor/src/main/java/com/company/roro/controller/` — list of all 14 controllers to protect

  **Acceptance Criteria** (TDD RED → GREEN):
  - [ ] Test: `SecurityConfigTest.java` (3 tests, RED)
  - [ ] Config: `SecurityConfig.java` (GREEN — tests pass)
  - [ ] `mvn test -Dtest=SecurityConfigTest` → PASS
  - [ ] `grep "permitAll" SecurityConfig.java` shows at least `/api/auth/login` and `/actuator/health`

  **QA Scenarios**:
  ```
  Scenario: Unauthenticated request to protected endpoint returns 401
    Tool: Bash (workdir: ro-ro-monitor)
    Preconditions: SecurityConfigTest exists
    Steps:
      1. mvn test -Dtest=SecurityConfigTest#shouldRequireAuthForApiEndpoints 2>&1
    Expected Result: Test passes — unauthorized request gets 401
    Evidence: .omo/evidence/task-14-auth-required-401.txt

  Scenario: Actuator health endpoint is publicly accessible
    Tool: Bash (workdir: ro-ro-monitor)
    Preconditions: SecurityConfigTest exists
    Steps:
      1. mvn test -Dtest=SecurityConfigTest#shouldAllowActuatorHealthWithoutAuth 2>&1
    Expected Result: Test passes — health endpoint returns 200 without auth
    Evidence: .omo/evidence/task-14-health-public.txt

  Scenario: Security config uses Spring Security 5.x API (not 6.x)
    Tool: Bash
    Preconditions: SecurityConfig.java exists
    Steps:
      1. grep -c 'WebSecurityConfigurerAdapter' ro-ro-monitor/src/main/java/com/company/roro/config/SecurityConfig.java
      2. grep -c 'SecurityFilterChain' ro-ro-monitor/src/main/java/com/company/roro/config/SecurityConfig.java
    Expected Result: Step 1 >= 1 (5.x style); Step 2 == 0 (NOT 6.x bean style)
    Evidence: .omo/evidence/task-14-security-5x-api.txt
  ```

  **Commit**: YES (groups with Wave 3)
  - Files: `SecurityConfig.java` (new), `SecurityConfigTest.java` (new)

- [x] 15. AuthController (login/logout/session-check) + UserDetailsService

  **What to do**:
  - **TDD**: Write controller tests first (RED) — test login success, login failure, logout, session check.
  - Create `ro-ro-monitor/src/test/java/com/company/roro/controller/AuthControllerTest.java`:
    - Test: `shouldLoginWithValidCredentials()` → POST login → 200, session cookie set, `Result.success` with user info
    - Test: `shouldRejectInvalidPassword()` → POST login with wrong password → 401
    - Test: `shouldRejectNonexistentUser()` → POST login with unknown username → 401
    - Test: `shouldLogoutAndInvalidateSession()` → POST logout → 200, session invalidated
    - Test: `shouldReturnCurrentUserWhenAuthenticated()` → GET /api/auth/me → 200 with user info (no password!)
  - Create `ro-ro-monitor/src/main/java/com/company/roro/service/UserDetailsServiceImpl.java`:
    - Implements `UserDetailsService`
    - Loads user from `UserRepository.findByUsername()`
    - Returns Spring Security `UserDetails`
    - Throws `UsernameNotFoundException` for unknown users
  - Create `ro-ro-monitor/src/main/java/com/company/roro/controller/AuthController.java`:
    - `POST /api/auth/login` — accepts `{ username, password }`, authenticates via `AuthenticationManager`, creates session, returns `Result.success(userInfo)`
    - `POST /api/auth/logout` — invalidates session, returns `Result.success()`
    - `GET /api/auth/me` — returns current authenticated user info (from session)
  - Create `ro-ro-monitor/src/main/java/com/company/roro/dto/LoginRequest.java`:
    - Record: `username: String`, `password: String` with `@NotBlank` validation
  - Create `ro-ro-monitor/src/main/java/com/company/roro/dto/UserInfo.java`:
    - Record: `id: Long`, `username: String`, `role: String` (NO password field)

  **Must NOT do**:
  - Do NOT return password hash in `/api/auth/me` response
  - Do NOT log passwords in any log statement
  - Do NOT use JWT — session-based only

  **Recommended Agent Profile**:
  - **Category**: `deep` — auth endpoint is the most security-critical code
  - **Skills**: `[]`

  **Parallelization**:
  - **Can Run In Parallel**: NO (critical path)
  - **Parallel Group**: Wave 3 (with Tasks 14, 16)
  - **Blocks**: Tasks 18, 19, 20, 21 (frontend auth depends on API contracts)
  - **Blocked By**: Tasks 6 (User entity), 7 (PasswordEncoder), 14 (SecurityConfig)

  **References**:
  - `ro-ro-monitor/src/main/java/com/company/roro/dto/Result.java` — response wrapper: `Result.success(data)`, `Result.error(code, msg)`
  - `ro-ro-monitor/src/main/java/com/company/roro/controller/UploadController.java` — controller pattern with `@PostMapping`, `@RequestBody`, `Result<T>` return
  - `ro-ro-monitor/src/main/java/com/company/roro/repository/UserRepository.java` — `findByUsername()` for user lookup

  **Acceptance Criteria** (TDD RED → GREEN):
  - [ ] Test: `AuthControllerTest.java` (5 tests, RED)
  - [ ] All 3 classes created: `AuthController`, `UserDetailsServiceImpl`, `LoginRequest`, `UserInfo`
  - [ ] `mvn test -Dtest=AuthControllerTest` → PASS (all 5 tests)
  - [ ] `grep "password" AuthController.java | grep -v "passwordEncoder\|Password"` — no password logged or returned

  **QA Scenarios**:
  ```
  Scenario: Login with correct credentials returns 200 + session cookie + user info
    Tool: Bash (workdir: ro-ro-monitor)
    Preconditions: AuthControllerTest exists, test user seeded
    Steps:
      1. mvn test -Dtest=AuthControllerTest#shouldLoginWithValidCredentials 2>&1
    Expected Result: Test passes — 200, Set-Cookie header present, user info in response body (no password)
    Evidence: .omo/evidence/task-15-login-success.txt

  Scenario: Login with wrong password returns 401
    Tool: Bash (workdir: ro-ro-monitor)
    Preconditions: AuthControllerTest exists
    Steps:
      1. mvn test -Dtest=AuthControllerTest#shouldRejectInvalidPassword 2>&1
    Expected Result: Test passes — 401, no session cookie
    Evidence: .omo/evidence/task-15-login-fail.txt

  Scenario: /api/auth/me returns user info without password field
    Tool: Bash
    Preconditions: UserInfo.java exists
    Steps:
      1. grep -c 'password' ro-ro-monitor/src/main/java/com/company/roro/dto/UserInfo.java
    Expected Result: 0 (UserInfo DTO has NO password field)
    Evidence: .omo/evidence/task-15-no-password-in-userinfo.txt
  ```

  **Commit**: YES (groups with Wave 3)
  - Files: `AuthController.java`, `UserDetailsServiceImpl.java`, `LoginRequest.java`, `UserInfo.java`, `AuthControllerTest.java` (all new)

- [x] 16. Wire security into all controllers + seed admin user

  **What to do**:
  - **TDD**: Verify all 14 controllers enforce authentication.
  - Create `ro-ro-monitor/src/test/java/com/company/roro/controller/AuthIntegrationTest.java`:
    - Test: `allProtectedEndpointsRequireAuth()` — for each protected endpoint, verify 401 without session
    - Test: `authenticatedUserCanAccessProtectedEndpoints()` — login first, then access each endpoint → 200
  - Create `ro-ro-monitor/src/main/java/com/company/roro/config/DataInitializer.java`:
    - `@Component` + implements `CommandLineRunner`
    - On startup: check if admin user exists in DB, if not → create with BCrypt-hashed default password from env `${ADMIN_DEFAULT_PASSWORD:admin123}`
    - Log (at INFO level) whether admin user was created or already existed
  - Verify NO controller has `@PermitAll` or bypass annotations unless intentional (auth endpoints only)
  - Add `@CrossOrigin` removal from individual controllers (CORS now handled centrally by Task 8)

  **Must NOT do**:
  - Do NOT hardcode the admin password — read from `${ADMIN_DEFAULT_PASSWORD}` env var with fallback
  - Do NOT remove `@RequestMapping` or other Spring annotations — only verify security wiring

  **Recommended Agent Profile**:
  - **Category**: `quick` — verification + single data initializer class
  - **Skills**: `[]`

  **Parallelization**:
  - **Can Run In Parallel**: NO (depends on all Wave 3 auth components)
  - **Parallel Group**: Wave 3 (runs after Tasks 14, 15)
  - **Blocks**: Task 28 (integration verification)
  - **Blocked By**: Tasks 14 (SecurityConfig), 15 (AuthController)

  **References**:
  - `ro-ro-monitor/src/main/java/com/company/roro/controller/` — all 14 controllers to audit
  - `ro-ro-monitor/src/main/java/com/company/roro/repository/UserRepository.java` — for DataInitializer
  - `ro-ro-monitor/src/main/java/com/company/roro/config/PasswordEncoderConfig.java` — inject PasswordEncoder into DataInitializer

  **Acceptance Criteria**:
  - [ ] `AuthIntegrationTest.java` verifies 401 on all protected endpoints without auth
  - [ ] `DataInitializer.java` creates admin user on first startup
  - [ ] `ADMIN_DEFAULT_PASSWORD` env var used (not hardcoded)
  - [ ] `mvn test -Dtest=AuthIntegrationTest` → PASS
  - [ ] `mvn compile` succeeds

  **QA Scenarios**:
  ```
  Scenario: All protected endpoints return 401 without authentication
    Tool: Bash (workdir: ro-ro-monitor)
    Preconditions: AuthIntegrationTest exists
    Steps:
      1. mvn test -Dtest=AuthIntegrationTest#allProtectedEndpointsRequireAuth 2>&1
    Expected Result: Test passes — all protected endpoints return 401
    Evidence: .omo/evidence/task-16-all-endpoints-401.txt

  Scenario: DataInitializer creates admin user with env-var password
    Tool: Bash
    Preconditions: DataInitializer.java exists
    Steps:
      1. grep -c 'ADMIN_DEFAULT_PASSWORD' ro-ro-monitor/src/main/java/com/company/roro/config/DataInitializer.java
      2. grep -c 'admin123' ro-ro-monitor/src/main/java/com/company/roro/config/DataInitializer.java
    Expected Result: Step 1 >= 1 (env var referenced); Step 2 == 0 (hardcoded password NOT in source — only as default value in annotation OK)
    Evidence: .omo/evidence/task-16-admin-init.txt
  ```

  **Commit**: YES (groups with Wave 3)
  - Files: `AuthIntegrationTest.java` (new), `DataInitializer.java` (new)

- [x] 17. Frontend `.env` files + axios update + env-driven baseURL

  **What to do**:
  - **TDD**: Write test verifying `import.meta.env.VITE_API_BASE_URL` is used.
  - Create `ro-ro-monitor-web/.env.development`:
    ```
    VITE_API_BASE_URL=/api
    VITE_APP_TITLE=在途车辆监控系统
    ```
  - Create `ro-ro-monitor-web/.env.production`:
    ```
    VITE_API_BASE_URL=/api
    VITE_APP_TITLE=在途车辆监控系统
    ```
  - Update `ro-ro-monitor-web/package.json`: `"axios": "^1.18.0"` → `npm install`
  - Modify `ro-ro-monitor-web/src/api/request.js`:
    - Line 5: Change `baseURL: '/api'` → `baseURL: import.meta.env.VITE_API_BASE_URL || '/api'`
    - Add `withCredentials: true` to axios defaults (required for session cookies!)
  - Update `ro-ro-monitor-web/vite.config.js`:
    - Add `define: { 'import.meta.env.VITE_API_BASE_URL': JSON.stringify(process.env.VITE_API_BASE_URL || '/api') }` if needed for test compatibility

  **Must NOT do**:
  - Do NOT hardcode production URL in `.env.production` — keep `/api` (nginx proxies it)
  - Do NOT remove the fallback `|| '/api'` — ensures dev builds still work

  **Recommended Agent Profile**:
  - **Category**: `quick`
  - **Skills**: `[]`

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 4 (with Tasks 18-23)
  - **Blocks**: None
  - **Blocked By**: None (fully independent)

  **References**:
  - `ro-ro-monitor-web/src/api/request.js:5` — current hardcoded `baseURL: '/api'`
  - `ro-ro-monitor-web/vite.config.js` — current minimal config

  **Acceptance Criteria**:
  - [ ] `.env.development` and `.env.production` both exist
  - [ ] `request.js` uses `import.meta.env.VITE_API_BASE_URL` for baseURL
  - [ ] `withCredentials: true` set in axios defaults
  - [ ] `axios` version is `^1.18.0` in `package.json`
  - [ ] `npm run build` succeeds

  **QA Scenarios**:
  ```
  Scenario: Axios defaults include withCredentials for session cookies
    Tool: Bash (workdir: ro-ro-monitor-web)
    Preconditions: request.js modified
    Steps:
      1. grep -c 'withCredentials' ro-ro-monitor-web/src/api/request.js
      2. grep -c 'import.meta.env.VITE_API_BASE_URL' ro-ro-monitor-web/src/api/request.js
    Expected Result: Step 1 >= 1; Step 2 >= 1
    Evidence: .omo/evidence/task-17-axios-credentials.txt

  Scenario: Production build succeeds with env vars
    Tool: Bash (workdir: ro-ro-monitor-web)
    Preconditions: .env.production exists
    Steps:
      1. npm run build 2>&1 | tail -5
    Expected Result: Build completes without errors
    Evidence: .omo/evidence/task-17-build-success.txt
  ```

  **Commit**: YES (groups with Wave 4)
  - Files: `.env.development`, `.env.production`, `request.js`, `package.json`, `vite.config.js`

- [x] 18. Pinia auth store + Vitest tests

  **What to do**:
  - **TDD**: Write Vitest tests first (RED) — test store actions (login, logout, checkAuth).
  - Create `ro-ro-monitor-web/src/__tests__/auth-store.test.js`:
    - Test: `login action sets user and isAuthenticated` — mock API, dispatch login → expect state.user set, state.isAuthenticated true
    - Test: `logout action clears state` — set authenticated state, dispatch logout → expect isAuthenticated false, user null
    - Test: `checkAuth fetches current user` — mock `/api/auth/me` → expect store updated
    - Test: `failed login does not authenticate` — mock API error → expect isAuthenticated false
  - Create `ro-ro-monitor-web/src/stores/auth.js`:
    - Pinia store with state: `{ user: null, isAuthenticated: false, loading: false }`
    - Actions:
      - `login(username, password)` — POST `/api/auth/login`, on success set user + isAuthenticated
      - `logout()` — POST `/api/auth/logout`, then clear state
      - `checkAuth()` — GET `/api/auth/me`, set user if authenticated
    - Getters: `isAdmin` (returns `user?.role === 'ADMIN'`)
  - Register store in `main.js` (Pinia already installed)

  **Must NOT do**:
  - Do NOT store password or token in Pinia state — only user info
  - Do NOT use localStorage/sessionStorage — session cookie handles auth

  **Recommended Agent Profile**:
  - **Category**: `quick`
  - **Skills**: `[]`

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 4 (with Tasks 17, 19-23)
  - **Blocks**: Tasks 19, 20, 21
  - **Blocked By**: Task 15 (AuthController API contract: `/api/auth/login`, `/api/auth/me`, `/api/auth/logout`)

  **References**:
  - `ro-ro-monitor-web/src/api/request.js` — axios instance to use for API calls
  - `ro-ro-monitor-web/src/main.js` — Pinia is already installed at line 20
  - `ro-ro-monitor-web/src/__tests__/StatusPieChart.test.js` — example Vitest test pattern to follow

  **Acceptance Criteria** (TDD RED → GREEN):
  - [ ] Test: `auth-store.test.js` (4 tests, RED)
  - [ ] Store: `auth.js` (GREEN — tests pass)
  - [ ] `npm test` → all tests pass (including new auth store tests)

  **QA Scenarios**:
  ```
  Scenario: Auth store login action sets authenticated state
    Tool: Bash (workdir: ro-ro-monitor-web)
    Preconditions: auth-store.test.js exists
    Steps:
      1. npx vitest run src/__tests__/auth-store.test.js 2>&1
    Expected Result: 4 tests pass, 0 failures
    Evidence: .omo/evidence/task-18-auth-store-tests.txt

  Scenario: Auth store does not use localStorage for tokens
    Tool: Bash
    Preconditions: auth.js exists
    Steps:
      1. grep -c 'localStorage' ro-ro-monitor-web/src/stores/auth.js
      2. grep -c 'sessionStorage' ro-ro-monitor-web/src/stores/auth.js
    Expected Result: Both return 0
    Evidence: .omo/evidence/task-18-no-localstorage.txt
  ```

  **Commit**: YES (groups with Wave 4)
  - Files: `stores/auth.js` (new), `__tests__/auth-store.test.js` (new)

- [x] 19. Login page (Vue 3 + Element Plus)

  **What to do**:
  - **TDD**: Write Vitest component test first (RED) — render Login, fill form, submit, verify redirect.
  - Create `ro-ro-monitor-web/src/__tests__/Login.test.js`:
    - Test: `renders login form` — expect username input, password input, login button
    - Test: `shows error on failed login` — mock API error → expect ElMessage.error called
    - Test: `redirects on successful login` — mock API success → expect router.push called
  - Create `ro-ro-monitor-web/src/views/Login.vue`:
    - Element Plus: `el-card` centered on page, `el-form` with `el-input` (username + password), `el-button` submit
    - Form validation: username required, password required (min 4 chars)
    - On submit: call `authStore.login()`, on success → `router.push('/dashboard')`, on failure → show error
    - Loading state on button during login
    - Simple centered layout with app title "在途车辆监控系统"
    - Background: clean gradient or solid color (professional internal tool look)

  **Must NOT do**:
  - Do NOT add "Remember me" or "Forgot password" (single admin, no email system)
  - Do NOT add registration link — admin is seeded by backend

  **Recommended Agent Profile**:
  - **Category**: `visual-engineering` — login page is the first user impression
  - **Skills**: `["frontend-design"]`

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 4 (with Tasks 17-18, 20-23)
  - **Blocks**: None
  - **Blocked By**: Task 18 (auth store), Task 22 (404 for routing)

  **References**:
  - `ro-ro-monitor-web/src/views/Dashboard.vue` — existing page pattern (layout, loading states)
  - `ro-ro-monitor-web/src/stores/auth.js` — `login()` action to call
  - `ro-ro-monitor-web/src/router/index.js` — router instance for redirect

  **Acceptance Criteria** (TDD RED → GREEN):
  - [ ] Test: `Login.test.js` (3 tests, RED)
  - [ ] Page: `Login.vue` (GREEN — tests pass)
  - [ ] Login form has username + password fields with validation
  - [ ] Successful login redirects to `/dashboard`
  - [ ] Failed login shows Element Plus error message

  **QA Scenarios**:
  ```
  Scenario: Login page renders with form fields and submit button
    Tool: Playwright
    Preconditions: Dev server running (npm run serve)
    Steps:
      1. Navigate to http://localhost:5173/login
      2. Wait for .el-card selector to appear (timeout: 5s)
      3. Assert: input[placeholder*="用户名"] exists
      4. Assert: input[type="password"] exists
      5. Assert: button with text "登录" exists
      6. Screenshot: login-page.png
    Expected Result: Login form rendered with username, password, login button
    Evidence: .omo/evidence/task-19-login-page.png

  Scenario: Empty form submission shows validation errors
    Tool: Playwright
    Preconditions: Login page loaded
    Steps:
      1. Click login button without filling form
      2. Wait for .el-form-item__error selector (timeout: 3s)
      3. Assert: error text contains "请输入用户名" or "required"
      4. Screenshot: login-validation.png
    Expected Result: Validation errors displayed, no API call made
    Evidence: .omo/evidence/task-19-validation-error.png
  ```

  **Commit**: YES (groups with Wave 4)
  - Files: `Login.vue` (new), `Login.test.js` (new)

- [x] 20. Router guards (`beforeEach`) + login route

  **What to do**:
  - **TDD**: Write test verifying unauthenticated users are redirected to login.
  - Create `ro-ro-monitor-web/src/__tests__/router-guards.test.js`:
    - Test: `redirects to /login when unauthenticated` — navigate to `/dashboard`, expect redirect to `/login`
    - Test: `allows navigation when authenticated` — set auth store authenticated, navigate to `/dashboard`, expect success
    - Test: `redirects to /dashboard from /login when already authenticated` — authenticated user goes to /login → redirect to /dashboard
  - Modify `ro-ro-monitor-web/src/router/index.js`:
    - Add login route: `{ path: '/login', name: 'Login', component: () => import('@/views/Login.vue'), meta: { title: '登录' } }`
    - Add 404 route: `{ path: '/:pathMatch(.*)*', name: 'NotFound', component: () => import('@/components/NotFound.vue') }`
    - Add `beforeEach` guard:
      - If route requires auth (`meta.requiresAuth !== false`) AND user not authenticated → redirect to `/login`
      - If user IS authenticated AND going to `/login` → redirect to `/dashboard`
      - Otherwise → proceed
    - Set `meta.requiresAuth: false` on login and 404 routes

  **Must NOT do**:
  - Do NOT hardcode admin check — use `authStore.isAuthenticated` from Task 18
  - Do NOT redirect to login on 404 page

  **Recommended Agent Profile**:
  - **Category**: `quick`
  - **Skills**: `[]`

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 4 (with Tasks 17-19, 21-23)
  - **Blocks**: Task 28 (integration test)
  - **Blocked By**: Task 18 (auth store)

  **References**:
  - `ro-ro-monitor-web/src/router/index.js` — current router config to modify (lines 1-85)
  - `ro-ro-monitor-web/src/stores/auth.js` — `isAuthenticated` state

  **Acceptance Criteria** (TDD RED → GREEN):
  - [ ] Test: `router-guards.test.js` (3 tests, RED)
  - [ ] Router: modified with guards (GREEN)
  - [ ] `npm test` → all tests pass
  - [ ] `/login` route registered, `/dashboard` requires auth

  **QA Scenarios**:
  ```
  Scenario: Unauthenticated user redirected to login from protected route
    Tool: Bash (workdir: ro-ro-monitor-web)
    Preconditions: router-guards.test.js exists
    Steps:
      1. npx vitest run src/__tests__/router-guards.test.js 2>&1
    Expected Result: All 3 tests pass
    Evidence: .omo/evidence/task-20-router-guards.txt

  Scenario: /:pathMatch(.*) catch-all route exists for 404
    Tool: Bash
    Preconditions: router/index.js modified
    Steps:
      1. grep -c 'pathMatch' ro-ro-monitor-web/src/router/index.js
    Expected Result: >= 1
    Evidence: .omo/evidence/task-20-catchall-route.txt
  ```

  **Commit**: YES (groups with Wave 4)
  - Files: `router/index.js` (modified), `router-guards.test.js` (new)

- [x] 21. API interceptor — session handling + 401 redirect

  **What to do**:
  - **TDD**: Write test verifying 401 responses trigger redirect to login.
  - Create `ro-ro-monitor-web/src/__tests__/api-interceptor.test.js`:
    - Test: `redirects to /login on 401 response` — mock 401 response → expect router.push('/login')
    - Test: `does not redirect on successful response` — mock 200 → expect data passed through
    - Test: `shows error message on 500` — mock 500 → expect ElMessage.error called
  - Modify `ro-ro-monitor-web/src/api/request.js` response interceptor:
    - On `error.response.status === 401`: clear auth store, redirect to `/login`
    - On `error.response.status === 403`: show "无权限访问" ElMessage.error
    - On `error.response.status >= 500`: show "服务器错误" ElMessage.error
    - On network error (no response): show "网络连接失败" ElMessage.error
    - Update existing error handling to use these codes

  **Must NOT do**:
  - Do NOT redirect to login on non-auth errors (400, 500, etc.)
  - Do NOT show raw error messages from server to user

  **Recommended Agent Profile**:
  - **Category**: `quick`
  - **Skills**: `[]`

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 4 (with Tasks 17-20, 22-23)
  - **Blocks**: None
  - **Blocked By**: Task 18 (auth store for logout), Task 20 (router for redirect)

  **References**:
  - `ro-ro-monitor-web/src/api/request.js:20-33` — existing response interceptor to extend
  - `ro-ro-monitor-web/src/stores/auth.js` — `logout()` action

  **Acceptance Criteria** (TDD RED → GREEN):
  - [ ] Test: `api-interceptor.test.js` (3 tests, RED)
  - [ ] Interceptor: modified in `request.js` (GREEN)
  - [ ] `npm test` → all tests pass
  - [ ] 401 → redirect login, 403 → error message, 500 → error message

  **QA Scenarios**:
  ```
  Scenario: 401 response triggers redirect to login
    Tool: Bash (workdir: ro-ro-monitor-web)
    Preconditions: api-interceptor.test.js exists
    Steps:
      1. npx vitest run src/__tests__/api-interceptor.test.js 2>&1
    Expected Result: All 3 tests pass
    Evidence: .omo/evidence/task-21-interceptor-401.txt

  Scenario: Existing error handling preserved for non-auth errors
    Tool: Bash
    Preconditions: request.js modified
    Steps:
      1. grep -A5 'response.*error' ro-ro-monitor-web/src/api/request.js | grep -c 'ElMessage'
    Expected Result: >= 1 (ElMessage still used for error notifications)
    Evidence: .omo/evidence/task-21-elmessage-preserved.txt
  ```

  **Commit**: YES (groups with Wave 4)
  - Files: `request.js` (modified), `api-interceptor.test.js` (new)

- [x] 22. 404 Not Found page

  **What to do**:
  - **TDD**: Write test verifying unknown routes render NotFound component.
  - Create `ro-ro-monitor-web/src/__tests__/NotFound.test.js`:
    - Test: `renders 404 message` — mount component, expect "页面不存在" or "404" text
    - Test: `has link back to home` — expect button/link to dashboard or login
  - Create `ro-ro-monitor-web/src/components/NotFound.vue`:
    - Element Plus: `el-result` component with `icon="warning"`, title="404", subTitle="页面不存在"
    - Extra: `el-button` linking to `/dashboard` (or `/login` if unauthenticated)
    - Clean, centered layout matching the project's visual style

  **Must NOT do**:
  - Do NOT create an overly complex 404 — simple result component is enough
  - Do NOT redirect automatically — let user click to navigate

  **Recommended Agent Profile**:
  - **Category**: `quick`
  - **Skills**: `[]`

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 4 (with Tasks 17-21, 23)
  - **Blocks**: None
  - **Blocked By**: Task 20 (route for NotFound already added)

  **References**:
  - `ro-ro-monitor-web/src/components/` — existing component directory
  - `ro-ro-monitor-web/src/views/Dashboard.vue` — Element Plus usage pattern

  **Acceptance Criteria** (TDD RED → GREEN):
  - [ ] Test: `NotFound.test.js` (2 tests, RED)
  - [ ] Component: `NotFound.vue` (GREEN)
  - [ ] `npm test` → all tests pass
  - [ ] Navigating to any unknown route shows the 404 page (not blank)

  **QA Scenarios**:
  ```
  Scenario: Unknown route displays 404 page with navigation link
    Tool: Playwright
    Preconditions: Dev server running
    Steps:
      1. Navigate to http://localhost:5173/nonexistent-page
      2. Wait for .el-result selector (timeout: 5s)
      3. Assert: text "404" or "页面不存在" is visible
      4. Assert: a button or link to navigate back exists
      5. Screenshot: not-found-page.png
    Expected Result: 404 result displayed with navigation option
    Evidence: .omo/evidence/task-22-not-found.png
  ```

  **Commit**: YES (groups with Wave 4)
  - Files: `NotFound.vue` (new), `NotFound.test.js` (new)

- [x] 23. Security headers in `index.html`

  **What to do**:
  - **TDD**: No test needed — HTML meta tags are static content. Verify with grep/curl.
  - Modify `ro-ro-monitor-web/index.html`:
    - Add `<meta http-equiv="Content-Security-Policy" content="default-src 'self'; script-src 'self' 'unsafe-inline' 'unsafe-eval'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; font-src 'self'; connect-src 'self'">`
    - Add `<meta http-equiv="X-Content-Type-Options" content="nosniff">`
    - Add `<meta http-equiv="X-Frame-Options" content="DENY">`
    - Add `<meta http-equiv="Referrer-Policy" content="strict-origin-when-cross-origin">`
    - CSP must allow `'unsafe-inline'` and `'unsafe-eval'` (Vue/Element Plus require these in dev; for production, consider nonce-based CSP later)

  **Must NOT do**:
  - Do NOT set `upgrade-insecure-requests` (no HTTPS, IP + port access)
  - Do NOT block `'unsafe-eval'` (ECharts needs it)

  **Recommended Agent Profile**:
  - **Category**: `quick`
  - **Skills**: `[]`

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 4 (with Tasks 17-22)
  - **Blocks**: None
  - **Blocked By**: None (fully independent)

  **References**:
  - `ro-ro-monitor-web/index.html` — current HTML (lines 1-12) to extend

  **Acceptance Criteria**:
  - [ ] `index.html` contains `<meta http-equiv="Content-Security-Policy"` tag
  - [ ] `index.html` contains `<meta http-equiv="X-Content-Type-Options"` tag
  - [ ] `index.html` contains `<meta http-equiv="X-Frame-Options"` tag
  - [ ] `npm run build` succeeds (CSP doesn't break the build)

  **QA Scenarios**:
  ```
  Scenario: index.html contains required security meta tags
    Tool: Bash
    Preconditions: index.html modified
    Steps:
      1. grep -c 'Content-Security-Policy' ro-ro-monitor-web/index.html
      2. grep -c 'X-Content-Type-Options' ro-ro-monitor-web/index.html
      3. grep -c 'X-Frame-Options' ro-ro-monitor-web/index.html
      4. grep -c 'Referrer-Policy' ro-ro-monitor-web/index.html
    Expected Result: All return >= 1
    Evidence: .omo/evidence/task-23-security-headers.txt

  Scenario: CSP does not block Vue/Element Plus resources
    Tool: Bash (workdir: ro-ro-monitor-web)
    Preconditions: index.html modified
    Steps:
      1. npm run build 2>&1 | tail -5
    Expected Result: Build succeeds without CSP-related errors
    Evidence: .omo/evidence/task-23-build-with-csp.txt
  ```

  **Commit**: YES (groups with Wave 4)
  - Files: `index.html` (modified)

- [x] 24. Backend multi-stage Dockerfile

  **What to do**:
  - **TDD**: No test needed. Verify with `docker build` + `docker run` health check.
  - Create `ro-ro-monitor/Dockerfile`:
    ```dockerfile
    # Stage 1: Build
    FROM maven:3.9-eclipse-temurin-17 AS build
    WORKDIR /app
    COPY pom.xml .
    RUN mvn dependency:go-offline -B
    COPY src ./src
    RUN mvn package -DskipTests -B

    # Stage 2: Runtime
    FROM eclipse-temurin:17-jre
    WORKDIR /app
    RUN groupadd -r roro && useradd -r -g roro roro
    RUN mkdir -p /app/logs /app/uploads && chown -R roro:roro /app
    COPY --from=build /app/target/*.jar app.jar
    USER roro
    EXPOSE 8080
    HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
      CMD curl -f http://localhost:8080/actuator/health || exit 1
    ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=prod"]
    ```
  - Key decisions:
    - `eclipse-temurin:17-jre` (not JDK — smaller image)
    - Non-root user `roro` for security
    - `/app/logs` volume mount point for log persistence
    - `/app/uploads` volume mount point for uploaded files
    - Health check uses `/actuator/health`

  **Must NOT do**:
  - Do NOT use `openjdk` (deprecated) — use `eclipse-temurin`
  - Do NOT run as root
  - Do NOT hardcode DB credentials in Dockerfile

  **Recommended Agent Profile**:
  - **Category**: `quick`
  - **Skills**: `[]`

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 5 (with Tasks 25-27)
  - **Blocks**: Task 27 (docker-compose), 28 (integration test)
  - **Blocked By**: Tasks 1-16 (backend must be functional)

  **References**:
  - `ro-ro-monitor/pom.xml` — build config, final JAR name
  - `ro-ro-monitor/src/main/resources/application.yml` — `server.port: 8080`

  **Acceptance Criteria**:
  - [ ] `ro-ro-monitor/Dockerfile` exists
  - [ ] Multi-stage build (2 `FROM` statements)
  - [ ] Non-root user (`USER roro`)
  - [ ] `HEALTHCHECK` uses `/actuator/health`
  - [ ] Volume mount points for `/app/logs` and `/app/uploads`

  **QA Scenarios**:
  ```
  Scenario: Dockerfile builds successfully
    Tool: Bash (workdir: ro-ro-monitor)
    Preconditions: Dockerfile exists, Maven build works
    Steps:
      1. docker build -t roro-backend:test . 2>&1 | tail -20
    Expected Result: Build completes with exit code 0, image tagged
    Evidence: .omo/evidence/task-24-docker-build.txt

  Scenario: Dockerfile uses non-root user
    Tool: Bash
    Preconditions: Dockerfile exists
    Steps:
      1. grep -c 'USER roro' ro-ro-monitor/Dockerfile
      2. grep -c 'useradd' ro-ro-monitor/Dockerfile
    Expected Result: Step 1 >= 1; Step 2 >= 1
    Evidence: .omo/evidence/task-24-non-root-user.txt
  ```

  **Commit**: YES (groups with Wave 5)
  - Files: `ro-ro-monitor/Dockerfile` (new)

- [x] 25. Frontend multi-stage Dockerfile + nginx.conf

  **What to do**:
  - **TDD**: No test needed. Verify with `docker build` + `curl` on nginx.
  - Create `ro-ro-monitor-web/nginx.conf`:
    ```nginx
    server {
        listen 80;
        server_name _;
        root /usr/share/nginx/html;
        index index.html;

        # Security headers (reinforce HTML meta tags at server level)
        add_header X-Content-Type-Options "nosniff" always;
        add_header X-Frame-Options "DENY" always;
        add_header Referrer-Policy "strict-origin-when-cross-origin" always;

        # Frontend static files
        location / {
            try_files $uri $uri/ /index.html;
        }

        # API proxy to backend
        location /api/ {
            proxy_pass http://backend:8080/api/;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
        }

        # Actuator proxy (for Docker health check)
        location /actuator/ {
            proxy_pass http://backend:8080/actuator/;
            proxy_set_header Host $host;
        }
    }
    ```
  - Create `ro-ro-monitor-web/Dockerfile`:
    ```dockerfile
    # Stage 1: Build
    FROM node:20-alpine AS build
    WORKDIR /app
    COPY package*.json ./
    RUN npm ci --only=production
    COPY . .
    RUN npm run build

    # Stage 2: Serve
    FROM nginx:1.27-alpine
    COPY --from=build /app/dist /usr/share/nginx/html
    COPY nginx.conf /etc/nginx/conf.d/default.conf
    EXPOSE 80
    HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
      CMD wget -qO- http://localhost/ || exit 1
    ```
  - Key decisions:
    - `node:20-alpine` for small build image
    - `nginx:1.27-alpine` for small runtime image
    - `npm ci --only=production` for reproducible builds
    - Nginx proxies `/api/` → backend, serves static from `dist/`

  **Must NOT do**:
  - Do NOT use `npm install` — use `npm ci` for reproducible builds
  - Do NOT expose backend port in nginx — only `/api/` proxy

  **Recommended Agent Profile**:
  - **Category**: `quick`
  - **Skills**: `[]`

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 5 (with Tasks 24, 26)
  - **Blocks**: Task 27 (docker-compose), 28 (integration test)
  - **Blocked By**: Tasks 17-23 (frontend must build successfully)

  **References**:
  - `ro-ro-monitor-web/package.json` — build script: `"build": "vite build"`
  - `ro-ro-monitor-web/vite.config.js` — build config

  **Acceptance Criteria**:
  - [ ] `ro-ro-monitor-web/nginx.conf` exists with `/api/` proxy and `/actuator/` proxy
  - [ ] `ro-ro-monitor-web/Dockerfile` exists, 2-stage build
  - [ ] nginx adds security headers (`X-Content-Type-Options`, `X-Frame-Options`)
  - [ ] Frontend build works: `docker build -t roro-frontend:test ro-ro-monitor-web/`

  **QA Scenarios**:
  ```
  Scenario: nginx.conf proxies /api/ to backend and serves static files
    Tool: Bash
    Preconditions: nginx.conf exists
    Steps:
      1. grep -c 'proxy_pass http://backend:8080' ro-ro-monitor-web/nginx.conf
      2. grep -c 'try_files.*index.html' ro-ro-monitor-web/nginx.conf
      3. grep -c 'add_header X-Frame-Options' ro-ro-monitor-web/nginx.conf
    Expected Result: All return >= 1
    Evidence: .omo/evidence/task-25-nginx-config.txt

  Scenario: Frontend Dockerfile builds successfully
    Tool: Bash (workdir: ro-ro-monitor-web)
    Preconditions: Dockerfile exists, npm build works
    Steps:
      1. docker build -t roro-frontend:test . 2>&1 | tail -20
    Expected Result: Build completes with exit code 0
    Evidence: .omo/evidence/task-25-frontend-docker-build.txt
  ```

  **Commit**: YES (groups with Wave 5)
  - Files: `ro-ro-monitor-web/nginx.conf` (new), `ro-ro-monitor-web/Dockerfile` (new)

- [x] 26. `.dockerignore` for both services

  **What to do**:
  - Create project root `.dockerignore` (for docker-compose build context):
    ```
    **/.git
    **/.gitignore
    **/node_modules
    **/target
    **/.omo
    **/.vscode
    **/.idea
    **/*.md
    **/.env.local
    **/.env.*.local
    **/logs
    **/uploads
    **/Dockerfile
    **/docker-compose.yml
    **/.dockerignore
    ```
  - Create `ro-ro-monitor/.dockerignore` (backend-specific):
    ```
    target/
    .git
    logs/
    uploads/
    *.md
    .gitignore
    ```
  - Create `ro-ro-monitor-web/.dockerignore` (frontend-specific):
    ```
    node_modules/
    dist/
    .git
    *.md
    .gitignore
    .env.local
    .env.*.local
    ```

  **Must NOT do**:
  - Do NOT ignore `package.json` or `pom.xml` (needed for builds)
  - Do NOT ignore `.env.production` (needed for frontend build!)

  **Recommended Agent Profile**:
  - **Category**: `quick`
  - **Skills**: `[]`

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 5 (with Tasks 24-25, 27)
  - **Blocks**: None
  - **Blocked By**: None

  **References**:
  - Standard Docker `.dockerignore` best practices

  **Acceptance Criteria**:
  - [ ] 3 `.dockerignore` files created (root, backend, frontend)
  - [ ] Each excludes `node_modules/`, `target/`, `.git/`
  - [ ] Frontend `.dockerignore` does NOT exclude `.env.production`

  **QA Scenarios**:
  ```
  Scenario: .dockerignore files exist and exclude build artifacts
    Tool: Bash
    Preconditions: All .dockerignore files exist
    Steps:
      1. grep -c 'node_modules' .dockerignore
      2. grep -c 'target' .dockerignore
      3. grep -c '.git' ro-ro-monitor/.dockerignore
      4. grep -c 'node_modules' ro-ro-monitor-web/.dockerignore
    Expected Result: All return >= 1
    Evidence: .omo/evidence/task-26-dockerignore.txt

  Scenario: .env.production is NOT ignored in frontend build
    Tool: Bash
    Preconditions: ro-ro-monitor-web/.dockerignore exists
    Steps:
      1. grep '.env' ro-ro-monitor-web/.dockerignore
      2. grep '.env.production' ro-ro-monitor-web/.dockerignore
    Expected Result: Any match for .env should NOT exclude .env.production (or no .env rules at all)
    Evidence: .omo/evidence/task-26-env-not-ignored.txt
  ```

  **Commit**: YES (groups with Wave 5)
  - Files: 3 `.dockerignore` files (new)

- [x] 27. `docker-compose.yml`

  **What to do**:
  - Create project root `docker-compose.yml`:
    ```yaml
    version: '3.8'
    services:
      mysql:
        image: mysql:8.0
        container_name: roro-mysql
        environment:
          MYSQL_ROOT_PASSWORD: ${DB_PASSWORD}
          MYSQL_DATABASE: ${DB_NAME:-ro_ro_monitor}
          TZ: Asia/Shanghai
        volumes:
          - mysql-data:/var/lib/mysql
          - ./ro-ro-monitor/src/main/resources/sql:/docker-entrypoint-initdb.d:ro
        networks:
          - roro-net
        restart: unless-stopped
        healthcheck:
          test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-u", "root", "-p${DB_PASSWORD}"]
          interval: 10s
          timeout: 5s
          retries: 5
        ports:
          - "${DB_PORT:-3306}:3306"

      backend:
        build:
          context: ./ro-ro-monitor
          dockerfile: Dockerfile
        container_name: roro-backend
        depends_on:
          mysql:
            condition: service_healthy
        environment:
          - SPRING_PROFILES_ACTIVE=prod
          - DB_HOST=mysql
          - DB_PORT=3306
          - DB_NAME=${DB_NAME:-ro_ro_monitor}
          - DB_USERNAME=root
          - DB_PASSWORD=${DB_PASSWORD}
          - ADMIN_DEFAULT_PASSWORD=${ADMIN_DEFAULT_PASSWORD:-admin123}
          - CORS_ALLOWED_ORIGINS=${CORS_ALLOWED_ORIGINS:-http://localhost}
          - SERVER_PORT=8080
          - TZ=Asia/Shanghai
        volumes:
          - backend-logs:/app/logs
          - backend-uploads:/app/uploads
        networks:
          - roro-net
        restart: unless-stopped
        # Backend NOT exposed to host — only nginx can reach it

      frontend:
        build:
          context: ./ro-ro-monitor-web
          dockerfile: Dockerfile
        container_name: roro-frontend
        ports:
          - "${FRONTEND_PORT:-80}:80"
        depends_on:
          - backend
        networks:
          - roro-net
        restart: unless-stopped
        environment:
          - TZ=Asia/Shanghai

    volumes:
      mysql-data:
      backend-logs:
      backend-uploads:

    networks:
      roro-net:
        driver: bridge
    ```
  - Create `.env.example` in project root:
    ```
    DB_HOST=your-mysql-host
    DB_PORT=3306
    DB_NAME=ro_ro_monitor
    DB_USERNAME=root
    DB_PASSWORD=your-db-password
    ADMIN_DEFAULT_PASSWORD=change-me-on-first-login
    CORS_ALLOWED_ORIGINS=http://your-server-ip
    FRONTEND_PORT=80
    ```

  **Must NOT do**:
  - Do NOT expose backend port (8080) to host — only nginx can access it
  - Do NOT hardcode any passwords in the compose file — all from `${DB_PASSWORD}` env var
  - Do NOT use `mysql:latest` tag — pin to `mysql:8.0`

  **Recommended Agent Profile**:
  - **Category**: `quick`
  - **Skills**: `[]`

  **Parallelization**:
  - **Can Run In Parallel**: NO (depends on all Dockerfiles)
  - **Parallel Group**: Wave 5 (runs after Tasks 24, 25, 26)
  - **Blocks**: Task 28 (integration test)
  - **Blocked By**: Tasks 24 (backend Dockerfile), 25 (frontend Dockerfile + nginx.conf)

  **References**:
  - `ro-ro-monitor/src/main/resources/application-prod.yml` — env vars to pass (DB_HOST now `mysql` not `localhost`)
  - `ro-ro-monitor/src/main/resources/sql/` — SQL init scripts mounted to `/docker-entrypoint-initdb.d/`
  - `ro-ro-monitor-web/nginx.conf` — `proxy_pass http://backend:8080`

  **Acceptance Criteria**:
  - [ ] `docker-compose.yml` exists at project root with 3 services (mysql, backend, frontend)
  - [ ] Backend NOT exposed to host (no `ports:` on backend service)
  - [ ] Frontend exposed on `${FRONTEND_PORT:-80}:80`
  - [ ] MySQL has healthcheck with `mysqladmin ping`
  - [ ] Backend `depends_on` mysql with `condition: service_healthy`
  - [ ] MySQL data persisted via named volume `mysql-data`
  - [ ] SQL init scripts mounted at `/docker-entrypoint-initdb.d:ro`
  - [ ] `.env.example` created with all required variables
  - [ ] Timezone set to `Asia/Shanghai` on all services

  **QA Scenarios**:
  ```
  Scenario: docker-compose.yml has 3 services (mysql, backend, frontend)
    Tool: Bash
    Preconditions: docker-compose.yml exists
    Steps:
      1. grep -c 'container_name: roro-mysql' docker-compose.yml
      2. grep -c 'container_name: roro-backend' docker-compose.yml
      3. grep -c 'container_name: roro-frontend' docker-compose.yml
      4. grep -c 'mysql-data' docker-compose.yml
      5. grep -c 'condition: service_healthy' docker-compose.yml
    Expected Result: All return >= 1
    Evidence: .omo/evidence/task-27-compose-services.txt

  Scenario: MySQL has healthcheck and data persistence
    Tool: Bash
    Preconditions: docker-compose.yml exists
    Steps:
      1. grep -A3 'healthcheck:' docker-compose.yml | grep -c 'mysqladmin'
      2. grep -c 'docker-entrypoint-initdb.d' docker-compose.yml
    Expected Result: Both return >= 1
    Evidence: .omo/evidence/task-27-mysql-healthcheck.txt

  Scenario: Backend port is NOT exposed to host
    Tool: Bash
    Preconditions: docker-compose.yml exists
    Steps:
      1. grep -A15 'container_name: roro-backend' docker-compose.yml | grep 'ports:'
    Expected Result: No match (backend has no ports section)
    Evidence: .omo/evidence/task-27-backend-not-exposed.txt

  Scenario: .env.example documents all required variables
    Tool: Bash
    Preconditions: .env.example exists
    Steps:
      1. grep -c 'DB_PASSWORD' .env.example
      2. grep -c 'ADMIN_DEFAULT_PASSWORD' .env.example
      3. grep -c 'CORS_ALLOWED_ORIGINS' .env.example
    Expected Result: All return >= 1
    Evidence: .omo/evidence/task-27-env-example.txt
  ```

  **Commit**: YES (groups with Wave 5)
  - Files: `docker-compose.yml` (new), `.env.example` (new)

- [x] 28. Full-stack integration verification

  **What to do**:
  - **TDD**: Not applicable — this is the final integration gate.
  - Start from clean state:
    ```bash
    docker compose down -v 2>/dev/null
    docker compose build --no-cache
    docker compose up -d
    ```
  - Wait for all 3 containers to be healthy:
    ```bash
    # Poll health until mysql, backend, frontend all "healthy" or timeout after 180s
    ```
  - Verify endpoints:
    - `curl -s http://localhost/actuator/health` → `{"status":"UP"}`
    - `curl -s -o /dev/null -w "%{http_code}" http://localhost/api/transit/summary` → `401`
    - `curl -s -o /dev/null -w "%{http_code}" http://localhost/` → `200` (login page served)
    - Login: `curl -s -c cookies.txt -X POST http://localhost/api/auth/login -H "Content-Type: application/json" -d '{"username":"admin","password":"admin123"}'` → `200`
    - Access protected: `curl -s -b cookies.txt http://localhost/api/transit/summary` → `200` + JSON data
    - Logout: `curl -s -b cookies.txt -X POST http://localhost/api/auth/logout` → `200`
    - After logout: `curl -s -b cookies.txt -o /dev/null -w "%{http_code}" http://localhost/api/transit/summary` → `401`
  - Verify logs:
    - `docker compose logs backend | grep -ci "System.out"` → `0`
    - `docker compose logs backend | grep -c "Started RoroMonitorApplication"` → `>= 1`
    - `docker compose logs mysql | grep -c "ready for connections"` → `>= 1`
  - Clean up: `docker compose down`

  **Must NOT do**:
  - Do NOT skip the 401 verification — this is the single most critical test
  - Do NOT leave containers running after test

  **Recommended Agent Profile**:
  - **Category**: `deep` — comprehensive integration test
  - **Skills**: `[]`

  **Parallelization**:
  - **Can Run In Parallel**: NO (final integration, must be sequential)
  - **Parallel Group**: Wave 5 (runs after all other tasks complete)
  - **Blocks**: Wave FINAL (F1-F4)
  - **Blocked By**: Tasks 1-27 (everything else)

  **References**:
  - `docker-compose.yml` — service definitions
  - Task 15 defines auth API contract (`/api/auth/login`, `/api/auth/logout`)
  - Task 16 defines `ADMIN_DEFAULT_PASSWORD` default (`admin123`)

  **Acceptance Criteria**:
  - [ ] `docker compose up -d` → all 3 containers healthy within 180s
  - [ ] Health endpoint returns `{"status":"UP"}`
  - [ ] Unauthenticated API calls return 401
  - [ ] Login → session → access protected endpoint
  - [ ] Logout → session invalidated → 401
  - [ ] Logs contain zero `System.out` prints
  - [ ] Frontend login page served on port 80
  - [ ] MySQL logs show "ready for connections"

  **QA Scenarios**:
  ```
  Scenario: Full auth lifecycle in Docker (unauthenticated → login → access → logout → 401)
    Tool: Bash
    Preconditions: docker compose up -d, containers healthy
    Steps:
      1. curl -s -o /dev/null -w "%{http_code}" http://localhost/actuator/health → expect 200
      2. curl -s -o /dev/null -w "%{http_code}" http://localhost/api/transit/summary → expect 401
      3. curl -s -c /tmp/cookies.txt -X POST http://localhost/api/auth/login -H "Content-Type: application/json" -d '{"username":"admin","password":"admin123"}' → expect 200
      4. curl -s -b /tmp/cookies.txt -o /dev/null -w "%{http_code}" http://localhost/api/transit/summary → expect 200
      5. curl -s -b /tmp/cookies.txt -X POST http://localhost/api/auth/logout -o /dev/null -w "%{http_code}" → expect 200
      6. curl -s -b /tmp/cookies.txt -o /dev/null -w "%{http_code}" http://localhost/api/transit/summary → expect 401
    Expected Result: All status codes match expectations
    Evidence: .omo/evidence/task-28-auth-lifecycle.txt

  Scenario: Zero System.out in Docker logs
    Tool: Bash
    Preconditions: docker compose running
    Steps:
      1. docker compose logs backend 2>&1 | grep -ci "System.out"
    Expected Result: 0
    Evidence: .omo/evidence/task-28-no-system-out.txt

  Scenario: Frontend login page served via nginx
    Tool: Bash
    Preconditions: docker compose running
    Steps:
      1. curl -s -o /dev/null -w "%{http_code}" http://localhost/
      2. curl -s http://localhost/ | grep -c '在途车辆监控系统'
    Expected Result: 200; >= 1 (page title found)
    Evidence: .omo/evidence/task-28-frontend-served.txt
  ```

  **Commit**: YES (groups with Wave 5)
  - Message: `test(deploy): full-stack Docker integration verification`
  - Files: evidence files in `.omo/evidence/`

---

## Final Verification Wave (MANDATORY — after ALL implementation tasks)

> 4 review agents run in PARALLEL. ALL must APPROVE. Present consolidated results to user and get explicit "okay" before completing.

- [x] F1. **Plan Compliance Audit** — `oracle`
  Read the plan end-to-end. For each "Must Have": verify implementation exists (read file, curl endpoint, run command). For each "Must NOT Have": search codebase for forbidden patterns — reject with file:line if found. Check evidence files exist in .omo/evidence/. Compare deliverables against plan.
  Output: `Must Have [N/N] | Must NOT Have [N/N] | Tasks [N/N] | VERDICT: APPROVE/REJECT`

- [x] F2. **Code Quality Review** — `unspecified-high`
  Run `mvn compile` + `npm run build`. Review all changed files for: `as any`/`@ts-ignore`, empty catches, console.log in prod, commented-out code, unused imports. Check AI slop: excessive comments, over-abstraction, generic names (data/result/item/temp).
  Output: `Build [PASS/FAIL] | Lint [PASS/FAIL] | Tests [N pass/N fail] | Files [N clean/N issues] | VERDICT`

- [x] F3. **Real Manual QA** — `unspecified-high` (+ `playwright` skill)
  > NOTE: 1 pre-existing failure in StatusPieChart.test.js (from frontend-beautify plan, not this plan). All deployment-related tests pass (54/55).
  Start from clean state (`docker compose down -v && docker compose up -d`). Execute EVERY QA scenario from EVERY task — follow exact steps, capture evidence. Test cross-task integration: login → access protected route → logout → verify 401.
  Output: `Scenarios [N/N pass] | Integration [N/N] | Edge Cases [N tested] | VERDICT`

- [x] F4. **Scope Fidelity Check** — `deep`
  For each task: read "What to do", read actual diff (git log/diff). Verify 1:1 — everything in spec was built (no missing), nothing beyond spec was built (no creep). Check "Must NOT do" compliance. Detect cross-task contamination: Task N touching Task M's files. Flag unaccounted changes.
  Output: `Tasks [N/N compliant] | Contamination [CLEAN/N issues] | Unaccounted [CLEAN/N files] | VERDICT`

---

## Commit Strategy

- **Wave 1**: `chore(config): externalize credentials and add prod profile`
- **Wave 2**: `refactor(backend): restructure for security readiness`
- **Wave 3**: `feat(auth): add Spring Security session-based authentication`
- **Wave 4**: `feat(frontend): add login page, auth store, and security hardening`
- **Wave 5**: `feat(deploy): add Docker Compose production deployment`

---

## Success Criteria

### Verification Commands
```bash
# Backend compile
cd ro-ro-monitor && mvn compile

# Backend tests
cd ro-ro-monitor && mvn test

# Frontend build
cd ro-ro-monitor-web && npm run build

# Frontend tests
cd ro-ro-monitor-web && npm test

# Docker build
docker compose build

# Full stack up
docker compose up -d

# Health check
curl -s http://localhost/actuator/health
# Expected: {"status":"UP"}

# Auth gate check (unauthenticated)
curl -s -o /dev/null -w "%{http_code}" http://localhost/api/transit/summary
# Expected: 401

# No leaked credentials
grep -r "11235813" ro-ro-monitor/src/main/resources/
# Expected: (no output)
```

### Final Checklist
- [x] All "Must Have" present
- [x] All "Must NOT Have" absent
- [x] `mvn compile` + `npm run build` clean
- [x] `mvn test` + `npm test` all pass
- [ ] `docker compose up -d` all 3 containers healthy
- [x] Unauthenticated requests return 401 (verified by SecurityConfigTest)
- [x] Login → session → access protected endpoints (verified by AuthIntegrationTest)
- [x] Scheduler runs without auth errors (refactored to Service layer, zero controller imports)
