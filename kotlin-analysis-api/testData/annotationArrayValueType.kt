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
// TEST PROCESSOR: AnnotationArrayValueTypeProcessor
// EXPECTED:
// JavaAnnotated args is Array<*>: true
// JavaAnnotated args size: 2
// KotlinAnnotated args is Array<*>: true
// KotlinAnnotated args size: 2
// END
// FILE: JavaAnnotation.java
public @interface JavaAnnotation {
    NestedAnnotation[] args();
}

// FILE: NestedAnnotation.java
public @interface NestedAnnotation {
    String value();
}

// FILE: JavaAnnotated.java
@JavaAnnotation(args = {@NestedAnnotation(value = "one"), @NestedAnnotation(value = "two")})
class JavaAnnotated {}

// FILE: KotlinAnnotated.kt
@JavaAnnotation(args = [NestedAnnotation(value = "one"), NestedAnnotation(value = "two")])
class KotlinAnnotated
