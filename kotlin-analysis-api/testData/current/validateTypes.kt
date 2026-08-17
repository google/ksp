/*
 * Copyright 2022 Google LLC
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
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

// WITH_RUNTIME
// TEST PROCESSOR: ValidateProcessor
// EXPECTED:
// ErrorInMember invalid
// goodProp valid
// badProp invalid
// errorFun invalid
// <init> valid
// SkipErrorInMember valid
// skipProp valid
// skipFun valid
// <init> valid
// GoodClass valid
// C valid
// BadJavaClass invalid
// ErrorAnnotationType invalid
// ErrorInAnnotationArgumentSingleType invalid
// ErrorInAnnotationArgumentMultipleTypes invalid
// ErrorInAnnotationArgumentComposed invalid
// ValidAnnotationArgumentType valid
// END
// FILE: a.kt
annotation class Anno(val i: Int)

annotation class AnnoWithTypes(
    val type: KClass<*> = Any::class,
    val types: Array<KClass<*>> = []
)

annotation class AnnoComposed(
    val composed: AnnoWithTypes
)

@Anno(1)
class ErrorInMember : C {
    val goodProp: Int
    val badProp: () -> NonExistType
    fun errorFun(): NonExistType {

    }
}

class SkipErrorInMember {
    val skipProp: NonExistType
    fun skipFun(): NonExitType {

    }
}

@NonExistAnnotation
class ErrorAnnotationType {
}

@Anno(1)
open class GoodClass {
    val a: Int = 1

    fun foo(): Int = 1

    fun bar() {
        val x = a
    }
}

@AnnoWithTypes(type = NonExistType::class)
class ErrorInAnnotationArgumentSingleType {

}

@AnnoWithTypes(types = [ GoodClass::class, NonExistType::class ])
class ErrorInAnnotationArgumentMultipleTypes {

}

@AnnoComposed(composed = AnnoWithTypes(type = NonExistType::class))
class ErrorInAnnotationArgumentComposed {

}

@AnnoWithTypes(type = GoodClass::class)
class ValidAnnotationArgumentType {

}

// FILE: C.java

public class C extends GoodClass {}

class BadJavaClass extends NonExistType {

}
