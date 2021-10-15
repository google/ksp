# Quickstart

[Here's](https://github.com/google/ksp/tree/main/examples/playground) a sample processor that you can check out.

## Create a processor of your own

* Create an empty gradle project.
* Specify version `1.5.31` of the Kotlin plugin in the root project for use in other project modules.

  ```kotlin
  plugins {
      kotlin("jvm") version "1.5.31" apply false
  }

  buildscript {
      dependencies {
          classpath(kotlin("gradle-plugin", version = "1.5.31"))
      }
  }
  ```

* Add a module for hosting the processor.
* In the module's `build.gradle.kts` file, do the following:
    * Apply Kotlin plugin
    * Add the KSP API to the `dependencies` block.

  ```kotlin
  plugins {
      kotlin("jvm")
  }

  repositories {
      mavenCentral()
  }

  dependencies {
      implementation("com.google.devtools.ksp:symbol-processing-api:1.5.31-1.0.0")
  }
  ```

* You'll need to implement [`com.google.devtools.ksp.processing.SymbolProcessor`](../api/src/main/kotlin/com/google/devtools/ksp/processing/SymbolProcessor.kt) and
 [`com.google.devtools.ksp.processing.SymbolProcessorProvider`](../api/src/main/kotlin/com/google/devtools/ksp/processing/SymbolProcessorProvider.kt).
 Your implementation of `SymbolProcessorProvider` will be loaded as a service to instantiate the `SymbolProcessor` you implement.
  Note the following:
  * Implement [`SymbolProcessorProvider.create()`](https://github.com/google/ksp/blob/master/api/src/main/kotlin/com/google/devtools/ksp/processing/SymbolProcessorProvider.kt) to create a `SymbolProcessor`. Dependencies your processor needs (e.g. `CodeGenerator`, processor options) are passed through the parameters of `SymbolProcessorProvider.create()`.
  * Your main logic should be in the [`SymbolProcessor.process()`](https://github.com/google/ksp/blob/master/api/src/main/kotlin/com/google/devtools/ksp/processing/SymbolProcessor.kt) method.
  * Use `resolver.getSymbolsWithAnnotation()` to get the symbols you want to process, given
    the fully-qualified name of an annotation.
  * A common use case for KSP is to implement a customized visitor (interface
    `com.google.devtools.ksp.symbol.KSVisitor`) for operating on symbols. A simple template
    visitor is `com.google.devtools.ksp.symbol.KSDefaultVisitor`.
  * For sample implementations of the `SymbolProcessorProvider` and `SymbolProcessor` interfaces, see the following files
    in the sample project.
    * `src/main/kotlin/BuilderProcessor.kt`
    * `src/main/kotlin/TestProcessor.kt`
  * After writing your own processor, register your processor provider to the package by including
    its fully-qualified name in
    `resources/META-INF/services/com.google.devtools.ksp.processing.SymbolProcessorProvider`.

## Use your own processor in a project

<details open>
<summary>Setup using Kotlin DSL</summary>
  
* Create another module that contains a workload where you want to try out your processor.
  
  ```kotlin
  pluginManagement {
      repositories {
         gradlePluginPortal()
      }
  }
  ```

* In the new module's `build.gradle.kts`, do the following:
  * Apply the `com.google.devtools.ksp` plugin with the specified version.
  * Add `ksp(<your processor>)` to the list of dependencies.
* Run `./gradlew build`. You can find the generated code under
  `build/generated/source/ksp`.
* Here's a `sample build.gradle.kts` to apply the KSP plugin to a workload. 

  ```kotlin
  plugins {
      id("com.google.devtools.ksp") version "1.5.31-1.0.0"
      kotlin("jvm") 
  }

  version = "1.0-SNAPSHOT"

  repositories {
      mavenCentral()
  }

  dependencies {
      implementation(kotlin("stdlib-jdk8"))
      implementation(project(":test-processor"))
      ksp(project(":test-processor"))
  }
  ```

</details>
<details>
<summary>Setup using Groovy</summary>

    
  ```groovy
  pluginManagement {
    repositories {
        gradlePluginPortal()
    }
  }
  ```
* In your projects `build.gradle` file add a plugins block containing the ksp plugin:

  ```groovy
  plugins {
    id "com.google.devtools.ksp" version "1.5.31-1.0.0"
  }
  ```
  
* In the modules `build.gradle`, add the following:
  * Apply the `com.google.devtools.ksp` plugin:
  
  ```groovy
  apply plugin: 'com.google.devtools.ksp'
  ```
  
  * Add `ksp <your processor>` to the list of dependencies.
  
  ```groovy
  dependencies {
      implementation "org.jetbrains.kotlin:kotlin-stdlib:$kotlin_version"
      implementation project(":test-processor")
      ksp project(":test-processor")
  }
  ```

</details>

## Pass Options to Processors
Processor options in `SymbolProcessorEnvironment.options` are specified in gradle build scripts:
```
  ksp {
    arg("option1", "value1")
    arg("option2", "value2")
    ...
  }
```

## Make IDE Aware Of Generated Code
By default, IntelliJ or other IDEs don't know about the generated code and therefore
references to those generated symbols will be marked unresolvable.
To make, for example, IntelliJ be able to reason about the generated symbols,
the following paths need to be marked as generated source root:

```
build/generated/ksp/main/kotlin/
build/generated/ksp/main/java/
```

and perhaps also resource directory if your IDE supports them:

```
build/generated/ksp/main/resources/
```

It may also be necessary to configure these directories in your KSP consumer module:

###### build.gradle.kts
```kotlin
kotlin {
    sourceSets.main {
        kotlin.srcDir("build/generated/ksp/main/kotlin")
    }
    sourceSets.test {
        kotlin.srcDir("build/generated/ksp/test/kotlin")
    }
}
```

If you are using IntelliJ IDEA and KSP in a Gradle plugin then the above snippet will give the following warning:

> Execution optimizations have been disabled for task ':publishPluginJar' to ensure correctness due to the following reasons:
>  - Gradle detected a problem with the following location: '../build/generated/ksp/main/kotlin'. Reason: Task ':publishPluginJar' uses this output of task ':kspKotlin' without declaring an explicit or implicit dependency. This can lead to incorrect results being produced, depending on what order the tasks are executed. Please refer to https://docs.gradle.org/7.2/userguide/validation_problems.html#implicit_dependency for more details about this problem.

In this case, use this instead:

```kotlin
plugins {
    // …
    idea
}
// …
idea {
    module {
        // Not using += due to https://github.com/gradle/gradle/issues/8749
        sourceDirs = sourceDirs + file("build/generated/ksp/main/kotlin") // or tasks["kspKotlin"].destination
        testSourceDirs = testSourceDirs + file("build/generated/ksp/test/kotlin")
        generatedSourceDirs = generatedSourceDirs + file("build/generated/ksp/main/kotlin") + file("build/generated/ksp/test/kotlin")
    }
}
```
