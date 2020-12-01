These binaries are built and copied from kotlin compiler: https://github.com/JetBrains/kotlin

Those prebuilt binaries are transient and therefore not signed. It's HIGHTLY RECOMMENDED to
build the libraries by yourself and copy them here.

test-common/ packs common test utilities in kotlinc. To build the jar:
```
$ # setup kotlin build environment. see https://github.com/JetBrains/kotlin
$ cd path_to_kotlin_compiler_src
$ patch -p1 < 0001-Package-test-classes.patch
$ ./gradlew :include:kotlin-compiler-tests:build
$ cp include/kotlin-compiler-tests/build/libs/*.jar path_to_below
$ ./gradlew :compiler:tests-mutes:build
$ cp compiler/tests-mutes/build/libs/*.jar path_to_below
```
