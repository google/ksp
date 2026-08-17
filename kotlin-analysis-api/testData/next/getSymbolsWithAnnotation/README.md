# Test directory for `Resolver.getSymbolsWithAnnotation`

This test directory is dedicated to verifying the different implementations of
`Resolver.getSymbolsWithAnnotation`, specifically focusing on parity between the two resolution
strategies available in the KSP2 Kotlin Analysis API backend:

1. **`AAResolutionStrategy`**: The default implementation that uses a full tree traversal
(`CollectAnnotatedSymbolsVisitor`) and relies on the Analysis API for all resolutions.
2. **`PsiResolutionStrategy`**: An experimental, optimized strategy that uses PSI to find
annotations more efficiently by avoiding full resolution where possible.

### Why this directory exists

* **Coverage**: There are many tests in KSP that cover various scenarios, but they often
use different processors that use other entry points than `Resolver.getSymbolsWithAnnotation`.
This directory ensures we have a set of tests specifically running with
`GetSymbolsWithAnnotationProcessor` to provide targeted coverage of these resolution strategies.
* **`inDepth = false`**: These tests primarily target the default `inDepth = false` mode.
This is the mode where the two strategies differ significantly. When `inDepth = true`,
`PsiResolutionStrategy` currently delegates its implementation to `AAResolutionStrategy`, meaning
they follow the same execution path.
* **Isolation**: By keeping these tests separate, we avoid cluttering broader API tests while
still ensuring that any changes to the resolution strategies are thoroughly validated across various
code constructs (like nested or local classes).

### Important note when writing tests

Tests that use `GetSymbolsWithAnnotationProcessor` must always have a comment containing the fully
qualified names of the annotations the processor should use to look up symbols. For instance,
the `localClasses.kt` file declares an annotation class `Anno` and contains the line:

```kotlin
// PROCESSOR INPUT: Anno
```

Likewise, the `metaAnnotations.kt` file contains the line:

```kotlin
// PROCESSOR INPUT: kotlin.annotation.Retention, kotlin.annotation.Target
```

It then finds all symbols annotated with `kotlin.annotation.Retention` and finds all symbols
annotated with `kotlin.annotation.Target`.
