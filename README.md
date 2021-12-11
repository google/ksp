# Kotlin Symbol Processing API

Kotlin Symbol Processing (KSP) is an API that you can use to develop
lightweight compiler plugins. KSP provides a simplified compiler plugin
API that leverages the power of Kotlin while keeping the learning curve at
a minimum. Compared to KAPT, annotation processors that use KSP can run up to 2x faster.

To learn more about how KSP compares to KAPT, check out [why KSP](/docs/why-ksp.md). To get started writing a KSP processor, take a look at the [KSP quickstart](/docs/quickstart.md).

## Overview

The KSP API processes Kotlin programs idiomatically. KSP understands
Kotlin-specific features, such as extension functions, declaration-site
variance, and local functions. KSP also models types explicitly and
provides basic type checking, such as equivalence and assign-compatibility.

The API models Kotlin program structures at the symbol level according to
[Kotlin grammar](https://kotlinlang.org/docs/reference/grammar.html). When
KSP-based plugins process source programs, constructs like classes, class
members, functions, and associated parameters are easily accessible for the
processors, while things like if blocks and for loops are not.

Conceptually, KSP is similar to
[KType](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.reflect/-k-type/)
in Kotlin reflection. The API allows processors to navigate from class
declarations to corresponding types with specific type arguments and
vice-versa. Substituting type arguments, specifying variances, applying
star projections, and marking nullabilities of types are also possible.

Another way to think of KSP is as a pre-processor framework of Kotlin
programs. If we refer to KSP-based plugins as _symbol processors_, or
simply _processors_, then the data flow in a compilation can be described
in the following steps:

1. Processors read and analyze source programs and resources.
1. Processors generate code or other forms of output.
1. The Kotlin compiler compiles the source programs together with the
   generated code.

Unlike a full-fledged compiler plugin, processors cannot modify the code.
A compiler plugin that changes language semantics can sometimes be very
confusing. KSP avoids that by treating the source programs as read-only.

## How KSP looks at source files

Most processors navigate through the various program structures of the
input source code. Before diving into usage of the API, let's look at how
a file might look from KSP's point of view:

```kotlin
KSFile
  packageName: KSName
  fileName: String
  annotations: List<KSAnnotation>  (File annotations)
  declarations: List<KSDeclaration>
    KSClassDeclaration // class, interface, object
      simpleName: KSName
      qualifiedName: KSName
      containingFile: String
      typeParameters: KSTypeParameter
      parentDeclaration: KSDeclaration
      classKind: ClassKind
      primaryConstructor: KSFunctionDeclaration
      superTypes: List<KSTypeReference>
      // contains inner classes, member functions, properties, etc.
      declarations: List<KSDeclaration>
    KSFunctionDeclaration // top level function
      simpleName: KSName
      qualifiedName: KSName
      containingFile: String
      typeParameters: KSTypeParameter
      parentDeclaration: KSDeclaration
      functionKind: FunctionKind
      extensionReceiver: KSTypeReference?
      returnType: KSTypeReference
      parameters: List<KSValueParameter>
      // contains local classes, local functions, local variables, etc.
      declarations: List<KSDeclaration>
    KSPropertyDeclaration // global variable
      simpleName: KSName
      qualifiedName: KSName
      containingFile: String
      typeParameters: KSTypeParameter
      parentDeclaration: KSDeclaration
      extensionReceiver: KSTypeReference?
      type: KSTypeReference
      getter: KSPropertyGetter
        returnType: KSTypeReference
      setter: KSPropertySetter
        parameter: KSValueParameter
```

This view lists common things that are declared in the file--classes,
functions, properties, and so on.

## SymbolProcessorProvider: The entry point

KSP expects an implementation of the `SymbolProcessorProvider` interface to instantiate `SymbolProcessor`:

```kotlin
interface SymbolProcessorProvider {
    fun create(environment: SymbolProcessorEnvironment): SymbolProcessor
}
```

While `SymbolProcessor` is defined as:

```kotlin
interface SymbolProcessor {
    fun process(resolver: Resolver): List<KSAnnotated> // Let's focus on this
    fun finish() {}
    fun onError() {}
}
```

A `Resolver` provides `SymbolProcessor` with access to compiler details
such as symbols. A processor that finds all top-level functions and non-local functions in top-level
classes might look something like this:

```kotlin
class HelloFunctionFinderProcessor : SymbolProcessor() {
    ...
    val functions = mutableListOf<String>()
    val visitor = FindFunctionsVisitor()

    override fun process(resolver: Resolver) {
        resolver.getAllFiles().map { it.accept(visitor, Unit) }
    }

    inner class FindFunctionsVisitor : KSVisitorVoid() {
        override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
            classDeclaration.getDeclaredFunctions().map { it.accept(this, Unit) }
        }

        override fun visitFunctionDeclaration(function: KSFunctionDeclaration, data: Unit) {
            functions.add(function)
        }

        override fun visitFile(file: KSFile, data: Unit) {
            file.declarations.map { it.accept(this, Unit) }
        }
    }
    ...
    
    class Provider : SymbolProcessorProvider {
        override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor = ...
    }
}
```
## Resources

Some handy links:

* [Quickstart](/docs/quickstart.md)
* [Why use KSP?](/docs/why-ksp.md)
* [Examples](/docs/examples.md)
* [How KSP models Kotlin code](/docs/ksp-additional-details.md)
* [Reference for Java annotation processor authors](/docs/reference.md)
* [Incremental processing notes](/docs/incremental.md)
* [Multiple round processing notes](/docs/multi-round.md)
* [KSP on multiplatform projects](/docs/kmp.md)
* [Running KSP from command line](/docs/cmdline.md)
* [Contributor guide](CONTRIBUTING.md)
* [FAQ](/docs/faq.md)

## Feedback and Bug Reporting

[Please let us know what you think about KSP by filing a Github issue](https://github.com/google/ksp/issues)
or connecting with our team in the `#ksp` channel in the
[Kotlin Slack workspace](https://surveys.jetbrains.com/s3/kotlin-slack-sign-up?_ga=2.185732459.358956950.1590619123-888878822.1567025441)!

## Ongoing and Future Works

Here are some planned features that have not yet been completely implemented:
* Support [new Kotlin compiler](https://kotlinlang.org/docs/roadmap.html)
* Improve support to multiplatform. E.g., running KSP on a subset of targets / sharing computations between targets
* Improve performance. There a bunch of optimizations to be done!
* Make the IDE aware of the generated code
* Keep fixing bugs!

## Supported libraries

The table below includes a list of popular libraries on Android and their various stages of support for KSP. If your library is missing, please feel free to submit a pull request.

|Library|Status|Tracking issue for KSP|
|---|---|---|
|Room|[Experimentally supported](https://developer.android.com/jetpack/androidx/releases/room#2.3.0-beta02)|   |
|Moshi|[Officially supported](https://github.com/square/moshi)|   |
|RxHttp|[Officially supported](https://github.com/liujingxing/rxhttp)|   |
|Kotshi|[Officially supported](https://github.com/ansman/kotshi)|   |
|Lyricist|[Officially supported](https://github.com/adrielcafe/lyricist)|   |
|Lich SavedState|[Officially supported](https://github.com/line/lich/tree/master/savedstate)|   |
|gRPC Dekorator|[Officially supported](https://github.com/mottljan/grpc-dekorator)|   |
|Auto Factory|Not yet supported|[Link](https://github.com/google/auto/issues/982)|
|Dagger|Not yet supported|[Link](https://github.com/google/dagger/issues/2349)|
|Hilt|Not yet supported|[Link](https://issuetracker.google.com/179057202)|
|Glide|Not yet supported|[Link](https://github.com/bumptech/glide/issues/4492)|
|DeeplinkDispatch|[Supported via airbnb/DeepLinkDispatch#323](https://github.com/airbnb/DeepLinkDispatch/pull/323)| |
|Databinding|Not yet supported|[Link](https://issuetracker.google.com/issues/173030256)|
