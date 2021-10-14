# Incremental Processing

Incremental processing is a processing technique that avoids re-processing of sources as much as possible.
The primary goal of incremental processing is to reduce the turn-around time of a typical change-compile-test cycle.
For general information, see Wikipedia's article on [incremental computing](https://en.wikipedia.org/wiki/Incremental_computing).

To determine which sources are _dirty_ (i.e., those that need to be reprocessed), KSP needs
processors' help to identify which input sources correspond to which generated outputs.
To help with this often cumbersome and error-prone process, KSP is designed to require only a minimal
set of *root sources* that processors use as starting points to navigate the code structure. In other
words, a processor needs to associate an output with the sources of the corresponding `KSNode` if the
`KSNode` is obtained from any of the following:
* `Resolver.getAllFiles`
* `Resolver.getSymbolsWithAnnotation`
* `Resolver.getClassDeclarationByName`
* `Resolver.getDeclarationsFromPackage`

Currently, only changes in Kotlin and Java sources are tracked. Changes to the classpath, namely to other modules
or libraries, trigger a full re-processing of all sources by default. To track changes in classpath, set the Gradle property
`ksp.incremental.intermodule=true` for an experimental implementation on JVM.

Incremental processing is currently enabled by default. To disable it, set the Gradle property
`ksp.incremental=false`. To enable logs that dump the dirty set according to dependencies and
outputs, use `ksp.incremental.log=true`. These log files can be found in the `build` output folder with a `.log` file extension.

## Aggregating v.s. Isolating

Similar to the concepts in [Gradle annotation processing](https://docs.gradle.org/current/userguide/java_plugin.html#sec:incremental_annotation_processing),
KSP supports both *aggregating* and *isolating* modes. Note that unlike Gradle annotation processing,
KSP categorizes each output as either aggregating or isolating, rather than the entire processor.


An aggregating output can potentially be affected by any input changes, with the exception of removing files that don't affect
other files. This means that any input change results in a rebuild of all aggregating outputs,
which in turn means that all of the corresponding registered, new, and modified source files are reprocessed.

As an example, an output that collects all symbols with a particular annotation is considered an `aggregating` output.


An *isolating* output depends only on its specified sources. Changes to other sources do not affect an isolating output.
Note that unlike Gradle annotation processing, you can define multiple source files for a given output.

As an example, a generated class that is dedicated to an interface it implements is considered *isolating*.

To summarize, if an output might depend on new or any changed sources, it is considered *aggregating*. Otherwise, the output is *isolating*.

Here's a summary for readers familiar with Java annotation processing:
* In an *isolating* Java annotation processor, all the outputs are *isolating* in KSP.
* In an *aggregating* Java annotation processor, some outputs can be *isolating* and some be
*aggregating* in KSP.


### Aggregating v.s. Isolating: how it is implemented?
The dependencies are calculated by the association of input and output files, instead of annotations.
Note that it is a many-to-many relation.

The dirtyness propagation rules due to input-output associations are:
1. If an input file is changed, it will always be reprocessed.
2. If an input file is changed, and it is associated with an output, then all other input files associated with the
   same output will also be reprocessed. Note that this is transitive, i.e., invalidation happens repeatedly until
   there is no new dirty file.
3. All input files that are associated with one or more aggregating outputs will be reprocessed.
   In other words, if an input file isn't associated with any aggregating outputs, it won't be reprocessed
   (unless it meets 1. or 2. in the above).

Reasons are:
1. If an input is changed, new information can be introduced and therefore processors need to run again with the input.
2. An output is made out of a set of inputs. Processors may need all the inputs to regenerate the output.
3. `aggregating=true` means that an output may potentially depend on new information, which can come from either new
   files, or changed, existing files.
   `aggregating=false` means that processor is sure that the information only comes from certain input files and never
   from other or new files.

## Example 1
A processor generates `outputForA` after reading class `A` in `A.kt` and class `B` in `B.kt`,
where `A` extends `B`. The processor got `A` by `Resolver.getSymbolsWithAnnotation` and then got
`B` by `KSClassDeclaration.superTypes` from `A`. Because the inclusion of `B` is due to `A`,
`B.kt` doesn't need to be specified in `dependencies` for `outputForA`. You can still specify `B.kt` in this case, but it is unnecessary.

```kotlin
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
        // B.kt isn't required, because it is able to be deduced as a dependency by KSP.
        val dependencies = Dependencies(aggregating = true, declA.containingFile!!)
        // outputForA.kt
        val outputName = "outputFor${declA.simpleName.asString()}"
        // outputForA depends on A.kt and B.kt.
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
  * Both sourceA and sourceB are reprocessed
* If outputB is isolating
  * Only sourceA is reprocessed.

When sourceC is added:
* If outputB is aggregating
  * Both sourceC and sourceB are reprocessed
* If outputB is isolating
  * Only sourceC is reprocessed.

When sourceA is removed:
* Nothing needs to be reprocessed.

When sourceB is removed:
* Nothing needs to be reprocessed.

## How file dirtiness is determined
A dirty file is either directly *changed* by users or indirectly *affected* by other dirty files. KSP propagates dirtiness in two steps:
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
and perform a clean build. This build produces two log files:

* `build/kspCaches/<source set>/logs/kspDirtySet.log`
* `build/kspCaches/<source set>/logs/kspSourceToOutputs.log`

You can then run successive incremental builds, which will generate two additional log files:

* `build/kspCaches/<source set>/logs/kspDirtySetByDeps.log`
* `build/kspCaches/<source set>/logs/kspDirtySetByOutputs.log`

These logs contain file names of sources and outputs, plus the timestamps of the builds.
