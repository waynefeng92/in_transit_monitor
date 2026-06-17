# Frontend Beautification — 在途监控系统前端美化

## TL;DR

> **Quick Summary**: 全面美化 Vue 3 前端：拆分巨型 Dashboard 组件、CSS 设计 token 化、统一卡片风格、美化管理页面、响应式侧边栏、ECharts 主题、vitest 测试搭建
>
> **Deliverables**: 12 个实现任务 + 4 个最终验证任务
> - 重构 style.css 设计 token 体系
> - Dashboard.vue 拆分为 3 个子组件
> - 6 个管理页面统一美化
> - 响应式侧边栏 + ECharts 主题
> - vitest 测试基础设施
>
> **Estimated Effort**: Medium
> **Parallel Execution**: YES — 4 波次，每波 3-4 个任务并行
> **Critical Path**: Wave 1 → Wave 2 → Wave 3 → Wave 4 → FINAL

---

## Context

### Original Request
对前端网页进行美化

### Interview Summary
**Key Discussions**:
- 风格方向：先看审计报告再决定 → 基于审计结果，全面修复所有问题
- 改动范围：全面整改（🔴+🟡+🟢 所有优先级）+ 管理页面与仪表盘同一水准
- 测试策略：搭建 vitest + Playwright 视觉 QA
- 可用工具：frontend-design skill 已安装

**Research Findings** (from explore agents):
- **结构**: 8 页面 + 6 图表组件 + 深色侧边栏布局 + 364 行 style.css
- **优点**: CSS 变量体系已建立、仪表盘卡片有渐变/hover动画、骨骼屏加载、语义化状态色
- **🔴 严重问题**: Dashboard.vue 1432行臃肿、大量硬编码颜色、毛玻璃图表卡片与实色面板风格冲突
- **🟡 中等问题**: 管理页面极度简陋、侧边栏不响应式、CSS 重复、border-radius 魔法数字
- **🟢 轻微问题**: ECharts 无主题、缺少排版 token、死代码、摘要网格列数不一致

---

## Work Objectives

### Core Objective
统一前端视觉语言，将现有"仪表盘精致 + 管理页简陋"的两极分化状态，改造为视觉一致、设计 token 驱动的专业监控系统界面。

### Concrete Deliverables
- 重构后的 `style.css`（完整 CSS 变量体系：排版、间距、圆角、阴影）
- `Dashboard.vue` 拆分为 `SegmentTab.vue` + `OverallTab.vue` + `ThreeSectionTab.vue`
- 6 个管理页面（BrandManage/PortManage/RouteManage/ExcelMapping/OtdConfig/Upload）统一美化
- `echarts-theme.js`（匹配蓝色主调的 ECharts 主题）
- 响应式侧边栏（折叠/Hamburger）
- vitest 配置 + 至少 2 个示例组件测试
- 全局删除死代码 + CSS 重复

### Definition of Done
- [ ] `npm run build` → 无错误
- [ ] vitest 用例通过
- [ ] Playwright 截图对比：管理页面与仪表盘视觉一致
- [ ] 侧边栏 < 768px 时自动折叠为 Hamburger
- [ ] style.css 中所有硬编码颜色被 CSS 变量替代
- [ ] 无 `dashboard-panel-chart` 等死类残留

### Must Have
- Dashboard.vue 拆分（降低文件复杂度）
- 硬编码颜色 → CSS 变量（可维护性）
- 管理页面统一卡片/表头/筛选栏样式
- 响应式侧边栏
- vitest 基础配置 + 示例测试
- 前端构建通过

### Must NOT Have (Guardrails)
- 不修改后端 API 路径或数据结构
- 不修改 ECharts 数据逻辑（只改主题/外观）
- 不动业务功能（上传、数据导入等核心逻辑）
- 不使用 CSS-in-JS 方案（保持现有 style.css + scoped style 模式）
- 不引入新的 UI 框架（继续用 Element Plus）
- 不生成无业务意义的装饰性动画

---

## Verification Strategy

### Test Decision
- **Infrastructure exists**: NO（新建 vitest）
- **Automated tests**: 搭建 vitest + 2 个示例组件测试
- **Framework**: vitest + @vue/test-utils
- **Agent-Executed QA**: MANDATORY — Playwright 截图对比 + 前端 build 验证

### QA Policy
- **Frontend/UI**: Playwright 打开页面 → 截图 → 对比设计规范（卡片圆角、间距、配色）
- **Build**: `npm run build` → 无报错无警告（除已知 ECharts chunk size）
- **Test**: `npx vitest run` → 全部通过

---

## Execution Strategy

### Parallel Execution Waves

```
Wave 1 (Start Immediately — foundation + cleanup):
├── Task 1: vitest setup + 2 component tests [quick]
├── Task 2: Expand CSS design tokens [quick]
├── Task 3: ECharts theme configuration [quick]
└── Task 4: Dead class cleanup + CSS dedup [quick]

Wave 2 (After Wave 1 — core refactoring, MAX PARALLEL):
├── Task 5: Split Dashboard.vue into sub-components [deep]
├── Task 6: Replace hardcoded colors with CSS variables [deep]
└── Task 7: Unify card styling (glassmorphism → gradient) [visual-engineering]

Wave 3 (After Wave 2 — page-level beautification, MAX PARALLEL):
├── Task 8: Beautify management pages: Brand + Port + Route [visual-engineering]
├── Task 9: Beautify management pages: ExcelMapping + OtdConfig + Upload [visual-engineering]
└── Task 10: Responsive sidebar + header polish [visual-engineering]

Wave 4 (After Wave 3 — final consistency):
└── Task 11: ArrivedDashboard consistency + full build verification [visual-engineering]

Wave FINAL (After ALL tasks — 4 parallel reviews, then user okay):
├── Task F1: Plan compliance audit (oracle)
├── Task F2: Code quality review (unspecified-high)
├── Task F3: Visual QA — Playwright screenshots (unspecified-high + playwright)
└── Task F4: Scope fidelity check (deep)
```

**Critical Path**: Task 1 → Task 5 → Task 8 → Task 11 → F1-F4
**Parallel Speedup**: ~60% faster than sequential
**Max Concurrent**: 4 (Wave 1 & 2), 3 (Wave 3)

---

## TODOs

### Wave 1 — Foundation & Cleanup (all independent, start immediately)

- [x] 1. Set up vitest test infrastructure + write 2 example component tests

  **What to do**:
  - Install vitest, @vue/test-utils, jsdom: `npm install -D vitest @vue/test-utils jsdom @vitejs/plugin-vue`
  - Create `vitest.config.js` alongside `vite.config.js`
  - Add `"test": "vitest run"` to package.json scripts
  - Create `src/__tests__/` directory
  - Write `StatusPieChart.test.js` — test component renders, receives props, computes status colors correctly
  - Write `style-tokens.test.js` — test that CSS variables are defined (read computed styles from a test component)

  **Must NOT do**:
  - Don't write tests for business logic (APIs, ECharts internals)
  - Don't configure coverage thresholds (add `"coverage": "vitest run --coverage"` later)

  **Recommended Agent Profile**:
  - **Category**: `quick`
  - **Skills**: none needed (standard vitest setup)
  - **Parallelization**:
    - Can Run In Parallel: YES (Wave 1, with Tasks 2, 3, 4)
    - Blocks: Task 5 (vitest needed for refactored component tests)

  **QA Scenarios**:
  ```
  Scenario: vitest runs and passes all tests
    Tool: Bash
    Steps:
      1. cd ro-ro-monitor-web && npx vitest run
    Expected Result: All tests pass, exit code 0
    Evidence: .omo/evidence/task-1-vitest-output.txt
  ```

- [x] 2. Expand CSS design tokens in style.css

  **What to do**:
  - Add `--font-size-*` scale: xs=12px, sm=13px, base=15px, md=17px, lg=20px, xl=24px, xxl=44px
  - Add `--spacing-*` scale: xs=4px, sm=8px, md=16px, lg=24px, xl=32px, xxl=48px
  - Add `--chart-*` tokens for ECharts colors (match the semantic status palette)
  - Ensure `--radius-sm`, `--radius-md`, `--radius-lg`, `--radius-xl` are used consistently
  - Add `--transition-*` tokens (fast=0.15s, normal=0.3s, slow=0.5s)

  **Must NOT do**:
  - Don't remove existing tokens
  - Don't change color values (only add missing tokens)

  **Recommended Agent Profile**:
  - **Category**: `quick`
  - **Skills**: `frontend-design`

  **Parallelization**:
    - Can Run In Parallel: YES (Wave 1)
    - Blocks: Task 6, Task 7 (color/radius replacement relies on tokens)

  **QA Scenarios**:
  ```
  Scenario: CSS variables are defined and accessible
    Tool: Bash
    Steps:
      1. grep "font-size" ro-ro-monitor-web/src/style.css | wc -l
      2. grep "spacing" ro-ro-monitor-web/src/style.css | wc -l
    Expected Result: At least 6 font-size tokens and 6 spacing tokens defined
    Evidence: .omo/evidence/task-2-token-count.txt
  ```

- [x] 3. Create ECharts theme configuration

  **What to do**:
  - Create `src/utils/echarts-theme.js`
  - Export a theme object matching the project's blue palette: `#1d72f3` primary, semantic greens/yellows/reds
  - Register via `echarts.registerTheme('roro', theme)`
  - Apply to all chart components: `init(dom, 'roro')` replacing default `init(dom)`
  - Theme should include: color palette, textStyle, title, legend, tooltip, grid config

  **Must NOT do**:
  - Don't change chart data logic or option computation
  - Don't remove existing chart features (dataZoom, tooltip, click handlers)

  **Recommended Agent Profile**:
  - **Category**: `quick`
  - **Skills**: none needed (standard ECharts config)

  **Parallelization**:
    - Can Run In Parallel: YES (Wave 1)
    - Blocks: None (cosmetic change, independent)

  **QA Scenarios**:
  ```
  Scenario: ECharts theme is applied to StackedBarChart
    Tool: Bash (grep)
    Steps:
      1. grep -r "registerTheme" ro-ro-monitor-web/src/
      2. grep -r "echarts.init.*roro" ro-ro-monitor-web/src/components/
    Expected Result: registerTheme called once in setup, theme 'roro' used in all init() calls
    Evidence: .omo/evidence/task-3-echarts-theme.txt
  ```

- [x] 4. Clean up dead CSS classes and remove duplication

  **What to do**:
  - Search for `dashboard-panel-chart` usage — if dead, remove from Dashboard.vue scoped styles
  - Search for `tab-view` class — if undefined in global CSS, add to style.css or remove
  - Remove scoped style blocks in Dashboard.vue that duplicate `style.css` (`.summary-card`, `.chart-layout`, `.card-header`, etc.)
  - Consolidate `.panel-title` overrides — remove inline `style="font-size: 16px;"` from line 476
  - Replace magic number border-radius values (`18px`, `20px`, `22px`) with `var(--radius-md)`, `var(--radius-lg)`, `var(--radius-xl)`

  **Must NOT do**:
  - Don't remove styles that are actually used (test with build first)
  - Don't change visual appearance (only consolidate references)

  **Recommended Agent Profile**:
  - **Category**: `quick`

  **Parallelization**:
    - Can Run In Parallel: YES (Wave 1)
    - Blocks: Task 5 (clean Dashboard.vue before splitting)

  **QA Scenarios**:
  ```
  Scenario: Build succeeds after CSS cleanup
    Tool: Bash
    Steps:
      1. cd ro-ro-monitor-web && npm run build
    Expected Result: Build succeeds, no CSS reference errors
    Evidence: .omo/evidence/task-4-build.txt

  Scenario: No dead class references remain
    Tool: Bash (grep)
    Steps:
      1. grep -r "dashboard-panel-chart" ro-ro-monitor-web/src/
      2. grep -r "tab-view" ro-ro-monitor-web/src/views/ --include="*.vue" | grep -v "class="
    Expected Result: No dead class usage found, or tab-view defined in style.css
    Evidence: .omo/evidence/task-4-dead-classes.txt
  ```

### Wave 2 — Core Refactoring (parallel after Wave 1)

- [x] 5. Split Dashboard.vue into sub-components

  **What to do**:
  - Read current `Dashboard.vue` (1432 lines) thoroughly
  - Create `src/components/dashboard/SegmentTab.vue` — 分段监控 tab content (~250 lines)
  - Create `src/components/dashboard/OverallTab.vue` — 整段监控 tab content (~200 lines)
  - Create `src/components/dashboard/ThreeSectionTab.vue` — 三段监控 tab content (~250 lines)
  - Keep `Dashboard.vue` as shell: imports tab components, contains tab switching logic, filter bar, time selector, animateNumbers function
  - Target: reduce Dashboard.vue to ~400 lines
  - Each sub-component receives props (chart data, loading state, drilldown handlers)
  - Components emit events for drilldown/brand-level navigation

  **Must NOT do**:
  - Don't change any data fetching logic
  - Don't refactor ECharts option building (that's Task 6 territory)
  - Don't break existing tab switching or drilldown behavior

  **Recommended Agent Profile**:
  - **Category**: `deep`
  - **Skills**: `frontend-design`

  **Parallelization**:
    - Can Run In Parallel: YES (Wave 2, with Tasks 6, 7)
    - Blocked By: Task 1 (vitest for verifying), Task 4 (clean Dashboard.vue before split)
    - Blocks: Task 8, Task 9 (management pages reference dashboard patterns)

  **QA Scenarios**:
  ```
  Scenario: All three tabs render correctly after split
    Tool: Bash (build + grep)
    Steps:
      1. cd ro-ro-monitor-web && npm run build
      2. ls src/components/dashboard/SegmentTab.vue
      3. ls src/components/dashboard/OverallTab.vue
      4. ls src/components/dashboard/ThreeSectionTab.vue
      5. wc -l src/views/Dashboard.vue
    Expected Result: Build succeeds, all 3 files exist, Dashboard.vue < 500 lines
    Evidence: .omo/evidence/task-5-build-lines.txt

  Scenario: Vitest import check for new components
    Tool: Bash (vitest)
    Steps:
      1. npx vitest run
    Expected Result: All tests pass (including any new ones for sub-components)
    Evidence: .omo/evidence/task-5-vitest.txt
  ```

- [x] 6. Replace hardcoded colors with CSS variables

  **What to do**:
  - Audit ALL files under `src/views/` and `src/components/` for hardcoded hex colors
  - Replace with CSS variable references:
    - `#dbe7f5` → `var(--color-primary-lightest)` (add to tokens)
    - `#e9f0f8` → `var(--color-primary-bg)` (add to tokens)
    - `#13233c` → `var(--text-primary)`
    - `#71839c` → `var(--text-muted)`
    - `#2d78d6` → `var(--color-primary-dark)`
    - `#1d72f3` → `var(--color-primary)`
    - `##67c23a` → `var(--color-success)`
    - `#f56c6c` → `var(--color-danger)`
    - `#e6a23c` → `var(--color-warning)`
  - Add missing tokens to style.css `:root` (primary-lightest, primary-bg, etc.)
  - Focus on: Dashboard.vue sub-components, ArrivedDashboard.vue, chart components

  **Must NOT do**:
  - Don't change the color values themselves — only the reference method
  - Don't touch Element Plus default colors in node_modules

  **Recommended Agent Profile**:
  - **Category**: `deep` (many files to audit)
  - **Skills**: none needed (find-and-replace with token mapping)

  **Parallelization**:
    - Can Run In Parallel: YES (Wave 2)
    - Blocked By: Task 2 (need expanded tokens), Task 5 (refactored file structure)
    - Blocks: None

  **QA Scenarios**:
  ```
  Scenario: No hardcoded hex colors remain in component files
    Tool: Bash (grep)
    Steps:
      1. grep -rn "#[0-9a-fA-F]{6}" ro-ro-monitor-web/src/views/ --include="*.vue" | grep -v "//\|/\*"
      2. grep -rn "#[0-9a-fA-F]{6}" ro-ro-monitor-web/src/components/ --include="*.vue" | grep -v "//\|/\*"
    Expected Result: Only CSS variable references found, no raw hex values
    Evidence: .omo/evidence/task-6-no-hardcoded-colors.txt

  Scenario: Build succeeds with token references
    Tool: Bash
    Steps:
      1. cd ro-ro-monitor-web && npm run build
    Expected Result: Build passes, no undefined variable warnings
    Evidence: .omo/evidence/task-6-build.txt
  ```

- [x] 7. Unify card styling — replace glassmorphism with solid gradient

  **What to do**:
  - Find all `background: rgba(255, 255, 255, 0.72)` and similar glassmorphism backgrounds
  - Replace with `var(--card-gradient)` (defined in style.css as `linear-gradient(135deg, #ffffff 0%, rgba(29, 114, 243, 0.03) 100%)`)
  - Affected: chart cards in Dashboard sub-components, ArrivedDashboard chart cards
  - Ensure chart cards use same border-radius (`var(--radius-xl)` = 22px) and shadow as summary cards
  - Add subtle top border accent: `border-top: 3px solid var(--color-primary)` on chart cards (optional visual polish)

  **Must NOT do**:
  - Don't change summary card styling (they already look good)
  - Don't remove chart functionality

  **Recommended Agent Profile**:
  - **Category**: `visual-engineering`
  - **Skills**: `frontend-design`

  **Parallelization**:
    - Can Run In Parallel: YES (Wave 2)
    - Blocked By: Task 5 (refactored file structure)
    - Blocks: None

  **QA Scenarios**:
  ```
  Scenario: No glassmorphism backgrounds remain
    Tool: Bash (grep)
    Steps:
      1. grep -rn "rgba(255.*255.*255" ro-ro-monitor-web/src/views/ ro-ro-monitor-web/src/components/ --include="*.vue"
    Expected Result: No matches (all replaced with CSS variables or solid/gradient backgrounds)
    Evidence: .omo/evidence/task-7-no-glassmorphism.txt
  ```

### Wave 3 — Page-Level Beautification (parallel after Wave 2)

- [x] 8. Beautify management pages: BrandManage + PortManage + RouteManage

  **What to do**:
  - Apply consistent card header pattern: `.card-header` class with flex layout (title left + actions right)
  - Add `.filter-bar` with search input + action buttons (Brand: WMI code search, Port: name search, Route: brand filter)
  - Apply custom table styling (`--el-table-header-bg-color`, row hover color) matching dashboard style
  - Use `var(--color-primary)` for primary action buttons, `var(--radius-lg)` for card border-radius
  - Add empty state with `el-empty` for tables with no data
  - BrandManage: add brand count summary badge
  - PortManage: add port count summary badge
  - RouteManage: bulk import dialog polish (step indicators, progress visualization)

  **Must NOT do**:
  - Don't change CRUD API calls or business logic
  - Don't restructure dialog forms (only visual polish)

  **Recommended Agent Profile**:
  - **Category**: `visual-engineering`
  - **Skills**: `frontend-design`

  **Parallelization**:
    - Can Run In Parallel: YES (Wave 3, with Tasks 9, 10)
    - Blocked By: Task 5, Task 7 (need dashboard patterns as reference)
    - Blocks: Task 11

  **QA Scenarios**:
  ```
  Scenario: BrandManage page has card-header with title and action button
    Tool: Playwright
    Preconditions: Backend running on localhost:8080
    Steps:
      1. Navigate to http://localhost:5173/brand
      2. Wait for selector: `.card-header`
      3. Assert: card-header contains text "品牌管理"
      4. Assert: card-header contains a primary button (新增品牌)
      5. Screenshot
    Expected Result: Card header visible with title and add button, table styled consistently
    Evidence: .omo/evidence/task-8-brand-manage.png

  Scenario: PortManage page matches BrandManage visual style
    Tool: Playwright
    Steps:
      1. Navigate to http://localhost:5173/port
      2. Wait for selector: `.card-header`
      3. Screenshot
    Expected Result: Same card header pattern, table styling, and spacing as BrandManage
    Evidence: .omo/evidence/task-8-port-manage.png
  ```

- [x] 9. Beautify management pages: ExcelMapping + OtdConfig + Upload

  **What to do**:
  - ExcelMapping: brand selector styled as card header filter, mapping table with colored field type badges
  - OtdConfig: 7-segment OTD form with visual grouping (card per segment group), color-coded OTD vs warning fields
  - Upload: drag-drop zone with gradient border, upload progress bar with status colors, history table with status tags
  - All pages: apply same `.card-header` + `.filter-bar` pattern from Task 8
  - All pages: table styling consistent with dashboard tables

  **Must NOT do**:
  - Don't change upload logic, Excel parsing, or OTD config save behavior

  **Recommended Agent Profile**:
  - **Category**: `visual-engineering`
  - **Skills**: `frontend-design`

  **Parallelization**:
    - Can Run In Parallel: YES (Wave 3)
    - Blocked By: Task 5, Task 7
    - Blocks: Task 11

  **QA Scenarios**:
  ```
  Scenario: Upload page drag-drop zone is visually polished
    Tool: Playwright
    Steps:
      1. Navigate to http://localhost:5173/upload
      2. Wait for selector: `.el-upload-dragger`
      3. Assert: drag zone has visible border, icon, and text
      4. Screenshot
    Expected Result: Polished upload zone with gradient/accent border
    Evidence: .omo/evidence/task-9-upload.png

  Scenario: OtdConfig form has visual grouping for 7 segments
    Tool: Playwright
    Steps:
      1. Navigate to http://localhost:5173/otd-config
      2. Wait for load
      3. Screenshot
    Expected Result: Segments visually grouped, OTD vs warning fields distinguishable
    Evidence: .omo/evidence/task-9-otd-config.png
  ```

- [x] 10. Responsive sidebar + header polish

  **What to do**:
  - Add `el-menu` collapse toggle: hamburger button in header
  - On collapse: sidebar shrinks to 64px (icon-only), expands on hover or click
  - At 768px breakpoint: sidebar becomes overlay drawer with backdrop
  - Header: add breadcrumb based on `$route.meta.title`, right-align user/settings area
  - Replace inline `background-color="#304156"` with `var(--sidebar-bg)` reference
  - Logo area: add subtle bottom border separator

  **Must NOT do**:
  - Don't remove any existing menu items
  - Don't change router behavior

  **Recommended Agent Profile**:
  - **Category**: `visual-engineering`
  - **Skills**: `frontend-design`

  **Parallelization**:
    - Can Run In Parallel: YES (Wave 3)
    - Blocked By: None (App.vue is independent)
    - Blocks: Task 11

  **QA Scenarios**:
  ```
  Scenario: Sidebar collapses on hamburger click
    Tool: Playwright (mobile viewport: 375x812)
    Steps:
      1. Resize viewport to 375x812
      2. Navigate to http://localhost:5173/
      3. Assert: sidebar is collapsed or hidden
      4. Click hamburger button
      5. Assert: sidebar drawer opens
      6. Screenshot
    Expected Result: Mobile hamburger menu works
    Evidence: .omo/evidence/task-10-mobile-sidebar.png

  Scenario: Desktop sidebar collapse expands on hover
    Tool: Playwright (desktop viewport: 1440x900)
    Steps:
      1. Navigate to http://localhost:5173/
      2. Click collapse toggle
      3. Assert: sidebar width ≈ 64px
      4. Screenshot as collapsed
      5. Hover over sidebar
      6. Assert: sidebar expands
    Expected Result: Collapse/expand animation smooth
    Evidence: .omo/evidence/task-10-desktop-collapse.png
  ```

### Wave 4 — Final Consistency

- [x] 11. ArrivedDashboard consistency fixes + full build verification

  **What to do**:
  - Align summary grid: 3 columns → 4 columns (add "总数" card matching Dashboard.vue pattern)
  - Replace any remaining hardcoded colors with CSS variables
  - Apply same card header pattern as management pages
  - `npm run build` — verify zero errors, only known ECharts chunk warning

  **Must NOT do**:
  - Don't change ArrivedDashboard data logic or chart calculations

  **Recommended Agent Profile**:
  - **Category**: `visual-engineering`
  - **Skills**: `frontend-design`

  **Parallelization**:
    - Can Run In Parallel: NO (sequential, must run after all others)
    - Blocked By: Task 8, Task 9, Task 10
    - Blocks: F1-F4

  **QA Scenarios**:
  ```
  Scenario: Both dashboards have consistent 4-column summary grids
    Tool: Bash (grep)
    Steps:
      1. grep -c "summary-card" ro-ro-monitor-web/src/views/Dashboard.vue
      2. grep -c "summary-card" ro-ro-monitor-web/src/views/ArrivedDashboard.vue
    Expected Result: Both dashboards show 4 summary cards
    Evidence: .omo/evidence/task-11-summary-count.txt

  Scenario: Production build succeeds
    Tool: Bash
    Steps:
      1. cd ro-ro-monitor-web && npm run build 2>&1
    Expected Result: Build succeeds, only ECharts chunk size warning
    Evidence: .omo/evidence/task-11-build.txt
  ```

---

## Final Verification Wave

- [x] F1. **Plan Compliance Audit** — `oracle`
  Read the plan end-to-end. For each Must Have: verify implementation exists. For each Must NOT Have: search codebase for violations. Check all evidence files exist.
  Output: `Must Have [N/N] | Must NOT Have [N/N] | Tasks [N/N] | VERDICT: APPROVE/REJECT`

- [x] F2. **Code Quality Review** — `unspecified-high`
  Run `npm run build`. Check for: unused imports, dead code, `!important` abuse, inline styles, console.log in production code.
  Output: `Build [PASS/FAIL] | Lint [N issues] | VERDICT`

- [x] F3. **Visual QA** — `unspecified-high` + `playwright`
  Start from clean state. Navigate to ALL 8 pages. Screenshot each at 1440px and 375px. Compare management pages against dashboard visual standard.
  Output: `Pages [8/8 rendered] | Mobile [N/N] | VERDICT`

- [x] F4. **Scope Fidelity Check** — `deep`
  Verify 1:1 — everything in spec was built, nothing beyond spec was built. Check Must NOT Do compliance across all tasks. Detect cross-task file contamination.
  Output: `Tasks [N/N compliant] | Contamination [CLEAN/N issues] | VERDICT`

---

## Commit Strategy

- **1-4**: `chore(frontend): set up vitest, expand CSS tokens, ECharts theme, cleanup dead classes` — style.css, vitest.config.js, echarts-theme.js, Dashboard.vue
- **5-7**: `refactor(frontend): split Dashboard, replace hardcoded colors, unify card styling` — dashboard/*.vue, all views/components
- **8-10**: `style(frontend): beautify management pages, responsive sidebar` — BrandManage.vue, PortManage.vue, RouteManage.vue, ExcelMapping.vue, OtdConfig.vue, Upload.vue, App.vue
- **11**: `style(frontend): ArrivedDashboard consistency, final build verification` — ArrivedDashboard.vue

---

## Success Criteria

### Verification Commands
```bash
cd ro-ro-monitor-web
npm run build          # Expected: BUILD SUCCESS (only ECharts chunk warning)
npx vitest run          # Expected: all tests pass
```

### Final Checklist
- [ ] All hardcoded hex colors replaced with CSS variables
- [ ] Dashboard.vue < 500 lines (split into 3 sub-components)
- [ ] Management pages visually consistent with dashboard pages
- [ ] Sidebar responsive (collapse + mobile drawer)
- [ ] ECharts theme applied to all charts
- [ ] vitest setup complete with 2+ passing tests
- [ ] No dead CSS classes or duplicate style definitions
- [ ] Build passes on first try after all changes
