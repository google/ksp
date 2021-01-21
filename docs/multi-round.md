## Introduction

To achieve multiple round processing in KSP, a change to compiler is needed, given the timeline of compiler release, it will be in stable release of 1.4.30. Therefore, we are providing this guide so you can try out multiple round for your project before stable compiler release. In this guide, we will go through how to apply the multiple round ready compiler and the multiple round version of KSP to your project.

Due to this being a preview version and pending merging into master, incremental processing is not supported in this version.


## Quick start guide


#### Compiler preparation



*   Add `kotlin-dev` maven repository to your project

```
repositories {
        ...
        maven("https://dl.bintray.com/kotlin/kotlin-dev")
}
```


*   Apply `1.4.30-M2-104` version of kotlin compiler to your project

```
plugins {
    kotlin("jvm") version "1.4.30-M2-104" apply false
}
```




#### (Optional) Check out and build KSP 



*   This is intended for those who prefer to build KSP locally
*   Check out <code>[multiple-round branch](https://github.com/google/ksp/tree/multi-round)</code> from ksp github repository
*   Build it to local maven repository with <code>./gradlew publishToMavenLocal</code>


#### Apply multiple round version KSP dependency to your project



*   Change KSP dependencies to be
    *   `implementation("com.google.devtools.ksp:symbol-processing-api:1.4.30-M2-104-multiple-round-preview-20201223")`
        *  If you are building KSP locally 
            *   You can check the locally built KSP version number at `~/.m2/repository/com/google/devtools/ksp/symbol-processing`, it is always sticking to your build date.
            *   Add `mavenLocal()` to your processors and workload module's repository configuration, as well as `settings.gradle` file

```
repositories {
    ...
    mavenLocal()
    ...
}
```




#### Breaking changes to your processor



*   Change your processor's `process()` function to return a list of deferred symbols `List&lt;KSAnnotated>`
*   Use `KSAnnotated.validate()` to filter invalid symbols to be deferred to next round.
*   Sample code to defer invalid symbols with validation check.

```
override fun process(resolver: Resolver): List<KSAnnotated> {
    val symbols = resolver.getSymbolsWithAnnotation("com.example.annotation.Builder")
    val ret = symbols.filter { !it.validate() }
    symbols
        .filter { it is KSClassDeclaration && it.validate() }
        .map { it.accept(BuilderVisitor(), Unit) }
    return ret
}
```




## Multiple round behavior


#### Termination condition

After applying multiple round processing, the termination condition will be until no new files are generated. If, when the termination condition is met, there are still deferred symbols not processed, KSP will log an error message for every processor with unprocessed deferred symbols.


#### Files accessible at each round

Both newly generated files and existing files are accessible via `Resolver`, there are 2 APIs provided for accessing files, `Resolver.getAllFiles()` and `Resolver.getNewFiles`. `getAllFiles` will return a combined list of both existing files and newly generated files, while `getNewFiles` only returns newly generated files.


#### Changes to getSymbolsAnnotatedWith

To avoid unnecessary reprocessing of symbols, `getSymbolsAnnotatedWith` will only return symbols found in newly generated files


#### Processor instantiating

Processor instance will only be created once, which means you can store information into the processor to be used for later rounds.


#### Information consistent cross rounds

All KSP symbols will not be reusable cross rounds, due to the resolution result can potentially change based on what was generated last round. However, since KSP does not allow modifying existing code, information such as the string value for a name of a symbol should still be reusable. To summarize, processors can keep information from previous round on their own, but need to bear in mind that they might be invalid in future rounds.


## Advanced


#### Write your own validation logic

Default validation logic provided by KSP checks all reachable symbols inside the enclosing scope of the symbol that is being checked. This might not suit your use case. You can reference `KSValidateVisitor` and write your own validation logic by providing a custom `predicate` lambda which is used by `KSValidateVisitor` to filter out symbols that need to be checked.
