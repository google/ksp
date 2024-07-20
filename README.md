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

## KSP2 is in Beta!
KSP2 is a new implementation of the KSP API. It will be faster and easier to use than KSP 1.x. Please refer to the
[KSP2 introduction](docs/ksp2.md) for more details.

### Nightly Builds
Nightly builds of KSP for the latest Kotlin stable releases are published
[here](https://oss.sonatype.org/content/repositories/snapshots/com/google/devtools/ksp/).

```
maven("https://oss.sonatype.org/content/repositories/snapshots")
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
