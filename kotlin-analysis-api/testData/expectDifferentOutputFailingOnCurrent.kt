/*
 * Copyright 2026 Google LLC
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// TEST PROCESSOR: TestFrameworkExpectDifferentOutputProcessor
// EXPECTED:
// This should be in all configurations
// EXPECT ENABLE_BACKING_FIELDS = FALSE: This should be expected when enableBackingFields = false
// EXPECT ENABLE_BACKING_FIELDS = TRUE: This different output should be expected when enableBackingFields = true
// EXPECT ENABLE_CONTEXT_PARAMETERS = FALSE: This should be expected when enableContextParameters = false
// EXPECT ENABLE_CONTEXT_PARAMETERS = TRUE: This different output should be expected when enableContextParameters = true
// EXPECT ENABLE_BACKING_FIELDS = FALSE, ENABLE_CONTEXT_PARAMETERS = FALSE: This should cause a failed test when enableBackingFields = false and enableContextParameters = false
// EXPECT ENABLE_BACKING_FIELDS = TRUE, ENABLE_CONTEXT_PARAMETERS = FALSE: Combined configuration: enableBackingFields = true, enableContextParameters = false
// EXPECT ENABLE_BACKING_FIELDS = FALSE, ENABLE_CONTEXT_PARAMETERS = TRUE: Combined configuration: enableBackingFields = false, enableContextParameters = true
// EXPECT ENABLE_BACKING_FIELDS = TRUE, ENABLE_CONTEXT_PARAMETERS = TRUE: Combined configuration: enableBackingFields = true, enableContextParameters = true
// This should also be in all configurations
// END

// N.B.: Explicitly empty file. This test asserts that the test framework can change the expected test output with
// the EXPECT directives.
