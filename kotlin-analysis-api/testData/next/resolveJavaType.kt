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

// TEST PROCESSOR: ResolveJavaTypeProcessor
// EXPECTED:
// kotlin.Int
// kotlin.String?
// kotlin.collections.MutableSet<*>?
// kotlin.Unit
// kotlin.IntArray?
// C.T?
// C.PFun.P?
// kotlin.collections.MutableList<out kotlin.collections.MutableSet<kotlin.Double?>?>?
// kotlin.collections.MutableList<in kotlin.collections.MutableList<out kotlin.Double?>?>?
// Bar?
// kotlin.Array<out Bar?>?
// C<*>
// <ERROR>?
// C<*>
// kotlin.collections.MutableList<Base.T?>?
// kotlin.Unit
// Base.T?
// kotlin.Unit
// Base.T?
// kotlin.Unit
// kotlin.Array<out Base.T?>?
// kotlin.Unit
// Foo<Base.T?, Base.Inner.P?>?
// Bar<Base.Inner.P?, Base.T?>?
// kotlin.collections.MutableList<Base.T?>?
// kotlin.Unit
// Base.T?
// kotlin.Unit
// Base.T?
// kotlin.Unit
// kotlin.Array<out Base.T?>?
// kotlin.Unit
// Base<Another.T?, Another.T?>?
// kotlin.Int
// kotlin.Unit
// kotlin.Int
// JavaEnum
// END
// FILE: dummy.kt

// FILE: C.java
import java.util.List;
import java.util.Set;

public class C<T> {
    public C() {}
    // to reproduce the case where type reference is owned by a constructor
    public <X> C(X x) {}
    public int intFun() {}

    public String strFun() {}

    public void wildcardParam(Set<?> param1) {}

    public int[] intArrayFun() {}

    public T TFoo() {}

    public <P> P PFun() {}

    public List<? extends Set<Double>> extendsSetFun() {}

    public List<? super List<? extends Double>> extendsListTFun() {}

    public Bar BarFun() {}

    public Bar[] BarArryFun() {}
}

// FILE: Base.java
import java.util.List;

class Foo<T1,T2> {}
class Bar<T1, T2> {}

class Base<T,P> {
    void genericT(List<T> t){};
    void singleT(T t){};
    void varargT(T... t){};
    void arrayT(T[] t){};

    class Inner<P> {
        void genericT(List<T> t){};
        void singleT(T t){};
        void varargT(T... t){};
        void arrayT(T[] t){};
        Foo<T, P> foo;
        Bar<P, T> bar;
    }
}

class Another<T> {
    Base<T, T> base;
}

public enum JavaEnum {
    VAL1(1),
    VAL2(2);

    private int x;

    JavaEnum(int x) {
        this.x = x;
    }
    void enumMethod() {}
}
