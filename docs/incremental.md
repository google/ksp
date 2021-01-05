# Incremental Processing

Incremental processing is a way to avoid re-processing of sources as much as possible.
The major goal is to reduce the turn-around time of a typical change-compile-test cycle.
Here is a [wiki link](https://en.wikipedia.org/wiki/Incremental_computing) to the more general
idea of incremental computation.

To be able to know which sources are dirty, i.e., those that need to be reprocessed, KSP needs
processors' help to identify the correspondence of input sources and generated outputs.
Because it is cumbersome and error prone to keep track of which inputs are involved in generating
which outputs, KSP is designed to help with that and only require a minimum set of
**sources that serve as roots that processors start to navigate the code structure**. In other
words, a processor needs to associate an output with sources of those `KSNode`, if obtained from:
* `Resolver.getAllFiles`
* `Resolver.getSymbolsWithAnnotation`
* `Resolver.getClassDesclarationByName`

Currently, only changes in Kotlin and Java sources are tracked. If there is a change in the
classpath, namely in other modules or libraries, a full re-processing will be triggered.

Incremental processing is currently disabled by default. To enable it, set the Gradle property
`ksp.incremental=true`. To enable logs, which dump the dirty set according to dependencies and
outputs, use `ksp.incremental.log=true`. They can be found as `build/*.log`.

## Aggregating v.s. Isolating
The idea is similar but slightly different to the [definition](https://docs.gradle.org/current/userguide/java_plugin.html#sec:incremental_annotation_processing)
in Gradle annotation processing. In KSP,
* *aggregating* / *isolating* is associated with each output, rather than the entire processor
* an isolating output can have several sources
* aggregating means that an output can be affected by any changes

If an output is `aggregating`, any changes may affect it potentially, except removal of files that
don't affect other files.
In other words, if there's a change, all `aggregating` outputs need to be regenerated and therefore
all of their sources will be reprocessed. Note that only registered files and changed / new files
will be re-processed.

For example, an output collecting all symbols with an interesting annotation is `aggregating`.

If an output is not `aggregating`, it only depends on the sources specified. Changes in other
sources do not affect it. Unlike Gradle's java annotation processing, there can be multiple source
files for an output.

For example, a generated class, which is dedicated to an interface it implements, is not
`aggregating`.

In short, if an output may depend on new or any changed sources, it is `aggregating`.
Otherwise it is not.

For readers familiar with Java annotation processing:
* In an *isolating* Java annotation processor, all the outputs are *isolating* in KSP.
* In an *aggregating* Java annotation processor, some outputs can be *isolating* and some be
*aggregating* in KSP.

## Example 1
A processor generates `outputForA` after reading class `A` in `A.kt` and class `B` in `B.kt`,
where `A` extends `B`. The processor got `A` by `Resolver.getSymbolsWithAnnotation` and then got
`B` by `KSClassDeclaration.superTypes` from `A`. Because the inclusion of `B` is due to `A`,
`B.kt` needn't to be specified in `dependencies` for `outputForA`. Note that specifying `B.kt` in this case
doesn't hurt, it is only unnecessary.

```
// A.kt
@Interesting
class A : B()

// B.kt
open class B

// Example1Processor.kt
class Example1Processor : SymbolProcessor {
    ...
    override fun process(resolver: Resolver) {
        val declA = resolver.getSymbolsWithAnnotation("Interesting").first() as KSClassDeclaration
        val declB = declA.superTypes.first().resolve().declaration
        // B.kt isn't required, because it is deducible by KSP.
        val dependencies = Dependencies(aggregating = false, declA.containingFile!!)
        // outputForA.kt
        val outputName = "outputFor${declA.simpleName.asString()}"
        // It depends on A.kt and B.kt.
        val output = codeGenerator.createNewFile(dependencies, "com.example", outputName, "kt")
        output.write("// $declA : $declB\n".toByteArray())
        output.close()
    }
    ...
}
```

## Example 2
Consider sourceA -> outputA, sourceB -> outputB.

When sourceA is changed:
* If outputB is aggregating
  * sourceA and sourceB are reprocessed
* If outputB is not aggregating
  * sourceA is reprocessed.

When sourceC is added:
* If outputB is aggregating
  * sourceC and sourceB are reprocessed
* If outputB is not aggregating
  * sourceC is reprocessed.

When sourceA is removed:
* nothing has to be done.

When sourceB is removed:
* nothing has to be done.

## How Dirtyness Are Determined
A dirty file is either *changed* by users directly, or *affected* by other dirty files
indirectly. In KSP, propagation of dirtyness is done in 2 steps:
* Propagation by *resolution tracing*:
  Resolving a type reference (implicitly or explicitly) is the only way to navigate from one file
  to another. When a type reference is resolved by a processor, a *changed* or *affected* file that
  contains a change that may potentially affect the resolution result will *affect* the file
  containing that reference.
* Propagation by *input-output correspondence*:
  If a source file is *changed* or *affected*, all other source files having some output in common
  with that file are *affected*.

Note that both of them are transitive and the second forms equivalence classes.

## Reporting Bugs
To report a bug, please set Gradle properties `ksp.incremental=true` and `ksp.incremental.log=true`,
and start with a clean build. There are 4 log files:

* `build/kspDirtySetByDeps.log`
* `build/kspDirtySetByOutputs.log`
* `build/kspDirtySet.log`
* `build/kspSourceToOutputs.log`

They contain file names of sources and outputs, plus the timestamps of the builds.
The first two are only avaiable in successive incremental builds and not available in clean builds.