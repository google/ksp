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

## KSP2 Is Here!
KSP2 is a new implementation of the KSP API. It is faster and easier to use than KSP 1.x. Please refer to the
[KSP2 introduction](docs/ksp2.md) for more details.

### Switching Between KSP1 And KSP2
Starting with KSP 2.0.0, KSP2 is enabled by default. You can still switch back to KSP1 with the Gradle property
`ksp.useKSP2=false`, or the `ksp` extension in Gradle build scripts:

```
ksp {
    useKsp2 = false
}
```

### KSP1 deprecation schedule
KSP1 will not be able to support newer Kotlin language features and will be deprecated starting from Kotlin 2.2.0.
This is because KSP1 is a compiler plugin of K1, which is already deprecated. Also, the Kotlin Gradle Plugin is
standardizing its API and disallowing accesses to its internal implementations on which KSP1 relies.

The KSP team will try to support KSP1 with best efforts so that users have more time to migrate to KSP2, but no promise
can be made. Please plan migrating to KSP2 as early as possible.

### Deprecation notice for KSP1

KSP1 is deprecated and support will be removed. We are focusing our development efforts on KSP2 to provide better performance, improved APIs, and a more robust architecture for the future.

#### Compatibility Limitations
Please be aware that KSP1 will not be updated to support upcoming major versions of the Android and Kotlin toolchains. Specifically, KSP1 will not be compatible with:
* Kotlin version `2.3.0` and higher.
* Android Gradle Plugin (AGP) version `9.0` and higher.

Projects using KSP1 may not behave correctly (or fail the build) if you upgrade to these or any subsequent versions of AGP or Kotlin.

To ensure your annotation processors continue to function correctly and to take advantage of future tooling advancements, it is crucial to migrate your projects to use KSP2 (which has been the default since beginning of 2025)

Please refer to the [KSP2 introduction](docs/ksp2.md) for further details.


## KSP Dependency Configurations Reference

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

## Ongoing and Future Works

Here are some planned features that have not yet been completely implemented:
* Improve support to multiplatform. E.g., running KSP on a subset of targets / sharing computations between targets
* Improve performance. There are a bunch of optimizations to be done!
* Keep fixing bugs!
