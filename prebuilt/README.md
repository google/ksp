These binaries are built and copied from kotlin compiler: https://github.com/JetBrains/kotlin

Those prebuilt binaries are transient and therefore not signed. It's HIGHTLY RECOMMENDED to
build the libraries by yourself and copy them here.

dist/ contains a few libraries that KSP tests read. They can also be obtained from formal
kotlin releases.

test-common/ packs common test utilities in kotlinc. To build the jar:
```
$ # setup kotlin build environment. see https://github.com/JetBrains/kotlin
$ cd path_to_kotlin_compiler_src
$ patch -p1 < 0001-Package-test-classes.patch
$ ./gradlew :include:kotlin-compiler-tests:build
$ cp include/kotlin-compiler-tests/build/libs/*.jar path_to_below
```

.
├── dist
│   └── kotlinc
│       └── lib
│           ├── kotlin-script-runtime.jar
│           ├── kotlin-stdlib.jar
│           └── kotlin-test.jar
└── tests-common
    ├── 0001-Package-test-classes.patch
    └── kotlin-compiler-tests-1.4.0.jar
