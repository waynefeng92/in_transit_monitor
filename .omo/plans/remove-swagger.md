# Remove Swagger/Knife4j

## TL;DR

> **Quick Summary**: Remove Knife4j/Swagger dependency and all `io.swagger.annotations.*` annotations from the project. The sole developer (AI-driven) doesn't need generated API docs — the code is the documentation.
>
> **Deliverables**:
> - `pom.xml` — knife4j dependency removed
> - `SwaggerConfig.java` — deleted
> - 13 DTO files — `@ApiModel`/`@ApiModelProperty` annotations + imports cleaned
> - 12 Controller files — `@Api`/`@ApiOperation`/`@ApiParam` annotations + imports cleaned
>
> **Estimated Effort**: Quick
> **Parallel Execution**: YES — 2 waves
> **Critical Path**: Task 1 → Task 2+3 (parallel) → Task 4

---

## Context

### Original Request
Spring Boot app crashes on startup due to Springfox/Spring Boot path matching incompatibility. User chose to remove Swagger entirely rather than fix or migrate it.

### Interview Summary
**Key Discussions**:
- Swagger is unnecessary: frontend and backend are both AI-written, no human team needs API docs
- User explicitly rejected SpringDoc migration — chose full removal
- "前后端都是我一个人（其实是你，都是ai写的）" — reasonable to drop the doc layer

### Scope Boundaries
- **IN**: pom.xml dependency removal, SwaggerConfig.java deletion, annotation cleanup in DTOs and Controllers
- **OUT**: No other refactoring, no migration to SpringDoc, no changes to application logic

---

## Work Objectives

### Core Objective
Remove Knife4j/Swagger from the project so the app compiles and starts without Swagger-related errors.

### Concrete Deliverables
- `ro-ro-monitor/pom.xml` — knife4j-openapi2-spring-boot-starter dependency removed
- `ro-ro-monitor/src/main/java/com/company/roro/config/SwaggerConfig.java` — deleted
- 13 DTOs cleaned of `io.swagger.annotations.*` imports and annotations
- 12 Controllers cleaned of `io.swagger.annotations.*` imports and annotations
- Application starts successfully on port 8080

### Definition of Done
- [ ] `mvn compile` passes with 0 errors
- [ ] Application starts without Swagger-related exceptions
- [ ] `grep -r "io.swagger.annotations" src/` returns 0 results
- [ ] `grep -r "swagger\|Swagger\|knife4j\|Knife4j" pom.xml` returns 0 results

### Must Have
- Zero `io.swagger.annotations` imports remaining in any Java file
- Zero mentions of knife4j/swagger in pom.xml
- Application compiles and starts

### Must NOT Have (Guardrails)
- Do NOT touch `@RequestMapping` / `@GetMapping` / `@PostMapping` etc. — only swagger annotations
- Do NOT change any business logic or controller method bodies
- Do NOT refactor or reorganize any files
- Do NOT add new dependencies or configuration

---

## Verification Strategy

### Test Decision
- **Automated tests**: None (removal task, verified by compilation + startup)
- **Agent-Executed QA**: Yes — `mvn compile` + app startup check

---

## Execution Strategy

### Parallel Execution Waves

```
Wave 1 (Start Immediately — dependency removal + config deletion, parallel):
├── Task 1: Remove knife4j dependency from pom.xml [quick]
└── Task 2: Delete SwaggerConfig.java [quick]

Wave 2 (After Wave 1 — clean all annotations, MAX PARALLEL):
├── Task 3: Clean DTO annotations (13 files) [quick]
└── Task 4: Clean Controller annotations (12 files) [quick]

Wave VERIFY (After Wave 2 — single verification):
└── Task 5: Compile + startup verification [quick]

Critical Path: Task 1 → Task 3/4 → Task 5
Parallel Speedup: Wave 1 (2 parallel) + Wave 2 (2 parallel)
Max Concurrent: 2
```

### Dependency Matrix

- **1**: - - 3, 4
- **2**: - - 3, 4
- **3**: 1, 2 - 5
- **4**: 1, 2 - 5
- **5**: 3, 4 -

---

## TODOs

- [x] 1. Remove knife4j dependency from pom.xml

  **What to do**:
  - Open `ro-ro-monitor/pom.xml`
  - Find and remove the entire `<dependency>` block containing `knife4j-openapi2-spring-boot-starter`
  - That means removing lines 82-85 (the comment + groupId + artifactId + version)

  **Must NOT do**:
  - Do NOT modify any other dependency
  - Do NOT add `spring.mvc.pathmatch.matching-strategy` config (Swagger is gone, the path matching issue becomes irrelevant)

  **Recommended Agent Profile**:
  - **Category**: `quick`
    - Reason: Single-file edit, well-defined removal, no logic changes

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 1 (with Task 2)
  - **Blocks**: Tasks 3, 4

  **References**:
  - `ro-ro-monitor/pom.xml:80-85` — Current knife4j dependency block to remove

  **Acceptance Criteria**:
  - [ ] `grep -i 'knife4j\|swagger' ro-ro-monitor/pom.xml` returns no output

  **QA Scenarios**:

  ```
  Scenario: knife4j dependency fully removed from pom.xml
    Tool: Bash (grep)
    Preconditions: pom.xml edited
    Steps:
      1. grep -i 'knife4j' /home/fengwei/projects/in_transit_monitor/ro-ro-monitor/pom.xml
      2. grep -i 'swagger' /home/fengwei/projects/in_transit_monitor/ro-ro-monitor/pom.xml
    Expected Result: Both grep commands return NO output (empty)
    Failure Indicators: Any match found means incomplete removal
    Evidence: .omo/evidence/task-1-pom-clean.txt

  Scenario: pom.xml remains valid XML after edit
    Tool: Bash (xmllint)
    Preconditions: Knife4j dependency removed
    Steps:
      1. xmllint --noout /home/fengwei/projects/in_transit_monitor/ro-ro-monitor/pom.xml
    Expected Result: No output (valid XML), exit code 0
    Failure Indicators: xmllint errors about malformed XML
    Evidence: .omo/evidence/task-1-xml-valid.txt
  ```

  **Commit**: YES (groups with Task 2)
  - Message: `chore: remove knife4j/swagger dependency and config`

---

- [x] 2. Delete SwaggerConfig.java

  **What to do**:
  - Delete the file `ro-ro-monitor/src/main/java/com/company/roro/config/SwaggerConfig.java`

  **Must NOT do**:
  - Do NOT touch any other config files
  - Do NOT create any replacement config

  **Recommended Agent Profile**:
  - **Category**: `quick`
    - Reason: Single file deletion, no logic involved

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 1 (with Task 1)
  - **Blocks**: Tasks 3, 4

  **References**:
  - `ro-ro-monitor/src/main/java/com/company/roro/config/SwaggerConfig.java` — File to delete

  **Acceptance Criteria**:
  - [ ] File no longer exists at the path

  **QA Scenarios**:

  ```
  Scenario: SwaggerConfig.java is deleted
    Tool: Bash
    Preconditions: File deleted
    Steps:
      1. ls /home/fengwei/projects/in_transit_monitor/ro-ro-monitor/src/main/java/com/company/roro/config/SwaggerConfig.java
    Expected Result: "No such file or directory" error
    Failure Indicators: File still exists
    Evidence: .omo/evidence/task-2-config-deleted.txt
  ```

  **Commit**: YES (groups with Task 1)
  - Message: `chore: remove knife4j/swagger dependency and config`
  - Files: `pom.xml`, `SwaggerConfig.java`

---

- [x] 3. Clean DTO annotations (13 files)

  **What to do**:
  Clean `io.swagger.annotations.*` imports and annotations from all 13 DTO files:
  - `dto/OtdConfigImportDTO.java`
  - `dto/RouteImportPreviewDTO.java`
  - `dto/ExcelMappingDTO.java`
  - `dto/RouteImportResultDTO.java`
  - `dto/RouteImportRowDTO.java`
  - `dto/CopyMappingRequest.java`
  - `dto/OtdConfigExportDTO.java`
  - `dto/OtdConfigImportResultDTO.java`
  - `dto/StandardFieldDTO.java`
  - `dto/BatchSaveMappingRequest.java`
  - `dto/RouteImportRequestDTO.java`
  - `dto/ExcelPreviewDTO.java`
  - `dto/SheetInfoDTO.java`

  For **each file**, do the following (noting that each DTO contains ONLY `@ApiModel` on the class and `@ApiModelProperty` on fields):

  **Step by step for each file**:
  1. Read the file to see current content
  2. Remove the lines: `import io.swagger.annotations.ApiModel;` and `import io.swagger.annotations.ApiModelProperty;`
  3. Remove the `@ApiModel(description = "...")` annotation from the class declaration
  4. Remove the `@ApiModelProperty(value = "...")` annotation from every field (keep the field itself, and any Jackson annotations like `@JsonProperty`)
  5. Write the cleaned file back

  **Must NOT do**:
  - Do NOT change field names, types, or any Jackson annotations (`@JsonProperty`, `@JsonFormat`, etc.)
  - Do NOT change class names or package declarations
  - Do NOT change any other imports
  - Do NOT change any methods or logic

  **Recommended Agent Profile**:
  - **Category**: `quick`
    - Reason: Mechanical find-and-remove across many files, no logic involved

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 2 (with Task 4)
  - **Blocked By**: Tasks 1, 2
  - **Blocks**: Task 5

  **References**:
  - `ro-ro-monitor/src/main/java/com/company/roro/dto/OtdConfigImportDTO.java` — Example DTO with `@ApiModel` + `@ApiModelProperty`
  - All 13 files under `ro-ro-monitor/src/main/java/com/company/roro/dto/`

  **Acceptance Criteria**:
  - [ ] `grep -r "io.swagger.annotations" ro-ro-monitor/src/main/java/com/company/roro/dto/` returns 0 results
  - [ ] `grep -r "@ApiModel" ro-ro-monitor/src/main/java/com/company/roro/dto/` returns 0 results
  - [ ] `grep -r "@ApiModelProperty" ro-ro-monitor/src/main/java/com/company/roro/dto/` returns 0 results

  **QA Scenarios**:

  ```
  Scenario: All DTO files are swagger-clean
    Tool: Bash (grep)
    Preconditions: All 13 DTO files edited
    Steps:
      1. grep -r "io.swagger.annotations" /home/fengwei/projects/in_transit_monitor/ro-ro-monitor/src/main/java/com/company/roro/dto/
      2. grep -r "@ApiModel\|@ApiModelProperty" /home/fengwei/projects/in_transit_monitor/ro-ro-monitor/src/main/java/com/company/roro/dto/
    Expected Result: Both grep commands return NO output (empty)
    Failure Indicators: Any swagger annotation or import remaining in any DTO file
    Evidence: .omo/evidence/task-3-dto-clean.txt

  Scenario: DTO files compile without swagger imports
    Tool: Bash (mvn compile)
    Preconditions: All DTOs cleaned, pom.xml updated
    Steps:
      1. cd /home/fengwei/projects/in_transit_monitor/ro-ro-monitor && mvn compile 2>&1
    Expected Result: BUILD SUCCESS, zero compilation errors
    Failure Indicators: Compilation errors referencing missing swagger classes or broken DTO files
    Evidence: .omo/evidence/task-3-compile.txt
  ```

  **Commit**: YES (groups with Task 4)
  - Message: `chore: remove swagger annotations from DTOs and controllers`
  - Files: All 13 DTO files + all 12 Controller files

---

- [x] 4. Clean Controller annotations (12 files)

  **What to do**:
  Clean `io.swagger.annotations.*` imports and annotations from all 12 Controller files:
  - `controller/BrandController.java`
  - `controller/TransportStatusController.java`
  - `controller/OrderController.java`
  - `controller/ArrivedController.java`
  - `controller/RouteController.java`
  - `controller/UploadController.java`
  - `controller/TransitController.java`
  - `controller/ChartController.java`
  - `controller/ExcelMappingController.java`
  - `controller/MonitorStatusController.java`
  - `controller/RouteOtdConfigController.java`
  - `controller/PortController.java`

  For **each file**:

  **Step by step**:
  1. Read the file to see current content
  2. Remove all `import io.swagger.annotations.Api;`, `import io.swagger.annotations.ApiOperation;`, `import io.swagger.annotations.ApiParam;` lines
  3. Remove the `@Api(tags = "...")` annotation from the class declaration
  4. Remove `@ApiOperation("...")` from each method (keep `@GetMapping` / `@PostMapping` etc.)
  5. Remove `@ApiParam("...")` from each method parameter (keep the parameter itself)
  6. Write the cleaned file back

  **Must NOT do**:
  - Do NOT remove or change `@RestController`, `@RequestMapping`, `@GetMapping`, `@PostMapping`, `@PutMapping`, `@DeleteMapping`
  - Do NOT change method signatures, parameter names, or types
  - Do NOT change any other imports (e.g., `org.springframework.web.bind.annotation.*`)
  - Do NOT change method bodies or logic

  **Recommended Agent Profile**:
  - **Category**: `quick`
    - Reason: Mechanical find-and-remove across many files, no logic involved

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 2 (with Task 3)
  - **Blocked By**: Tasks 1, 2
  - **Blocks**: Task 5

  **References**:
  - `ro-ro-monitor/src/main/java/com/company/roro/controller/BrandController.java` — Example controller with `@Api` + `@ApiOperation` + `@ApiParam`

  **Acceptance Criteria**:
  - [ ] `grep -r "io.swagger.annotations" ro-ro-monitor/src/main/java/com/company/roro/controller/` returns 0 results
  - [ ] `grep -r "@Api\b" ro-ro-monitor/src/main/java/com/company/roro/controller/` returns 0 results (using word boundary to exclude false positives)
  - [ ] `grep -r "@ApiOperation" ro-ro-monitor/src/main/java/com/company/roro/controller/` returns 0 results
  - [ ] `grep -r "@ApiParam" ro-ro-monitor/src/main/java/com/company/roro/controller/` returns 0 results

  **QA Scenarios**:

  ```
  Scenario: All Controller files are swagger-clean
    Tool: Bash (grep)
    Preconditions: All 12 Controller files edited
    Steps:
      1. grep -r "io.swagger.annotations" /home/fengwei/projects/in_transit_monitor/ro-ro-monitor/src/main/java/com/company/roro/controller/
      2. grep -rn "@Api\b\|@ApiOperation\|@ApiParam" /home/fengwei/projects/in_transit_monitor/ro-ro-monitor/src/main/java/com/company/roro/controller/
    Expected Result: Both grep commands return NO output
    Failure Indicators: Any swagger annotation remaining in any Controller file
    Evidence: .omo/evidence/task-4-controller-clean.txt

  Scenario: Spring annotations intact in Controllers
    Tool: Bash (grep)
    Preconditions: All 12 Controller files edited
    Steps:
      1. grep -rn "@RestController\|@RequestMapping\|@GetMapping\|@PostMapping\|@PutMapping\|@DeleteMapping" /home/fengwei/projects/in_transit_monitor/ro-ro-monitor/src/main/java/com/company/roro/controller/
    Expected Result: Multiple matches across all 12 files (Spring annotations preserved)
    Failure Indicators: Any Controller file missing all Spring annotations (over-cleaned)
    Evidence: .omo/evidence/task-4-spring-intact.txt
  ```

  **Commit**: YES (groups with Task 3)
  - Message: `chore: remove swagger annotations from DTOs and controllers`
  - Files: All 12 Controller files + all 13 DTO files

---

- [x] 5. Compile + startup verification

  **What to do**:
  After all cleanup tasks complete, verify the project compiles and the application starts.

  **Steps**:
  1. Run `mvn compile` in `ro-ro-monitor/` — must pass
  2. Run `mvn package -DskipTests` — must pass
  3. Start the application and check it boots without Swagger-related errors
  4. Run a final global sweep: `grep -r "io.swagger.annotations" src/` must return nothing

  **Must NOT do**:
  - Do NOT skip the global grep sweep

  **Recommended Agent Profile**:
  - **Category**: `quick`
    - Reason: Verification only — run commands, check output

  **Parallelization**:
  - **Can Run In Parallel**: NO
  - **Parallel Group**: Wave VERIFY (sequential)
  - **Blocked By**: Tasks 3, 4

  **References**:
  - AGENTS.md verification commands section

  **Acceptance Criteria**:
  - [ ] `mvn compile` → BUILD SUCCESS
  - [ ] `mvn package -DskipTests` → BUILD SUCCESS
  - [ ] Application starts on port 8080 without NPE/Springfox errors
  - [ ] Global grep for `io.swagger.annotations` across all `src/` returns 0 results

  **QA Scenarios**:

  ```
  Scenario: Project compiles cleanly
    Tool: Bash (mvn)
    Preconditions: All swagger references removed, pom.xml cleaned
    Steps:
      1. cd /home/fengwei/projects/in_transit_monitor/ro-ro-monitor && mvn compile 2>&1
    Expected Result: "BUILD SUCCESS" in output, 0 compilation errors, 0 swagger-related errors
    Failure Indicators: Any "COMPILATION ERROR" or swagger class not found errors
    Evidence: .omo/evidence/task-5-compile.txt

  Scenario: Application starts without swagger errors
    Tool: Bash (java)
    Preconditions: Application compiled successfully
    Steps:
      1. cd /home/fengwei/projects/in_transit_monitor/ro-ro-monitor && mvn package -DskipTests 2>&1
      2. Start the application using the java command
      3. Wait up to 30 seconds for startup
      4. Check output for errors
    Expected Result: Application starts, Tomcat on port 8080, NO NullPointerException or springfox-related errors
    Failure Indicators: Any stack trace mentioning springfox, swagger, WebMvcPatternsRequestConditionWrapper, or documentationPluginsBootstrapper
    Evidence: .omo/evidence/task-5-startup.txt

  Scenario: Zero swagger references remaining project-wide
    Tool: Bash (grep)
    Preconditions: All cleanup complete
    Steps:
      1. grep -r "io.swagger.annotations" /home/fengwei/projects/in_transit_monitor/ro-ro-monitor/src/
      2. grep -ri "knife4j\|swagger" /home/fengwei/projects/in_transit_monitor/ro-ro-monitor/pom.xml
    Expected Result: Both commands return NO output
    Failure Indicators: Any remaining swagger import or knife4j dependency reference
    Evidence: .omo/evidence/task-5-global-sweep.txt
  ```

  **Commit**: NO (verification only)

---

## Final Verification Wave

>- [x] F1. **Global grep sweep** — verify zero `io.swagger.annotations` imports anywhere in `src/`
>- [x] F2. **Compile check** — `mvn compile` passes cleanly
>- [x] F3. **Startup check** — Application boots without Swagger-related exceptions
>- [x] F4. **Spring annotation integrity** — verify `@RestController`/`@RequestMapping`/`@GetMapping` etc. are all intact across all Controllers

---

## Commit Strategy

- **Commit 1** (Tasks 1 + 2): `chore: remove knife4j/swagger dependency and config` — `pom.xml`, `SwaggerConfig.java`
- **Commit 2** (Tasks 3 + 4): `chore: remove swagger annotations from DTOs and controllers` — 25 Java files
- **Task 5**: No commit (verification only)

---

## Success Criteria

### Verification Commands
```bash
# Compile check
cd ro-ro-monitor && mvn compile
# Expected: BUILD SUCCESS

# Global sweep — must be empty
grep -r "io.swagger.annotations" ro-ro-monitor/src/

# Dependency check — must be empty
grep -i "knife4j" ro-ro-monitor/pom.xml
```

### Final Checklist
- [ ] Zero `io.swagger.annotations` imports in entire project
- [ ] Zero knife4j/swagger in pom.xml
- [ ] SwaggerConfig.java deleted
- [ ] All Spring MVC annotations intact
- [ ] Application compiles and starts cleanly
