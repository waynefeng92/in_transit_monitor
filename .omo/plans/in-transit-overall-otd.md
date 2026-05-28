# 整段在途时效预警功能开发计划

## TL;DR

> **Quick Summary**: 在现有的分段时效预警基础上，增加整段（累计）时效预警功能。从订单释放开始计算累计已用时间，与到当前状态为止的累计标准 OTD 对比，判断 NORMAL/WARN/OVERDUE。Dashboard 新增标签页切换「分段监控」/「整段监控」。
>
> **Deliverables**:
> - `StatusCalculator.calculateOverallMonitorStatus()` 新计算方法
> - `VehicleTransit.overallMonitorStatus` 新字段 + DB 列
> - `application.yml` 新增 `monitor.overall-warn-ratio: 0.8`
> - 后端 API 增强（/summary + /chart/brand-status?type=overall）
> - Dashboard 标签页切换 + 整段监控视图
>
> **Estimated Effort**: Medium
> **Parallel Execution**: YES — 4 waves
> **Critical Path**: 测试 → 实现 → API → 前端

---

## Context

### Original Request
在现有分段时效预警基础上，增加针对整段（累计）时效预警功能。即从订单释放时间到当前时刻的总耗时，对比到当前状态为止的累计标准 OTD 时间。

### Interview Summary
**Key Decisions**:
- **WARN 阈值**：累计标准 OTD 的 80%（可配置于 application.yml）
- **OVERDUE 阈值**：到当前状态为止的各段标准 OTD 之和
- **图表维度**：品牌 × 整体监控状态（去掉在途状态维度）
- **ARRIVED 处理**：分段监控始终 NORMAL；整段监控计算全程 OTD 对比但不展示在 Dashboard 中（排除 ARRIVED）
- **展示方式**：Dashboard 标签页切换「分段监控」/「整段监控」
- **测试策略**：TDD
- **RouteOtdConfig**：不修改

**Research Findings**:
- StatusCalculator 是静态工具类，已有 8 个单元测试
- VehicleTransit 使用 MyBatis-Plus，数据库字段使用下划线命名（`overallMonitorStatus` → `overall_monitor_status`）
- TransitSummaryDTO 当前结构：normal/warn/overdue/total
- ChartDataDTO 当前结构：brand/transportStatus/normal/warn/overdue
- Dashboard 使用 Vue 3 Composition API（无 Pinia），API 直接通过 `request.js` 调用

### Metis Review
**Identified Gaps** (addressed):
- Q1: WARN 阈值公式 → 累计 OTD 的 80%，可配置 ✅
- Q2: 图表数据形状 → 品牌 × 整体监控状态（无在途状态维度） ✅
- Q3: ARRIVED 包含 → 不包含，与现有分段逻辑一致 ✅
- Q4: API 策略 → 增强现有端点，不新建 ✅
- Guardrail: 不修改 RouteOtdConfig ✅
- Guardrail: 不修改 StatusCalculator.calculateMonitorStatus()（新增方法） ✅

---

## Work Objectives

### Core Objective
为现有的 Ro-Ro 在途监控系统增加累计整段时效预警功能，与现有的分段时效预警并存。

### Concrete Deliverables
- StatusCalculator 新增 `calculateOverallMonitorStatus()` 静态方法
- VehicleTransit 新增 `overallMonitorStatus` 字段
- 数据库 `vehicle_transit` 表新增 `overall_monitor_status` 列
- application.yml 新增 `monitor.overall-warn-ratio: 0.8` 配置项
- 新增 `MonitorConfig.java` 配置类
- TransitSummaryDTO 新增 overallNormal/overallWarn/overallOverdue 字段
- TransitController./summary 返回整体统计
- 新增 `OverallChartDataDTO`（brand, normal, warn, overdue）
- ChartController./brand-status 支持 `type=overall` 参数
- Dashboard.vue 新增标签页切换 + 整段监控视图
- StatusCalculatorTest 新增整体计算测试

### Definition of Done
- [ ] `bun test` (Java: `mvn test`) — 所有测试通过，包括新增的整体计算测试
- [ ] `curl http://localhost:8080/api/transit/summary` 返回新增的整体字段
- [ ] `curl 'http://localhost:8080/api/chart/brand-status?type=overall'` 返回品牌×整体状态数据
- [ ] Dashboard 标签页切换正常工作，整段视图展示正确的汇总和图表数据
- [ ] 现有分段监控功能无回归

### Must Have
- 累计已用时间 = `now - orderReleaseTime`（在途）或 `arriveShopTime - orderReleaseTime`（已到达，但仅内部计算）
- 累计 OTD = 到当前状态为止的各段标准 OTD 之和
- 累计 WARN = 累计 OTD × `overallWarnRatio`（默认 0.8）
- 整段监控状态：已用时间 > 累计 OTD → OVERDUE；> 累计 WARN → WARN；否则 NORMAL
- Dashboard 整段标签页排除 ARRIVED 车辆
- TDD：先写测试，再实现

### Must NOT Have (Guardrails)
- 不修改 `RouteOtdConfig` 实体、表结构或配置字段
- 不修改 `StatusCalculator.calculateMonitorStatus()` 方法（老方法不变）
- 不新增后端 API 端点（只增强现有端点）
- 不新增前端路由或视图文件（只在 Dashboard.vue 内做标签页切换）
- 不包括已到达车辆的整段数据回填脚本（NULL 兼容处理即可）
- 不包括 ETA/剩余时间等预测功能（纯状态检测）

---

## Verification Strategy

> **ZERO HUMAN INTERVENTION** — ALL verification is agent-executed.

### Test Decision
- **Infrastructure exists**: YES (JUnit 5, Maven Surefire)
- **Automated tests**: TDD
- **Framework**: JUnit 5 + Maven

### QA Policy
Every task MUST include agent-executed QA scenarios. Evidence saved to `.omo/evidence/task-{N}-{scenario-slug}.{ext}`.

- **Backend API**: Bash (curl) — Send requests, assert status + response fields
- **Java Logic**: Bash (mvn test) — Run tests, verify pass/fail
- **Frontend**: Playwright (if UI verification needed) or manual review of code structure

---

## Execution Strategy

### Parallel Execution Waves

```
Wave 1 (Start Immediately — foundation, 4 tasks in parallel):
├── Task 1: StatusCalculatorTest — Write failing tests (RED)
├── Task 2: VehicleTransit entity + DB migration SQL
├── Task 3: application.yml + MonitorConfig.java
└── Task 4: OverallChartDataDTO — New DTO class

Wave 2 (After Wave 1 — core implementation, 2 tasks):
├── Task 5: StatusCalculator — Implement calculateOverallMonitorStatus() (GREEN)
└── Task 6: TransitDataServiceImpl — Integrate overall monitor status into Excel import

Wave 3 (After Wave 2 — API layer, 2 tasks):
├── Task 7: TransitSummaryDTO + TransitController — Return overall stats
└── Task 8: ChartController — Support type=overall param, return OverallChartDataDTO list

Wave 4 (After Wave 3 — frontend, 2 tasks):
├── Task 9: Dashboard.vue — Tab switching + overall summary cards + overall chart/table

Wave FINAL (After ALL — 4 parallel reviews, then user okay):
├── Task F1: Plan compliance audit (oracle)
├── Task F2: Code quality review (unspecified-high)
├── Task F3: Real manual QA (unspecified-high)
└── Task F4: Scope fidelity check (deep)
-> Present results -> Get explicit user okay

Critical Path: Task 1 → Task 5 → Task 7/8 → Task 9 → F1-F4 → user okay
Parallel Speedup: ~60% faster than sequential
Max Concurrent: 4 (Wave 1)
```

### Dependency Matrix

- **1**: - | 5, 1
- **2**: - | 6, 1
- **3**: - | 5, 1
- **4**: - | 8, 1
- **5**: 1, 3 | 6, 2
- **6**: 2, 5 | 7, 3
- **7**: 6 | 9, 3
- **8**: 4, 6 | 9, 3
- **9**: 7, 8 | F1-F4, 4
- **F1-F4**: 9 | -, FINAL

### Agent Dispatch Summary

- **Wave 1**: 4 — T1→`deep`, T2→`quick`, T3→`quick`, T4→`quick`
- **Wave 2**: 2 — T5→`deep`, T6→`unspecified-high`
- **Wave 3**: 2 — T7→`unspecified-high`, T8→`unspecified-high`
- **Wave 4**: 1 — T9→`visual-engineering`
- **FINAL**: 4 — F1→`oracle`, F2→`unspecified-high`, F3→`unspecified-high`, F4→`deep`

---

## TODOs

### Wave 1: Foundation (4 tasks in parallel)

- [x] 1. **StatusCalculatorTest — RED: Write failing tests for calculateOverallMonitorStatus**

  **What to do**:
  - Add 7+ test methods to `ro-ro-monitor/src/test/java/com/company/roro/util/StatusCalculatorTest.java`
  - Tests should compile (import new static method) but fail because method doesn't exist yet
  - Follow existing test patterns (use `baseConfig()` for common config setup)
  - Method signature to test: `StatusCalculator.calculateOverallMonitorStatus(transit, otdConfig, orderReleaseTime, now, warnRatio)`

  **Test cases to cover**:
  ```
  1. notDeparted_underWarn → NORMAL
     Config: NOT_DEPARTED OTD=48, WARN=24h
     orderReleaseTime = now - 20h (elapsed=20)
     Cumulative OTD = 48, Cumulative WARN = 48 * 0.8 = 38.4
     20 < 38.4 → NORMAL

  2. notDeparted_pastWarn_underOtd → WARN
     orderReleaseTime = now - 30h (elapsed=30)
     30 < 48 (OTD) but 30 > 38.4 (WARN)? No, 30 < 38.4... need bigger
     Actually let me recalculate: warnRatio=0.8, cumulative OTD=48, WARN threshold = 48*0.8 = 38.4
     elapsed=30: 30 < 38.4 → NORMAL. Need elapsed between 38.4 and 48.
     elapsed=40: 40 > 38.4 and 40 < 48 → WARN ✓

  3. notDeparted_pastOtd → OVERDUE
     elapsed=50: 50 > 48 → OVERDUE ✓

  4. toPort_cumulativeWarn → WARN
     Cumulative OTD = 48+12=60, WARN = 60*0.8=48
     elapsed=50: 50 > 48 and 50 < 60 → WARN ✓

  5. onSea_cumulativeOtd → OVERDUE
     Cumulative OTD = 48+12+48+48=156, WARN = 156*0.8=124.8
     elapsed=160: 160 > 156 → OVERDUE ✓

  6. nullOtdConfig → NORMAL
     When otdConfig is null → return NORMAL

  7. nullOrderReleaseTime → NORMAL
     When orderReleaseTime is null → return NORMAL (graceful fallback)
  ```

  **Must NOT do**:
  - Don't implement the method yet (RED phase only)
  - Don't modify existing test methods

  **Recommended Agent Profile**:
  - **Category**: `deep` — Logic-heavy with precise expected values
  - **Skills**: None needed (pure JUnit testing)

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 1 (with Tasks 2, 3, 4)
  - **Blocks**: Task 5 (implementation)
  - **Blocked By**: None (can start immediately)

  **References**:
  - `ro-ro-monitor/src/test/java/com/company/roro/util/StatusCalculatorTest.java` (entire file) — Follow exact test patterns: `baseConfig()` helper, `now` fixed time, `assertEquals` assertions
  - `ro-ro-monitor/src/main/java/com/company/roro/util/StatusCalculator.java:65-111` — Existing `calculateMonitorStatus` as reference for the new method's behavior pattern
  - `ro-ro-monitor/src/main/java/com/company/roro/entity/RouteOtdConfig.java:23-44` — 7 segment OTD fields to understand cumulative sums

  **Acceptance Criteria**:

  **QA Scenarios (MANDATORY)**:
  ```
  Scenario: TDD RED phase — tests compile but fail
    Tool: Bash
    Preconditions: New test methods written, calculateOverallMonitorStatus() does NOT exist yet
    Steps:
      1. Run `mvn compile -pl ro-ro-monitor`
      2. Assert compilation fails (method not found)
    Expected Result: Compilation error for undefined calculateOverallMonitorStatus
    Evidence: .omo/evidence/task-1-compile-fail.log
  ```

  **Evidence to Capture**:
  - [ ] Test file with new methods
  - [ ] Compilation fails as expected

  **Commit**: NO (groups with Task 5)
  - Message: `test(monitor): add calculateOverallMonitorStatus tests and implementation`
  - Files: `ro-ro-monitor/src/test/java/com/company/roro/util/StatusCalculatorTest.java`
  - Pre-commit: `mvn test`

- [x] 2. **VehicleTransit entity + DB migration — Add overallMonitorStatus field**

  **What to do**:
  - Add `overallMonitorStatus` field to `VehicleTransit.java` (camelCase → `overall_monitor_status` in DB via MyBatis-Plus)
  - Create `ro-ro-monitor/src/main/resources/sql/migration-v2-overall-monitor.sql` with ALTER TABLE
  - Field type: `VARCHAR(20)`, nullable (NULL = not yet computed or NORMAL by convention)
  - Default NULL — existing records get NULL, code treats NULL as NORMAL in aggregations
  - Add Javadoc: "整段监控状态：NORMAL / WARN / OVERDUE"

  **Must NOT do**:
  - Don't modify any other entity
  - Don't run the SQL (just create the script — DB changes should be reviewed)
  - Don't add any bean validation annotations

  **Recommended Agent Profile**:
  - **Category**: `quick` — Simple field addition + SQL file creation

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 1 (with Tasks 1, 3, 4)
  - **Blocks**: Task 6 (integration)
  - **Blocked By**: None

  **References**:
  - `ro-ro-monitor/src/main/java/com/company/roro/entity/VehicleTransit.java:52-53` — Existing `monitorStatus` field pattern (camelCase String, Javadoc)
  - `ro-ro-monitor/src/main/java/com/company/roro/entity/VehicleTransit.java:14` — `@TableName("vehicle_transit")` → DB table name
  - MyBatis-Plus `map-underscore-to-camel-case: true` in `application.yml:19` — Ensures `overallMonitorStatus` maps to `overall_monitor_status`

  **Acceptance Criteria**:

  **QA Scenarios (MANDATORY)**:
  ```
  Scenario: Verify entity field exists and compiles
    Tool: Bash
    Preconditions: Entity file modified
    Steps:
      1. Run `mvn compile -pl ro-ro-monitor`
      2. Assert compilation succeeds
    Expected Result: Build successful
    Evidence: .omo/evidence/task-2-compile.log

  Scenario: Verify SQL file is valid syntax
    Tool: Bash
    Preconditions: SQL file created
    Steps:
      1. Run `cat ro-ro-monitor/src/main/resources/sql/migration-v2-overall-monitor.sql`
      2. Assert SQL contains ALTER TABLE vehicle_transit ADD COLUMN overall_monitor_status
    Expected Result: SQL file exists with correct ALTER TABLE syntax
    Evidence: .omo/evidence/task-2-sql-content.log
  ```

  **Evidence to Capture**:
  - [ ] VehicleTransit.java with new field
  - [ ] SQL migration file
  - [ ] Compilation success

  **Commit**: NO (groups with Task 6)

- [x] 3. **application.yml + MonitorConfig — Add overall-warn-ratio configuration**

  **What to do**:
  - Add to `application.yml`:
    ```yaml
    monitor:
      overall-warn-ratio: 0.8
    ```
  - Create `ro-ro-monitor/src/main/java/com/company/roro/config/MonitorConfig.java`:
    - `@ConfigurationProperties(prefix = "monitor")`
    - Field: `Double overallWarnRatio` with getter/setter
    - Default value: 0.8 (fallback if not configured)
  - Register `@EnableConfigurationProperties(MonitorConfig.class)` in `RoroMonitorApplication.java` or add `@ConfigurationPropertiesScan` — check existing pattern
  - Follow existing config class pattern (see ThreadPoolConfig)

  **Must NOT do**:
  - Don't modify any existing config classes
  - Don't add unused properties

  **Recommended Agent Profile**:
  - **Category**: `quick` — Standard Spring Boot config class

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 1 (with Tasks 1, 2, 4)
  - **Blocks**: Task 5 (implementation needs warnRatio)
  - **Blocked By**: None

  **References**:
  - `ro-ro-monitor/src/main/java/com/company/roro/config/ThreadPoolConfig.java` — Existing config class pattern (no `@ConfigurationProperties` used yet, so this is the first one)
  - `ro-ro-monitor/src/main/resources/application.yml` — Existing config structure
  - Spring Boot docs: `@ConfigurationProperties` with `prefix` binding

  **Acceptance Criteria**:

  **QA Scenarios (MANDATORY)**:
  ```
  Scenario: Config class compiles and loads
    Tool: Bash
    Preconditions: MonitorConfig.java created, application.yml updated
    Steps:
      1. Run `mvn compile -pl ro-ro-monitor`
      2. Assert compilation succeeds
    Expected Result: Build successful
    Evidence: .omo/evidence/task-3-compile.log
  ```

  **Evidence to Capture**:
  - [ ] MonitorConfig.java file
  - [ ] Updated application.yml
  - [ ] Compilation success

  **Commit**: NO (groups with Task 6)

- [x] 4. **OverallChartDataDTO — New DTO for overall chart data**

  **What to do**:
  - Create `ro-ro-monitor/src/main/java/com/company/roro/dto/OverallChartDataDTO.java`
  - Fields: `String brand`, `Long normal`, `Long warn`, `Long overdue`
  - Javadoc: "整段监控图表数据 DTO — 品牌维度的整体监控状态统计"
  - Use `@Data` (Lombok) — follow existing DTO pattern

  **Must NOT do**:
  - Don't modify existing ChartDataDTO
  - Don't add transportStatus field (overall view has no transportStatus dimension)

  **Recommended Agent Profile**:
  - **Category**: `quick` — Simple POJO with Lombok

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 1 (with Tasks 1, 2, 3)
  - **Blocks**: Task 8 (chart endpoint needs this DTO)
  - **Blocked By**: None

  **References**:
  - `ro-ro-monitor/src/main/java/com/company/roro/dto/ChartDataDTO.java` — Existing chart DTO pattern (Lombok `@Data`, fields, Javadoc)
  - `ro-ro-monitor/src/main/java/com/company/roro/dto/TransitSummaryDTO.java` — Similar pattern

  **Acceptance Criteria**:

  **QA Scenarios (MANDATORY)**:
  ```
  Scenario: DTO compiles
    Tool: Bash
    Preconditions: OverallChartDataDTO.java created
    Steps:
      1. Run `mvn compile -pl ro-ro-monitor`
      2. Assert compilation succeeds
    Expected Result: Build successful
    Evidence: .omo/evidence/task-4-compile.log
  ```

  **Evidence to Capture**:
  - [ ] OverallChartDataDTO.java
  - [ ] Compilation success

  **Commit**: NO (groups with Task 8)

### Wave 2: Core Logic

- [x] 5. **StatusCalculator — Implement calculateOverallMonitorStatus() (GREEN)**

  **What to do**:
  - Add `calculateOverallMonitorStatus()` static method to `StatusCalculator.java`
  - Signature:
    ```java
    public static String calculateOverallMonitorStatus(VehicleTransit transit, RouteOtdConfig otdConfig,
                                                        LocalDateTime orderReleaseTime, LocalDateTime now, double warnRatio)
    ```
  - Logic:
    1. If otdConfig == null → return "NORMAL"
    2. If orderReleaseTime == null → return "NORMAL"
    3. Get transport status from transit
    4. If "ARRIVED": endTime = transit.getArriveShopTime(), compute cumulative OTD = sum of ALL 7 segments
    5. Else: endTime = now, cumulative OTD = sum of segment OTDs up to current status
    6. If endTime == null → return "NORMAL" (graceful)
    7. elapsedHours = Duration.between(orderReleaseTime, endTime).toHours()
    8. cumulativeWarn = (long) Math.floor(cumulativeOtd * warnRatio)
    9. if elapsedHours > cumulativeOtd → return "OVERDUE"
    10. else if elapsedHours > cumulativeWarn → return "WARN"
    11. else → return "NORMAL"

  - Helper method: `getCumulativeOtd(RouteOtdConfig config, String status)` — sums OTDs up to the given status
  - Make Task 1's tests pass (GREEN)
  - REFACTOR: Ensure code is clean, no duplication with existing methods

  **Cumulative OTD calculation by status**:
  ```
  NOT_DEPARTED:            notDepartedOtd
  TO_PORT:                 notDepartedOtd + toPortOtd
  AT_PORT_WAIT_SHIP:       notDepartedOtd + toPortOtd + atPortWaitOtd
  ON_SEA:                  + onSeaOtd
  AT_DEST_WAIT_UNLOAD:     + atDestWaitOtd
  UNLOADED_WAIT_DISPATCH:  + unloadWaitDispatchOtd
  DISPATCHING:             + dispatchingOtd
  ARRIVED:                 sum of all 7
  ```

  **Must NOT do**:
  - Don't modify `calculateMonitorStatus()` (existing method)
  - Don't modify `getOtdHours()`, `getWarnHours()`, or `getStartTime()` (existing helpers)
  - Don't add any Spring dependencies to StatusCalculator (keep it a pure utility class)

  **Recommended Agent Profile**:
  - **Category**: `deep` — Logic-heavy core algorithm

  **Parallelization**:
  - **Can Run In Parallel**: NO
  - **Parallel Group**: Sequential (depends on Task 1, 3)
  - **Blocks**: Task 6
  - **Blocked By**: Task 1 (test RED must compile), Task 3 (warnRatio config)

  **References**:
  - `ro-ro-monitor/src/main/java/com/company/roro/util/StatusCalculator.java:77-111` — Existing `calculateMonitorStatus` as algorithmic reference
  - `ro-ro-monitor/src/main/java/com/company/roro/util/StatusCalculator.java:140-158` — `getOtdHours()` as reference for the switch-case pattern
  - `ro-ro-monitor/src/test/java/com/company/roro/util/StatusCalculatorTest.java` (after Task 1) — Tests to make pass
  - `ro-ro-monitor/src/main/java/com/company/roro/entity/RouteOtdConfig.java:23-44` — 7 segment OTD field names

  **Acceptance Criteria**:

  **QA Scenarios (MANDATORY)**:
  ```
  Scenario: All tests pass (GREEN)
    Tool: Bash
    Preconditions: StatusCalculator updated, tests from Task 1 exist
    Steps:
      1. Run `mvn test -pl ro-ro-monitor`
      2. Assert all tests pass, including the 7+ new ones
    Expected Result: BUILD SUCCESS — all tests GREEN
    Evidence: .omo/evidence/task-5-tests-pass.log

  Scenario: Existing tests still pass (no regression)
    Tool: Bash
    Preconditions: Same as above
    Steps:
      1. Run `mvn test -pl ro-ro-monitor -Dtest=StatusCalculatorTest`
      2. Assert original 8 tests + new tests all pass
    Expected Result: All 15+ tests pass
    Evidence: .omo/evidence/task-5-regression-check.log
  ```

  **Evidence to Capture**:
  - [ ] Updated StatusCalculator.java with new method
  - [ ] Test results (all GREEN)
  - [ ] No regression on existing tests

  **Commit**: YES (groups with Task 1)
  - Message: `test(monitor): add calculateOverallMonitorStatus tests and implementation`
  - Files: `ro-ro-monitor/src/main/java/com/company/roro/util/StatusCalculator.java`, `ro-ro-monitor/src/test/java/com/company/roro/util/StatusCalculatorTest.java`
  - Pre-commit: `mvn test`

- [x] 6. **TransitDataServiceImpl — Integrate overall monitor status into Excel import**

  **What to do**:
  - Inject `MonitorConfig` into `TransitDataServiceImpl` (add constructor param or `@Autowired`)
  - After step 7 (existing monitorStatus calculation), add step 7b:
    ```java
    // 7b. 计算整段监控状态
    String overallMonitorStatus = StatusCalculator.calculateOverallMonitorStatus(
        transit,
        otdConfig,
        order.getOrderReleaseTime(),
        LocalDateTime.now(),
        monitorConfig.getOverallWarnRatio()
    );
    transit.setOverallMonitorStatus(overallMonitorStatus);
    ```
  - Verify that `VehicleTransit.overallMonitorStatus` is set before upsert

  **Must NOT do**:
  - Don't modify the existing monitorStatus calculation
  - Don't modify the upsert method
  - Don't add any new dependencies to the service

  **Recommended Agent Profile**:
  - **Category**: `unspecified-high` — Integration logic with multiple dependencies

  **Parallelization**:
  - **Can Run In Parallel**: NO
  - **Parallel Group**: Sequential (depends on Task 2, 5)
  - **Blocks**: Task 7, 8
  - **Blocked By**: Task 2 (entity field), Task 5 (calculator method)

  **References**:
  - `ro-ro-monitor/src/main/java/com/company/roro/service/impl/TransitDataServiceImpl.java:70-83` — Steps 5-7 where transport/monitor status is calculated (new step goes here)
  - `ro-ro-monitor/src/main/java/com/company/roro/service/impl/TransitDataServiceImpl.java:21-27` — Constructor injection pattern (uses `@RequiredArgsConstructor`)
  - `ro-ro-monitor/src/main/java/com/company/roro/config/MonitorConfig.java` (from Task 3) — Config class to inject
  - `ro-ro-monitor/src/main/java/com/company/roro/util/StatusCalculator.java` (after Task 5) — New method signature

  **Acceptance Criteria**:

  **QA Scenarios (MANDATORY)**:
  ```
  Scenario: Compilation with new integration code
    Tool: Bash
    Preconditions: TransitDataServiceImpl updated
    Steps:
      1. Run `mvn compile -pl ro-ro-monitor`
      2. Assert compilation succeeds
    Expected Result: Build successful
    Evidence: .omo/evidence/task-6-compile.log

  Scenario: All tests still pass
    Tool: Bash
    Preconditions: Integration complete
    Steps:
      1. Run `mvn test -pl ro-ro-monitor`
      2. Assert all tests pass
    Expected Result: BUILD SUCCESS
    Evidence: .omo/evidence/task-6-tests.log
  ```

  **Evidence to Capture**:
  - [ ] Updated TransitDataServiceImpl.java
  - [ ] Compilation success
  - [ ] All tests pass

  **Commit**: YES (groups with Task 2, 3)
  - Message: `feat(monitor): add overallMonitorStatus field, config, and calculation integration`
  - Files: all Wave 1 + Wave 2 committed files
  - Pre-commit: `mvn test`

### Wave 3: API Layer

- [x] 7. **TransitSummaryDTO + TransitController — Return overall stats in /summary**

  **What to do**:
  - Add 3 fields to `TransitSummaryDTO`:
    ```java
    /** 整段监控正常数量 */
    private Long overallNormal;
    /** 整段监控预警数量 */
    private Long overallWarn;
    /** 整段监控超期数量 */
    private Long overallOverdue;
    ```
  - Update `TransitController./summary`:
    - After computing existing monitorStatus counts, compute overall counts from the same list
    - Since overallMonitorStatus may be NULL on old records, treat NULL as NORMAL
    - Exclude ARRIVED vehicles (same filter as per-segment)
    ```java
    long overallNormal = list.stream()
        .filter(v -> v.getOverallMonitorStatus() == null || "NORMAL".equals(v.getOverallMonitorStatus()))
        .count();
    long overallWarn = list.stream()
        .filter(v -> "WARN".equals(v.getOverallMonitorStatus()))
        .count();
    long overallOverdue = list.stream()
        .filter(v -> "OVERDUE".equals(v.getOverallMonitorStatus()))
        .count();
    dto.setOverallNormal(overallNormal);
    dto.setOverallWarn(overallWarn);
    dto.setOverallOverdue(overallOverdue);
    ```

  **Must NOT do**:
  - Don't change existing DTO field names or types (backward compatible)
  - Don't add a separate endpoint

  **Recommended Agent Profile**:
  - **Category**: `unspecified-high` — DTO + Controller modification

  **Parallelization**:
  - **Can Run In Parallel**: YES (with Task 8 — different files)
  - **Parallel Group**: Wave 3 (with Task 8)
  - **Blocks**: Task 9
  - **Blocked By**: Task 6 (entity must have overallMonitorStatus)

  **References**:
  - `ro-ro-monitor/src/main/java/com/company/roro/dto/TransitSummaryDTO.java` (entire file) — Existing DTO structure
  - `ro-ro-monitor/src/main/java/com/company/roro/controller/TransitController.java:118-131` — Existing count logic to extend

  **Acceptance Criteria**:

  **QA Scenarios (MANDATORY)**:
  ```
  Scenario: /summary returns overall fields
    Tool: Bash (curl)
    Preconditions: App running locally with DB data
    Steps:
      1. curl -s http://localhost:8080/api/transit/summary
      2. Assert JSON response contains: overallNormal, overallWarn, overallOverdue
      3. Assert existing fields (normal, warn, overdue, total) are unchanged
    Expected Result: Response includes both old and new fields
    Evidence: .omo/evidence/task-7-summary-response.json
  ```

  **Evidence to Capture**:
  - [ ] Updated TransitSummaryDTO.java + TransitController.java
  - [ ] /summary response with overall fields
  - [ ] Compilation + tests pass

  **Commit**: NO (groups with Task 8)

- [x] 8. **ChartController — Support type=overall param for overall brand-status chart**

  **What to do**:
  - Add `type` query parameter to `/api/chart/brand-status` endpoint:
    - `type=overall` → return `List<OverallChartDataDTO>` instead of `List<ChartDataDTO>`
    - default (or any other value) → return existing `List<ChartDataDTO>` (backward compatible)
  - For `type=overall`:
    - Query same data (non-ARRIVED vehicles)
    - Group by brand → count overallMonitorStatus (NULL → NORMAL)
    - Return `OverallChartDataDTO` list: brand + normal/warn/overdue counts
    - Sort by brand name
  - Update Swagger annotation to document the new parameter

  **Must NOT do**:
  - Don't change the existing return type or response format
  - Don't create a new endpoint
  - Don't modify ChartDataDTO

  **Recommended Agent Profile**:
  - **Category**: `unspecified-high` — Controller logic with grouping

  **Parallelization**:
  - **Can Run In Parallel**: YES (with Task 7 — different Controller)
  - **Parallel Group**: Wave 3 (with Task 7)
  - **Blocks**: Task 9
  - **Blocked By**: Task 4 (DTO), Task 6 (entity)

  **References**:
  - `ro-ro-monitor/src/main/java/com/company/roro/controller/ChartController.java:46-142` — Existing `/brand-status` endpoint (follow grouping/sorting pattern)
  - `ro-ro-monitor/src/main/java/com/company/roro/dto/OverallChartDataDTO.java` (from Task 4) — Return type for type=overall
  - `ro-ro-monitor/src/main/java/com/company/roro/dto/ChartDataDTO.java` — Existing DTO (for reference, not modification)

  **Acceptance Criteria**:

  **QA Scenarios (MANDATORY)**:
  ```
  Scenario: /chart/brand-status?type=overall returns correct structure
    Tool: Bash (curl)
    Preconditions: App running with data
    Steps:
      1. curl -s 'http://localhost:8080/api/chart/brand-status?type=overall'
      2. Assert response is JSON array
      3. Assert each item has: brand, normal, warn, overdue
      4. Assert no item has 'transportStatus' field
    Expected Result: List of {brand, normal, warn, overdue} objects
    Evidence: .omo/evidence/task-8-overall-chart.json

  Scenario: /chart/brand-status (no type param) still returns original format
    Tool: Bash (curl)
    Preconditions: Same
    Steps:
      1. curl -s 'http://localhost:8080/api/chart/brand-status'
      2. Assert response contains transportStatus field (original format)
    Expected Result: Original ChartDataDTO format — no regression
    Evidence: .omo/evidence/task-8-regression-chart.json

  Scenario: All tests pass
    Tool: Bash
    Steps:
      1. Run `mvn test -pl ro-ro-monitor`
    Expected Result: BUILD SUCCESS
    Evidence: .omo/evidence/task-8-tests.log
  ```

  **Evidence to Capture**:
  - [ ] Updated ChartController.java
  - [ ] Both API response samples
  - [ ] Tests pass

  **Commit**: YES (groups with Task 4, 7)
  - Message: `feat(api): enhance summary and chart endpoints for overall monitor data`
  - Files: TransitSummaryDTO.java, TransitController.java, ChartController.java, OverallChartDataDTO.java
  - Pre-commit: `mvn test`

### Wave 4: Frontend

- [x] 9. **Dashboard.vue — Add tab switching between per-segment and overall monitoring views**

  **What to do**:
  - Add `el-tabs` component at the top of Dashboard.vue (above the time filter):
    ```html
    <el-tabs v-model="activeMonitorTab" @tab-change="handleTabChange">
      <el-tab-pane label="分段监控" name="segment" />
      <el-tab-pane label="整段监控" name="overall" />
    </el-tabs>
    ```
  - Add reactive state: `const activeMonitorTab = ref('segment')`
  - Modify `loadData()` to call appropriate endpoints based on active tab:
    - `segment` tab: existing `/transit/summary` + `/chart/brand-status` (unchanged)
    - `overall` tab: same `/transit/summary` (but display overallNormal/Warn/Overdue from response) + `/chart/brand-status?type=overall`
  - For overall tab:
    - Summary cards show: overallNormal / overallWarn / overallOverdue / total
    - Replace the `StackedBarChart` + `StatusPieChart` layout with a simpler display
    - Show an `el-table` with columns: 品牌, 正常, 预警, 超期, 合计
    - Add a simple ECharts bar chart showing each brand's NORMAL/WARN/OVERDUE distribution
  - Keep existing per-segment layout unchanged (just hide it when overall tab is active)
  - Use `v-if="activeMonitorTab === 'segment'"` and `v-if="activeMonitorTab === 'overall'"` for clean layout separation
  - Handle `undefined` overall fields (backend may return null for old records)

  **Must NOT do**:
  - Don't create a new Vue view file (everything stays in Dashboard.vue)
  - Don't modify existing per-segment layout
  - Don't add new npm dependencies (use existing Element Plus + ECharts)
  - Don't modify API wrapper files or create new ones
  - Don't use Pinia stores (follow existing pattern with local ref/reactive)

  **Recommended Agent Profile**:
  - **Category**: `visual-engineering` — UI layout + component composition + API integration
  - **Skills**:
    - `playwright`: For QA verification of tab switching

  **Parallelization**:
  - **Can Run In Parallel**: NO
  - **Parallel Group**: Sequential (depends on Task 7, 8)
  - **Blocks**: Nothing (last implementation task)
  - **Blocked By**: Task 7 (summary API), Task 8 (chart API)

  **References**:
  - `ro-ro-monitor-web/src/views/Dashboard.vue` (entire file) — Existing dashboard layout patterns
  - `ro-ro-monitor-web/src/components/StackedBarChart.vue` — Existing ECharts component for reference
  - `ro-ro-monitor-web/src/api/request.js` — Existing API call pattern
  - Element Plus docs: `el-tabs`, `el-table` components

  **Acceptance Criteria**:

  **QA Scenarios (MANDATORY)**:
  ```
  Scenario: Tab switching between segment and overall views
    Tool: Playwright (browser)
    Preconditions: App running, Dashboard open
    Steps:
      1. Click "整段监控" tab
      2. Assert summary cards change to show overall data
      3. Assert chart area updates (no in-transport-status breakdown)
      4. Click "分段监控" tab
      5. Assert original layout is restored
    Expected Result: Smooth tab switch, correct data per tab
    Evidence: .omo/evidence/task-9-tab-switch.mp4

  Scenario: Overall summary cards display correct data
    Tool: Playwright (browser)
    Preconditions: App running with data
    Steps:
      1. Click "整段监控" tab
      2. Assert 4 summary cards visible: normal/预警/超期/总数
      3. Assert card values are valid numbers
    Expected Result: Summary cards show valid overall data
    Evidence: .omo/evidence/task-9-overall-summary.png

  Scenario: Overall brand status table has correct columns
    Tool: Playwright (browser)
    Preconditions: Overall tab active
    Steps:
      1. Assert an el-table is visible
      2. Assert columns: 品牌, 正常, 预警, 超期, 合计
      3. Assert each row has valid brand name and numbers
    Expected Result: Table shows brand-level overall monitor stats
    Evidence: .omo/evidence/task-9-overall-table.png
  ```

  **Evidence to Capture**:
  - [ ] Updated Dashboard.vue with tab switching
  - [ ] Tab switch video
  - [ ] Overall summary and table screenshots
  - [ ] No regression on per-segment tab

  **Commit**: YES (single commit)
  - Message: `feat(ui): add overall monitor dashboard tab with summary and chart`
  - Files: `ro-ro-monitor-web/src/views/Dashboard.vue`
  - Pre-commit: Verify tab switching works

---

## Final Verification Wave

> 4 review agents run in PARALLEL. ALL must APPROVE. Present consolidated results to user and get explicit "okay" before completing.

- [x] F1. **Plan Compliance Audit** — `oracle`
  Read the plan end-to-end. For each "Must Have": verify implementation exists (read file, curl endpoint, run command). For each "Must NOT Have": search codebase for forbidden patterns — reject with file:line if found. Check evidence files exist in .omo/evidence/. Compare deliverables against plan.
  Output: `Must Have [N/N] | Must NOT Have [N/N] | Tasks [N/N] | VERDICT: APPROVE/REJECT`

- [x] F2. **Code Quality Review** — `unspecified-high`
  Run `mvn compile` + `mvn test`. Review all changed files for: null-safety issues with overallMonitorStatus, incorrect cumulative sum logic, edge-case handling (null times), unused imports, commented-out code. Check for per-segment regression: verify `calculateMonitorStatus()` unchanged.
  Output: `Build [PASS/FAIL] | Lint/Pmd [PASS/FAIL] | Tests [N pass/N fail] | Files [N clean/N issues] | VERDICT`

- [x] F3. **Real Manual QA** — `unspecified-high` (+ `playwright` skill if UI verification needed)
  Start from a clean state (checkout + build + DB with sample data). Execute EVERY QA scenario from EVERY task — follow exact steps, capture evidence. Test cross-task integration: Excel upload → overallMonitorStatus gets stored → /summary returns overall field → Dashboard shows correct chart. Test edge cases: null overallMonitorStatus on old records, empty brand list.
  Output: `Scenarios [N/N pass] | Integration [N/N] | Edge Cases [N tested] | VERDICT`

- [x] F4. **Scope Fidelity Check** — `deep`
  For each task: read "What to do", read actual diff (git log/diff). Verify 1:1 — everything in spec was built (no missing), nothing beyond spec was built (no creep). Check "Must NOT do" compliance, especially: RouteOtdConfig unchanged, calculateMonitorStatus() unchanged, no new API endpoints. Detect cross-task contamination.
  Output: `Tasks [N/N compliant] | Contamination [CLEAN/N issues] | Unaccounted [CLEAN/N files] | VERDICT`

---

## Commit Strategy

- **1, 5**: `test(monitor): add calculateOverallMonitorStatus tests and implementation`
  - Files: StatusCalculatorTest.java, StatusCalculator.java
- **2, 3, 6**: `feat(monitor): add overallMonitorStatus field, config, and integration`
  - Files: VehicleTransit.java, sql/migration-v2-overall-monitor.sql, application.yml, MonitorConfig.java, TransitDataServiceImpl.java
- **7, 8**: `feat(api): enhance summary and chart endpoints for overall monitor data`
  - Files: TransitSummaryDTO.java, TransitController.java, ChartController.java, OverallChartDataDTO.java
- **9**: `feat(ui): add overall monitor dashboard tab with summary and chart`
  - Files: Dashboard.vue

---

## Success Criteria

### Verification Commands
```bash
mvn test -pl ro-ro-monitor  # Expected: All tests pass (incl. new overall tests)
curl http://localhost:8080/api/transit/summary  # Expected: Response includes overallNormal/Warn/Overdue
curl 'http://localhost:8080/api/chart/brand-status?type=overall'  # Expected: [{brand, normal, warn, overdue}, ...]
curl http://localhost:8080/api/chart/brand-status  # Expected: Original format unchanged (no regression)
```

### Final Checklist
- [x] All "Must Have" implemented
- [x] All "Must NOT Have" absent
- [x] All tests pass
- [x] No regression in per-segment monitoring
