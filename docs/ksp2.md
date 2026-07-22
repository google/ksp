# Introduction to KSP2

KSP2 is a new implementation of the KSP API. Unlike KSP 1.x, it is no longer a compiler plugin and is built on the same
set of Kotlin compiler APIs shared with IntelliJ IDEA, Android Lint, etc. Compared to the compiler-plugin approach, this
allows a finer control of the program life cycle, simplifies KSP’s implementation and is more efficient. It is also a
response to the compiler migration to K2, whose compiler plugin API is different from the one KSP1 uses.

Not being a compiler plugin and having a better control of the program life cycles means that it’s straightforward to
provide an entry point that can be called by other programs. This is especially convenient for testing processors 
as a processor author.
KSP2 also provides a new command line tool that has its own main function, instead of being part of the compiler
invocation.

With the new implementation, it is also a great opportunity to introduce some refinements in the API behavior so that
developers building on KSP will be more productive, have better debuggability and error recovery. For example, when
resolving `Map<String, NonExistentType>`, KSP1 simply returns an error type. In KSP2, `Map<String, ErrorType>` will be
returned instead.

## API Changes

Please refer to [KSP2 API Changes](ksp2api.md) to see more details.

## Debugging

Unlike KSP1, which runs in `KotlinCompileDaemon`, KSP2 runs in Gradle daemon. This has 2 implications:
1. Gradle daemon usually has a lower default heap limit. You may need to increase it by the following Gradle property,
for example: `org.gradle.jvmargs=-Xmx4096M -XX:MaxMetaspaceSize=1024m`
2. The debug flags now need to be passed to Gradle rather than `KotlinCompileDaemon`. To debug KSP2 and/or processors,
   pass the system property `-Dorg.gradle.debug=true` in the Gradle command line.

## Using KSP 2 from command line and in program
Please refer to [Using KSP2 command line](ksp2cmdline.md) and [Calling KSP2 in program](ksp2entrypoints.md).

## Known Issues
The main gaps between KSP2 Beta and KSP2 Stable are:
* Support of mangled (for `inline` and `internal`) JVM names in `Resolver.getJvmWildcard` and
  `Resolver.mapToJvmSignature`
* Support of synthesized Java getters and setters for Kotlin properties in `Resolver.findOverridee` and 
  `Resolver.asMemberOf`.
* Performance and memory footage are not fully taken care of yet.
* Some type of annotation values are not supported (e.g. nested annotations)

Please check the KSP [2.0](https://github.com/google/ksp/issues?q=is%3Aopen+is%3Aissue+milestone%3A2.0) and
[2.1](https://github.com/google/ksp/issues?q=is%3Aopen+is%3Aissue+milestone%3A2.1) milestones on GitHub for the complete
lists of known issues.

## Feedback
If you find any bugs that are not included in the above milestones, or if you have any concerns or ideas on API changes,
[please let us know](https://github.com/google/ksp/issues)!
