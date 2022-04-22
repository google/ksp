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

For changes that involves API changes(new API, API signature change), please also update [api.base](./api/api.base) file. You can monitor api change with `./gradlew :api:apiCheck`, and`./gradlew :api:updateApi` to generate new api signature.

## Testing
For incoming PRs, we would like to request changes covered by tests for good practice.
We do end-to-end test for KSP, which means you need to write a lightweight processor to be loaded with KSP for testing.
The form of the test itself is flexible as long as the logic is being covered. 

Here are some [sample test processors](compiler-plugin/src/test/kotlin/com/google/devtools/ksp/processor) for your reference.

#### Steps for writing a test
* KSP needs to be built with JDK 11+, because of some test dependencies.
* Create a test processor under the sample processor folder.
it should be extending [AbstractTestProcessor](compiler-plugin/src/test/kotlin/com/google/devtools/ksp/processor/AbstractTestProcessor.kt)
* Write your logic by overriding corresponding functions. 
    * Test is performed by running test processor and get a collection of test results in the form of List<String>.
    * Make sure you override toResult() function to collect test result. 
    * Leverage visitors for easy traverse of the test case.
    * To help with easy testing, you can create an annotation for test, and annotate the specific part of the code to avoid doing 
    excess filtering when traveling along the program.
* Write your test case to work with test processor.
    * Create a test kt file under [testData](compiler-plugin/testData/api) folder. 
    Every kt file under this folder corresponds to a test case.
    * Inside the test file:
        * [optional] Add ```// WITH_RUNTIME``` to the top if you need access to standard library.
        * Add ```// TEST PROCESSOR:<Your test processor name>``` to provide the test processor for this test case. Processors can 
        be reused if necessary.
        * Immediately after test processor line, start your expected result lines. Every line should start with
         ```// ```(with a space after //)
        * Add ```// END``` to indicate end of expected test result.
        * Then follows virtual files section till the end of test file.
        * You can use ```// FILE: <file name>``` to create files that will be available at run time of the test.
            * E.g. ```// FILE: a.kt``` will result in a file named ```a.kt``` at run time.
* Add new test to [test suite](compiler-plugin/src/test/java/com/google/devtools/ksp/test/KotlinKSPTestGenerated.java)
* Run generated tests with ```:compiler-plugin:test``` gradle task.
    * This will execute all tests in KSP test suite. To run your test only, specify the test name with 
    ```--tests "com.google.devtools.ksp.test.KotlinKSPTestGenerated.<name of your generated test>"```
    * Make sure your change is not breaking any existing test as well :).
