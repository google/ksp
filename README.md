# Kotlin Symbol Processing API

Welcome to KSP!

Kotlin Symbol Processing (KSP) is an API that you can use to develop
lightweight compiler plugins. KSP provides a simplified compiler plugin
API that leverages the power of Kotlin while keeping the learning curve at
a minimum. Compared to KAPT, annotation processors that use KSP can run up to 2x faster.

Most of the documentation of KSP can be found on [kotlinlang.org](https://kotlinlang.org/docs/ksp-overview.html). Here are some handy links:

* [Overview](https://kotlinlang.org/docs/ksp-overview.html)
* [Quickstart](https://kotlinlang.org/docs/ksp-quickstart.html)
* [Libraries that support KSP](https://kotlinlang.org/docs/ksp-overview.html#supported-libraries)
* [Why KSP?](https://kotlinlang.org/docs/ksp-why-ksp.html)
* [Examples](https://kotlinlang.org/docs/ksp-examples.html)
* [How KSP models Kotlin code](https://kotlinlang.org/docs/ksp-additional-details.html)
* [Reference for Java annotation processor authors](https://kotlinlang.org/docs/ksp-reference.html)
* [Incremental processing notes](https://kotlinlang.org/docs/ksp-incremental.html)
* [Multiple round processing notes](https://kotlinlang.org/docs/ksp-multi-round.html)
* [KSP on multiplatform projects](https://kotlinlang.org/docs/ksp-multiplatform.html)
* [Running KSP from command line](https://kotlinlang.org/docs/ksp-command-line.html)
* [FAQ](https://kotlinlang.org/docs/ksp-faq.html)

For debugging and testing processors, as well as KSP itself, please check [DEVELOPMENT.md](DEVELOPMENT.md)

---

## KSP Gradle Configurations Reference

This section contains a reference of Gradle configurations for KSP.
If you have not used KSP before, or this section seems unclear,
please read the documentation on the Kotlin lang website which is linked above.

When applying KSP in your Gradle project, place symbol processor dependencies into the appropriate configuration inside the `dependencies { ... }` block of your `build.gradle.kts` (or `build.gradle`) file based on your target platforms, source sets, and build variants.

### Single-Platform (JVM & Android)

| Configuration Name / Pattern | Project Type / Target | Source Set / Scope | Usage Example (`build.gradle.kts`) | Details & Behavior |
| --- | --- | --- | --- | --- |
| `ksp` | Single-target JVM / Android | Main source set (`src/main`) | `ksp("com.example:processor:1.0")` | Applied to default JVM compilation and single-platform Android main source set. Deprecated in KMP unless `ksp.allow.all.target.configuration=true`. |
| `kspTest` | Single-target JVM / Android | Unit tests (`src/test`) | `kspTest("com.example:test-processor:1.0")` | Runs symbol processing exclusively for unit test sources. |
| `ksp<SourceSet>` | Single-target JVM | Custom JVM source set | `add("kspIntegrationTest", "...")` | Generates sources for custom source sets like `integrationTest`. |
| `ksp<BuildType>` | Android (Single-platform) | Specific build type (e.g., debug, release) | `add("kspDebug", "...")` | Runs processor only when compiling the specified Android build variant. |
| `ksp<Flavor>` | Android (Single-platform) | Product flavor (e.g., free, paid) | `add("kspFree", "...")` | Runs processor for all build variants matching the specified flavor. |
| `ksp<Flavor><BuildType>` | Android (Single-platform) | Flavor + Build Type combination | `add("kspFreeDebug", "...")` | Targeted execution for a specific flavor + build type variant. |
| `kspTest<Flavor><BuildType>` | Android (Single-platform) | Local Unit Tests for variant/flavor | `add("kspTestDebug", "...")` | Runs processing on unit test code in `src/testDebug`. |
| `kspAndroidTest<Variant>` | Android (Single-platform) | Instrumentation Tests (`src/androidTest`) | `add("kspAndroidTestDebug", "...")` | Runs processing on Android instrumentation test code (`src/androidTest`). |

### Kotlin Multiplatform (KMP)

| Configuration Name / Pattern | Project Type / Target | Source Set / Scope | Usage Example (`build.gradle.kts`) | Details & Behavior |
| --- | --- | --- | --- | --- |
| `ksp<Target>` | Kotlin Multiplatform | KMP target main compilation (JVM, JS, Native, Wasm) | `add("kspJvm", "...")`<br>`add("kspJs", "...")`<br>`add("kspIosArm64", "...")`<br>`add("kspAndroid", "...")`<br>`add("kspAndroidHostTest", "...")`<br>`add("kspAndroidDeviceTest", "...")` | Target-specific configuration. Omits the Main suffix (e.g., target `jvm` becomes `kspJvm`, not `kspJvmMain`). |
| `ksp<Target>Test` | Kotlin Multiplatform | KMP target test compilation | `add("kspJvmTest", "...")`<br>`add("kspJsTest", "...")` | Target-specific test configuration. |
| `kspCommonMainMetadata` | Kotlin Multiplatform | Common Main metadata compilation | `add("kspCommonMainMetadata", "...")` | Target-specific configuration for KMP `commonMain` metadata processing. |

## KSP Gradle Properties Reference

This section provides a reference of Gradle properties for KSP.
These properties can be set in your project's `gradle.properties` file or passed via command line
options (`-Pproperty=value` or `-Dproperty=value`).

### Reference Table

| Property Key                                | Type      | Default | Description                                                                                                                                                                                                                                     | Documentation                                                                                                             |
|:--------------------------------------------|:----------|:--------|:------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|:--------------------------------------------------------------------------------------------------------------------------|
| `ksp.incremental`                           | `Boolean` | `true`  | Enables or disables incremental symbol processing in KSP.                                                                                                                                                                                       | [Incremental Processing](https://kotlinlang.org/docs/ksp-incremental.html)                                                |
| `ksp.incremental.log`                       | `Boolean` | `false` | Enables verbose log output detailing incremental processing decisions and symbol changes.                                                                                                                                                       | [Incremental Logging](https://kotlinlang.org/docs/ksp-incremental.html#reporting-bugs)                                    |
| `ksp.incremental.log.graph.origin`          | `String?` | `null`  | Filters incremental log tracing to a specific symbol origin class/package. The value should be of the form `com.example.MyClass`.                                                                                                               | [Incremental Dependency Graph](https://kotlinlang.org/docs/ksp-incremental.html#visualizing-the-symbol-dependency-graph)  |
| `ksp.allow.all.target.configuration`        | `Boolean` | `false` | Permits the legacy global `ksp` configuration in both single-platform and Kotlin Multiplatform (KMP) projects with a warning instead of an error.                                                                                               | [`ksp` Configuration](https://kotlinlang.org/docs/ksp-multiplatform.html#avoid-the-ksp-configuration-on-ksp-1-0-1)        |
| `ksp.project.isolation.enabled`             | `Boolean` | `false` | Enables Gradle Project Isolation compatible codepaths for output directory registration. **NOTE:** KSP also enables this if you have `org.gradle.unsafe.isolated-projects=true` configured.                                                     | [Gradle Isolated Projects](https://docs.gradle.org/current/userguide/isolated_projects.html)                              |
| `ksp.experimental.psi.resolution`           | `Boolean` | `false` | Enables experimental PSI-based resolution inside the KSP compiler engine. Enabling this may improve build times, but expect crashes. Feel free to open a bug report if you encounter a crash.                                                   | None                                                                                                                      |
| `ksp.ksp2.profiling.mode`                   | `Boolean` | `false` | Enables performance profiling for KSP2 task execution. This is mostly for developing KSP itself.                                                                                                                                                | None                                                                                                                      |
| `kotlin.native.enableKlibsCrossCompilation` | `Boolean` | `true`  | Enables running symbol processing tasks for other targets than the host machine in KMP projects, i.e., cross-compilation. This property is not created by the KSP Gradle plugin. It is owned by the Kotlin compiler but KSP uses this property. | [Kotlin Cross Compilation](https://kotlinlang.org/docs/whatsnew21.html#ability-to-publish-kotlin-libraries-from-any-host) |

---

## Nightly Builds
Nightly builds of KSP for the latest Kotlin stable releases are published here:

```
maven("https://central.sonatype.com/repository/maven-snapshots/")
```

## Feedback and Bug Reporting

[Please let us know what you think about KSP by filing a Github issue](https://github.com/google/ksp/issues)
or connecting with our team in the `#ksp` channel in the
[Kotlin Slack workspace](https://kotlinlang.slack.com/)!

If you are interested in sending PRs, please also check out the [Contributor guide](CONTRIBUTING.md).

## Ongoing and Future Work

Here are some planned features that have not yet been completely implemented:
* Improve support to multiplatform. E.g., running KSP on a subset of targets / sharing computations between targets
* Improve performance. There are a bunch of optimizations to be done!
* Keep fixing bugs!

## A Note on KSP1

KSP 1.x has been removed and is no longer supported.
