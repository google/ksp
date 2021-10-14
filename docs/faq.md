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

## Why do I get "ksp-A.B.C is too old for kotlin-X.Y.Z. Please upgrade ksp or downgrade kotlin-gradle-plugin"?
KSP is comprised of two parts: API and implementation:
* The API rarely changes and is backward compatible; There can be new interfaces, but old interfaces never change.
* Each release of KSP implementation is tied to a specific compiler version.

Processors only depend on API and therefore are not tied to compiler versions.
On the other hand, users of processors need to bump KSP version when bumping the compiler version in their project.
Note that they don't need to bump processor's version because processors only depend on API.

For example, Some-Nice-Processor-2.0 is released and tested with KSP 1.0.1, which depends strictly on Kotlin 1.6.0.
Users can expect that the same Some-Nice-Processor-2.0 will work with Kotlin 1.6.20. The only thing users
need to do is bump KSP to a version (e.g., KSP 1.1.0) that is built for Kotlin 1.6.20.

## Can I use a newer KSP implementation with an older Kotlin compiler?
With the same language version, Kotlin compiler is supposed to be backward compatible. Bumping Kotlin compiler should be
trivial most of the time. If you need a newer KSP implementation, please upgrade the Kotlin compiler accordingly.

## How often do you update KSP?
KSP tries to follow [Sematic Versioning](https://semver.org/) as close as possible.
With KSP version `major.minor.patch`,
* `major` is reserved for incompatible API changes. There is no pre-determined schedule for this.
* `minor` is reserved for new features. This is going to be updated approximately quarterly.
* `patch` is reserved for bug fixes and new Kotlin releases. It's updated roughly monthly.

Usually a corresponding KSP release is available within a couple of days after a new Kotlin version is released,
including the pre-releases (e.g., M1/M2/RC).

## What is KSP’s future roadmap?
The following items have been planned:
* Support [new Kotlin compiler](https://kotlinlang.org/docs/roadmap.html)
* Improve support to multiplatform. E.g., running KSP on a subset of targets / sharing computations between targets.
* Improve performance. There a bunch of optimizations to be done!
* Keep fixing bugs.

Please feel free to reach out to us on
[Kotlin Slack workspace](https://surveys.jetbrains.com/s3/kotlin-slack-sign-up?_ga=2.185732459.358956950.1590619123-888878822.1567025441)
if you would like to discuss any ideas. Filing GitHub issues / feature requests or pull requests are also welcome!
