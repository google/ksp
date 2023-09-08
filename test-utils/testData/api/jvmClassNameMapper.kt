/*
 * Copyright 2020 Google LLC
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
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

// TEST PROCESSOR: MapJvmClassNameProcessor
// EXPECTED:
// Cls1
// Cls1$Cls2
// Cls1$Cls3
// Cls1$Cls4
// JavaInterfaceWithVoid
// JavaClass1
// JavaClass1$JavaClass2
// JavaClass1$JavaClass3
// JavaClass1$JavaClass4
// JavaClass5
// JavaAnno
// END

// FILE: Cls1.kt
class Cls1 {
    class Cls2

    open class Cls3

    inner class Cls4
}

// FILE: JavaInterfaceWithVoid.java
interface JavaInterfaceWithVoid {}

// FILE: JavaClass1.java
class JavaClass1 {
    static final class JavaClass2 {}
    static class JavaClass3 {}
    class JavaClass4 {}
}

class JavaClass5 {}

// FILE: JavaAnno.java
@interface JavaAnno {}
