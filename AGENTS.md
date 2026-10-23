# AGENTS.md — Guidelines for AI Coding Agents in KSP

Welcome! This file defines the mandatory workflow, engineering methodology, and project context for AI coding agents working in the **Kotlin Symbol Processing (KSP)** repository.

---

## 1. Documentation Discovery & Reference Map

1. **Read `CONTRIBUTING.md` First**: Before proposing plans or modifying code, read `CONTRIBUTING.md`. It is the authoritative source for the bug contribution workflow (`@Bug` / `BugState`), `api/api.base` verification (`checkApi` / `updateApi`), and writing unit tests in `kotlin-analysis-api`.
2. **Consult Additional Documentation On-Demand**: Read the following repository documents when relevant to your task:

| Document | When to Read & What It Covers |
| :--- | :--- |
| `README.md` | **Gradle configurations & properties**: Read when working on `:gradle-plugin`, KMP/Android configuration patterns (`ksp`, `kspTest`, `ksp<Target>`, `kspCommonMainMetadata`), or Gradle properties (`ksp.incremental`, `ksp.experimental.psi.resolution`, `ksp.project.isolation.enabled`, etc.). |
| `docs/ksp2.md`<br>`docs/ksp2api.md`<br>`docs/ksp2cmdline.md`<br>`docs/ksp2entrypoints.md` | **KSP2 architecture & behavior**: Read when investigating KSP1 vs. KSP2 semantic changes (`ksp2api.md`), command-line runners (`KSPJvmMain`, `KSPJsMain`, `KSPNativeMain`, `KSPCommonMain` in `ksp2cmdline.md`), programmatic invocation (`KotlinSymbolProcessing` in `ksp2entrypoints.md`), or debugging KSP2 inside the Gradle/worker daemon (`ksp2.md`). |
| `DEVELOPMENT.md` | **Legacy debugging reference**: Note that `DEVELOPMENT.md` describes legacy KSP1 `KotlinCompileDaemon` debugging; KSP2 runs via Gradle worker actions (`KspAATask` / `KspAAWorkerAction` in `:gradle-plugin`) rather than as a `kotlinc` compiler plugin (see `docs/ksp2.md`). |
| `LICENSE` | **Apache License, Version 2.0**: Full license terms governing this repository. |

---

## 2. Core Engineering Methodology: Scientific Method & Zero Speculation

You are working on a compiler plugin and symbol processing engine. **There must be zero speculation, hand-waving, or hallucination.**

* **The Codebase is the Single Source of Truth**: Every claim, diagnosis, or proposed change must be backed by concrete code snippets and traced through the actual codebase.
* **Falsify Hypotheses First (Scientific Method)**:
  * Whenever you form a hypothesis about how a symbol is resolved, how a type is computed, or why a bug occurs, **actively try to disprove your hypothesis first** using concrete code examples and dataflow examples from the codebase.
  * Trace the exact **dataflow** end-to-end (e.g., from `Resolver` entry points $\rightarrow$ `AnnotationResolutionStrategy` / Analysis API session calls $\rightarrow$ `KSNode` / `KSType` wrappers $\rightarrow$ PSI / FIR symbols) and cite the concrete classes, functions, and line ranges involved.
  * Revise your mental model repeatedly whenever the code contradicts your assumptions.
* **No Guessing**: If an API contract, compiler behavior, or user requirement is ambiguous, inspect the implementation and test suite (`kotlin-analysis-api/testData` and `KSPUnitTestSuite.kt`) or ask the user clarifying questions rather than guessing.
* **Outline a Detailed Plan First**: Always outline a clear, detailed plan supported by code references and dataflow analysis for the user to review before making changes.
* **Preserve & Synchronize Documentation**:
  * Preserve all existing comments and docstrings unrelated to your changes unless explicitly instructed otherwise.
  * **Keep Documentation in Sync with Code Changes**: Whenever you modify code, behavior, APIs, Gradle properties, or build configurations, you **must** also update the corresponding KDoc/doc comments and any relevant documentation files in the repository (e.g., `README.md`, `CONTRIBUTING.md`, `DEVELOPMENT.md`, `AGENTS.md`, or `docs/`) so that documentation never drifts out of sync with the implementation.

---

## 3. License & Legal Compliance

* **License**: This repository is licensed under the **Apache License, Version 2.0** (see `LICENSE`).
* **CLA**: Contributions require a Google Contributor License Agreement (`CONTRIBUTING.md`).
* **Copyright Headers**: All new source files (`.kt`, `.java`, `.kts` in `buildSrc`/modules) must include the standard Apache 2.0 license header used across the codebase:

```kotlin
/*
 * Copyright 2026 Google LLC
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
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

---

## 4. Repository Architecture & Module Guide

> **Important Note on KSP1**: KSP 1.x has been removed and is no longer supported (`README.md`). All active compiler engine development targets **KSP2** (`kotlin-analysis-api`).

The project consists of 10 Gradle subprojects configured in `settings.gradle.kts`:

| Module | Purpose & Key Details |
| :--- | :--- |
| `:api` | Public-facing, dependency-free library (`com.google.devtools.ksp:symbol-processing-api`). Divided into `processing` (`SymbolProcessor`, `Resolver`, `CodeGenerator`), `symbol` (`KSNode`, `KSDeclaration`, `KSType`), and `visitor` (`KSVisitor`, `KSDefaultVisitor`). Tracked by Metalava against `api/api.base`. |
| `:kotlin-analysis-api` | Core KSP2 engine implemented on top of the Kotlin Analysis API (FIR/K2). Key entry points: `KotlinSymbolProcessing.kt` (`src/main/kotlin/com/google/devtools/ksp/impl/`), `ResolverAAImpl.kt`, `AnnotationResolutionStrategy.kt` (`src/main/kotlin/com/google/devtools/ksp/common/impl/` with `AAResolutionStrategy` and `PsiResolutionStrategy`), all `KS*` symbol implementations (`src/main/kotlin/com/google/devtools/ksp/impl/symbol/`), and the primary unit test suite (`KSPUnitTestSuite.kt`). |
| `:symbol-processing-aa-embeddable` | Repackages `:kotlin-analysis-api` and its dependencies into a self-contained uber-jar, relocating bundled packages under the `ksp.*` namespace (`symbol-processing-aa-embeddable/build.gradle.kts`) to avoid classpath conflicts. Entry point: `KSPLoader.loadAndRunKSP()`. |
| `:gradle-plugin` | The KSP Gradle plugin (`id("com.google.devtools.ksp")`, `KspGradleSubplugin` in `KspSubplugin.kt`, and `KspAATask.kt`). Configures KSP tasks for JVM, Android, and Kotlin Multiplatform (KMP) projects. |
| `:common-util` | Shared internal utilities used by `:kotlin-analysis-api` and other modules. |
| `:common-deps` | Shared dependencies and command-line argument parsing utilities (`symbol-processing-common-deps`). Uses `:cmdline-parser-gen` via KSP. |
| `:cmdline-parser-gen` | Internal KSP processor used during the build to generate command-line parser code for `:common-deps`. |
| `:integration-tests` | End-to-end Gradle TestKit tests validating the published KSP uber-jar and Gradle plugin across real build scenarios (`primaryTest`, `secondaryTest`, `androidTest`, `agpTest`). |
| `:symbol-processing` | Dummy POM-only artifact maintained for `KotlinCompilerPluginSupportPlugin` compatibility. |
| `:test-utils` | Legacy test utilities. **Deprecated for new unit tests** (`CONTRIBUTING.md`)—all new unit tests belong in `:kotlin-analysis-api`. |

---

## 5. Contribution, Bug & Testing Workflows (from `CONTRIBUTING.md`)

### Bug Contribution Workflow
1. **Verify Issue**: Check if a GitHub issue exists (`@Bug` requires a linked GitHub issue URL).
2. **Reproduce via Failing Test**:
   - Prefer unit tests in `:kotlin-analysis-api` over integration tests unless testing Gradle/incremental build behavior.
   - In a reproduction PR, register the test in `kotlin-analysis-api/src/test/kotlin/com/google/devtools/ksp/test/KSPUnitTestSuite.kt` using `runFailingTest` or `runThrowingTest`, annotated with `@Bug("https://github.com/google/ksp/issues/<id>", BugState.OPEN)`.
3. **Fix the Bug**:
   - In the fix PR, update the test call in `KSPUnitTestSuite.kt` from `runFailingTest`/`runThrowingTest` to `runTest` and change `BugState.OPEN` to `BugState.FIXED`.

### Writing Unit Tests in `:kotlin-analysis-api`
1. **Create or Reuse a Test Processor**:
   - Location: `kotlin-analysis-api/src/test/kotlin/com/google/devtools/ksp/processor/`
   - Package **must** be `com.google.devtools.ksp.processor`.
   - Class **must** extend `AbstractTestProcessor` and override `toResult(): List<String>`.
   - Constructor **must** accept `override val enableNewFeatures: Boolean`:
     ```kotlin
     class MyProcessor(override val enableNewFeatures: Boolean) : AbstractTestProcessor()
     ```
   - If the processor accepts `// PROCESSOR INPUT:`, add a `List<String>` parameter **before** `enableNewFeatures`:
     ```kotlin
     class MyParametricProcessor(val inputs: List<String>, override val enableNewFeatures: Boolean) : AbstractTestProcessor()
     ```
2. **Create the Test Data File**:
   - Location: `kotlin-analysis-api/testData/<category>/<testName>.kt`
   - Structure of the `.kt` test file (**Note**: `// EXPECTED:` is mandatory—`AbstractKSPTest.parseExpectedOutput` drops all lines until `// EXPECTED:`):
     ```kotlin
     // WITH_RUNTIME
     // TEST PROCESSOR: MyProcessor
     // PROCESSOR INPUT: optional_input
     // EXPECTED:
     // expected output line 1
     // expected output line 2
     // END
     // FILE: a.kt
     package test
     // test source code...
     ```
   - When expected output differs based on `enableNewFeatures` (`false` vs. `true`), use `// EXPECT CURRENT: <line>` and `// EXPECT NEXT: <line>` inside the `// EXPECTED:` ... `// END` block.
   - Optional backend directives (from Kotlin compiler test framework): `// TARGET_BACKEND: NATIVE`, `// WITH_FIXED_TARGET: linux_x64`.
3. **Register in `KSPUnitTestSuite` (and Concrete Suite Subclasses if Needed)**:
   - `KSPUnitTestSuite` (`kotlin-analysis-api/src/test/kotlin/com/google/devtools/ksp/test/KSPUnitTestSuite.kt`) is an `abstract class` executed via 4 concrete JUnit test classes:
     - `AAConfiguredUnitTestSuite` (`experimentalPsiResolution = false, enableNewFeatures = false`)
     - `AAConfiguredNewFeaturesUnitTestSuite` (`experimentalPsiResolution = false, enableNewFeatures = true`)
     - `PsiConfiguredUnitTestSuite` (`experimentalPsiResolution = true, enableNewFeatures = false`)
     - `PsiConfiguredNewFeaturesUnitTestSuite` (`experimentalPsiResolution = true, enableNewFeatures = true`)
   - For standard tests that behave identically across configurations, add a concrete `@Test` method in `KSPUnitTestSuite.kt`:
     ```kotlin
     @Bug("https://github.com/google/ksp/issues/<id>", BugState.FIXED)
     @TestMetadata("<category>/<testName>.kt")
     @Test
     fun testMyFeature() {
         runTest("$AA_PATH/<category>/<testName>.kt")
     }
     ```
   - If a test behaves differently between AA and PSI resolution (or between `enableNewFeatures` modes), declare it `abstract fun` in `KSPUnitTestSuite.kt` and override it in `AAConfiguredUnitTestSuite.kt` (`AAConfiguredUnitTestSuiteBase`) and `PsiConfiguredUnitTestSuite.kt` (`PsiConfiguredUnitTestSuiteBase`).

---

## 6. Build, Version Synchronization & Verification Reference

### JDK & Kotlin Version Constraints
* **JDK 17 Required**: The Gradle build and Kotlin JVM toolchain require **Java 17** (`JavaLanguageVersion.of(17)` in `build.gradle.kts` and `buildSrc/build.gradle.kts`).
* **Target Bytecode & API Compatibility**:
  * Most modules compile targeting `JVM_1_8` (`gradle-plugin` and `integration-tests` target `JVM_11`) with Kotlin `languageVersion` and `apiVersion` set to `KotlinVersion.KOTLIN_2_3` (`build.gradle.kts`). Do not call stdlib APIs newer than the configured `apiVersion`.
* **Decoupled Versions (`gradle.properties` & `gradle/libs.versions.toml`)**:
  * `buildKotlinVersion` / `buildKspVersion`: Used in `gradle.properties` to bootstrap and compile the KSP project itself.
  * `kotlinBaseVersion` (`kotlin-base`) & `agpBaseVersion` (`agp-base`): **Duplicated in both `gradle.properties` and `gradle/libs.versions.toml`**. `gradle-plugin`'s `TestConfig` loads `gradle.properties` directly at test runtime—**always keep both files in sync**.
  * `aa-kotlin-base` (`aaKotlinBaseVersion`): The specific (often developmental) Kotlin version used for Analysis API dependencies in `:kotlin-analysis-api` and `:symbol-processing-aa-embeddable`.

### Essential Gradle Tasks

* **Run a single unit test method across all 4 suite configurations**:
  > Note: Because `KSPUnitTestSuite` is `abstract`, use a wildcard or a concrete subclass name (`AAConfiguredUnitTestSuite`, `PsiConfiguredUnitTestSuite`, etc.) with `--tests`:
  ```bash
  ./gradlew :kotlin-analysis-api:test --tests "*.<testMethodName>"
  ./gradlew :kotlin-analysis-api:test --tests "com.google.devtools.ksp.test.AAConfiguredUnitTestSuite.<testMethodName>"
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
* **Run Integration Tests**:
  Integration tests publish all artifacts to `<rootProject>/build/repos/test` and run Gradle TestKit suites (`integration-tests/build.gradle.kts`):
  ```bash
  ./gradlew :integration-tests:primaryTest
  ./gradlew :integration-tests:secondaryTest
  ./gradlew :integration-tests:androidTest
  ./gradlew :integration-tests:agpTest
  ```
* **Full Verification**:
  > Note: It may be necessary to pass `--rerun-tasks` to ensure cached test or task outputs are re-evaluated after changes.
  ```bash
  ./gradlew check
  ./gradlew check --rerun-tasks
  ```
