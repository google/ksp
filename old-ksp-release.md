## Migration from old KSP releases
KSP was moved out of Kotlin source tree for easier setup and faster compilation.
Package name, group ID and artifact ID are changed as well.

|              | **Old** | **New** |
| ------------ | ------- | ------- |
| Package Name | org.jetbrains.kotlin.ksp | com.google.devtools.ksp |
| Group ID | org.jetbrains.kotlin | com.google.devtools.ksp |
| Artifact ID of API | kotlin-symbol-processing-api | symbol-processing-api |
| Artifact ID of Gradle Plugin | kotlin-ksp | symbol-processing |

Migrating your projects should be as simple as string replacements, for example,
```
# Package name
$ find PATH_TO_PROJECT_ROOT -name "*.kt" | \
  xargs sed -e "s/org\.jetbrains\.kotlin\.ksp/com.google.devtools.ksp/g" -i

# Group ID and Artifact ID of API
$ find PATH_TO_PROCESSOR_SRC_ROOT -name "*.kts" | \
  xargs sed -e "s/org\.jetbrains\.kotlin:kotlin-symbol-processing-api/com.google.devtools.ksp:symbol-processing-api/g" -i

# Groupd ID and Artifact ID of the gradle plugin
$ find PATH_TO_APPLICATION_SRC_ROOT -name "*.kts" | \
  xargs sed -e "s/org\.jetbrains\.kotlin:kotlin-ksp/com.google.devtools.ksp:symbol-processing/g" -i
$ find PATH_TO_APPLICATION_SRC_ROOT -name "*.kts" | \
  xargs sed -e "s/kotlin-ksp/symbol-processing/g" -i
```

and rename the service file from

```src/main/resources/META-INF/services/org.jetbrains.kotlin.ksp.processing.SymbolProcessor```

to

``` src/main/resources/META-INF/services/com.google.devtools.ksp.processing.SymbolProcessor ```

