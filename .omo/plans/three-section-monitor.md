# 三段监控维度开发计划

## TL;DR

> **核心目标**：在现有分段/整段监控之间，新增"三段监控"维度（前段/中段/后段），复用整段监控的实现模式（入库时计算 + 数据库存储 + API 查询）。
>
> **交付物**：
> - 后端：StatusCalculator.calculateSectionMonitorStatus()、VehicleTransit.sectionMonitorStatus、ChartController type=three-section 支持
> - 前端：Dashboard.vue 新增"三段监控" tab（summary cards + 段级图表 + 品牌钻取）
> - 数据库：vehicle_transit 表新增 section_monitor_status 列
> - 测试：StatusCalculatorTest 单元测试、API 集成测试、Playwright 前端验证
>
> **预估工作量**：中等（Medium）
> **并行执行**：是，分 3 个 wave + 1 个 final wave
> **关键路径**：DB 迁移 → StatusCalculator → VehicleTransit/Service → ChartController → Dashboard.vue → Playwright 验证

---

## Context

### 原始需求
用户需要在现有的分段（8 个 transport status）和整段（overallMonitorStatus）监控之间，新增一个"三段监控"维度：
- 前段：未发运、去港途中、在港等船
- 中段：水运在途、到港等卸
- 后段：卸完等发运、发运中
- 已到店：不纳入统计

### 访谈摘要
**关键决策**：
- 三段 OTD 计算逻辑 = 段内累加到当前状态为止的所有分段 OTD 之和（和整段逻辑一致，但段内相对）
- 预警阈值 = 段 OTD × warnRatio（默认 80%，复用 overallWarnRatio）
- 前端图表：主视图按段聚合（前段/中段/后段），支持钻取到品牌级别
- Summary cards：3 张聚合卡片（normal / warn / overdue）
- 实现模式：完全复用 overallMonitorStatus 的实现模式（数据库字段 + 入库时计算 + API 查询）

### Metis 审查
**识别的缺口**（已处理）：
- section-relative cumulative OTD 的计算方式（从各段起始状态开始累加，不是从 NOT_DEPARTED）
- 前端图表分组方式和 summary cards 布局（已和用户确认）
- NULL 处理策略（所有 null 输入返回 NORMAL，复用现有防御模式）
- 单个 sectionMonitorStatus 列，只存储车辆当前所处段的状态

---

## Work Objectives

### Core Objective
新增"三段监控"维度，将 8 个 transport status 聚合为 3 个业务段（前段/中段/后段），提供和分段/整段监控类似的图表和统计能力。

### Concrete Deliverables
- `migration-v3-section-monitor.sql`：数据库迁移脚本
- `StatusCalculator.java`：新增 `calculateSectionMonitorStatus()` 方法
- `VehicleTransit.java`：新增 `sectionMonitorStatus` 字段
- `TransitDataServiceImpl.java`：入库时计算 sectionMonitorStatus
- `SectionChartDataDTO.java`：三段图表数据 DTO
- `TransitSummaryDTO.java`：扩展三段统计字段
- `ChartController.java`：新增 `type=three-section` 处理
- `Dashboard.vue`：新增三段监控 tab
- `StatusCalculatorTest.java`：三段监控单元测试

### Definition of Done
- [ ] 数据库迁移执行成功，section_monitor_status 列存在
- [ ] 新数据导入后，sectionMonitorStatus 字段被正确填充
- [ ] API `/chart/brand-status?type=three-section` 返回正确的段级聚合数据
- [ ] Dashboard.vue 三段监控 tab 正常渲染，图表和 summary cards 数据正确
- [ ] 所有单元测试和集成测试通过
- [ ] Playwright 验证截图显示三段监控 tab 正常工作

### Must Have
- 三段划分规则固定（前段/中段/后段的具体 transport status 映射）
- section-relative cumulative OTD 计算逻辑正确
- 入库时计算 sectionMonitorStatus（和 overallMonitorStatus 同时）
- 前端三段监控 tab 展示段级聚合图表 + 品牌钻取
- 3 张 summary cards（normal/warn/overdue）
- 已到店车辆不纳入三段统计

### Must NOT Have (Guardrails)
- 不修改现有分段/整段监控逻辑
- 不添加动态重计算/定时任务
- 不创建新数据库表
- 不添加新的 OTD 配置字段
- 不添加数据库索引（本迭代）
- 不重构现有代码（不重命名、不提取工具类）
- 不添加前端动画或新图表组件类型
- 已到店车辆监控模块（未来独立开发）

---

## Verification Strategy

### Test Decision
- **Infrastructure exists**：YES（JUnit 5 + Playwright）
- **Automated tests**：Tests-after（先实现，后补测试）
- **Framework**：JUnit 5（后端）、Playwright（前端）
- **Agent-Executed QA**：所有任务都包含 Agent-Executed QA Scenarios

### QA Policy
每任务必须包含 agent-executed QA scenarios（见 TODO 模板）。证据保存到 `.omo/evidence/task-{N}-{scenario-slug}.{ext}`。

- **后端**：Bash（curl）发送请求，断言状态码和响应字段
- **前端**：Playwright 打开浏览器，导航，断言 DOM，截图
- **数据库**：Bash（mysql）执行 SQL，断言列存在

---

## Execution Strategy

### Parallel Execution Waves

```
Wave 1 (基础 + 后端核心 - 可立即开始):
├── 1. 数据库迁移：新增 section_monitor_status 列
├── 2. StatusCalculator：新增 calculateSectionMonitorStatus() 方法
├── 3. VehicleTransit：新增 sectionMonitorStatus 字段
├── 4. DTO 扩展：SectionChartDataDTO + TransitSummaryDTO 扩展
└── 5. 单元测试：StatusCalculatorTest 三段监控测试

Wave 2 (后端 API + 前端 - 依赖 Wave 1):
├── 6. TransitDataServiceImpl：入库时计算 sectionMonitorStatus
├── 7. ChartController：新增 type=three-section 处理
├── 8. Dashboard.vue：新增三段监控 tab 基础结构 + summary cards
└── 9. Dashboard.vue：段级图表 + 品牌钻取功能

Wave 3 (集成测试 + 验证 - 依赖 Wave 2):
├── 10. API 集成测试：curl 验证三段监控 API
├── 11. Playwright 前端验证：三段监控 tab 截图
└── 12. 数据验证：导入测试数据，验证 sectionMonitorStatus 计算正确

Wave FINAL (4 并行审查):
├── F1. Plan compliance audit (oracle)
├── F2. Code quality review (unspecified-high)
├── F3. Real manual QA (unspecified-high + playwright)
└── F4. Scope fidelity check (deep)
-> 呈现结果 -> 获得用户明确 okay

Critical Path: Task 1 → Task 2 → Task 3 → Task 6 → Task 7 → Task 8 → Task 9 → Task 11 → F1-F4 → user okay
Parallel Speedup: ~60% faster than sequential
Max Concurrent: 5 (Wave 1)
```

### Dependency Matrix

| Task | Blocks | Blocked By |
|---|---|---|
| 1 (DB 迁移) | 3, 6 | None |
| 2 (StatusCalculator) | 5, 6 | None |
| 3 (VehicleTransit) | 6 | 1 |
| 4 (DTO 扩展) | 7, 8 | None |
| 5 (单元测试) | None | 2 |
| 6 (ServiceImpl) | 12 | 1, 2, 3 |
| 7 (ChartController) | 10 | 4 |
| 8 (Dashboard 基础) | 9 | 4 |
| 9 (Dashboard 图表) | 11 | 7, 8 |
| 10 (API 测试) | None | 7 |
| 11 (Playwright) | None | 9 |
| 12 (数据验证) | None | 6 |

### Agent Dispatch Summary

| Wave | Tasks | Agents |
|---|---|---|
| 1 | 5 | 1→quick, 2→deep, 3→quick, 4→quick, 5→quick |
| 2 | 4 | 6→unspecified-high, 7→unspecified-high, 8→visual-engineering, 9→visual-engineering |
| 3 | 3 | 10→quick, 11→unspecified-high, 12→quick |
| FINAL | 4 | F1→oracle, F2→unspecified-high, F3→unspecified-high, F4→deep |

---

## TODOs

- [x] 1. 数据库迁移：新增 section_monitor_status 列

  **What to do**:
  - 创建 `ro-ro-monitor/src/main/resources/sql/migration-v3-section-monitor.sql`
  - 执行 `ALTER TABLE vehicle_transit ADD COLUMN section_monitor_status VARCHAR(20) DEFAULT NULL COMMENT '三段监控状态：NORMAL/WARN/OVERDUE'`
  - 在 MySQL 生产数据库执行该迁移

  **Must NOT do**:
  - 不要创建新表
  - 不要添加索引（本迭代不需要）
  - 不要修改现有列

  **Recommended Agent Profile**:
  - **Category**: `quick`
  - **Skills**: 无特殊 skill 需求

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 1 (with Tasks 2, 3, 4)
  - **Blocks**: Tasks 3, 6
  - **Blocked By**: None

  **References**:
  - `ro-ro-monitor/src/main/resources/sql/migration-v2-overall-monitor.sql` - 参考现有迁移格式
  - `ro-ro-monitor/src/main/java/com/company/roro/entity/VehicleTransit.java:56` - overallMonitorStatus 字段定义

  **Acceptance Criteria**:
  - [ ] 迁移文件创建成功
  - [ ] MySQL 执行后，`DESCRIBE vehicle_transit` 显示 section_monitor_status 列
  - [ ] 现有数据的 section_monitor_status 为 NULL

  **QA Scenarios**:

  ```
  Scenario: 验证数据库列存在
    Tool: Bash (mysql)
    Steps:
      1. mysql -e "DESCRIBE vehicle_transit" | grep section_monitor_status
    Expected Result: 输出包含 "section_monitor_status VARCHAR(20) DEFAULT NULL"
    Evidence: .omo/evidence/task-1-db-column.png
  ```

  **Commit**: YES (Wave 1)
  - Message: `feat(db): add section_monitor_status column to vehicle_transit`
  - Files: `ro-ro-monitor/src/main/resources/sql/migration-v3-section-monitor.sql`

- [x] 2. StatusCalculator：新增 calculateSectionMonitorStatus() 方法

  **What to do**:
  - 在 `StatusCalculator.java` 中新增 `calculateSectionMonitorStatus()` 方法
  - 逻辑：
    1. 根据 transportStatus 判断车辆当前所属段（前段/中段/后段）
    2. 计算段起始时间：前段=orderReleaseTime，中段=shipDepartTime，后段=unloadFinishTime
    3. 计算段内累计 OTD：从段起始状态累加到当前状态的所有分段 OTD 之和
    4. 计算 elapsedHours = 段起始时间 → now（或 arriveShopTime 如果已到店）
    5. 判定：elapsedHours > cumulativeSectionOtd → OVERDUE，> cumulativeWarn → WARN，else NORMAL
  - 新增私有辅助方法：`getSectionStartTime()`、`getSectionCumulativeOtd()`、`getSectionFromStatus()`
  - ARRIVED 状态返回 null（不纳入三段统计）
  - 所有 null 输入返回 NORMAL（防御性编程）

  **Must NOT do**:
  - 不要修改现有的 calculateMonitorStatus() 或 calculateOverallMonitorStatus()
  - 不要改变现有的 getCumulativeOtd() 方法
  - 不要添加新的 OTD 配置字段

  **Recommended Agent Profile**:
  - **Category**: `deep`
  - **Skills**: 无特殊 skill 需求
  - **Reason**: 需要精确理解现有 OTD 计算逻辑并正确扩展

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 1 (with Tasks 1, 3, 4)
  - **Blocks**: Tasks 5, 6
  - **Blocked By**: None

  **References**:
  - `ro-ro-monitor/src/main/java/com/company/roro/util/StatusCalculator.java:126-162` - calculateOverallMonitorStatus() 完整模式（必须复用）
  - `ro-ro-monitor/src/main/java/com/company/roro/util/StatusCalculator.java:164-187` - getCumulativeOtd() 累加逻辑
  - `ro-ro-monitor/src/main/java/com/company/roro/util/StatusCalculator.java:192-211` - getStartTime() 时间戳获取模式
  - `ro-ro-monitor/src/main/java/com/company/roro/entity/VehicleTransit.java` - 可用的时间戳字段

  **Acceptance Criteria**:
  - [ ] calculateSectionMonitorStatus() 方法存在且编译通过
  - [ ] 单元测试覆盖所有 7 个非 ARRIVED 状态
  - [ ] 单元测试覆盖 null 输入（返回 NORMAL）
  - [ ] 单元测试覆盖边界值（刚好在 warn 阈值、刚好在 OTD 阈值）

  **QA Scenarios**:

  ```
  Scenario: 验证前段 NOT_DEPARTED 状态计算
    Tool: Bash (mvn test)
    Preconditions: StatusCalculatorTest 已编写
    Steps:
      1. mvn test -Dtest=StatusCalculatorTest#testSectionStatusNotDeparted
    Expected Result: 测试通过，返回 NORMAL（elapsed=0 < warnThreshold）
    Evidence: .omo/evidence/task-2-unit-test.png

  Scenario: 验证中段 ON_SEA 超时状态
    Tool: Bash (mvn test)
    Steps:
      1. mvn test -Dtest=StatusCalculatorTest#testSectionStatusOnSeaOverdue
    Expected Result: 测试通过，返回 OVERDUE（elapsed > cumulativeSectionOtd）
    Evidence: .omo/evidence/task-2-unit-test-overdue.png
  ```

  **Commit**: YES (Wave 1)
  - Message: `feat(backend): add three-section monitor status calculation`
  - Files: `ro-ro-monitor/src/main/java/com/company/roro/util/StatusCalculator.java`
  - Pre-commit: `mvn test -Dtest=StatusCalculatorTest`

- [x] 3. VehicleTransit：新增 sectionMonitorStatus 字段

  **What to do**:
  - 在 `VehicleTransit.java` 中新增 `private String sectionMonitorStatus;` 字段
  - 添加 Javadoc 注释：`/** 三段监控状态：NORMAL / WARN / OVERDUE */`
  - 由于使用 MyBatis-Plus + Lombok @Data，不需要手动添加 getter/setter

  **Must NOT do**:
  - 不要修改现有字段
  - 不要改变表名或注解

  **Recommended Agent Profile**:
  - **Category**: `quick`
  - **Skills**: 无特殊 skill 需求

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 1 (with Tasks 1, 2, 4)
  - **Blocks**: Task 6
  - **Blocked By**: Task 1 (DB 列必须先存在，但 Java 代码可以并行写)

  **References**:
  - `ro-ro-monitor/src/main/java/com/company/roro/entity/VehicleTransit.java:53-56` - monitorStatus 和 overallMonitorStatus 字段定义模式

  **Acceptance Criteria**:
  - [ ] VehicleTransit.java 新增 sectionMonitorStatus 字段
  - [ ] 编译通过（mvn compile）

  **QA Scenarios**:

  ```
  Scenario: 验证实体类编译
    Tool: Bash (mvn)
    Steps:
      1. mvn compile
    Expected Result: BUILD SUCCESS，无编译错误
    Evidence: .omo/evidence/task-3-compile.txt
  ```

  **Commit**: YES (Wave 1)
  - Message: `feat(entity): add sectionMonitorStatus field to VehicleTransit`
  - Files: `ro-ro-monitor/src/main/java/com/company/roro/entity/VehicleTransit.java`

- [x] 4. DTO 扩展：SectionChartDataDTO + TransitSummaryDTO 扩展

  **What to do**:
  - 创建 `SectionChartDataDTO.java`：
    - 字段：`String sectionName`（前段/中段/后段）
    - 字段：`Long normal, warn, overdue`
  - 创建 `SectionBrandChartDataDTO.java`（钻取用）：
    - 字段：`String brand, String sectionName`
    - 字段：`Long normal, warn, overdue`
  - 扩展 `TransitSummaryDTO.java`：
    - 新增字段：`Long sectionNormal, sectionWarn, sectionOverdue`
  - 由于前端主视图按段聚合展示，段级 DTO 不需要 brand 字段（除非钻取）

  **Must NOT do**:
  - 不要修改现有的 ChartDataDTO 或 OverallChartDataDTO
  - 不要在 DTO 中添加业务逻辑

  **Recommended Agent Profile**:
  - **Category**: `quick`
  - **Skills**: 无特殊 skill 需求

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 1 (with Tasks 1, 2, 3)
  - **Blocks**: Tasks 7, 8
  - **Blocked By**: None

  **References**:
  - `ro-ro-monitor/src/main/java/com/company/roro/dto/ChartDataDTO.java` - 现有 DTO 模式
  - `ro-ro-monitor/src/main/java/com/company/roro/dto/OverallChartDataDTO.java` - 现有 DTO 模式
  - `ro-ro-monitor/src/main/java/com/company/roro/dto/TransitSummaryDTO.java` - 现有 summary DTO 模式

  **Acceptance Criteria**:
  - [ ] 两个新 DTO 类创建成功
  - [ ] TransitSummaryDTO 扩展成功
  - [ ] 编译通过

  **QA Scenarios**:

  ```
  Scenario: 验证 DTO 编译
    Tool: Bash (mvn)
    Steps:
      1. mvn compile
    Expected Result: BUILD SUCCESS，新 DTO 类编译通过
    Evidence: .omo/evidence/task-4-dto-compile.txt
  ```

  **Commit**: YES (Wave 1)
  - Message: `feat(dto): add SectionChartDataDTO and extend TransitSummaryDTO`
  - Files: `ro-ro-monitor/src/main/java/com/company/roro/dto/SectionChartDataDTO.java`, `TransitSummaryDTO.java`

- [x] 5. 单元测试：StatusCalculatorTest 三段监控测试

  **What to do**:
  - 在 `StatusCalculatorTest.java` 中新增三段监控的单元测试
  - 测试用例覆盖：
    - 前段：NOT_DEPARTED（elapsed=0 → NORMAL）、TO_PORT（<warn → NORMAL, >warn → WARN, >OTD → OVERDUE）、AT_PORT_WAIT_SHIP（边界值）
    - 中段：ON_SEA（<warn → NORMAL, >warn → WARN, >OTD → OVERDUE）、AT_DEST_WAIT_UNLOAD（段内累加验证）
    - 后段：UNLOADED_WAIT_DISPATCH（<warn → NORMAL）、DISPATCHING（>OTD → OVERDUE）
    - ARRIVED：返回 null 或不计算
    - NULL 输入：null config → NORMAL、null startTime → NORMAL
  - 使用已知 OTD 值构造 RouteOtdConfig 对象，验证段内累计 OTD 计算正确

  **Must NOT do**:
  - 不要测试已有的 monitorStatus 或 overallMonitorStatus 逻辑
  - 不要使用真实数据库连接

  **Recommended Agent Profile**:
  - **Category**: `quick`
  - **Skills**: 无特殊 skill 需求

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 1 (with Tasks 1, 2, 3, 4)
  - **Blocks**: None
  - **Blocked By**: Task 2

  **References**:
  - `ro-ro-monitor/src/test/java/com/company/roro/util/StatusCalculatorTest.java` - 现有测试模式
  - `ro-ro-monitor/src/main/java/com/company/roro/util/StatusCalculator.java` - 被测试的方法

  **Acceptance Criteria**:
  - [ ] 新增测试用例 ≥ 10 个
  - [ ] `mvn test -Dtest=StatusCalculatorTest` 全部通过

  **QA Scenarios**:

  ```
  Scenario: 运行所有单元测试
    Tool: Bash (mvn)
    Steps:
      1. mvn test -Dtest=StatusCalculatorTest
    Expected Result: Tests run: N, Failures: 0, Errors: 0
    Evidence: .omo/evidence/task-5-test-results.txt
  ```

  **Commit**: YES (Wave 1)
  - Message: `test: add unit tests for calculateSectionMonitorStatus`
  - Files: `ro-ro-monitor/src/test/java/com/company/roro/util/StatusCalculatorTest.java`
  - Pre-commit: `mvn test -Dtest=StatusCalculatorTest`

- [x] 6. TransitDataServiceImpl：入库时计算 sectionMonitorStatus

  **What to do**:
  - 在 `TransitDataServiceImpl.processExcelData()` 中，在计算完 overallMonitorStatus 之后（第 95 行之后），新增：
    ```java
    String sectionMonitorStatus = StatusCalculator.calculateSectionMonitorStatus(
        transit, otdConfig, order.getOrderReleaseTime(), LocalDateTime.now(), monitorConfig.getOverallWarnRatio()
    );
    transit.setSectionMonitorStatus(sectionMonitorStatus);
    ```
  - 注意：calculateSectionMonitorStatus 的签名需要接收 orderReleaseTime（用于前段起始时间）

  **Must NOT do**:
  - 不要修改现有的 monitorStatus 或 overallMonitorStatus 计算逻辑
  - 不要改变导入流程的其他部分

  **Recommended Agent Profile**:
  - **Category**: `unspecified-high`
  - **Skills**: 无特殊 skill 需求
  - **Reason**: 需要理解 Service 层的完整数据流和事务边界

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 2 (with Tasks 7, 8, 9)
  - **Blocks**: Task 12
  - **Blocked By**: Tasks 1, 2, 3

  **References**:
  - `ro-ro-monitor/src/main/java/com/company/roro/service/impl/TransitDataServiceImpl.java:87-95` - overallMonitorStatus 计算位置（就在此处之后插入）
  - `ro-ro-monitor/src/main/java/com/company/roro/util/StatusCalculator.java` - 新方法的签名

  **Acceptance Criteria**:
  - [ ] 导入 Excel 数据后，vehicle_transit 表的 section_monitor_status 字段被正确填充
  - [ ] NULL sectionMonitorStatus 被正确处理（导入时不会报错）

  **QA Scenarios**:

  ```
  Scenario: 验证导入后 sectionMonitorStatus 被填充
    Tool: Bash (mysql + curl)
    Preconditions: 有一段测试数据可导入
    Steps:
      1. 导入测试 Excel
      2. mysql -e "SELECT section_monitor_status, COUNT(*) FROM vehicle_transit GROUP BY section_monitor_status"
    Expected Result: 返回 NORMAL/WARN/OVERDUE 的分布，无 NULL（或 NULL 仅存在于旧数据）
    Evidence: .omo/evidence/task-6-import-status.txt
  ```

  **Commit**: YES (Wave 2)
  - Message: `feat(service): compute sectionMonitorStatus during data import`
  - Files: `ro-ro-monitor/src/main/java/com/company/roro/service/impl/TransitDataServiceImpl.java`

- [x] 7. ChartController：新增 type=three-section 处理

  **What to do**:
  - 在 `ChartController.getBrandStatusChart()` 中新增 `type=three-section` 分支：
    1. 查询所有非 ARRIVED 车辆
    2. 根据 transportStatus 映射到段（前段/中段/后段）
    3. 按段分组，统计每段的 normal/warn/overdue 数量
    4. 返回 `List<SectionChartDataDTO>`
  - 同时新增品牌钻取接口（或复用同一接口加参数）：
    - 当 `type=three-section` 且提供 `sectionName` 参数时，返回该段下各品牌的数据 `List<SectionBrandChartDataDTO>`
  - 更新 `/transit/summary` 接口（或其他 summary 接口）返回三段统计

  **Must NOT do**:
  - 不要修改 type=segment 或 type=overall 的处理逻辑
  - 不要改变现有 API 的返回格式

  **Recommended Agent Profile**:
  - **Category**: `unspecified-high`
  - **Skills**: 无特殊 skill 需求
  - **Reason**: 需要理解 Controller 层的分组聚合逻辑和 API 设计

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 2 (with Tasks 6, 8, 9)
  - **Blocks**: Tasks 10
  - **Blocked By**: Task 4

  **References**:
  - `ro-ro-monitor/src/main/java/com/company/roro/controller/ChartController.java:104-189` - 现有 type=overall 处理模式
  - `ro-ro-monitor/src/main/java/com/company/roro/controller/TransitController.java` - summary 接口模式
  - `ro-ro-monitor/src/main/java/com/company/roro/dto/SectionChartDataDTO.java` - Task 4 创建的 DTO

  **Acceptance Criteria**:
  - [ ] `GET /api/chart/brand-status?type=three-section` 返回正确的段级聚合数据
  - [ ] 段级数据包含前段/中段/后段的 normal/warn/overdue 计数
  - [ ] 钻取接口返回品牌级数据

  **QA Scenarios**:

  ```
  Scenario: 验证三段监控段级 API
    Tool: Bash (curl)
    Steps:
      1. curl "http://localhost:8080/api/chart/brand-status?type=three-section"
    Expected Result: 返回 JSON 数组，每个元素有 sectionName、normal、warn、overdue 字段
    Evidence: .omo/evidence/task-7-section-api.json

  Scenario: 验证三段监控品牌钻取 API
    Tool: Bash (curl)
    Steps:
      1. curl "http://localhost:8080/api/chart/brand-status?type=three-section&sectionName=中段"
    Expected Result: 返回 JSON 数组，每个元素有 brand、sectionName、normal、warn、overdue 字段
    Evidence: .omo/evidence/task-7-brand-drilldown.json
  ```

  **Commit**: YES (Wave 2)
  - Message: `feat(api): add type=three-section support to ChartController`
  - Files: `ro-ro-monitor/src/main/java/com/company/roro/controller/ChartController.java`

- [x] 8. Dashboard.vue：新增三段监控 tab 基础结构 + summary cards

  **What to do**:
  - 在 Dashboard.vue 的 `el-tabs` 中新增第三个 tab：`el-tab-pane label="三段监控" name="three-section"`
  - 新增三段监控的数据结构：`sectionSummary`（normal/warn/overdue）、`sectionChartData`、section 相关的 filter 状态
  - 新增 `loadSectionData()` 方法，调用 `/chart/brand-status?type=three-section` 和 summary 接口
  - 新增 3 张 summary cards（normal/warn/overdue），复用现有样式
  - 新增骨架屏 loading 状态（复用现有的 skeleton 模式）

  **Must NOT do**:
  - 不要修改现有的分段/整段 tab 逻辑
  - 不要改变路由或导航结构

  **Recommended Agent Profile**:
  - **Category**: `visual-engineering`
  - **Skills**: 无特殊 skill 需求
  - **Reason**: 前端 UI 开发，需要匹配现有设计系统

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 2 (with Tasks 6, 7, 9)
  - **Blocks**: Task 9
  - **Blocked By**: Task 4

  **References**:
  - `ro-ro-monitor-web/src/views/Dashboard.vue` - 现有分段/整段 tab 实现
  - `ro-ro-monitor-web/src/style.css` - 现有设计系统（card-header、dashboard-panel 等类）
  - `ro-ro-monitor-web/src/components/StackedBarChart.vue` - 图表组件接口

  **Acceptance Criteria**:
  - [ ] Dashboard.vue 显示三个 tab（分段监控、整段监控、三段监控）
  - [ ] 点击"三段监控"tab 正常切换，无报错
  - [ ] Summary cards 正确显示 normal/warn/overdue 数量
  - [ ] 骨架屏在加载时显示

  **QA Scenarios**:

  ```
  Scenario: 验证三段监控 tab 存在且可点击
    Tool: Playwright
    Steps:
      1. 打开 http://localhost:5173/dashboard
      2. 等待页面加载
      3. 查找 tab 文本 "三段监控"
      4. 点击 "三段监控" tab
    Expected Result: tab 被激活，summary cards 显示数据（或骨架屏），无 console 报错
    Evidence: .omo/evidence/task-8-tab-exists.png
  ```

  **Commit**: YES (Wave 2)
  - Message: `feat(frontend): add three-section tab base structure and summary cards`
  - Files: `ro-ro-monitor-web/src/views/Dashboard.vue`

- [x] 9. Dashboard.vue：段级图表 + 品牌钻取功能

  **What to do**:
  - 在三段监控 tab 中，添加段级堆叠柱状图（StackedBarChart）
    - X 轴：前段 / 中段 / 后段
    - Y 轴：车辆数量
    - 堆叠：normal（绿色）/ warn（黄色）/ overdue（红色）
  - 添加饼图（StatusPieChart）展示三段总体状态分布
  - 实现品牌钻取功能：
    - 点击柱状图的某个段（如"中段"），切换到品牌视图
    - 品牌视图展示该段下各品牌的 normal/warn/overdue 分布
    - 提供"返回段级视图"按钮
  - 添加 filter controls（品牌筛选、状态筛选），复用现有组件
  - 添加数字动画（复用现有的 count-up 动画）
  - 所有图表数据通过 computed 属性转换，适配 StackedBarChart/StatusPieChart 的接口

  **Must NOT do**:
  - 不要修改 StackedBarChart.vue 或 StatusPieChart.vue 组件
  - 不要引入新的图表库

  **Recommended Agent Profile**:
  - **Category**: `visual-engineering`
  - **Skills**: 无特殊 skill 需求
  - **Reason**: 前端图表交互开发，需要复用现有组件

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 2 (with Tasks 6, 7, 8)
  - **Blocks**: Task 11
  - **Blocked By**: Tasks 7, 8

  **References**:
  - `ro-ro-monitor-web/src/components/StackedBarChart.vue` - 图表组件接口
  - `ro-ro-monitor-web/src/components/StatusPieChart.vue` - 饼图组件接口
  - `ro-ro-monitor-web/src/views/Dashboard.vue` - 现有分段/整段图表实现模式

  **Acceptance Criteria**:
  - [ ] 段级柱状图正确渲染，显示前段/中段/后段的数据
  - [ ] 饼图正确渲染，显示三段总体状态分布
  - [ ] 点击某一段可钻取到品牌视图
  - [ ] 品牌视图正确显示该段下各品牌数据
  - [ ] filter controls 正常工作

  **QA Scenarios**:

  ```
  Scenario: 验证段级图表渲染
    Tool: Playwright
    Steps:
      1. 打开 http://localhost:5173/dashboard
      2. 点击 "三段监控" tab
      3. 等待图表加载（或骨架屏消失）
      4. 截图柱状图区域
    Expected Result: 柱状图显示三个柱子（前段/中段/后段），有堆叠颜色（绿/黄/红）
    Evidence: .omo/evidence/task-9-section-chart.png

  Scenario: 验证品牌钻取功能
    Tool: Playwright
    Steps:
      1. 在三段监控 tab 中，点击"中段"柱子
      2. 等待品牌视图加载
      3. 截图品牌视图
    Expected Result: 显示品牌视图，包含各品牌的 normal/warn/overdue 数据
    Evidence: .omo/evidence/task-9-brand-drilldown.png
  ```

  **Commit**: YES (Wave 2)
  - Message: `feat(frontend): add section-level chart and brand drill-down`
  - Files: `ro-ro-monitor-web/src/views/Dashboard.vue`

- [x] 10. API 集成测试：curl 验证三段监控 API

  **What to do**:
  - 编写 curl 命令验证三段监控 API：
    1. `GET /api/chart/brand-status?type=three-section` - 验证返回段级聚合数据
    2. `GET /api/chart/brand-status?type=three-section&sectionName=中段` - 验证返回品牌钻取数据
    3. `GET /api/transit/summary` - 验证返回包含 sectionNormal/sectionWarn/sectionOverdue
  - 验证响应格式符合预期
  - 验证 NULL sectionMonitorStatus 被正确处理（计数为 NORMAL）

  **Must NOT do**:
  - 不要编写自动化测试脚本（项目没有 API 测试框架）
  - 不要修改后端代码

  **Recommended Agent Profile**:
  - **Category**: `quick`
  - **Skills**: 无特殊 skill 需求

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 3 (with Tasks 11, 12)
  - **Blocks**: None
  - **Blocked By**: Task 7

  **References**:
  - `ro-ro-monitor/src/main/java/com/company/roro/controller/ChartController.java` - API 端点
  - `ro-ro-monitor/src/main/java/com/company/roro/controller/TransitController.java` - summary 端点

  **Acceptance Criteria**:
  - [ ] curl 命令返回 HTTP 200
  - [ ] 响应 JSON 包含预期的字段
  - [ ] 数据格式正确（数字、字符串类型）

  **QA Scenarios**:

  ```
  Scenario: 验证段级 API 响应
    Tool: Bash (curl)
    Steps:
      1. curl -s "http://localhost:8080/api/chart/brand-status?type=three-section" | jq .
    Expected Result: JSON 数组，每个元素有 sectionName（"前段"/"中段"/"后段"）、normal、warn、overdue 字段
    Evidence: .omo/evidence/task-10-section-api.json

  Scenario: 验证品牌钻取 API 响应
    Tool: Bash (curl)
    Steps:
      1. curl -s "http://localhost:8080/api/chart/brand-status?type=three-section&sectionName=中段" | jq .
    Expected Result: JSON 数组，每个元素有 brand、sectionName、normal、warn、overdue 字段
    Evidence: .omo/evidence/task-10-brand-api.json
  ```

  **Commit**: YES (Wave 3)
  - Message: `test: add API integration tests for three-section monitor`
  - Files: 无（curl 命令记录在 QA scenarios 中）

- [x] 11. Playwright 前端验证：三段监控 tab 截图

  **What to do**:
  - 启动 Vite dev server（端口 5173）和后端（端口 8080）
  - 使用 Playwright 打开 Dashboard 页面
  - 验证：
    1. 三个 tab 存在（分段监控、整段监控、三段监控）
    2. 点击"三段监控"tab，页面正常渲染
    3. Summary cards 显示正确（3 张卡片：normal/warn/overdue）
    4. 柱状图渲染（前段/中段/后段三个柱子）
    5. 饼图渲染
    6. 无 console.error
  - 截图保存到 `.omo/evidence/`

  **Must NOT do**:
  - 不要修改前端代码（这是验证任务）
  - 不要引入新的测试框架

  **Recommended Agent Profile**:
  - **Category**: `unspecified-high`
  - **Skills**: `playwright`
  - **Reason**: 前端 E2E 验证

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 3 (with Tasks 10, 12)
  - **Blocks**: None
  - **Blocked By**: Task 9

  **References**:
  - `ro-ro-monitor-web/src/views/Dashboard.vue` - 被验证的页面
  - `ro-ro-monitor-web/vite.config.js` - dev server 配置

  **Acceptance Criteria**:
  - [ ] Playwright 截图显示三段监控 tab 正常
  - [ ] 无 console.error
  - [ ] 图表渲染正确

  **QA Scenarios**:

  ```
  Scenario: Playwright 验证三段监控 tab
    Tool: Playwright
    Preconditions: Vite dev server 和后端已启动
    Steps:
      1. 打开 http://localhost:5173/dashboard
      2. 等待页面加载完成
      3. 查找 tab "三段监控"
      4. 点击 "三段监控"
      5. 等待 2 秒（图表渲染）
      6. 截图全屏
      7. 检查 console.error
    Expected Result: 
      - tab "三段监控"存在且可点击
      - 点击后显示 3 张 summary cards
      - 柱状图和饼图区域有内容（非空白）
      - console.error 为空数组
    Evidence: .omo/evidence/task-11-playwright-fullpage.png
  ```

  **Commit**: NO（验证任务，不提交代码）

- [x] 12. 数据验证：导入测试数据，验证 sectionMonitorStatus 计算正确

  **What to do**:
  - 准备测试 Excel 数据（至少包含各段的车辆数据）
  - 通过 Upload 页面导入数据
  - 查询数据库验证 section_monitor_status 被正确填充：
    - 前段车辆应有 NORMAL/WARN/OVERDUE 值
    - 中段车辆应有 NORMAL/WARN/OVERDUE 值
    - 后段车辆应有 NORMAL/WARN/OVERDUE 值
    - 已到店车辆应为 NULL（不纳入三段）
  - 对比手动计算的 sectionMonitorStatus 和数据库值

  **Must NOT do**:
  - 不要修改后端代码（这是验证任务）
  - 不要使用生产数据

  **Recommended Agent Profile**:
  - **Category**: `quick`
  - **Skills**: 无特殊 skill 需求

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 3 (with Tasks 10, 11)
  - **Blocks**: None
  - **Blocked By**: Task 6

  **References**:
  - `ro-ro-monitor-web/src/views/Upload.vue` - 数据导入页面
  - `ro-ro-monitor/src/main/java/com/company/roro/util/StatusCalculator.java` - 计算逻辑参考

  **Acceptance Criteria**:
  - [ ] 导入后数据库中有 section_monitor_status 非 NULL 的记录
  - [ ] section_monitor_status 值符合预期（NORMAL/WARN/OVERDUE）
  - [ ] 已到店车辆 section_monitor_status 为 NULL

  **QA Scenarios**:

  ```
  Scenario: 验证导入后 sectionMonitorStatus 分布
    Tool: Bash (mysql + curl)
    Steps:
      1. 通过 Upload 页面导入测试数据
      2. mysql -e "SELECT section_monitor_status, COUNT(*) FROM vehicle_transit WHERE transport_status != 'ARRIVED' GROUP BY section_monitor_status"
    Expected Result: 返回 NORMAL、WARN、OVERDUE 的计数，无 NULL（或 NULL 仅存在于旧数据）
    Evidence: .omo/evidence/task-12-data-validation.txt
  ```

  **Commit**: NO（验证任务，不提交代码）

---

## Final Verification Wave

> 4 个审查代理并行运行。全部通过后才能向用户呈现结果并获取明确"okay"。

- [x] F1. **Plan Compliance Audit** — `oracle`
  Read the plan end-to-end. For each "Must Have": verify implementation exists (read file, curl endpoint, run command). For each "Must NOT Have": search codebase for forbidden patterns — reject with file:line if found. Check evidence files exist in .omo/evidence/. Compare deliverables against plan.
  Output: `Must Have [N/N] | Must NOT Have [N/N] | Tasks [N/N] | VERDICT: APPROVE/REJECT`

- [x] F2. **Code Quality Review** — `unspecified-high`
  Run `mvn test` + linter. Review all changed files for: empty catches, commented-out code, unused imports. Check AI slop: excessive comments, over-abstraction, generic names.
  Output: `Build [PASS/FAIL] | Tests [N pass/N fail] | Files [N clean/N issues] | VERDICT`

- [x] F3. **Real Manual QA** — `unspecified-high` (+ `playwright` skill)
  Start from clean state. Execute EVERY QA scenario from EVERY task — follow exact steps, capture evidence. Test cross-task integration (features working together, not isolation). Test edge cases: empty state, invalid input, rapid actions. Save to `.omo/evidence/final-qa/`.
  Output: `Scenarios [N/N pass] | Integration [N/N] | Edge Cases [N tested] | VERDICT`

- [x] F4. **Scope Fidelity Check** — `deep`
  For each task: read "What to do", read actual diff (git log/diff). Verify 1:1 — everything in spec was built (no missing), nothing beyond spec was built (no creep). Check "Must NOT do" compliance. Detect cross-task contamination.
  Output: `Tasks [N/N compliant] | Contamination [CLEAN/N issues] | Unaccounted [CLEAN/N files] | VERDICT`

---

## Commit Strategy

- **Wave 1**: `feat(backend): add three-section monitor status calculation and DB column` - migration, StatusCalculator, VehicleTransit, DTOs, tests
- **Wave 2**: `feat(backend+frontend): add three-section API and Dashboard tab` - ServiceImpl, ChartController, Dashboard.vue
- **Wave 3**: `test: add integration tests and Playwright verification` - curl tests, Playwright, data validation

## Success Criteria

### Verification Commands
```bash
# 数据库迁移验证
mysql -e "DESCRIBE vehicle_transit" | grep section_monitor_status
# 预期输出：section_monitor_status VARCHAR(20) DEFAULT NULL

# API 测试
curl "http://localhost:8080/api/chart/brand-status?type=three-section"
# 预期返回：[{sectionName, normal, warn, overdue}]

# 单元测试
mvn test -Dtest=StatusCalculatorTest
# 预期：全部通过

# 前端构建
cd ro-ro-monitor-web && npm run build
# 预期：0 errors

# Playwright 验证（截图三段监控 tab）
# 见 Task 11 QA scenarios
```

### Final Checklist
- [ ] All "Must Have" present
- [ ] All "Must NOT Have" absent
- [ ] All tests pass
- [ ] DB migration executed
- [ ] Playwright screenshots captured
- [ ] User explicit "okay" obtained
