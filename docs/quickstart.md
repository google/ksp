# Quickstart

[Here's](https://github.com/google/ksp/releases/download/1.4.32-1.0.0-alpha06/playground.zip) a sample processor that you can check out.

## Create a processor of your own

* Create an empty gradle project.
* Specify version `1.4.32` of the Kotlin plugin in the root project for use in other project modules.

  ```
  plugins {
      kotlin("jvm") version "1.4.32" apply false
  }

  buildscript {
      dependencies {
          classpath(kotlin("gradle-plugin", version = "1.4.32"))
      }
  }
  ```

* Add a module for hosting the processor.
* In the module's `build.gradle.kts` file, do the following:
    * Add `google()` to repositories so that Gradle can find our plugins.
    * Apply Kotlin plugin
    * Add the KSP API to the `dependencies` block.

  ```
  plugins {
      kotlin("jvm")
  }

  repositories {
      mavenCentral()
      google()
  }

  dependencies {
      implementation("com.google.devtools.ksp:symbol-processing-api:1.4.32-1.0.0-alpha06")
  }
  ```

* The processor you're writing needs to implement [`com.google.devtools.ksp.processing.SymbolProcessor`](../api/src/main/kotlin/com/google/devtools/ksp/processing/SymbolProcessor.kt).
  Note the following:
  * Your main logic should be in the `process()` method.
  * Use `CodeGenerator` in the `init()` method for code generation. You can also save
    the `CodeGenerator` instance for later use in either `process()` or `finish()`.
  * Use `resolver.getSymbolsWithAnnotation()` to get the symbols you want to process, given
    the fully-qualified name of an annotation.
  * A common use case for KSP is to implement a customized visitor (interface
    `com.google.devtools.ksp.symbol.KSVisitor`) for operating on symbols. A simple template
    visitor is `com.google.devtools.ksp.symbol.KSDefaultVisitor`.
  * For sample implementations of the `SymbolProcessor` interface, see the following files
    in the sample project.
    * `src/main/kotlin/BuilderProcessor.kt`
    * `src/main/kotlin/TestProcessor.kt`
  * After writing your own processor, register your processor to the package by including
    the fully-qualified name of that processor in
    `resources/META-INF/services/com.google.devtools.ksp.processing.SymbolProcessor`.

## Use your own processor in a project

<details open>
<summary>Setup using Kotlin DSL</summary>
  
* Create another module that contains a workload where you want to try out your processor.
* In the project's `settings.gradle.kts`, add `google()` to `repositories` for the KSP plugin.
  
  ```
  pluginManagement {
      repositories {
         gradlePluginPortal()
         google()
      }
  }
  ```

* In the new module's `build.gradle.kts`, do the following:
  * Apply the `com.google.devtools.ksp` plugin with the specified version.
  * Add `ksp(<your processor>)` to the list of dependencies.
* Run `./gradlew build`. You can find the generated code under
  `build/generated/source/ksp`.
* Here's a `sample build.gradle.kts` to apply the KSP plugin to a workload. 

  ```
  plugins {
      id("com.google.devtools.ksp") version "1.4.32-1.0.0-alpha06"
      kotlin("jvm") 
  }

  version = "1.0-SNAPSHOT"

  repositories {
      mavenCentral()
      google()
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

* In the projects `settings.gradle`, add `google()` to `repositories` for the KSP plugin:
    
  ```groovy
  pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
    }
  }
  ```
* In your projects `build.gradle` file add a plugins block containing the ksp plugin:

  ```groovy
  plugins {
    id "com.google.devtools.ksp" version "1.4.32-1.0.0-alpha06"
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
Processor options in `SymbolProcessor.init(options: Map<String, String>, ...)` are specified in gradle build scripts:
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
build/generated/ksp/main/resources
```
