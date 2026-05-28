# 到达车辆监控模块

## TL;DR

> **Quick Summary**: 新建独立路由页面 `/dashboard/arrived`，展示已到店（ARRIVED）车辆的到达效率延迟分布（分段/三段/整段3个tab），以及按周/月的到店数量和效率统计。左侧菜单新增"车辆监控"父菜单，下挂"在途车辆"和"到达车辆"两个子菜单。
>
> **Deliverables**:
> - `ArrivedEfficiencyCalculator` + JUnit测试
> - 4个新DTO（ArrivedChartDataDTO, ArrivedSummaryDTO, ArrivedWeeklyMonthlyDTO, ArrivedVehicleDTO）
> - `ArrivedVehicleService`（查询ARRIVED车辆+计算效率+聚合）
> - `ArrivedController`（summary/chart/statistics API）
> - `ArrivedDashboard.vue` + 4个子组件（OverallTab, SegmentTab, ThreeSectionTab, StatisticsPanel）
> - 路由和菜单重构（`/dashboard/in-transit`, `/dashboard/arrived`）
>
> **Estimated Effort**: Medium (~3-4 hours)
> **Parallel Execution**: YES - 4 waves
> **Critical Path**: Task 1 → Task 3 → Task 4 → Task 6 → Task 13 → F1-F4

---

## Context

### Original Request
监控大屏下面做两个模块：一个叫"在途车辆监控"（即目前的3个监控dashboard，数据为所有未到店车辆），另一个叫"到达车辆监控"，数据为所有已到店车辆。到达车辆监控里，也分为分段、三段和整段3个tab。

### Interview Summary
**Key Discussions**:
- 页面结构: 两个独立路由，通过左侧"车辆监控"父菜单切换
- 在途车辆 → `/dashboard/in-transit`，到达车辆 → `/dashboard/arrived`
- "已到店"定义: 仅 ARRIVED 状态（`transportStatus == "ARRIVED"`）
- 监控维度: 选项B — 各层级的"延迟分布"（高效/正常/延迟）
  - 分段tab: 各分段的延迟分布统计
  - 三段tab: 前段/中段/后段的延迟分布
  - 整段tab: 整体延迟分布
- 效率定义: 到达效率 = 实际总用时 / 标准OTD
  - < 80% → 高效（绿色）
  - 80%-100% → 正常（黄色）
  - ≥100% → 延迟（红色）
- 额外维度: 按周/月统计到店数量和平均到达效率（基于 arriveShopTime）
- 缺失时间戳: 缺失中间时间戳的分段标记为 N/A，不计入统计
- 页面展示: 仅聚合统计（图表），不展示单车明细
- 测试策略: 后端 JUnit + 前端 curl/手动验证

**Research Findings**:
- 当前 `/dashboard` 在 `Dashboard.vue`，所有后端 API 排除 ARRIVED（`.ne(transportStatus, "ARRIVED")`）
- 没有展示已到店车辆的任何页面或 API
- `StatusCalculator` 已有 OTD 计算逻辑，`VehicleTransit` 有 `arriveShopTime` 字段
- 测试基础设施: 后端 Maven + JUnit 5（已有 `StatusCalculatorTest.java`），前端无测试框架
- 路由配置在 `router/index.js`，左侧菜单在 `App.vue`

### Metis Review
**Identified Gaps** (addressed):
- **到达效率计算逻辑**: 已明确为延迟分布（选项B），各tab展示聚合统计而非单车效率
- **缺失时间戳处理**: 默认策略为标记 N/A
- **Scope OUT**: 已明确6项不做内容
- **前端QA策略**: 明确不安装测试框架，使用 curl + 手动验证

---

## Work Objectives

### Core Objective
新建到达车辆监控模块，提供独立的页面展示已到店车辆的延迟分布和周/月统计。

### Concrete Deliverables
- `StatusCalculator.java` 新增到达效率计算方法
- `ArrivedChartDataDTO.java`, `ArrivedSummaryDTO.java`, `ArrivedWeeklyMonthlyDTO.java`, `ArrivedVehicleDTO.java`
- `ArrivedVehicleService.java`
- `ArrivedController.java`
- `ArrivedDashboard.vue` (主页面)
- `ArrivedOverallTab.vue`, `ArrivedSegmentTab.vue`, `ArrivedThreeSectionTab.vue`, `ArrivedStatisticsPanel.vue`
- 路由重构: `/dashboard/in-transit`, `/dashboard/arrived`
- 菜单重构: "车辆监控"父菜单 + 2个子菜单

### Definition of Done
- [ ] 访问 `/dashboard/arrived` 能看到到达监控页面，含3个tab
- [ ] 每个tab展示对应层级的延迟分布图表（高效/正常/延迟）
- [ ] 周/月统计面板展示到店数量和平均效率趋势
- [ ] 品牌筛选正常工作
- [ ] JUnit测试全部通过
- [ ] curl验证所有API端点返回正确数据
- [ ] `/dashboard` 重定向到 `/dashboard/in-transit`

### Must Have
- 到达效率计算和延迟分布展示（3个tab）
- 按周/月的到店数量和效率统计
- 品牌筛选功能
- 路由和菜单重构
- JUnit单元测试
- API QA验证

### Must NOT Have (Guardrails)
- 不展示单车明细列表
- 不添加导出/下载功能
- 不添加历史对比视图（在途 vs 到达）
- 不添加已到店车辆数据编辑功能
- 不添加实时推送/WebSocket 更新
- 不修改现有在途监控 Dashboard.vue 的功能和样式
- 不安装前端测试框架（Jest/Vitest/Playwright）

### Defaults Applied
- **默认时间范围**: 到达监控页面默认展示最近 30 天的已到店车辆（避免加载全部历史数据）
- **分页**: 列表查询默认 pageSize=20（如果未来添加列表视图）
- **周定义**: ISO 周（周一到周日）

---

## Verification Strategy

### Test Decision
- **Infrastructure exists**: YES (后端 JUnit 5)
- **Automated tests**: YES (Tests-after) — 到达效率计算逻辑需要 JUnit 测试
- **Framework**: JUnit 5 (Maven)
- **Frontend**: NO automated tests — curl API验证 + 手动浏览器验证

### QA Policy
Every task MUST include agent-executed QA scenarios.

- **Frontend/UI**: Manual browser verification — Navigate, interact, verify rendering
- **API/Backend**: Bash (curl) — Send requests, assert status + response fields
- **Library/Module**: Bash (mvn test) — Run JUnit tests, verify PASS

---

## Execution Strategy

### Parallel Execution Waves

```
Wave 1 (Start Immediately - foundation):
├── Task 1: ArrivedEfficiencyCalculator + JUnit测试 [quick]
├── Task 2: 到达监控DTOs (4个文件) [quick]
├── Task 3: ArrivedVehicleService (查询+计算+聚合) [unspecified-high]
└── Task 4: 路由菜单重构 [quick]

Wave 2 (After Wave 1 - backend APIs):
├── Task 5: ArrivedController - summary & chart APIs [unspecified-high]
└── Task 6: ArrivedController - statistics API (周/月) [unspecified-high]

Wave 3 (After Wave 2 - frontend, MAX PARALLEL):
├── Task 7: ArrivedDashboard.vue 骨架 + 数据加载 [visual-engineering]
├── Task 8: ArrivedOverallTab.vue (整段延迟分布) [visual-engineering]
├── Task 9: ArrivedSegmentTab.vue (分段延迟分布) [visual-engineering]
├── Task 10: ArrivedThreeSectionTab.vue (三段延迟分布+drilldown) [visual-engineering]
└── Task 11: ArrivedStatisticsPanel.vue (周/月统计) [visual-engineering]

Wave 4 (After Wave 3 - integration + QA):
├── Task 12: JUnit边界情况测试补充 [quick]
├── Task 13: API curl验证 [unspecified-high]
└── Task 14: 前端手动验证清单执行 [unspecified-high]

Wave FINAL (After ALL tasks - 4 parallel reviews, then user okay):
├── Task F1: Plan compliance audit (oracle)
├── Task F2: Code quality review (unspecified-high)
├── Task F3: Real manual QA (unspecified-high)
└── Task F4: Scope fidelity check (deep)
-> Present results -> Get explicit user okay

Critical Path: Task 1 → Task 3 → Task 5 → Task 7 → Task 13 → F1-F4 → user okay
Parallel Speedup: ~60% faster than sequential
Max Concurrent: 5 (Wave 3)
```

### Dependency Matrix

| Task | Blocked By | Blocks |
|------|-----------|--------|
| 1 | None | 3, 12 |
| 2 | None | 5, 6 |
| 3 | 1 | 5, 6 |
| 4 | None | 7, 13, 14 |
| 5 | 2, 3 | 13 |
| 6 | 2, 3 | 13 |
| 7 | 4, 5 | 8, 9, 10, 11, 14 |
| 8 | 7 | 14 |
| 9 | 7 | 14 |
| 10 | 7 | 14 |
| 11 | 7 | 14 |
| 12 | 1 | F2 |
| 13 | 5, 6, 4 | F3 |
| 14 | 7, 8, 9, 10, 11 | F3 |

### Agent Dispatch Summary

- **Wave 1**: 4 tasks → `quick`, `quick`, `unspecified-high`, `quick`
- **Wave 2**: 2 tasks → `unspecified-high`, `unspecified-high`
- **Wave 3**: 5 tasks → `visual-engineering` × 5
- **Wave 4**: 3 tasks → `quick`, `unspecified-high`, `unspecified-high`
- **FINAL**: 4 tasks → `oracle`, `unspecified-high`, `unspecified-high`, `deep`

---

## TODOs

- [x] 1. **ArrivedEfficiencyCalculator 工具类 + JUnit 测试**

  **What to do**:
  - 在 `StatusCalculator.java` 旁新建 `ArrivedEfficiencyCalculator.java`
  - 实现方法 `calculateEfficiency(VehicleTransit transit, RouteOtdConfig config)`：返回到达效率百分比（actualTotalTime / standardOtd * 100）
  - 实现方法 `getEfficiencyBucket(double efficiency)`：返回 "EFFICIENT"(<80%), "NORMAL"(80-100%), "DELAYED"(≥100%)
  - 实现方法 `calculateSegmentEfficiency(VehicleTransit transit, RouteOtdConfig config, String segment)`：计算单个分段的效率（需要该分段的起止时间戳）
  - 处理缺失时间戳：如果某分段缺少起止时间，返回 `null`（不计入统计）
  - 处理缺失 config：返回 `null`
  - 处理缺失 orderReleaseTime 或 arriveShopTime：返回 `null`
  - 写 JUnit 测试 `ArrivedEfficiencyCalculatorTest.java`：覆盖所有正常路径和边界情况

  **Must NOT do**:
  - 不要修改现有的 `StatusCalculator.java`
  - 不要假设所有时间戳都存在

  **Recommended Agent Profile**:
  - **Category**: `quick`
  - Reason: 纯工具类 + 单元测试，逻辑清晰

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 1 (with Tasks 2, 3, 4)
  - **Blocks**: Task 3, Task 12
  - **Blocked By**: None

  **References**:
  - `StatusCalculator.java` — 参考 OTD 计算模式和现有测试结构
  - `VehicleTransit.java` — 查看所有时间字段（arriveShopTime, shipDepartTime, unloadFinishTime 等）
  - `RouteOtdConfig.java` — 查看 OTD 配置字段
  - `StatusCalculatorTest.java` — 参考 JUnit 5 测试模式

  **Acceptance Criteria**:
  - [ ] `ArrivedEfficiencyCalculator.java` 创建并编译通过
  - [ ] `ArrivedEfficiencyCalculatorTest.java` 创建且 `mvn test` PASS（≥10 个测试用例）
  - [ ] 测试覆盖：正常计算、缺失时间戳、缺失 config、边界值（79.9%, 80%, 100%, 100.1%）

  **QA Scenarios**:
  ```
  Scenario: 正常计算到达效率
    Tool: Bash (mvn test)
    Steps:
      1. cd ro-ro-monitor && mvn test -Dtest=ArrivedEfficiencyCalculatorTest
    Expected Result: Tests PASS (10+ tests, 0 failures)
    Evidence: .omo/evidence/task-1-junit-pass.txt
  ```

  **Commit**: YES
  - Message: `feat(arrived): add ArrivedEfficiencyCalculator with JUnit tests`
  - Files: `ro-ro-monitor/src/main/java/.../ArrivedEfficiencyCalculator.java`, `ro-ro-monitor/src/test/java/.../ArrivedEfficiencyCalculatorTest.java`

- [x] 2. **到达监控 DTOs（4 个文件）**

  **What to do**:
  - 新建 `ArrivedVehicleDTO.java`：包含车辆基本信息 + 到达效率字段（efficiency, efficiencyBucket）
  - 新建 `ArrivedSummaryDTO.java`：包含 efficientCount / normalCount / delayedCount / totalCount + 平均效率
  - 新建 `ArrivedChartDataDTO.java`：图表数据（品牌 × 效率桶 的计数矩阵），复用现有 ChartDataDTO 模式
  - 新建 `ArrivedWeeklyMonthlyDTO.java`：周/月统计（period, arrivalCount, avgEfficiency）
  - 所有 DTO 放在 `dto` 包下

  **Must NOT do**:
  - 不要修改现有 DTO（ChartDataDTO, OverallChartDataDTO, TransitSummaryDTO 等）

  **Recommended Agent Profile**:
  - **Category**: `quick`

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 1
  - **Blocks**: Task 5, Task 6
  - **Blocked By**: None

  **References**:
  - `TransitSummaryDTO.java` — 参考 summary 统计 DTO 结构
  - `ChartDataDTO.java` — 参考图表数据 DTO 结构
  - `OverallChartDataDTO.java` — 参考整体监控图表 DTO

  **Acceptance Criteria**:
  - [ ] 4 个 DTO 文件创建且编译通过
  - [ ] DTO 包含所有必要字段，类型正确

  **QA Scenarios**:
  ```
  Scenario: DTO 编译验证
    Tool: Bash (mvn compile)
    Steps:
      1. cd ro-ro-monitor && mvn compile
    Expected Result: BUILD SUCCESS，无 DTO 相关编译错误
    Evidence: .omo/evidence/task-2-compile.txt
  ```

  **Commit**: YES (groups with Task 1)

- [x] 3. **ArrivedVehicleService（查询 + 计算 + 聚合）**

  **What to do**:
  - 新建 `ArrivedVehicleService.java`（或 `ArrivedVehicleServiceImpl.java`）
  - 实现 `listArrivedVehicles(startTime, endTime, brandId)`：查询 `transportStatus == "ARRIVED"` 的车辆，支持时间范围和品牌筛选
  - 实现 `calculateSummary(startTime, endTime, brandId)`：返回 `ArrivedSummaryDTO`（按效率桶聚合）
  - 实现 `calculateChartData(type, startTime, endTime, brandId)`：返回 `ArrivedChartDataDTO`
    - `type=segment`：按分段 × 效率桶 聚合
    - `type=three-section`：按三段 × 效率桶 聚合
    - `type=overall`：按品牌 × 效率桶 聚合
  - 实现 `calculateWeeklyMonthly(period, startTime, endTime, brandId)`：按周/月聚合到店数量和平均效率
  - 处理边界：无数据时返回空但合法的 DTO（counts = 0）
  - 处理分页：如果数据量大，添加分页参数（page, size）

  **Must NOT do**:
  - 不要修改现有 TransitDataServiceImpl
  - 不要在 Service 层暴露 HTTP 相关逻辑

  **Recommended Agent Profile**:
  - **Category**: `unspecified-high`

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 1
  - **Blocks**: Task 5, Task 6
  - **Blocked By**: Task 1

  **References**:
  - `TransitDataServiceImpl.java` — 参考查询模式（LambdaQueryWrapper, JOIN OrderInfo）
  - `ChartController.java` — 参考图表聚合逻辑
  - `StatusCalculator.java` — 参考状态映射逻辑
  - `VehicleTransit.java` — 查看 entity 字段和关联关系

  **Acceptance Criteria**:
  - [ ] `ArrivedVehicleService.java` 创建且编译通过
  - [ ] Service 方法能正确查询 ARRIVED 车辆（不包括其他状态）
  - [ ] 效率计算调用 `ArrivedEfficiencyCalculator`
  - [ ] 支持时间范围和品牌筛选

  **QA Scenarios**:
  ```
  Scenario: Service 查询验证
    Tool: Bash (curl - 需要先启动后端)
    Preconditions: 后端服务运行中
    Steps:
      1. curl -s "http://localhost:8080/api/arrived/summary"
    Expected Result: 返回 JSON，包含 efficientCount/normalCount/delayedCount/totalCount 字段
    Failure Indicators: 返回空或缺少字段
    Evidence: .omo/evidence/task-3-api-summary.json
  ```

  **Commit**: YES (groups with Tasks 1-2)

- [x] 4. **路由和菜单重构**

  **What to do**:
  - 修改 `router/index.js`：
    - 将 `/dashboard` 改为 `/dashboard/in-transit`，component 仍指向 `Dashboard.vue`
    - 新增 `/dashboard/arrived`，component 指向新建的 `ArrivedDashboard.vue`
    - 添加 `/dashboard` → `/dashboard/in-transit` 的重定向
  - 修改 `App.vue`（或菜单配置文件）：
    - 将现有的 "监控大屏" 单菜单项改为 "车辆监控" 子菜单（`el-sub-menu`）
    - 子菜单1："在途车辆" → `/dashboard/in-transit`
    - 子菜单2："到达车辆" → `/dashboard/arrived`
    - 设置默认激活项为 "在途车辆"
  - 检查所有硬编码的 `/dashboard` 路由引用（如导航守卫、面包屑、跳转逻辑），更新为 `/dashboard/in-transit`

  **Must NOT do**:
  - 不要删除 Dashboard.vue 或修改其内部逻辑
  - 不要改变现有在途监控的 URL（如果外部有书签）——通过重定向保持兼容

  **Recommended Agent Profile**:
  - **Category**: `quick`

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 1
  - **Blocks**: Task 7, Task 13, Task 14
  - **Blocked By**: None

  **References**:
  - `router/index.js` — 查看现有路由定义
  - `App.vue` — 查看现有菜单结构
  - 搜索 `'/dashboard'` 字符串的所有引用（grep）

  **Acceptance Criteria**:
  - [ ] 访问 `/dashboard` 自动重定向到 `/dashboard/in-transit`
  - [ ] 访问 `/dashboard/in-transit` 正常加载现有在途监控页面
  - [ ] 左侧菜单显示 "车辆监控" 父菜单，下有 "在途车辆" 和 "到达车辆"
  - [ ] 点击菜单项正确跳转

  **QA Scenarios**:
  ```
  Scenario: 路由重定向验证
    Tool: Browser (手动)
    Steps:
      1. 访问 http://localhost:5173/dashboard
      2. 观察 URL 是否变为 /dashboard/in-transit
    Expected Result: URL 重定向成功，页面加载在途监控
    Evidence: .omo/evidence/task-4-routing.png

  Scenario: 菜单结构验证
    Tool: Browser (手动)
    Steps:
      1. 查看左侧菜单
      2. 点击 "车辆监控" → "到达车辆"
    Expected Result: 菜单展开，点击后跳转到 /dashboard/arrived（即使页面404也是路由正确）
    Evidence: .omo/evidence/task-4-menu.png
  ```

  **Commit**: YES (groups with Wave 1)

- [x] 5. **ArrivedController - summary & chart APIs**

  **What to do**:
  - 新建 `ArrivedController.java`
  - 实现 `GET /api/arrived/summary`：返回 `ArrivedSummaryDTO`
    - 参数: `startTime`, `endTime` (可选), `brandId` (可选)
  - 实现 `GET /api/arrived/chart`：返回 `ArrivedChartDataDTO`
    - 参数: `type` (segment|three-section|overall), `startTime`, `endTime`, `brandId` (可选)
    - `type=segment`：各分段（NOT_DEPARTED, TO_PORT, ...）× 效率桶 的分布
    - `type=three-section`：三段（前段/中段/后段）× 效率桶 的分布，支持 `sectionName` 钻取到品牌
    - `type=overall`：品牌 × 效率桶 的分布
  - 所有端点只查询 `transportStatus == "ARRIVED"` 的车辆
  - 正确处理无数据情况（返回空数组，HTTP 200）

  **Must NOT do**:
  - 不要修改现有 TransitController 或 ChartController
  - 不要返回单车明细（只返回聚合统计）

  **Recommended Agent Profile**:
  - **Category**: `unspecified-high`

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 2 (with Task 6)
  - **Blocks**: Task 13
  - **Blocked By**: Task 2, Task 3

  **References**:
  - `ChartController.java` — 参考图表 API 的参数处理和数据组装模式
  - `TransitController.java` — 参考 summary API 模式
  - `ArrivedSummaryDTO.java`, `ArrivedChartDataDTO.java` — 按这些 DTO 返回数据

  **Acceptance Criteria**:
  - [ ] `ArrivedController.java` 创建且编译通过
  - [ ] `GET /api/arrived/summary` 返回正确 JSON 结构
  - [ ] `GET /api/arrived/chart?type=overall` 返回品牌 × 效率桶 数据
  - [ ] `GET /api/arrived/chart?type=segment` 返回分段 × 效率桶 数据
  - [ ] `GET /api/arrived/chart?type=three-section` 返回三段 × 效率桶 数据

  **QA Scenarios**:
  ```
  Scenario: Summary API 验证
    Tool: Bash (curl)
    Preconditions: 后端运行，数据库有 ARRIVED 车辆数据
    Steps:
      1. curl -s "http://localhost:8080/api/arrived/summary"
    Expected Result: HTTP 200，JSON 包含 efficientCount, normalCount, delayedCount, totalCount
    Evidence: .omo/evidence/task-5-summary.json

  Scenario: Chart API - overall 验证
    Tool: Bash (curl)
    Steps:
      1. curl -s "http://localhost:8080/api/arrived/chart?type=overall"
    Expected Result: HTTP 200，JSON 包含品牌列表和效率桶分布数据
    Evidence: .omo/evidence/task-5-chart-overall.json

  Scenario: Chart API - segment 验证
    Tool: Bash (curl)
    Steps:
      1. curl -s "http://localhost:8080/api/arrived/chart?type=segment"
    Expected Result: HTTP 200，JSON 包含分段列表和效率桶分布数据
    Evidence: .omo/evidence/task-5-chart-segment.json

  Scenario: Chart API - three-section 验证
    Tool: Bash (curl)
    Steps:
      1. curl -s "http://localhost:8080/api/arrived/chart?type=three-section"
    Expected Result: HTTP 200，JSON 包含三段（前段/中段/后段）和效率桶分布数据
    Evidence: .omo/evidence/task-5-chart-three-section.json
  ```

  **Commit**: YES
  - Message: `feat(arrived): add ArrivedController with summary and chart APIs`

- [x] 6. **ArrivedController - statistics API（周/月统计）**

  **What to do**:
  - 在 `ArrivedController.java` 中新增 `GET /api/arrived/statistics`
  - 参数: `period` (week|month), `startTime`, `endTime`, `brandId` (可选)
  - `period=week`：按 ISO 周（周一到周日）分组，返回每周的到店数量和平均效率
  - `period=month`：按自然月分组，返回每月的到店数量和平均效率
  - 返回 `List<ArrivedWeeklyMonthlyDTO>`
  - 边界：时间范围内无数据时返回空数组

  **Must NOT do**:
  - 不要在前端做周/月分组（后端聚合）

  **Recommended Agent Profile**:
  - **Category**: `unspecified-high`

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 2
  - **Blocks**: Task 13
  - **Blocked By**: Task 2, Task 3

  **References**:
  - `ArrivedWeeklyMonthlyDTO.java` — 按此 DTO 结构返回数据
  - `ArrivedVehicleService.java` — 调用 Service 层方法

  **Acceptance Criteria**:
  - [ ] `GET /api/arrived/statistics?period=week` 返回周统计数组
  - [ ] `GET /api/arrived/statistics?period=month` 返回月统计数组
  - [ ] 每个统计项包含 period, arrivalCount, avgEfficiency

  **QA Scenarios**:
  ```
  Scenario: 周统计 API 验证
    Tool: Bash (curl)
    Preconditions: 后端运行，数据库有 ARRIVED 车辆数据
    Steps:
      1. curl -s "http://localhost:8080/api/arrived/statistics?period=week"
    Expected Result: HTTP 200，JSON 数组，每项包含 period, arrivalCount, avgEfficiency
    Evidence: .omo/evidence/task-6-weekly.json

  Scenario: 月统计 API 验证
    Tool: Bash (curl)
    Steps:
      1. curl -s "http://localhost:8080/api/arrived/statistics?period=month"
    Expected Result: HTTP 200，JSON 数组，每项包含 period, arrivalCount, avgEfficiency
    Evidence: .omo/evidence/task-6-monthly.json
  ```

  **Commit**: YES (groups with Task 5)

- [x] 7. **ArrivedDashboard.vue 骨架 + 数据加载**

  **What to do**:
  - 新建 `ArrivedDashboard.vue`（参考现有 `Dashboard.vue` 的结构）
  - 实现 `el-tabs` 3 个 tab：分段监控 / 三段监控 / 整段监控
  - 实现公共数据加载逻辑：
    - `loadSummary()` → 调用 `/api/arrived/summary`
    - `loadChartData(type)` → 调用 `/api/arrived/chart?type=xxx`
    - `loadStatistics(period)` → 调用 `/api/arrived/statistics?period=xxx`
  - 实现公共 UI 元素：
    - 日期范围选择器（与 Dashboard.vue 一致）
    - 品牌筛选下拉框
    - 周/月切换按钮（用于统计面板）
    - 3 个 Summary Cards（高效 / 正常 / 延迟）
    - Skeleton 加载状态
  - 使用 `ref` 和 `computed` 管理状态
  - 预留子组件插槽位置（ArrivedOverallTab, ArrivedSegmentTab, ArrivedThreeSectionTab, ArrivedStatisticsPanel）

  **Must NOT do**:
  - 不要复制 Dashboard.vue 的图表组件代码（使用子组件）
  - 不要修改 Dashboard.vue

  **Recommended Agent Profile**:
  - **Category**: `visual-engineering`

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 3 (with Tasks 8, 9, 10, 11)
  - **Blocks**: Tasks 8, 9, 10, 11, 14
  - **Blocked By**: Task 4, Task 5

  **References**:
  - `Dashboard.vue` — 参考 tab 结构、数据加载模式、日期选择器、品牌筛选、summary cards 布局
  - `request.js` — API 请求封装

  **Acceptance Criteria**:
  - [ ] `ArrivedDashboard.vue` 创建且编译通过（npm run build）
  - [ ] 页面有 3 个 tab 可切换
  - [ ] 日期选择器、品牌筛选、周/月切换按钮可见
  - [ ] Summary Cards 显示高效/正常/延迟计数
  - [ ] 数据加载时有 skeleton 状态

  **QA Scenarios**:
  ```
  Scenario: 页面骨架渲染
    Tool: Browser (手动)
    Preconditions: 前端 dev server 运行
    Steps:
      1. 访问 http://localhost:5173/dashboard/arrived
    Expected Result: 页面加载，显示 3 个 tab、日期选择器、品牌筛选、3 个 summary cards
    Evidence: .omo/evidence/task-7-skeleton.png
  ```

  **Commit**: YES
  - Message: `feat(arrived): add ArrivedDashboard.vue skeleton with data loading`

- [x] 8. **ArrivedOverallTab.vue（整段延迟分布）**

  **What to do**:
  - 新建 `ArrivedOverallTab.vue`
  - 展示品牌 × 效率桶（高效/正常/延迟）的堆叠柱状图
  - 复用现有的 `StackedBarChart.vue` 组件（或参考其实现）
  - 展示效率桶分布的饼图（各桶占比）
  - 颜色映射：高效=绿色，正常=黄色，延迟=红色
  - 当品牌数量多时可横向滚动

  **Must NOT do**:
  - 不要引入新的图表库（复用 ECharts）

  **Recommended Agent Profile**:
  - **Category**: `visual-engineering`

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 3
  - **Blocks**: Task 14
  - **Blocked By**: Task 7

  **References**:
  - `Dashboard.vue` overall tab 部分 — 参考图表配置
  - `StackedBarChart.vue` — 复用或参考此组件
  - `StatusPieChart.vue` — 参考饼图实现

  **Acceptance Criteria**:
  - [ ] 堆叠柱状图正确展示各品牌的 高效/正常/延迟 数量
  - [ ] 饼图展示整体分布比例
  - [ ] 颜色正确（绿/黄/红）

  **QA Scenarios**:
  ```
  Scenario: 整段图表渲染
    Tool: Browser (手动)
    Steps:
      1. 访问 /dashboard/arrived
      2. 切换到"整段监控"tab
    Expected Result: 显示堆叠柱状图和饼图，数据正确，颜色正确
    Evidence: .omo/evidence/task-8-overall-chart.png
  ```

  **Commit**: YES (groups with Wave 3)

- [x] 9. **ArrivedSegmentTab.vue（分段延迟分布）**

  **What to do**:
  - 新建 `ArrivedSegmentTab.vue`
  - 展示 7 个分段（未出库/集港在途/.../分拨在途）× 效率桶 的堆叠柱状图
  - 注意：不包含 ARRIVED 分段（因为只统计 ARRIVED 车辆在各分段的效率）
  - 展示各分段效率分布的饼图或条形图
  - 对于缺失时间戳导致无法计算效率的分段，显示 "N/A" 或跳过

  **Must NOT do**:
  - 不要显示 ARRIVED 作为一个分段（ARRIVED 是终点状态，不是运输分段）

  **Recommended Agent Profile**:
  - **Category**: `visual-engineering`

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 3
  - **Blocks**: Task 14
  - **Blocked By**: Task 7

  **References**:
  - `Dashboard.vue` segment tab 部分
  - `StatusCalculator.java` — 查看 7 个分段定义

  **Acceptance Criteria**:
  - [ ] 展示 7 个分段的延迟分布
  - [ ] 不包含 ARRIVED 作为分段
  - [ ] 图表数据与后端 API 返回一致

  **QA Scenarios**:
  ```
  Scenario: 分段图表渲染
    Tool: Browser (手动)
    Steps:
      1. 访问 /dashboard/arrived
      2. 切换到"分段监控"tab
    Expected Result: 显示 7 个分段的堆叠柱状图，无 ARRIVED 分段
    Evidence: .omo/evidence/task-9-segment-chart.png
  ```

  **Commit**: YES (groups with Wave 3)

- [x] 10. **ArrivedThreeSectionTab.vue（三段延迟分布 + drilldown）**

  **What to do**:
  - 新建 `ArrivedThreeSectionTab.vue`
  - 展示三段（前段/中段/后段）× 效率桶 的堆叠柱状图
  - 实现 drilldown：点击某一段（如前段）→ 展示该段内各品牌的效率分布
  - 提供"返回"按钮从 drilldown 回到三段视图
  - 颜色映射：高效=绿色，正常=黄色，延迟=红色

  **Must NOT do**:
  - 不要修改 Dashboard.vue 的三段 tab 逻辑

  **Recommended Agent Profile**:
  - **Category**: `visual-engineering`

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 3
  - **Blocks**: Task 14
  - **Blocked By**: Task 7

  **References**:
  - `Dashboard.vue` three-section tab 部分（lines 334-513）— 参考 drilldown 实现
  - `Dashboard.vue` `handleSectionBarClick` 和 `sectionDrillDown` 逻辑

  **Acceptance Criteria**:
  - [ ] 三段堆叠柱状图正确展示
  - [ ] 点击某段可 drilldown 到品牌分布
  - [ ] 返回按钮正常工作

  **QA Scenarios**:
  ```
  Scenario: 三段图表和钻取
    Tool: Browser (手动)
    Steps:
      1. 访问 /dashboard/arrived
      2. 切换到"三段监控"tab
      3. 点击"前段"柱状图
    Expected Result: 进入品牌钻取视图，显示各品牌在前段的效率分布
      4. 点击"返回"
    Expected Result: 回到三段视图
    Evidence: .omo/evidence/task-10-three-section.png
  ```

  **Commit**: YES (groups with Wave 3)

- [x] 11. **ArrivedStatisticsPanel.vue（周/月统计）**

  **What to do**:
  - 新建 `ArrivedStatisticsPanel.vue`
  - 展示按周或按月的到店数量和平均效率趋势图（折线图或柱状图组合）
  - 周/月切换按钮控制数据粒度
  - 与 `ArrivedDashboard.vue` 中的周/月切换按钮联动
  - 支持品牌筛选（与全局品牌筛选联动）
  - 展示平均效率的趋势线（参考线 80% 和 100%）

  **Must NOT do**:
  - 不要在前端做周/月分组计算（数据来自后端）

  **Recommended Agent Profile**:
  - **Category**: `visual-engineering`

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 3
  - **Blocks**: Task 14
  - **Blocked By**: Task 7

  **References**:
  - `Dashboard.vue` 中的图表配置
  - ECharts 折线图/柱状图组合图配置

  **Acceptance Criteria**:
  - [ ] 折线图展示周/月到店数量趋势
  - [ ] 趋势线展示平均效率变化
  - [ ] 参考线 80% 和 100% 可见
  - [ ] 周/月切换正常工作

  **QA Scenarios**:
  ```
  Scenario: 周/月统计面板
    Tool: Browser (手动)
    Steps:
      1. 访问 /dashboard/arrived
      2. 点击"周"按钮
    Expected Result: 显示周统计图表（到店数量 + 平均效率）
      3. 点击"月"按钮
    Expected Result: 切换为月统计图表
    Evidence: .omo/evidence/task-11-statistics.png
  ```

  **Commit**: YES (groups with Wave 3)

- [x] 12. **JUnit 边界情况测试补充**

  **What to do**:
  - 补充 `ArrivedEfficiencyCalculatorTest.java` 的边界测试：
    - 效率 = 79.9% → EFFICIENT
    - 效率 = 80% → NORMAL
    - 效率 = 80.1% → NORMAL
    - 效率 = 99.9% → NORMAL
    - 效率 = 100% → DELAYED
    - 效率 = 100.1% → DELAYED
    - orderReleaseTime = null → null
    - arriveShopTime = null → null
    - RouteOtdConfig = null → null
    - 某分段缺少起始时间戳 → 该分段效率 = null
    - 空车辆列表 → summary 全为 0
  - 确保所有测试在 `mvn test` 中通过

  **Must NOT do**:
  - 不要只测试正常路径

  **Recommended Agent Profile**:
  - **Category**: `quick`

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 4
  - **Blocks**: F2
  - **Blocked By**: Task 1

  **References**:
  - `ArrivedEfficiencyCalculator.java` — 测试此类的所有方法
  - `StatusCalculatorTest.java` — 参考测试模式

  **Acceptance Criteria**:
  - [ ] 边界测试用例 ≥ 10 个
  - [ ] `mvn test -Dtest=ArrivedEfficiencyCalculatorTest` 全部 PASS

  **QA Scenarios**:
  ```
  Scenario: 边界测试验证
    Tool: Bash (mvn test)
    Steps:
      1. cd ro-ro-monitor && mvn test -Dtest=ArrivedEfficiencyCalculatorTest
    Expected Result: 所有测试 PASS
    Evidence: .omo/evidence/task-12-junit-boundary.txt
  ```

  **Commit**: YES
  - Message: `test(arrived): add boundary tests for efficiency calculator`

- [x] 13. **API curl 验证**

  **What to do**:
  - 启动后端服务
  - 使用 curl 验证所有 ArrivedController API 端点：
    - `GET /api/arrived/summary`
    - `GET /api/arrived/summary?brandId=1`
    - `GET /api/arrived/chart?type=overall`
    - `GET /api/arrived/chart?type=segment`
    - `GET /api/arrived/chart?type=three-section`
    - `GET /api/arrived/chart?type=three-section&sectionName=前段`
    - `GET /api/arrived/statistics?period=week`
    - `GET /api/arrived/statistics?period=month`
  - 验证返回 JSON 结构正确、包含预期字段
  - 验证错误处理：无效参数返回 400 或合适的状态码
  - 保存所有响应到 `.omo/evidence/`

  **Must NOT do**:
  - 不要假设数据库一定有数据（空数据应返回空数组）

  **Recommended Agent Profile**:
  - **Category**: `unspecified-high`

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 4 (with Tasks 12, 14)
  - **Blocks**: F3
  - **Blocked By**: Tasks 5, 6, 4

  **References**:
  - `ArrivedController.java` — 查看所有端点定义

  **Acceptance Criteria**:
  - [ ] 所有 8 个 API 端点 curl 测试通过
  - [ ] 返回 JSON 包含预期字段
  - [ ] 空数据时返回空数组（HTTP 200）

  **QA Scenarios**:
  ```
  Scenario: API 端点验证
    Tool: Bash (curl)
    Preconditions: 后端运行中
    Steps:
      1. 对每个端点执行 curl 请求
      2. 检查 HTTP 状态码和 JSON 结构
    Expected Result: 所有端点返回 HTTP 200 + 正确 JSON
    Evidence: .omo/evidence/task-13-api-*.json
  ```

  **Commit**: YES (groups with Wave 4)

- [x] 14. **前端手动验证清单执行**

  **What to do**:
  - 启动前端 dev server (`npm run dev`)
  - 执行以下验证：
    1. 访问 `/dashboard` → 重定向到 `/dashboard/in-transit`
    2. 在途监控页面正常显示，3 个 tab 正常工作
    3. 左侧菜单显示 "车辆监控" 父菜单，下有 "在途车辆" 和 "到达车辆"
    4. 点击 "到达车辆" → 跳转到 `/dashboard/arrived`
    5. 到达监控页面加载，显示 3 个 tab
    6. "整段监控" tab：显示堆叠柱状图和饼图，颜色正确
    7. "分段监控" tab：显示 7 个分段的图表
    8. "三段监控" tab：显示三段图表，可 drilldown 到品牌
    9. 周/月统计面板：切换正常，图表显示趋势
    10. 日期选择器：选择不同范围，数据更新
    11. 品牌筛选：选择不同品牌，数据过滤
    12. 浏览器控制台无报错
  - 截图保存到 `.omo/evidence/`

  **Must NOT do**:
  - 不要只验证一个 tab
  - 不要忽略浏览器控制台报错

  **Recommended Agent Profile**:
  - **Category**: `unspecified-high`

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 4
  - **Blocks**: F3
  - **Blocked By**: Tasks 7, 8, 9, 10, 11

  **References**:
  - `ArrivedDashboard.vue` — 验证此页面的所有功能

  **Acceptance Criteria**:
  - [ ] 12 项验证全部通过
  - [ ] 浏览器控制台无报错
  - [ ] 所有截图保存到 `.omo/evidence/`

  **QA Scenarios**:
  ```
  Scenario: 完整前端验证
    Tool: Browser (手动)
    Steps:
      1. 访问 /dashboard，验证重定向
      2. 在途页面功能验证
      3. 菜单结构验证
      4. 到达页面功能验证（3 tabs + statistics + filters）
    Expected Result: 所有功能正常，控制台无报错
    Evidence: .omo/evidence/task-14-*.png
  ```

  **Commit**: NO (只验证，不改代码)

---

## Final Verification Wave

- [x] F1. **Plan Compliance Audit** — `oracle`
  Read the plan end-to-end. For each "Must Have": verify implementation exists. For each "Must NOT Have": search codebase for forbidden patterns. Check evidence files exist in `.omo/evidence/`.
  Output: `Must Have [N/N] | Must NOT Have [N/N] | Tasks [N/N] | VERDICT: APPROVE/REJECT`

- [x] F2. **Code Quality Review** — `unspecified-high`
  Run `mvn test` + check for compilation errors. Review all changed files for: `as any`/`@ts-ignore`, empty catches, console.log in prod, commented-out code, unused imports.
  Output: `Build [PASS/FAIL] | Tests [N pass/N fail] | Files [N clean/N issues] | VERDICT`

- [x] F3. **Real Manual QA** — `unspecified-high`
  Start from clean state. Execute EVERY QA scenario from EVERY task — follow exact steps, capture evidence. Test cross-task integration.
  Output: `Scenarios [N/N pass] | Integration [N/N] | VERDICT`

- [x] F4. **Scope Fidelity Check** — `deep`
  For each task: read "What to do", read actual diff. Verify 1:1 — everything in spec was built, nothing beyond spec was built.
  Output: `Tasks [N/N compliant] | Contamination [CLEAN/N issues] | VERDICT`

---

## Commit Strategy

- **Wave 1**: `feat(arrived): add efficiency calculator, DTOs, service, routing`
- **Wave 2**: `feat(arrived): add controller APIs for summary, chart, statistics`
- **Wave 3**: `feat(arrived): add ArrivedDashboard with tabs and statistics panel`
- **Wave 4**: `test(arrived): add JUnit tests and verify APIs`

## Success Criteria

### Verification Commands
```bash
# Backend tests
cd ro-ro-monitor && mvn test

# API verification (examples)
curl -s "http://localhost:8080/api/arrived/summary"
curl -s "http://localhost:8080/api/arrived/chart?type=overall"
curl -s "http://localhost:8080/api/arrived/statistics?period=week"
```

### Final Checklist
- [ ] All "Must Have" present
- [ ] All "Must NOT Have" absent
- [ ] JUnit tests pass
- [ ] API endpoints return correct data
- [ ] Frontend page renders correctly with all tabs
- [ ] Menu navigation works
- [ ] `/dashboard` redirects to `/dashboard/in-transit`
