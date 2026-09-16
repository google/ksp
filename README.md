# Kotlin Symbol Processing API

Welcome to KSP!

Kotlin Symbol Processing (KSP) is an API that you can use to develop
lightweight compiler plugins. KSP provides a simplified compiler plugin
API that leverages the power of Kotlin while keeping the learning curve at
a minimum. Compared to KAPT, annotation processors that use KSP can run up to 2x faster.

Most of the documentation of KSP can be found
on [kotlinlang.org](https://kotlinlang.org/docs/ksp-overview.html). Here are some handy links:

* [Overview](https://kotlinlang.org/docs/ksp-overview.html)
* [Quickstart](https://kotlinlang.org/docs/ksp-quickstart.html)
* [Libraries that support KSP](https://kotlinlang.org/docs/ksp-overview.html#supported-libraries)
* [Examples](https://kotlinlang.org/docs/ksp-examples.html)
* [How KSP models Kotlin code](https://kotlinlang.org/docs/ksp-additional-details.html)
* [Reference for Java annotation processor authors](https://kotlinlang.org/docs/ksp-reference.html)
* [Incremental processing notes](https://kotlinlang.org/docs/ksp-incremental.html)
* [Multiple round processing notes](https://kotlinlang.org/docs/ksp-multi-round.html)
* [KSP on multiplatform projects](https://kotlinlang.org/docs/ksp-multiplatform.html)
* [Running KSP from command line](https://kotlinlang.org/docs/ksp-command-line.html)
* [FAQ](https://kotlinlang.org/docs/ksp-faq.html)

For debugging and testing processors, as well as KSP itself, please
check [DEVELOPMENT.md](DEVELOPMENT.md)

---

## KSP Gradle Configurations Reference

This section contains a reference of Gradle configurations for KSP.
If you have not used KSP before, or this section seems unclear,
please read the documentation on the Kotlin lang website which is linked above.

When applying KSP in your Gradle project, place symbol processor dependencies into the appropriate
configuration inside the `dependencies { ... }` block of your `build.gradle.kts` (or `build.gradle`)
file based on your target platforms, source sets, and build variants.

### Single-Platform (JVM & Android)

| Configuration Name / Pattern | Project Type / Target       | Source Set / Scope                         | Usage Example (`build.gradle.kts`)          | Details & Behavior                                                                                                                                  |
|------------------------------|-----------------------------|--------------------------------------------|---------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------|
| `ksp`                        | Single-target JVM / Android | Main source set (`src/main`)               | `ksp("com.example:processor:1.0")`          | Applied to default JVM compilation and single-platform Android main source set. Deprecated in KMP unless `ksp.allow.all.target.configuration=true`. |
| `kspTest`                    | Single-target JVM / Android | Unit tests (`src/test`)                    | `kspTest("com.example:test-processor:1.0")` | Runs symbol processing exclusively for unit test sources.                                                                                           |
| `ksp<SourceSet>`             | Single-target JVM           | Custom JVM source set                      | `add("kspIntegrationTest", "...")`          | Generates sources for custom source sets like `integrationTest`.                                                                                    |
| `ksp<BuildType>`             | Android (Single-platform)   | Specific build type (e.g., debug, release) | `add("kspDebug", "...")`                    | Runs processor only when compiling the specified Android build variant.                                                                             |
| `ksp<Flavor>`                | Android (Single-platform)   | Product flavor (e.g., free, paid)          | `add("kspFree", "...")`                     | Runs processor for all build variants matching the specified flavor.                                                                                |
| `ksp<Flavor><BuildType>`     | Android (Single-platform)   | Flavor + Build Type combination            | `add("kspFreeDebug", "...")`                | Targeted execution for a specific flavor + build type variant.                                                                                      |
| `kspTest<Flavor><BuildType>` | Android (Single-platform)   | Local Unit Tests for variant/flavor        | `add("kspTestDebug", "...")`                | Runs processing on unit test code in `src/testDebug`.                                                                                               |
| `kspAndroidTest<Variant>`    | Android (Single-platform)   | Instrumentation Tests (`src/androidTest`)  | `add("kspAndroidTestDebug", "...")`         | Runs processing on Android instrumentation test code (`src/androidTest`).                                                                           |

### Kotlin Multiplatform (KMP)

| Configuration Name / Pattern | Project Type / Target | Source Set / Scope                                  | Usage Example (`build.gradle.kts`)                                                                                                                                                         | Details & Behavior                                                                                            |
|------------------------------|-----------------------|-----------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------|
| `ksp<Target>`                | Kotlin Multiplatform  | KMP target main compilation (JVM, JS, Native, Wasm) | `add("kspJvm", "...")`<br>`add("kspJs", "...")`<br>`add("kspIosArm64", "...")`<br>`add("kspAndroid", "...")`<br>`add("kspAndroidHostTest", "...")`<br>`add("kspAndroidDeviceTest", "...")` | Target-specific configuration. Omits the Main suffix (e.g., target `jvm` becomes `kspJvm`, not `kspJvmMain`). |
| `ksp<Target>Test`            | Kotlin Multiplatform  | KMP target test compilation                         | `add("kspJvmTest", "...")`<br>`add("kspJsTest", "...")`                                                                                                                                    | Target-specific test configuration.                                                                           |
| `kspCommonMainMetadata`      | Kotlin Multiplatform  | Common Main metadata compilation                    | `add("kspCommonMainMetadata", "...")`                                                                                                                                                      | Target-specific configuration for KMP `commonMain` metadata processing.                                       |

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
| `ksp.project.isolation.enabled`             | `Boolean` | `false` | Enables Gradle Project Isolation compatible codepaths for output directory registration. **NOTE:** KSP also enables this if you have `org.gradle.unsafe.isolated-projects=true` or `org.gradle.isolated-projects=true` configured.    | [Gradle Isolated Projects](https://docs.gradle.org/current/userguide/isolated_projects.html)                              |
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

If you are interested in sending PRs, please also check out
the [Contributor guide](CONTRIBUTING.md).

## Ongoing and Future Work

Here are some planned features that have not yet been completely implemented:

* Improve support to multiplatform. E.g., running KSP on a subset of targets / sharing computations
  between targets
* Improve performance. There are a bunch of optimizations to be done!
* Keep fixing bugs!

## A Note on KSP1

KSP 1.x has been removed and is no longer supported.


## 🌐 Web Resources & Aesthetic Symbols Index
- [HEARTS](https://cyber-clan-tags-38.pages.dev/ru/hearts/)
- [SYM 263A](https://gothic-bio-fonts-86.pages.dev/symbol/sym-263a/)
- [SYM 26BA](https://matrix-hacker-text-52.pages.dev/symbol/sym-26ba/)
- [SYM 1F497](https://sleek-line-symbols-51.pages.dev/symbol/sym-1f497/)
- [OPEN CENTRE STAR](https://neon-gamer-symbols-64.pages.dev/symbol/open-centre-star/)
- [ZODIAC CELESTIAL](https://neon-gamer-symbols-64.pages.dev/zodiac-celestial/)
- [UPWARD DIAGONAL ARROW](https://neon-gamer-symbols-64.pages.dev/symbol/upward-diagonal-arrow/)
- [SYM 1D40A](https://futuristic-gaming-fonts-52.pages.dev/symbol/sym-1d40a/)
- [SYM 1D464](https://gothic-bio-fonts-13.pages.dev/symbol/sym-1d464/)
- [SYM 26AF](https://clean-aesthetic-fonts-73.pages.dev/symbol/sym-26af/)
- [DOWNWARD DIAGONAL ARROW](https://vintage-library-rune-80.pages.dev/symbol/downward-diagonal-arrow/)
- [BRACKETS](https://scholar-rune-symbols-77.pages.dev/vi/brackets/)
- [SYM 2612](https://dolly-kaomoji-text-94.pages.dev/symbol/sym-2612/)
- [BIOHAZARD SYMBOL](https://sleek-line-symbols-51.pages.dev/symbol/biohazard-symbol/)
- [SYM 2639](https://vintage-library-rune-80.pages.dev/symbol/sym-2639/)
- [VI](https://matrix-glitch-text-37.pages.dev/vi/)
- [SYM 1F47D](https://cyber-clan-tags-23.pages.dev/symbol/sym-1f47d/)
- [SYM 1F910](https://gothic-bio-fonts-61.pages.dev/symbol/sym-1f910/)
- [SYM 26AB](https://vintage-library-rune-80.pages.dev/symbol/sym-26ab/)
- [SYM 1F640](https://anime-sparkle-text-58.pages.dev/symbol/sym-1f640/)
- [FOUR POINT STAR SPARKLE](https://anime-sparkle-text-58.pages.dev/symbol/four-point-star-sparkle/)
- [SYM 2685](https://matrix-glitch-text-37.pages.dev/symbol/sym-2685/)
- [FREEFIRE NAMES](https://dark-poetry-fonts-30.pages.dev/pt/freefire-names/)
- [SYM 1D4A1](https://futuristic-gaming-fonts-52.pages.dev/symbol/sym-1d4a1/)
- [SYM 2689](https://vintage-library-rune-80.pages.dev/symbol/sym-2689/)
- [SYM 265B](https://clean-dot-aesthetic-48.pages.dev/symbol/sym-265b/)
- [SYM 1D41F](https://clean-dot-aesthetic-48.pages.dev/symbol/sym-1d41f/)
- [SYM 273C](https://scholar-rune-symbols-77.pages.dev/symbol/sym-273c/)
- [SYM 2625](https://matrix-glitch-text-59.pages.dev/symbol/sym-2625/)
- [TIBETAN LOTUS BLOSSOM](https://vintage-library-rune-80.pages.dev/symbol/tibetan-lotus-blossom/)
- [LITTLE CAT PAWS KAOMOJI](https://matrix-glitch-text-59.pages.dev/symbol/little-cat-paws-kaomoji/)
- [OUTLINED STAR](https://matrix-glitch-text-59.pages.dev/symbol/outlined-star/)
- [SYM 1FAE0](https://sleek-line-symbols-51.pages.dev/symbol/sym-1fae0/)
- [SYM 1D43A](https://clean-dot-aesthetic-48.pages.dev/symbol/sym-1d43a/)
- [SYM 1D427](https://matrix-glitch-text-37.pages.dev/symbol/sym-1d427/)
- [SYM 1FAE2](https://scholar-rune-symbols-77.pages.dev/symbol/sym-1fae2/)
- [SYM 1D44D](https://gothic-bio-fonts-61.pages.dev/symbol/sym-1d44d/)
- [SYM 1D448](https://anime-sparkle-text-58.pages.dev/symbol/sym-1d448/)
- [SYM 1D458](https://sleek-line-symbols-51.pages.dev/symbol/sym-1d458/)
- [SYM 267E](https://mecha-crosshair-tags-20.pages.dev/symbol/sym-267e/)
- [SYM 26C1](https://sleek-line-symbols-51.pages.dev/symbol/sym-26c1/)
- [SYM 26D5](https://cyber-clan-tags-23.pages.dev/symbol/sym-26d5/)
- [SYM 1F628](https://futuristic-gaming-fonts-52.pages.dev/symbol/sym-1f628/)
- [SYM 1F92D](https://neon-gamer-symbols-64.pages.dev/symbol/sym-1f92d/)
- [SYM 26DE](https://vintage-library-rune-80.pages.dev/symbol/sym-26de/)
- [SYM 1D48C](https://futuristic-gaming-fonts-52.pages.dev/symbol/sym-1d48c/)
- [SYM 26F9](https://cyber-clan-tags-38.pages.dev/symbol/sym-26f9/)
- [SYM 2644](https://nordic-minimal-fonts-67.pages.dev/symbol/sym-2644/)
- [SYM 1D4A0](https://cyber-clan-tags-23.pages.dev/symbol/sym-1d4a0/)
- [STARS](https://dolly-kaomoji-text-94.pages.dev/stars/)
- [JA](https://vintage-library-rune-80.pages.dev/ja/)
- [PT](https://dark-poetry-fonts-30.pages.dev/pt/)
- [TWELVE POINTED STAR](https://nordic-minimal-fonts-67.pages.dev/symbol/twelve-pointed-star/)
- [JA](https://nordic-minimal-fonts-67.pages.dev/ja/)
- [SYM 26D3](https://manga-emotion-symbols-69.pages.dev/symbol/sym-26d3/)
- [SIX POINTED BLACK STAR](https://cyber-clan-tags-23.pages.dev/symbol/six-pointed-black-star/)
- [BORDERS DIVIDERS](https://futuristic-gaming-fonts-52.pages.dev/borders-dividers/)
- [SYM 1D42E](https://clean-dot-aesthetic-48.pages.dev/symbol/sym-1d42e/)
- [SYM 1D41B](https://futuristic-gaming-fonts-52.pages.dev/symbol/sym-1d41b/)
- [SYM 1F60A](https://matrix-glitch-text-37.pages.dev/symbol/sym-1f60a/)
- [TABLE FLIP RAGE KAOMOJI](https://scholar-rune-symbols-77.pages.dev/symbol/table-flip-rage-kaomoji/)
- [SYM 2638](https://vintage-library-rune-80.pages.dev/symbol/sym-2638/)
- [SCHOLAR RUNE SYMBOLS 77.PAGES.DEV](https://scholar-rune-symbols-77.pages.dev/)
- [RIGHT MATHEMATICAL WHITE SQUARE BRACKET](https://sleek-line-symbols-51.pages.dev/symbol/right-mathematical-white-square-bracket/)
- [SYM 1D419](https://matrix-glitch-text-37.pages.dev/symbol/sym-1d419/)
- [BORDERS DIVIDERS](https://neon-gamer-symbols-64.pages.dev/es/borders-dividers/)
- [SYM 1D405](https://sleek-line-symbols-51.pages.dev/symbol/sym-1d405/)
- [ZODIAC CELESTIAL](https://neon-gamer-symbols-64.pages.dev/ja/zodiac-celestial/)
- [CURVED HEART BLOOMY](https://matrix-glitch-text-59.pages.dev/symbol/curved-heart-bloomy/)
- [SYM 1D41F](https://dark-poetry-fonts-30.pages.dev/symbol/sym-1d41f/)
- [SYM 26D2](https://matrix-glitch-text-37.pages.dev/symbol/sym-26d2/)
- [SYM 1D41C](https://futuristic-gaming-fonts-52.pages.dev/symbol/sym-1d41c/)
- [SYM 265A](https://cyber-clan-tags-38.pages.dev/symbol/sym-265a/)
- [SYM 1D451](https://matrix-glitch-text-59.pages.dev/symbol/sym-1d451/)
- [NATURE FLOWERS](https://neon-gamer-symbols-64.pages.dev/nature-flowers/)
- [GOTHIC OBSIDIAN SKULL CREST](https://clean-dot-aesthetic-48.pages.dev/symbol/gothic-obsidian-skull-crest/)
- [SYM 1D41A](https://dark-poetry-fonts-30.pages.dev/symbol/sym-1d41a/)
- [CUPID FEATHERY ARROW](https://scholar-rune-symbols-77.pages.dev/symbol/cupid-feathery-arrow/)
- [TIKTOK CAPTIONS](https://neon-gamer-symbols-64.pages.dev/ru/tiktok-captions/)
- [SYM 1D43A](https://dark-poetry-fonts-30.pages.dev/symbol/sym-1d43a/)
- [SYM 1D444](https://cyber-clan-tags-23.pages.dev/symbol/sym-1d444/)
- [SYM 1F628](https://matrix-glitch-text-37.pages.dev/symbol/sym-1f628/)
- [SYM 1D422](https://soft-pastel-unicode-78.pages.dev/symbol/sym-1d422/)
- [LEFT HEAVY BRACKET BOX](https://cyber-clan-tags-38.pages.dev/symbol/left-heavy-bracket-box/)
- [SYM 1D438](https://clean-aesthetic-fonts-73.pages.dev/symbol/sym-1d438/)
- [SYM 1D484](https://clean-aesthetic-fonts-73.pages.dev/symbol/sym-1d484/)
- [SYM 2617](https://matrix-glitch-text-59.pages.dev/symbol/sym-2617/)
- [SYM 1D462](https://scholar-rune-symbols-77.pages.dev/symbol/sym-1d462/)
- [SYM 1D423](https://gothic-bio-fonts-61.pages.dev/symbol/sym-1d423/)
- [SYM 26D6](https://manga-emotion-symbols-69.pages.dev/symbol/sym-26d6/)
- [SYM 1D45F](https://sleek-line-symbols-51.pages.dev/symbol/sym-1d45f/)
- [SYM 1D430](https://clean-dot-aesthetic-48.pages.dev/symbol/sym-1d430/)
- [GAMING WEAPONS](https://anime-sparkle-text-58.pages.dev/gaming-weapons/)
- [SYM 1D42F](https://sleek-bio-symbols-51.pages.dev/symbol/sym-1d42f/)
- [INSTAGRAM BIO](https://neon-gamer-symbols-64.pages.dev/pt/instagram-bio/)
- [SYM 260F](https://dark-poetry-fonts-30.pages.dev/symbol/sym-260f/)
- [SYM 1D49A](https://sleek-line-symbols-51.pages.dev/symbol/sym-1d49a/)
- [SHADOWED WHITE STAR](https://cyber-clan-tags-38.pages.dev/symbol/shadowed-white-star/)
- [SYM 2743](https://dark-poetry-fonts-30.pages.dev/symbol/sym-2743/)
- [SYM 1D49E](https://sleek-line-symbols-51.pages.dev/symbol/sym-1d49e/)
- [SYM 1D410](https://anime-sparkle-text-58.pages.dev/symbol/sym-1d410/)
- [SYM 265D](https://dark-poetry-fonts-30.pages.dev/symbol/sym-265d/)
- [DISCORD STATUS](https://sleek-line-symbols-51.pages.dev/ja/discord-status/)
- [SKULL AND CROSSBONES](https://dark-poetry-fonts-30.pages.dev/symbol/skull-and-crossbones/)
- [ANIME SPARKLE TEXT 58.PAGES.DEV](https://anime-sparkle-text-58.pages.dev/)
- [SYM 1F921](https://vintage-library-rune-80.pages.dev/symbol/sym-1f921/)
- [SYM 2744](https://cyber-clan-tags-38.pages.dev/symbol/sym-2744/)
- [TAURUS ZODIAC BULL](https://gothic-bio-fonts-61.pages.dev/symbol/taurus-zodiac-bull/)
- [SYM 1F606](https://anime-sparkle-text-58.pages.dev/symbol/sym-1f606/)
- [FLOWER GIRL SMILE KAOMOJI](https://neon-gamer-symbols-64.pages.dev/symbol/flower-girl-smile-kaomoji/)
- [SYM 1D47B](https://sleek-line-symbols-51.pages.dev/symbol/sym-1d47b/)
- [SYM 1F625](https://sleek-bio-symbols-51.pages.dev/symbol/sym-1f625/)
- [SYM 2743](https://cyber-clan-tags-38.pages.dev/symbol/sym-2743/)
- [SYM 1D46F](https://anime-sparkle-text-58.pages.dev/symbol/sym-1d46f/)
- [TENDER GENTLE TEAR KAOMOJI](https://clean-dot-aesthetic-48.pages.dev/symbol/tender-gentle-tear-kaomoji/)
- [SYM 1F498](https://vintage-library-rune-80.pages.dev/symbol/sym-1f498/)
- [HEARTS](https://neon-gamer-symbols-64.pages.dev/pt/hearts/)
- [TRENDING](https://sleek-line-symbols-51.pages.dev/es/trending/)
- [DOWNWARD DIAGONAL ARROW](https://scholar-rune-symbols-77.pages.dev/symbol/downward-diagonal-arrow/)
- [FLOWER GIRL SMILE KAOMOJI](https://sleek-line-symbols-51.pages.dev/symbol/flower-girl-smile-kaomoji/)
- [KAOMOJI](https://anime-sparkle-text-58.pages.dev/ru/kaomoji/)
- [SYM 1D406](https://clean-aesthetic-fonts-73.pages.dev/symbol/sym-1d406/)
- [STARRY LOVE AURA](https://matrix-glitch-text-59.pages.dev/symbol/starry-love-aura/)
- [SYM 1F979](https://neon-gamer-symbols-64.pages.dev/symbol/sym-1f979/)
- [SYM 1D420](https://soft-pastel-unicode-78.pages.dev/symbol/sym-1d420/)
- [ZODIAC CELESTIAL](https://gothic-bio-fonts-61.pages.dev/vi/zodiac-celestial/)
- [SYM 26D8](https://soft-pastel-unicode-78.pages.dev/symbol/sym-26d8/)
- [CHEERING FIGHTING FIST KAOMOJI](https://mecha-crosshair-tags-20.pages.dev/symbol/cheering-fighting-fist-kaomoji/)
- [TIKTOK CAPTIONS](https://sleek-bio-symbols-51.pages.dev/ru/tiktok-captions/)
- [SYM 2631](https://dark-poetry-fonts-30.pages.dev/symbol/sym-2631/)
