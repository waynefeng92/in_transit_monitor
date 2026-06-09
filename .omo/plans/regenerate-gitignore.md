# Regenerate .gitignore

## TL;DR

> **Quick Summary**: 合并根目录和子项目的 3 个 .gitignore，生成一份全面的 .gitignore 覆盖 OS/IDE/Maven/Node/AI 所有应忽略文件。
>
> **Estimated Effort**: Quick
> **Parallel Execution**: NO - single file

---

## Context

当前 `.gitignore` 过于简陋（仅 11 条规则），子项目有各自的 `.gitignore`，存在规则分散、冗余、遗漏的问题。
`git status` 显示 Eclipse 文件、AI agent 文件未被忽略。

## Work Objectives

### Must Have
- 覆盖 macOS / Windows / Linux OS 文件
- 覆盖 Eclipse / IDEA / VSCode IDE 文件
- 覆盖 Java/Maven 构建产物
- 覆盖 Node/npm 依赖和构建产物
- 覆盖日志、环境配置、AI 工具文件

### Must NOT Have
- 不删除子项目的 `.gitignore`（保留兼容性）
- 不忽略 `package-lock.json`（需要提交）
- 不忽略 `src/main/resources/sql/` 下的 SQL 文件

---

## TODOs

- [ ] 1. 更新根目录 .gitignore

  **What to do**:
  - 用下方完整内容覆盖 `/home/fengwei/projects/in_transit_monitor/.gitignore`

  **新 .gitignore 内容**:
  ```
  # ==================== OS ====================
  .DS_Store
  .AppleDouble
  .LSOverride
  Thumbs.db
  ehthumbs.db
  Desktop.ini

  # ==================== IDE ====================
  # Eclipse
  .classpath
  .factorypath
  .project
  .settings/

  # IntelliJ IDEA
  .idea/
  *.iml
  *.iws
  *.ipr

  # VSCode
  .vscode/
  !.vscode/extensions.json

  # ==================== Java / Maven ====================
  target/
  out/
  *.jar
  *.war
  *.ear
  *.class
  .mvn/
  mvnw
  mvnw.cmd
  *.pid
  *.orig

  # ==================== Node / Frontend ====================
  node_modules/
  .pnp/
  .pnp.js
  dist/
  dist-ssr/
  *.local
  .cache/
  .parcel-cache/
  .vite/
  *.tsbuildinfo

  # ==================== Logs ====================
  logs/
  *.log
  npm-debug.log*
  yarn-debug.log*
  yarn-error.log*
  pnpm-debug.log*
  lerna-debug.log*

  # ==================== Environment / Config ====================
  .env
  .env.local
  .env.*.local
  application-local.yml
  application-prod.yml

  # ==================== AI / OpenCode ====================
  .agents/
  skills-lock.json
  .playwright-mcp/

  # ==================== OMO Runtime ====================
  .omo/evidence/
  .omo/run-continuation/
  .omo/boulder.json
  .omo/drafts/
  .omo/notepads/

  # ==================== Misc ====================
  *.swp
  *.swo
  *.swn
  *.bak
  *~
  \#*\#
  ```

  **QA Scenarios**:
  ```
  Scenario: git status shows no unwanted files
    Tool: Bash (git status)
    Steps:
      1. git status --porcelain
    Expected Result: No Eclipse files (.classpath, .factorypath, .project, .settings/) appear
                     No .agents/ directory appears
                     No skills-lock.json appears
    Expected Result (negative): modified pom.xml and package-lock.json SHOULD still appear
    Evidence: .omo/evidence/task-1-git-status.txt

  Scenario: SQL dump is NOT ignored
    Tool: Bash (git check-ignore)
    Steps:
      1. git check-ignore ro-ro-monitor/src/main/resources/sql/ro_ro_monitor_full.sql
    Expected Result: Exit code 1 (file is NOT ignored — should be tracked)
    Evidence: .omo/evidence/task-1-sql-check.txt
  ```

  **Commit**: NO (leave for user to review and commit)

---

## Verification Strategy

- `git status --porcelain` — 确认 Eclipse 文件、.agents/、skills-lock.json 不再出现
- `git check-ignore` 确认 SQL 文件不被忽略
- `git check-ignore` 确认 node_modules 被忽略

## Success Criteria
- [ ] Eclipse IDE 文件不再出现在 git status 中
- [ ] .agents/ 被忽略
- [ ] SQL dump 文件不被忽略（应被追踪）
- [ ] pom.xml 和 package-lock.json 的修改仍可见
