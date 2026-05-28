# Git 初始化 — Monorepo 版本管理

## TL;DR

> **Quick Summary**: 清理当前 git 仓库的脏暂存区，删除嵌套子仓库的 `.git` 目录，创建根 `.gitignore`，分支重命名为 `main`，完成初始提交。
>
> **Deliverables**:
> - 根目录 `.gitignore` 文件
> - 分支重命名 `master` → `main`
> - 清理后的暂存区（无 `.DS_Store`、`node_modules`、`target/` 等）
> - 删除 `ro-ro-monitor/.git` 和 `ro-ro-monitor-web/.git`
> - 初始提交（包含所有源代码）
>
> **Estimated Effort**: Quick
> **Parallel Execution**: NO — 严格顺序操作（gitlink 清理必须先于 `.git` 删除）
> **Critical Path**: `.gitignore` 创建 → gitlink 清理 → `.git` 删除 → stage → verify → commit

---

## Context

### Original Request
把本项目设为 git 仓库并进行版本管理。

### Interview Summary
**Key Discussions**:
- 仓库已初始化但无提交，暂存区包含大量不应提交的文件
- 嵌套仓库策略: 合并为 Monorepo（删除子仓库 `.git`）
- 分支命名: `master` → `main`
- `.omo/` 策略: 只提交 `plans/`，其余 gitignore
- 不配置远程仓库

**Research Findings**:
- `ro-ro-monitor` 和 `ro-ro-monitor-web` 在暂存区是 **gitlink 模式 (160000)**，必须 `git rm --cached` 先于删除 `.git`
- 子仓库各有 3 个提交历史（将丢弃）
- `node_modules/` 约 221MB，必须排除
- 根 `.gitignore` 不存在

### Metis Review
**Identified Gaps** (addressed):
- Gitlink (160000) 陷阱: 操作顺序是关键约束，已纳入计划
- 分支命名不一致: 用户选择 `main`
- `.omo/` 内容分类: 用户选择只提交 `plans/`
- Maven wrapper: 保留子仓库 `.gitignore` 规则（排除 `.mvn/`）
- `AGENTS.md`: 纳入提交

---

## Work Objectives

### Core Objective
清理项目 git 仓库状态，建立规范的 Monorepo 版本管理，完成初始提交。

### Concrete Deliverables
- `.gitignore` — 根目录忽略规则文件
- 分支 `main`（从 `master` 重命名）
- 一次干净的初始提交

### Definition of Done
- [ ] `git ls-files --stage | grep 160000` 输出为空（无 gitlink）
- [ ] `git ls-files | grep "\.DS_Store"` 输出为空
- [ ] `git ls-files | grep "node_modules"` 输出为空
- [ ] `git ls-files | grep "target/"` 输出为空
- [ ] `git rev-list --count main` 返回 `2`
- [ ] `git ls-files | grep "\.java$" | wc -l` > 0
- [ ] `git ls-files | grep "\.vue$" | wc -l` > 0

### Must Have
- 根 `.gitignore` 排除 `.DS_Store`、`node_modules/`、`target/`、`.playwright-mcp/`
- 根 `.gitignore` 排除 `.omo/evidence/`、`.omo/run-continuation/`、`.omo/boulder.json`、`.omo/drafts/`、`.omo/notepads/`
- 分支名为 `main`
- `ro-ro-monitor/.git` 和 `ro-ro-monitor-web/.git` 已删除
- 子仓库 `.gitignore` 文件保留

### Must NOT Have (Guardrails)
- ❌ 不得在 `.gitignore` 创建前执行任何 `git add`
- ❌ 不得在 `git rm --cached` gitlink 之前删除嵌套 `.git`
- ❌ 不得提交 `.DS_Store`、`node_modules/`、`target/`、`.playwright-mcp/`
- ❌ 不得提交 `.omo/evidence/`、`.omo/run-continuation/`、`.omo/boulder.json`
- ❌ 不得配置远程仓库
- ❌ 不得修改子仓库现有 `.gitignore` 文件

---

## Verification Strategy (MANDATORY)

> **ZERO HUMAN INTERVENTION** — ALL verification is agent-executed via bash commands.

### Test Decision
- **Infrastructure exists**: N/A (git ops, not code)
- **Automated tests**: N/A
- **Framework**: bash verification commands

### QA Policy
Every task includes agent-executed verification via bash commands. Evidence saved to `.omo/evidence/`.

---

## Execution Strategy

### Sequential Wave (single operator)

```
Step 1: Create root .gitignore
    ↓
Step 2: Remove gitlinks from index (git rm --cached)
    ↓
Step 3: Delete nested .git directories
    ↓
Step 4: Stage all files (git add)
    ↓
Step 5: Verify staging area (dry-run + inspection)
    ↓
Step 6: Rename branch + initial commit
```

**Critical Path**: Step 1 → Step 2 → Step 3 → Step 4 → Step 5 → Step 6（顺序依赖，不可并行）

---

## TODOs

> Implementation + Verification = ONE Task. NEVER separate.
> EVERY task MUST have: Recommended Agent Profile + QA Scenarios.

- [x] 1. 创建根目录 `.gitignore`

  **What to do**:
  - 创建 `/Users/fengwei/projects/in_transit_monitor/.gitignore`
  - 包含以下忽略规则：
    - `.DS_Store`（macOS 系统文件）
    - `node_modules/`（前端依赖，221MB）
    - `target/`（Java 编译产物）
    - `.playwright-mcp/`（浏览器自动化临时文件）
    - `.omo/evidence/`（QA 截图和日志）
    - `.omo/run-continuation/`（会话状态文件）
    - `.omo/boulder.json`（工作追踪状态）
    - `.omo/drafts/`（工作计划草稿）
    - `.omo/notepads/`（AI 学习笔记）
    - `*.log`（日志文件）
  - 不排除 `.omo/plans/`（需要提交）
  - 保留子仓库现有 `.gitignore` 不变

  **Must NOT do**:
  - 不得修改 `ro-ro-monitor/.gitignore` 或 `ro-ro-monitor-web/.gitignore`
  - 不得在此步骤执行 `git add`

  **Recommended Agent Profile**:
  - **Category**: `quick`
    - Reason: 单文件创建，内容明确，无需深度分析
  - **Skills**: []
    - 纯文件写入，无需特定技能

  **Parallelization**:
  - **Can Run In Parallel**: NO
  - **Parallel Group**: Sequential (Step 1)
  - **Blocks**: Task 2（必须先有 .gitignore 才能安全 stage）
  - **Blocked By**: None

  **Acceptance Criteria**:
  - [ ] 文件存在: `test -f .gitignore && echo "PASS"`
  - [ ] 包含 `.DS_Store` 规则: `grep -q "\.DS_Store" .gitignore && echo "PASS"`
  - [ ] 包含 `node_modules/` 规则: `grep -q "node_modules" .gitignore && echo "PASS"`
  - [ ] 包含 `target/` 规则: `grep -q "target" .gitignore && echo "PASS"`
  - [ ] 包含 `.playwright-mcp/` 规则: `grep -q "playwright-mcp" .gitignore && echo "PASS"`
  - [ ] 包含 `.omo/evidence/` 规则: `grep -q "evidence" .gitignore && echo "PASS"`
  - [ ] 包含 `.omo/boulder.json` 规则: `grep -q "boulder.json" .gitignore && echo "PASS"`
  - [ ] 不存在 `plans` 排除规则: `! grep -q "/plans" .gitignore || echo "PASS: plans not excluded"`

  **QA Scenarios (MANDATORY)**:

  ```
  Scenario: .gitignore 创建并包含所有必要规则
    Tool: Bash
    Preconditions: 根目录不存在 .gitignore
    Steps:
      1. 写入 .gitignore 文件
      2. 验证文件存在: test -f .gitignore
      3. 逐条验证关键规则存在（.DS_Store, node_modules/, target/, .playwright-mcp/, .omo/evidence/, .omo/boulder.json）
      4. 验证 .omo/plans/ 未被排除
    Expected Result: 文件存在，所有必要规则包含，plans 未被排除
    Failure Indicators: 文件不存在，或缺少关键规则，或 plans 被错误排除
    Evidence: .omo/evidence/task-1-gitignore-verify.log
  ```

  **Evidence to Capture**:
  - [ ] `.omo/evidence/task-1-gitignore-verify.log` — `grep` 验证输出

  **Commit**: YES
  - Message: `chore: add root .gitignore`
  - Files: `.gitignore`

---

- [x] 2. 清理暂存区并移除 gitlink 条目

  **What to do**:
  - 执行 `git rm --cached ro-ro-monitor ro-ro-monitor-web` 移除 gitlink (160000) 条目
  - 执行 `git rm --cached .DS_Store` 移除所有已暂存的 `.DS_Store`
  - 执行 `git rm -r --cached .omo/evidence/` 移除已暂存的 evidence
  - 执行 `git rm -r --cached .playwright-mcp/` 移除已暂存的 playwright 文件
  - 执行 `git rm -r --cached .omo/run-continuation/` 移除已暂存的会话文件
  - 验证 `git ls-files --stage | grep 160000` 返回空

  **Must NOT do**:
  - 不得执行不含 `--cached` 的 `git rm`（会删除工作区文件）
  - 不得在此步骤删除 `ro-ro-monitor/.git` 或 `ro-ro-monitor-web/.git`
  - 不得在此步骤执行 `git add`

  **Recommended Agent Profile**:
  - **Category**: `quick`
    - Reason: 标准 git 命令序列，无歧义
  - **Skills**: [`git-master`]
    - `git-master`: Git 操作需要遵循最佳实践，包括 `--cached` 安全性

  **Parallelization**:
  - **Can Run In Parallel**: NO
  - **Parallel Group**: Sequential (Step 2)
  - **Blocks**: Task 3
  - **Blocked By**: Task 1

  **Acceptance Criteria**:
  - [ ] 无 gitlink 条目: `git ls-files --stage | grep 160000` → 空输出
  - [ ] 无 .DS_Store 在暂存区: `git ls-files | grep "\.DS_Store"` → 空输出
  - [ ] 无 .omo/evidence 在暂存区: `git ls-files | grep "\.omo/evidence"` → 空输出
  - [ ] 无 .playwright-mcp 在暂存区: `git ls-files | grep "\.playwright-mcp"` → 空输出
  - [ ] 无 .omo/run-continuation 在暂存区: `git ls-files | grep "\.omo/run-continuation"` → 空输出
  - [ ] ro-ro-monitor 和 ro-ro-monitor-web 目录在工作区仍然存在

  **QA Scenarios (MANDATORY)**:

  ```
  Scenario: Gtlink 条目成功移除
    Tool: Bash
    Preconditions: 暂存区有 gitlink 条目 (160000)
    Steps:
      1. git rm --cached ro-ro-monitor ro-ro-monitor-web
      2. git ls-files --stage | grep 160000
    Expected Result: grep 无匹配（空输出），exit code 1
    Failure Indicators: grep 仍有 160000 匹配

  Scenario: 脏文件全部从暂存区移除
    Tool: Bash
    Preconditions: 暂存区有 .DS_Store, .omo/evidence/, .playwright-mcp/
    Steps:
      1. git rm --cached 所有脏文件
      2. git ls-files 逐一检查
    Expected Result: 所有不应提交的文件不在 git ls-files 输出中
    Failure Indicators: 仍有 .DS_Store 或其他脏文件在暂存区
    Evidence: .omo/evidence/task-2-gitlink-clean.log
  ```

  **Evidence to Capture**:
  - [ ] `.omo/evidence/task-2-gitlink-clean.log` — `git ls-files --stage` 和 `git ls-files` 输出

  **Commit**: NO（准备工作，不单独提交）

---

- [x] 3. 删除嵌套 `.git` 目录

  **What to do**:
  - 执行 `rm -rf ro-ro-monitor/.git`
  - 执行 `rm -rf ro-ro-monitor-web/.git`
  - 验证目录不存在: `test -d ro-ro-monitor/.git && echo "FAIL" || echo "PASS"`
  - 验证目录不存在: `test -d ro-ro-monitor-web/.git && echo "FAIL" || echo "PASS"`

  **Must NOT do**:
  - 不得删除 `ro-ro-monitor/` 或 `ro-ro-monitor-web/` 目录本身
  - 不得删除其他 `.git` 相关文件（如 `.gitignore`）

  **Recommended Agent Profile**:
  - **Category**: `quick`
    - Reason: 单行命令，无歧义
  - **Skills**: []

  **Parallelization**:
  - **Can Run In Parallel**: NO
  - **Parallel Group**: Sequential (Step 3)
  - **Blocks**: Task 4
  - **Blocked By**: Task 2

  **Acceptance Criteria**:
  - [ ] `test -d ro-ro-monitor/.git` → FAIL（目录不存在）
  - [ ] `test -d ro-ro-monitor-web/.git` → FAIL（目录不存在）
  - [ ] `test -d ro-ro-monitor/` → PASS（父目录仍存在）
  - [ ] `test -d ro-ro-monitor-web/` → PASS（父目录仍存在）

  **QA Scenarios (MANDATORY)**:

  ```
  Scenario: 嵌套 .git 目录成功删除
    Tool: Bash
    Preconditions: ro-ro-monitor/.git 和 ro-ro-monitor-web/.git 存在
    Steps:
      1. rm -rf ro-ro-monitor/.git ro-ro-monitor-web/.git
      2. test -d ro-ro-monitor/.git → expect FAIL
      3. test -d ro-ro-monitor-web/.git → expect FAIL
      4. test -d ro-ro-monitor/ → expect PASS
    Expected Result: .git 子目录不存在，父目录完整
    Failure Indicators: .git 目录仍存在，或父目录被误删
    Evidence: .omo/evidence/task-3-nested-git-removed.log
  ```

  **Evidence to Capture**:
  - [ ] `.omo/evidence/task-3-nested-git-removed.log` — 验证命令输出

  **Commit**: NO（准备工作，不单独提交）

---

- [x] 4. 暂存所有源文件并验证

  **What to do**:
  - 执行 `git add --dry-run .` 预览将被暂存的文件
  - 目视确认预览中不包含: `.DS_Store`, `node_modules/`, `target/`, `.playwright-mcp/`, `.omo/evidence/`
  - 执行 `git add .` 暂存所有文件
  - 执行 `git status` 确认状态

  **Must NOT do**:
  - 不得跳过 `--dry-run` 预览步骤
  - 不得使用 `git add -f`（强制添加）

  **Recommended Agent Profile**:
  - **Category**: `quick`
    - Reason: 标准 git 操作，但需仔细验证
  - **Skills**: [`git-master`]
    - `git-master`: Git staging 操作需要遵循最佳实践

  **Parallelization**:
  - **Can Run In Parallel**: NO
  - **Parallel Group**: Sequential (Step 4)
  - **Blocks**: Task 5
  - **Blocked By**: Task 3

  **Acceptance Criteria**:
  - [ ] dry-run 输出不含 `.DS_Store`: `git add --dry-run . 2>&1 | grep "\.DS_Store"` → 空
  - [ ] dry-run 输出不含 `node_modules`: `git add --dry-run . 2>&1 | grep "node_modules"` → 空
  - [ ] dry-run 输出不含 `target/`: `git add --dry-run . 2>&1 | grep "target/"` → 空
  - [ ] Java 文件已暂存: `git ls-files | grep "\.java$" | wc -l` > 0
  - [ ] Vue 文件已暂存: `git ls-files | grep "\.vue$" | wc -l` > 0
  - [ ] `.gitignore` 已暂存: `git ls-files | grep "\.gitignore$"` 有匹配
  - [ ] `.omo/plans/` 已暂存: `git ls-files | grep "\.omo/plans"` 有匹配
  - [ ] `AGENTS.md` 已暂存: `git ls-files | grep "AGENTS.md"` 有匹配

  **QA Scenarios (MANDATORY)**:

  ```
  Scenario: 暂存区只包含应提交的文件（无脏文件）
    Tool: Bash
    Preconditions: .gitignore 已创建，gitlink 已清除，.git 已删除
    Steps:
      1. git add --dry-run . > /tmp/dry-run-output.txt
      2. grep "\.DS_Store" /tmp/dry-run-output.txt → expect no matches
      3. grep "node_modules" /tmp/dry-run-output.txt → expect no matches
      4. grep "target/" /tmp/dry-run-output.txt → expect no matches
      5. grep "\.playwright-mcp" /tmp/dry-run-output.txt → expect no matches
      6. grep "\.omo/evidence" /tmp/dry-run-output.txt → expect no matches
    Expected Result: 所有脏文件路径在 dry-run 中均无匹配
    Failure Indicators: 任何脏文件出现在 dry-run 输出中

  Scenario: 源文件正确暂存
    Tool: Bash
    Preconditions: 执行 git add .
    Steps:
      1. git ls-files | grep "\.java$" | wc -l → expect > 0
      2. git ls-files | grep "\.vue$" | wc -l → expect > 0
      3. git ls-files | grep "\.gitignore$" → expect matches
    Expected Result: Java 和 Vue 源文件已追踪
    Failure Indicators: 源文件数量为 0
    Evidence: .omo/evidence/task-4-staging-verify.log
  ```

  **Evidence to Capture**:
  - [ ] `.omo/evidence/task-4-staging-verify.log` — dry-run 输出 + `git ls-files` 统计

  **Commit**: NO（与 Task 5 合并提交）

---

- [x] 5. 分支重命名并完成初始提交

  **What to do**:
  - 执行 `git branch -m master main` 重命名分支
  - 验证分支名: `git rev-parse --abbrev-ref HEAD` → `main`
  - 执行初始提交: `git commit -m "chore: initial monorepo commit

  - 合并 ro-ro-monitor (Spring Boot backend) 和 ro-ro-monitor-web (Vue 3 frontend) 为 monorepo
  - 子仓库原始历史: ro-ro-monitor@d17a84a, ro-ro-monitor-web@0d2438e
  - 分支重命名: master → main"`
  - 验证提交: `git rev-list --count main` → `2`
  - 验证无未提交更改: `git status --porcelain` → 空

  **Must NOT do**:
  - 不得配置远程仓库
  - 不得 push

  **Recommended Agent Profile**:
  - **Category**: `quick`
    - Reason: git 分支操作 + commit，标准流程
  - **Skills**: [`git-master`]
    - `git-master`: 提交信息格式、分支操作

  **Parallelization**:
  - **Can Run In Parallel**: NO
  - **Parallel Group**: Sequential (Step 5)
  - **Blocks**: Task F1
  - **Blocked By**: Task 4

  **Acceptance Criteria**:
  - [ ] 分支为 `main`: `git rev-parse --abbrev-ref HEAD` → `main`
  - [ ] 恰好 1 个提交: `git rev-list --count main` → `1`
  - [ ] 工作区干净: `git status --porcelain` → 空输出
  - [ ] commit message 包含 "initial monorepo commit"

  **QA Scenarios (MANDATORY)**:

  ```
  Scenario: 分支重命名成功 + 初始提交包含所有源文件
    Tool: Bash
    Preconditions: 所有文件已暂存
    Steps:
      1. git branch -m master main
      2. git rev-parse --abbrev-ref HEAD → expect "main"
      3. git commit -m "..."
      4. git rev-list --count main → expect "1"
      5. git status --porcelain → expect empty
      6. git ls-files | wc -l → expect > 100 (大量源文件被追踪)
    Expected Result: 分支 main，恰好 1 个提交，工作区干净
    Failure Indicators: 分支名不是 main，提交数不为 1，工作区不干净
    Evidence: .omo/evidence/task-5-initial-commit.log
  ```

  **Evidence to Capture**:
  - [ ] `.omo/evidence/task-5-initial-commit.log` — 完整验证输出

  **Commit**: YES（这是最终的初始提交）
  - Message: `chore: initial monorepo commit`
  - Files: 所有暂存文件

---

## Final Verification Wave (MANDATORY — after ALL implementation tasks)

> 2 个验证任务按顺序执行（依赖初始提交完成）

- [x] F1. **完整性验证** — `quick`

  对初始提交执行全面验证：

  ```
  1. git ls-files --stage | grep 160000 → 必须为空（无 gitlink）
  2. git ls-files | grep "\.DS_Store" → 必须为空
  3. git ls-files | grep "node_modules" → 必须为空
  4. git ls-files | grep "target/" → 必须为空
  5. git ls-files | grep "\.playwright-mcp" → 必须为空
  6. git ls-files | grep "\.omo/evidence" → 必须为空
  7. git ls-files | grep "\.omo/run-continuation" → 必须为空
  8. git ls-files | grep "\.omo/boulder.json" → 必须为空
  9. git ls-files | grep "\.java$" | wc -l → > 0
  10. git ls-files | grep "\.vue$" | wc -l → > 0
  11. git ls-files | grep "\.gitignore$" → 应有 3 个匹配（根 + 2 个子项目）
  12. git ls-files | grep "AGENTS.md" → 1 个匹配
  13. git rev-parse --abbrev-ref HEAD → main
   14. git rev-list --count main → 2
  ```

  Output: `检查项 [N/14] 通过 | VERDICT: APPROVE/REJECT`

- [x] F2. **git log 确认** — `quick`

  验证提交历史结构正确：

  ```
  1. git log --oneline → 恰好 1 行，包含 "initial monorepo commit"
  2. git log --stat → 确认包含 .gitignore, AGENTS.md, Java/Vue 源文件
  3. git show --name-only HEAD | head -50 → 预览首批文件
  ```

  Output: `Commit [OK] | Files [N tracked] | VERDICT: APPROVE/REJECT`

---

## Commit Strategy

- **Task 1**: 独立提交 `.gitignore`（`chore: add root .gitignore`）
- **Task 2-3**: 不提交（准备工作）
- **Task 4-5**: 合并为初始提交（`chore: initial monorepo commit`）

最终结果: **2 个提交**
1. `chore: add root .gitignore`
2. `chore: initial monorepo commit`

---

## Success Criteria

### Verification Commands
```bash
# 分支确认
git rev-parse --abbrev-ref HEAD          # Expected: main

# 提交数量
git rev-list --count main                 # Expected: 2

# 无 gitlink
git ls-files --stage | grep 160000       # Expected: empty

# 无脏文件
git ls-files | grep -E "(\.DS_Store|node_modules|target/|\.playwright-mcp|\.omo/evidence)"  # Expected: empty

# 源文件已追踪
git ls-files | grep "\.java$" | wc -l    # Expected: > 0
git ls-files | grep "\.vue$" | wc -l     # Expected: > 0
```

### Final Checklist
- [ ] 分支名为 `main`
- [ ] 根 `.gitignore` 存在且内容正确
- [ ] 无 gitlink (160000) 条目
- [ ] 无 `.DS_Store`、`node_modules/`、`target/` 在版本控制中
- [ ] 无 `.omo/evidence/`、`.omo/boulder.json` 在版本控制中
- [ ] `.omo/plans/` 已提交
- [ ] Java 和 Vue 源文件已追踪
- [ ] 子仓库 `.gitignore` 文件保留
- [ ] 工作区干净（`git status --porcelain` 为空）
