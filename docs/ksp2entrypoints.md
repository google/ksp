# Calling KSP2 In Programs

There are two flavors of KSP2 artifacts: `symbol-processing-aa` and `symbol-processing-aa-embeddable`. They are both
uber jars that include almost all runtime dependencies except `kotlin-stdlib` and `symbol-processing-common-deps`.
The `-embeddable` version is the regular version with all the runtime dependencies renamed, so that it can be used with
a Kotlin compiler in the same classpath without name clash. When in doubt, use `symbol-processing-aa-embeddable`.

Calling KSP2 consists of just 4 steps:
1. Load processors
2. Provide an implementation of `KSPLogger`, or use `KspGradleLogger`, which currently simply writes to stdout.
3. Fill `KSPConfig`
4. Call `KotlinSymbolProcessing(kspConfig, processors, kspLogger).execute()`


```
// Implement a logger or use KspGradleLogger
val logger = KspGradleLogger(KspGradleLogger.LOGGING_LEVEL_WARN)

// Load processors
val processorClassloader = URLClassLoader(classpath.map { File(it).toURI().toURL() }.toTypedArray())
val processorProviders = ServiceLoader.load(
  processorClassloader.loadClass("com.google.devtools.ksp.processing.SymbolProcessorProvider"),
  processorClassloader
).toList() as List<SymbolProcessorProvider>

// Fill the config
val kspConfig = KSPJvmConfig.Builder().apply {
  // All configurations happen here. See KSPConfig.kt for all available options.
  moduleName = "main"
  sourceRoots = listOf(File("/path/to/src1), File("/path/to/src2"))
  kotlinOutputDir = File("/path/to/kotlin/out")
  ...
}.build()

// Run!
val exitCode = KotlinSymbolProcessing(kspConfig, listOfProcessors, kspLoggerImpl).execute()
```