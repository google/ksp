# How to Contribute

We'd love to accept your patches and contributions to this project. There are
just a few small guidelines you need to follow.

## Contributor License Agreement

Contributions to this project must be accompanied by a Contributor License
Agreement (CLA). You (or your employer) retain the copyright to your
contribution; this simply gives us permission to use and redistribute your
contributions as part of the project. Head over to
<https://cla.developers.google.com/> to see your current agreements on file or
to sign a new one.

You generally only need to submit a CLA once, so if you've already submitted one
(even if it was for a different project), you probably don't need to do it
again.

## Code reviews

All submissions, including submissions by project members, require review. We
use GitHub pull requests for this purpose. Consult
[GitHub Help](https://help.github.com/articles/about-pull-requests/) for more
information on using pull requests.

## Community Guidelines

This project follows
[Google's Open Source Community Guidelines](https://opensource.google/conduct/).

## API verification

For changes that involves API changes(new API, API signature change), please also update [api.base](api/api.base) file. You can monitor api change with `./gradlew :api:checkApi`, and`./gradlew :api:updateApi` to generate new api signature.

## Bug Contribution Workflow
If you want to submit a bug fix or report a bug via a test, follow this workflow:

1. **Bug found**
2. **Does a GitHub issue exist?**
    - **Yes**: Do not open a new one. Feel free to comment / continue the discussion in that issue.
    - **No**: Feel free to open a new issue detailing your bug (since `@Bug` requires a linked GitHub issue).
3. **Open a PR with a failing test**
    - *Note*: Reproducing issues with unit tests is the preferred way of reproducing issues. Some issues such as those related to Gradle or incremental processing may require an integration test. However, if it's an API bug, then please do spend some time to find a small unit test case.
    - The test should be added **first** in a separate PR where it either fails or throws via `runFailingTest` or `runThrowingTest` respectively (annotated with `@Bug(..., BugState.OPEN)`).
4. **Open a PR with a bug fix**
    - In the subsequent PR containing the fix, flip the test call from `runFailingTest`/`runThrowingTest` to `runTest` and update the `BugState` value (e.g., to `BugState.FIXED`).

You may choose to follow any or all of the above steps as long as they are in that order.

## Testing
For incoming PRs, we request that changes are covered by tests for good practice.
We do end-to-end testing for KSP, which means you write a lightweight processor to be loaded with KSP for testing.
The form of the test itself is flexible as long as the logic is covered. 

> [!NOTE]
> The `test-utils` directory is deprecated. Unit tests should be placed in `kotlin-analysis-api`.

Here are some [sample test processors](kotlin-analysis-api/src/test/kotlin/com/google/devtools/ksp/processor) for your reference.

#### Steps for writing a test
* KSP needs to be built with JDK 11+, because of test dependencies.
* Create a test processor under the sample processor folder.
* Write your logic by overriding corresponding functions. 
    * Test is performed by running test processor and getting a collection of test results in the form of `List<String>`.
    * [required] Ensure your test processor is in the package `com.google.devtools.ksp.processor`.
    * [required] The test processor must extend the `AbstractTestProcessor` class.
    * [required] Make sure you override `toResult()` function to collect test results.
    * [required] The test processor must have a constructor parameter of type [`ApiFeatures`](api/src/main/kotlin/com/google/devtools/ksp/ApiFeatures.kt).
      Specifically, it must override the `apiFeatures` property in [`SymbolProcessor`](api/src/main/kotlin/com/google/devtools/ksp/processing/SymbolProcessor.kt).
      In other words, the constructor parameter list must be as follows: `class MyProcessor(override val apiFeatures: ApiFeatures)`.
    * Leverage visitors for easy traversal of the test case.
    * To help with easy testing, you can create an annotation for test, and annotate the specific part of the code to avoid doing excess filtering when traveling along the program.
* Write your test case to work with the test processor.
    * Create a test `.kt` file under the `testData` folder in `kotlin-analysis-api` (e.g. [kotlin-analysis-api/testData](kotlin-analysis-api/testData)). 
    Every `.kt` file under this folder corresponds to a test case.
    * Inside the test file:
        * [optional] Add `// WITH_RUNTIME` to the top if you need access to the standard library.
        * [optional] Specify `// TARGET_BACKEND: <backend>` (e.g. `// TARGET_BACKEND: NATIVE`) and `// WITH_FIXED_TARGET: <target>` (e.g. `// WITH_FIXED_TARGET: linux_x64`) directives to ensure tests run on klib/native backends (as in [`packageDeclarations.kt`](kotlin-analysis-api/testData/native/packageDeclarations.kt)). These directives come from the Kotlin compiler test framework.
        * Add `// TEST PROCESSOR:<Your test processor name>` to provide the test processor for this test case. Processors can be reused if necessary.
        * [optional] Add `// PROCESSOR INPUT: <input/predicate>` to specify inputs or predicates for the test processor (e.g. `// PROCESSOR INPUT: Anno` or `// PROCESSOR INPUT: kotlin.annotation.Retention, kotlin.annotation.Target`, as in [`aliasedAnnotation.kt`](kotlin-analysis-api/testData/getSymbolsWithAnnotation/aliasedAnnotation.kt)).
          Note: for a processor to accept input, its constructor must have a single `List<String>` parameter.
          This is a handy way of writing a parametric test processor.
          The `List<String>` parameter must come before the `apiFeatures` property (e.g. `class MyProcessor(val input: List<String>, override val apiFeatures: ApiFeatures)`).
        * Immediately after the test processor line(s), add `// EXPECTED:` and start your expected result lines. Every line should start with `// ` (with a space after `//`).
            * Regular `// <expected output>` lines apply to all [`ApiFeatures`](api/src/main/kotlin/com/google/devtools/ksp/ApiFeatures.kt) configurations.
            * [optional] If an expected output line only applies to specific [`ApiFeatures`](api/src/main/kotlin/com/google/devtools/ksp/ApiFeatures.kt) configurations, use the `EXPECT` directive:
              `// EXPECT <FEATURE> = <BOOLEAN>[, <FEATURE> = <BOOLEAN>]: <expected output>`
              where `<FEATURE>` is the screaming snake case name of a property in [`ApiFeatures`](api/src/main/kotlin/com/google/devtools/ksp/ApiFeatures.kt) (e.g. `ENABLE_BACKING_FIELDS` or `ENABLE_CONTEXT_PARAMETERS`) and `<BOOLEAN>` is `TRUE` or `FALSE`.
            * Multiple feature constraints can be combined on the same line separated by a comma (e.g. `// EXPECT ENABLE_BACKING_FIELDS = TRUE, ENABLE_CONTEXT_PARAMETERS = FALSE: <expected output>`). If a feature is omitted on an `EXPECT` line, the expectation applies to both `TRUE` and `FALSE` values of the omitted feature. Use the minimal number of directives needed (see [`expectDifferentOutput.kt`](kotlin-analysis-api/testData/expectDifferentOutput.kt) for an example).
        * Add `// END` to indicate the end of expected test results.
        * Then follows the virtual files section till the end of the test file.
        * You can use `// FILE: <file name>` to create files that will be available at runtime of the test.
            * E.g. `// FILE: a.kt` will result in a file named `a.kt` at runtime.
* Add new test to [`KSPUnitTestSuite`](kotlin-analysis-api/src/test/kotlin/com/google/devtools/ksp/test/KSPUnitTestSuite.kt) in `kotlin-analysis-api`.
    * Annotate the test with `@Bug` and `@BugState`.
    * The `@Bug` annotation requires a link to an open or existing GitHub issue (e.g., `@Bug("https://github.com/google/ksp/issues/<issue_number>", BugState.OPEN)`).
    * Use the `@Negative` marker annotation if applicable.
* Run generated tests with the `:kotlin-analysis-api:test` gradle task.
    * This will execute all tests in the KSP test suite across all configured [`ApiFeatures`](api/src/main/kotlin/com/google/devtools/ksp/ApiFeatures.kt) and resolution strategy test suites ([`AAConfiguredUnitTestSuite.kt`](kotlin-analysis-api/src/test/kotlin/com/google/devtools/ksp/test/AAConfiguredUnitTestSuite.kt) and [`PsiConfiguredUnitTestSuite.kt`](kotlin-analysis-api/src/test/kotlin/com/google/devtools/ksp/test/PsiConfiguredUnitTestSuite.kt)). To run your test only, specify the test name with
    `--tests "com.google.devtools.ksp.test.*UnitTestSuite.<name of your test>"`
    * Make sure your change is not breaking any existing test as well :).
* [optional] You can run the `./gradlew check` task as well to run all tests.
    * This task runs all integration tests as well, so it might take a while to complete.
