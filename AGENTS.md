# AGENTS.md — Guidelines for AI Coding Agents in KSP

Welcome! This file defines the mandatory workflow, engineering methodology, and project context for AI coding agents working in the **Kotlin Symbol Processing (KSP)** repository.

---

## 1. Documentation Discovery & Reference Map

1. **Read `CONTRIBUTING.md` First**: Before proposing plans or modifying code, read `CONTRIBUTING.md`. It is the authoritative source for the bug contribution workflow (`@Bug` / `BugState`), `api/api.base` verification (`checkApi` / `updateApi`), and writing unit tests in `kotlin-analysis-api`.
2. **Consult Additional Documentation On-Demand**: Read the following repository documents when relevant to your task. Documentation can be found in Markdown files, LICENSE, and doc comments in Kotlin source files.

---

## 2. Core Engineering Methodology: Scientific Method & Zero Speculation

You are working on a compiler plugin and symbol processing engine. **There must be zero speculation, hand-waving, or hallucination.**

* **The Codebase is the Single Source of Truth**: Every claim, diagnosis, or proposed change must be backed by concrete code snippets and traced through the actual codebase.
* **Falsify Hypotheses First (Scientific Method)**:
  * Whenever you form a hypothesis about how a symbol is resolved, how a type is computed, or why a bug occurs, **actively try to disprove your hypothesis first** using concrete code examples and dataflow examples from the codebase.
  * Trace the exact **dataflow** end-to-end (e.g., from `Resolver` entry points to `AnnotationResolutionStrategy` / Analysis API session calls to `KSNode` / `KSType` wrappers to PSI / FIR symbols) and cite the concrete classes, functions, and line ranges involved.
  * Revise your mental model repeatedly whenever the code contradicts your assumptions.
* **No Guessing**: If an API contract, compiler behavior, or user requirement is ambiguous, inspect the implementation and test suite (`kotlin-analysis-api/testData` and `KSPUnitTestSuite.kt`) or ask the user clarifying questions rather than guessing.
* **Outline a Detailed Plan First**: Always outline a clear, detailed plan supported by code references and dataflow analysis before you begin. Use this plan as your reference when you are making changes.
* **Preserve & Synchronize Documentation**:
  * Preserve all existing comments and docstrings unrelated to your changes unless explicitly instructed otherwise.
  * **Keep Documentation in Sync with Code Changes**: Whenever you modify code, behavior, APIs, Gradle properties, or build configurations, you **must** also update the corresponding KDoc/doc comments and any relevant documentation files in the repository (e.g., `README.md`, `CONTRIBUTING.md`, `DEVELOPMENT.md`, `AGENTS.md`, or `docs/`) so that documentation never drifts out of sync with the implementation. This AGENTS.md file must also be kept in sync.

---

## 3. License & Legal Compliance

* **License**: This repository is licensed under the **Apache License, Version 2.0** (see `LICENSE`).
* **CLA**: Contributions require a Google Contributor License Agreement (`CONTRIBUTING.md`).
* **Copyright Headers**: All new source files (`.kt`, `.java`) must include the standard Apache 2.0 license header used across the codebase:

```kotlin
/*
 * Copyright <CURRENT YEAR> Google LLC
 * Copyright 2010-<CURRENT YEAR> JetBrains s.r.o. and Kotlin Programming Language contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
```

where `<CURRENT YEAR>` is the current year.

You will likely find files that do not have a copyright header. The headers may have been lost when
moving the file in the editor. Do not restore those.

---

## 4. Repository Architecture & Module Guide

> **Important Note on KSP1**: KSP 1.x has been removed and is no longer supported (`README.md`). All active compiler engine development targets **KSP2** (`kotlin-analysis-api`).

The project consists of Gradle subprojects configured in `settings.gradle.kts`:

| Module                             | Purpose & Key Details                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  |
|:-----------------------------------|:-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `:api`                             | Public-facing, dependency-free library (`com.google.devtools.ksp:symbol-processing-api`). Tracked by Metalava against `api/api.base`.                                                                                                                                                                                                                                                                                                                                                                  |
| `:kotlin-analysis-api`             | Core KSP2 engine implemented on top of the Kotlin Analysis API (FIR/K2). Key entry points: `KotlinSymbolProcessing.kt` (`src/main/kotlin/com/google/devtools/ksp/impl/`), `ResolverAAImpl.kt`, `AnnotationResolutionStrategy.kt` (`src/main/kotlin/com/google/devtools/ksp/common/impl/` with `AAResolutionStrategy` and `PsiResolutionStrategy`), all `KS*` symbol implementations (`src/main/kotlin/com/google/devtools/ksp/impl/symbol/`), and the primary unit test suite (`KSPUnitTestSuite.kt`). |
| `:symbol-processing-aa-embeddable` | Repackages `:kotlin-analysis-api` and its dependencies into a self-contained uber-jar, relocating bundled packages under the `ksp.*` namespace (`symbol-processing-aa-embeddable/build.gradle.kts`) to avoid classpath conflicts. Entry point: `KSPLoader.loadAndRunKSP()`.                                                                                                                                                                                                                            |
| `:gradle-plugin`                   | The KSP Gradle plugin (`id("com.google.devtools.ksp")`, `KspGradleSubplugin` in `KspSubplugin.kt`, and `KspAATask.kt`). Configures KSP tasks for JVM, Android, and Kotlin Multiplatform (KMP) projects.                                                                                                                                                                                                                                                                                                |
| `:common-util`                     | Shared internal utilities used by `:kotlin-analysis-api` and other modules.                                                                                                                                                                                                                                                                                                                                                                                                                            |
| `:common-deps`                     | Shared dependencies and command-line argument parsing utilities (`symbol-processing-common-deps`). Uses `:cmdline-parser-gen` via KSP.                                                                                                                                                                                                                                                                                                                                                                 |
| `:cmdline-parser-gen`              | Internal KSP processor used during the build to generate command-line parser code for `:common-deps`.                                                                                                                                                                                                                                                                                                                                                                                                  |
| `:integration-tests`               | End-to-end Gradle TestKit tests validating the published KSP uber-jar and Gradle plugin across real build scenarios (`primaryTest`, `secondaryTest`, `androidTest`, `agpTest`).                                                                                                                                                                                                                                                                                                                        |
| `:symbol-processing`               | Dummy POM-only artifact maintained for `KotlinCompilerPluginSupportPlugin` compatibility.                                                                                                                                                                                                                                                                                                                                                                                                              |
| `:test-utils`                      | Legacy test utilities. **Deprecated for new unit tests** (`CONTRIBUTING.md`)—all new unit tests belong in `:kotlin-analysis-api`.                                                                                                                                                                                                                                                                                                                                                                      |

---

## 5. Contribution, Bug & Testing Workflows
Read `CONTRIBUTING.md`.

## 6. Build, Version Synchronization & Verification Reference

### JDK & Kotlin Version Constraints
* Carefully read the Gradle build files for a particular module to find out what its Java and Kotlin constraints are.
* **Decoupled Versions (`gradle.properties` & `gradle/libs.versions.toml`)**:
  * `buildKotlinVersion` / `buildKspVersion`: Used in `gradle.properties` to bootstrap and compile the KSP project itself.
  * `kotlinBaseVersion` (`kotlin-base`) & `agpBaseVersion` (`agp-base`): **Duplicated in both `gradle.properties` and `gradle/libs.versions.toml`**. `gradle-plugin`'s `TestConfig` loads `gradle.properties` directly at test runtime—**always keep both files in sync**.
  * `aa-kotlin-base` (`aaKotlinBaseVersion`): The specific (often developmental) Kotlin version used for Analysis API dependencies in `:kotlin-analysis-api` and `:symbol-processing-aa-embeddable`.

### Essential Gradle Tasks

* **Run a single unit test method across all 4 suite configurations**:
  > Note: Because `KSPUnitTestSuite` is `abstract`, use a wildcard or a concrete subclass name with `--tests`:
  ```bash
  ./gradlew :kotlin-analysis-api:test --tests "*.<testMethodName>"
  ```
* **Run all Analysis API unit tests**:
  ```bash
  ./gradlew :kotlin-analysis-api:test
  ```
* **Check / Update Public API Signatures (`Metalava`)**:
  Whenever modifying public declarations in `:api`, verify compatibility against `api/api.base`:
  ```bash
  ./gradlew :api:checkApi
  ./gradlew :api:updateApi
  ```
* **Shadow JAR & Relocation Validation**:
  * `./gradlew :kotlin-analysis-api:validateShadowJar`: Runs `jdeps` against `kotlin-analysis-api/shadow-validation-baseline.txt` and verifies no forbidden packages (such as `org/apache/log4j/jdbc`) leak into the shadow JAR.
  * `./gradlew :symbol-processing-aa-embeddable:shadowJar`: Verifies that all bundled classes are relocated into allowed `ksp.*` namespaces.
* **Gradle Plugin Tests**:
  * `./gradlew :gradle-plugin:test`: Runs tests for the KSP Gradle plugin.
* **Run Integration Tests**:
  Integration tests publish all artifacts to `<rootProject>/build/repos/test` and run Gradle TestKit suites (`integration-tests/build.gradle.kts`):
  ```bash
  ./gradlew :integration-tests:primaryTest
  ./gradlew :integration-tests:secondaryTest
  ./gradlew :integration-tests:androidTest
  ./gradlew :integration-tests:agpTest
  ```
  Integration tests may take a long time to run so it may be helpful to just run a single test or test class when making changes, e.g.,
  ```bash
  ./gradlew :integration-tests:primaryTest --tests "com.google.devtools.ksp.test.primary.<TestClass>.<method>"
  ```
  where `TestClass` is the name of the class containing tests and `method` is the name of test method/entrypoint.
* **Full Verification**:
  Before submitting a pull request to the KSP repository, you must always run the entire `./gradlew check` task and check that it completes successfully.
  > Note: It may be necessary to pass `--rerun-tasks` to ensure cached test or task outputs are re-evaluated after changes.
  ```bash
  ./gradlew check --rerun-tasks
  ```
