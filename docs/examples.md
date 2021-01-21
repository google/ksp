# Examples

Get all member functions that are declared directly within a class:

```kotlin
fun KSClassDeclaration.getDeclaredFunctions(): List<KSFunctionDeclaration> {
    return this.declarations.filterIsInstance<KSFunctionDeclaration>()
}
```

Determine whether a class or function is local to another function:

```kotlin
fun KSDeclaration.isLocal(): Boolean {
    return this.parentDeclaration != null && this.parentDeclaration !is KSClassDeclaration
}
```

Determine whether a class member is visible to another:

```kotlin
fun KSDeclaration.isVisibleFrom(other: KSDeclaration): Boolean {
    return when {
        // locals are limited to lexical scope
        this.isLocal() -> this.parentDeclaration == other
        // file visibility or member
        this.isPrivate() -> {
            this.parentDeclaration == other.parentDeclaration
                    || this.parentDeclaration == other
                    || (
                        this.parentDeclaration == null
                            && other.parentDeclaration == null
                            && this.containingFile == other.containingFile
                    )
        }
        this.isPublic() -> true
        this.isInternal() && other.containingFile != null && this.containingFile != null -> true
        else -> false
    }
}
```

### Example annotations

```kotlin
// Find out suppressed names in a file annotation:
// @file:kotlin.Suppress("Example1", "Example2")
fun KSFile.suppressedNames(): List<String> {
    val ignoredNames = mutableListOf<String>()
    annotations.forEach {
        if (it.shortName.asString() == "Suppress" && it.annotationType.resolve()?.declaration?.qualifiedName?.asString() == "kotlin.Suppress") {
            it.arguments.forEach {
                (it.value as List<String>).forEach { ignoredNames.add(it) }
            }
        }
    }
    return ignoredNames
}
```
