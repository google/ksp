/*
 * Copyright 2023 Google LLC
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
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
// TEST PROCESSOR: OverrideeProcessor
// EXPECTED:
// JavaSubject.Subject:
// Subject.openFun() -> Base.openFun()
// Subject.abstractFun() -> Base.abstractFun()
// Subject.openFunWithGenericArg(t:String) -> Base.openFunWithGenericArg(t:T)
// Subject.abstractFunWithGenericArg(t:String) -> Base.abstractFunWithGenericArg(t:T)
// Subject.nonOverridingMethod() -> null
// Subject.overriddenGrandBaseFun() -> Base.overriddenGrandBaseFun()
// Subject.overriddenAbstractGrandBaseFun() -> Base.overriddenAbstractGrandBaseFun()
// Subject.openGrandBaseFun() -> GrandBase.openGrandBaseFun()
// Subject.abstractGrandBaseFun() -> GrandBase.abstractGrandBaseFun()
// Subject.staticMethod() -> null
// END

// FILE: dummy.kt
class Dummy

// FILE: JavaSubject.java
public class JavaSubject {
    static abstract class GrandBase {
        void openGrandBaseFun() {}
        abstract void abstractGrandBaseFun();
        void overriddenGrandBaseFun() {}
        abstract void overriddenAbstractGrandBaseFun();
    }
    static abstract class Base<T> extends GrandBase {
        void openFun() {}
        abstract void abstractFun();
        T openFunWithGenericArg(T t) {
            return null;
        }
        abstract T abstractFunWithGenericArg(T t);
        void overriddenGrandBaseFun() {}
        void overriddenAbstractGrandBaseFun() {}
    }

    static abstract class Subject extends Base<String> {
        void openFun() {}
        void abstractFun() {}
        String openFunWithGenericArg(String t) {
            return null;
        }
        String abstractFunWithGenericArg(String t) {
            return null;
        }
        String nonOverridingMethod() {
            return null;
        }
        void overriddenGrandBaseFun() {}
        void overriddenAbstractGrandBaseFun() {}
        void openGrandBaseFun() {}
        void abstractGrandBaseFun() {}
        static String staticMethod() {
            return null;
        }
    }
}
