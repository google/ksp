# Using KSP2 from Command Line

KSP2 has 4 main classes, one for each platform: `KSPJvmMain`, `KSPJsMain`, `KSPNativeMain`, `KSPCommonMain`. They reside
in the same jars from the
[artifacts.zip](https://github.com/google/ksp/releases/download/2.0.0-1.0.21/artifacts.zip) in the
[release page](https://github.com/google/ksp/releases/tag/2.0.0-1.0.21):
* `symbol-processing-aa-2.0.0-1.0.21.jar`

and depend on:
* `symbol-processing-common-deps-2.0.0-1.0.21.jar`

You’ll also need the Kotlin runtime:
* `kotlin-stdlib-2.0.0.jar`

Taking `KSPJvmMain` for example,

```
java -cp \
kotlin-analysis-api-2.0.0-1.0.21.jar:common-deps-2.0.0-1.0.21.jar:symbol-processing-api-2.0.0-1.0.21.jar:kotlin-stdlib-2.0.0.jar \
com.google.devtools.ksp.cmdline.KSPJvmMain \
-jvm-target 11 \
-module-name=main \
-source-roots project_dir/src/kotlin/main \
-project-base-dir project_dir/ \
-output-base-dir=project_dir/build/ \
-caches-dir=project_dir/build/caches/ \
-class-output-dir=project_dir/build/out/main/classes \
-kotlin-output-dir=project_dir/build/out/main/kotlin/ \
-java-output-dir project_dir/build/out/main/java/ \
-resource-output-dir project_dir/build/out/main/res/ \
-language-version=2.0 \
-api-version=2.0 \
path/to/processor.jar
```

A comprehensive options list can be obtained with `-h`:

```
Available options:

    -java-source-roots=List<File>
*   -java-output-dir=File
    -jdk-home=File?
*   -jvm-target=String
    -jvm-default-mode=String
*   -module-name=String
*   -source-roots=List<File>
    -common-source-roots=List<File>
    -libraries=List<File>
    -processor-options=Map<String, String>
*   -project-base-dir=File
*   -output-base-dir=File
*   -caches-dir=File
*   -class-output-dir=File
*   -kotlin-output-dir=File
*   -resource-output-dir=File
    -incremental=Boolean
    -incremental-log=Boolean
    -modified-sources=List<File>
    -removed-sources=List<File>
    -changed-classes=List<String>
*   -language-version=String
*   -api-version=String
    -all-warnings-as-errors=Boolean
    -map-annotation-arguments-in-java=Boolean
*   <processor classpath>

where:
* is required
  List is colon separated. E.g., arg1:arg2:arg3
  Map is in the form key1=value1:key2=value2
```