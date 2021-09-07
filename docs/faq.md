# Frequently Asked Questions

## Why KSP?
KSP has several advantages over KAPT:
* It is faster.
* The API is more fluent for Kotlin users.
* It supports multiple round processing on generated Kotlin sources.
* It is being designed with multiplatform compatibility in mind.

## Why is KSP faster than KAPT?
KAPT has to parse and resolve every type reference in order to generate Java stubs, whereas KSP resolves references on-demand. Delegating to javac also takes time.

Additionally, KSP’s incremental processing model has a finer granularity than just isolating and aggregating. It finds more opportunities to avoid reprocessing everything. Also, because KSP traces symbol resolutions dynamically, a change in a file is less likely to pollute other files and therefore the set of files to be reprocessed is smaller. This is not possible for KAPT because it delegates processing to javac.

## Is KSP Kotlin specific?
KSP can process Java sources as well. The API is unified, meaning that when you parse a Java class and a Kotlin class you get a unified data structure in KSP.

## What is KSP’s future roadmap?
The following items have been planned:
* Support [new Kotlin compiler](https://kotlinlang.org/docs/roadmap.html)
* Improve support to multiplatform. E.g., running KSP on a subset of targets / sharing computations between targets.
* Improve performance. There a bunch of optimizations to be done!
* Keep fixing bugs.

Please feel free to reach out to us on
[Kotlin Slack workspace](https://surveys.jetbrains.com/s3/kotlin-slack-sign-up?_ga=2.185732459.358956950.1590619123-888878822.1567025441)
if you would like to discuss any ideas. Filing GitHub issues / feature requests or pull requests are also welcome!
