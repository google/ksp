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
// TEST PROCESSOR: AbstractFunctionsProcessor
// EXPECTED:
// abstractF01
// abstractF02
// abstractF04
// abstractF05
// END
// FILE: AbstractClassKotlin.kt
abstract class AbstractClassKotlin {
    abstract fun abstractF01()
    fun concreteF01() = Unit
    companion object {
        fun concreteF02() = Unit
        @JvmStatic fun concreteF02() = Unit
    }
}

// FILE: InterfaceKotlin.kt
interface InterfaceKotlin {
    fun abstractF02()
    fun abstractWithDefaultF03() { /*default*/ Unit }
    companion object {
        fun concreteF03() = Unit
        @JvmStatic fun concreteF04() = Unit
    }
}

// FILE: AbstractClassJava.java
public abstract class AbstractClassJava {
    public abstract void abstractF04();
    public void concreteF05() {}
    public static void staticF01() {}
}

// FILE: InterfaceJava.java
public interface InterfaceJava {
    public void abstractF05();
    public default void abstractWithDefaultF06() { /* default */ }
    public static void staticF02() {}
}
