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

// TEST PROCESSOR: ParentProcessor
// EXPECTED:
// parent of File: B.java: null
// parent of ITF: ITF
// parent of ITF: B
// parent of Any: (Any..Any?)
// parent of (Any..Any?): T
// parent of T: B
// parent of Anno: Anno
// parent of Anno: @Anno
// parent of param:: @Anno
// parent of @Anno: B
// parent of Bnno: Bnno
// parent of Bnno: @Bnno
// parent of @Bnno: B
// parent of B: File: B.java
// parent of T: (T..T?)
// parent of (T..T?): t
// parent of t: B
// parent of T: (T..T?)
// parent of (T..T?): t
// parent of t: foo
// parent of Int: Int
// parent of Int: i
// parent of i: foo
// parent of Int: Int
// parent of Int: foo
// parent of foo: B
// parent of B<*>: synthetic constructor for B
// parent of Any: (Any..Any?)
// parent of (Any..Any?): T
// parent of T: B
// parent of synthetic constructor for B: B
// parent of RGB: (RGB..RGB?)
// parent of (RGB..RGB?): INVARIANT (RGB..RGB?)
// parent of INVARIANT (RGB..RGB?): Enum<INVARIANT (RGB..RGB?)>
// parent of Enum<INVARIANT (RGB..RGB?)>: Enum<(RGB..RGB?)>
// parent of Enum<(RGB..RGB?)>: RGB
// parent of RGB: File: B.java
// parent of RGB.R: RGB
// parent of RGB.G: RGB
// parent of RGB.B: RGB
// parent of Array<RGB>: values
// parent of values: RGB
// parent of String: value
// parent of value: valueOf
// parent of RGB: valueOf
// parent of valueOf: RGB
// parent of EnumEntries<RGB>: entries
// parent of entries: RGB
// parent of File: Bnno.kt: null
// parent of Annotation: Annotation
// parent of Annotation: Bnno
// parent of Bnno: File: Bnno.kt
// parent of Bnno: synthetic constructor for Bnno
// parent of synthetic constructor for Bnno: Bnno
// parent of File: a.kt: null
// parent of Int: Int
// parent of Int: INVARIANT Int
// parent of INVARIANT Int: List<INVARIANT Int>
// parent of List<INVARIANT Int>: List<INVARIANT Int>
// parent of List<INVARIANT Int>: topProp
// parent of List<INVARIANT Int>: List<INVARIANT Int>
// parent of List<INVARIANT Int>: topProp.getter()
// parent of topProp.getter(): topProp
// parent of Anno: Anno
// parent of Anno: @Anno
// parent of @Anno: topProp
// parent of topProp: File: a.kt
// parent of T: T
// parent of T: topFun
// parent of Any?: T
// parent of T: topFun
// parent of Anno: Anno
// parent of Anno: @Anno
// parent of @Anno: topFun
// parent of topFun: File: a.kt
// parent of Annotation: Annotation
// parent of Annotation: Anno
// parent of Anno: File: a.kt
// parent of String: String
// parent of String: param
// parent of String: param.getter()
// parent of param.getter(): param
// parent of param: Anno
// parent of String: String
// parent of String: param
// parent of param: <init>
// parent of Anno: Anno
// parent of Anno: <init>
// parent of <init>: Anno
// parent of Int: Int
// parent of Int: INVARIANT Int
// parent of INVARIANT Int: Map<INVARIANT Int, INVARIANT T>
// parent of T: T
// parent of T: INVARIANT T
// parent of INVARIANT T: Map<INVARIANT Int, INVARIANT T>
// parent of Map<INVARIANT Int, INVARIANT T>: Map<Int, T>
// parent of Map<Int, T>: Alias
// parent of Any?: T
// parent of T: File: a.kt
// parent of Alias: File: a.kt
// parent of Any: ITF
// parent of ITF: File: a.kt
// parent of ITF: ITF
// parent of ITF: topClass
// parent of Anno: Anno
// parent of Anno: @Anno
// parent of @Anno: topClass
// parent of topClass: File: a.kt
// parent of Int: Int
// parent of Int: i
// parent of i: memberFun
// parent of Int: Int
// parent of Int: memberFun
// parent of memberFun: topClass
// parent of Int: Int
// parent of Int: a
// parent of Int: Int
// parent of Int: a.getter()
// parent of a.getter(): a
// parent of a: topClass
// parent of String: String
// parent of String: b
// parent of String: String
// parent of String: b.getter()
// parent of b.getter(): b
// parent of String: value
// parent of value: b.setter()
// parent of b.setter(): b
// parent of b: topClass
// parent of topClass: synthetic constructor for topClass
// parent of synthetic constructor for topClass: topClass
// parent of Any: InnerClass
// parent of Any?: P
// parent of P: InnerClass
// parent of InnerClass: topClass
// parent of P: P
// parent of P: p
// parent of p: innerFun
// parent of Int: Int
// parent of Int: innerFun
// parent of innerFun: InnerClass
// parent of InnerClass<*>: synthetic constructor for InnerClass
// parent of Any?: P
// parent of synthetic constructor for InnerClass: InnerClass
// parent of CMYK: CMYK
// parent of CMYK: INVARIANT CMYK
// parent of INVARIANT CMYK: Enum<INVARIANT CMYK>
// parent of Enum<INVARIANT CMYK>: Enum<CMYK>
// parent of Enum<CMYK>: CMYK
// parent of CMYK: File: a.kt
// parent of CMYK: synthetic constructor for CMYK
// parent of synthetic constructor for CMYK: CMYK
// parent of CMYK.C: CMYK
// parent of CMYK.M: CMYK
// parent of CMYK.Y: CMYK
// parent of CMYK.K: CMYK
// parent of Array<CMYK>: values
// parent of values: CMYK
// parent of String: value
// parent of value: valueOf
// parent of CMYK: valueOf
// parent of valueOf: CMYK
// parent of EnumEntries<CMYK>: entries
// parent of EnumEntries<CMYK>: entries.getter()
// parent of entries.getter(): entries
// parent of entries: CMYK
// parent of YUV: YUV
// parent of YUV: INVARIANT YUV
// parent of INVARIANT YUV: Enum<INVARIANT YUV>
// parent of Enum<INVARIANT YUV>: Enum<YUV>
// parent of Enum<YUV>: YUV
// parent of YUV: null
// parent of YUV: YUV
// parent of YUV: <init>
// parent of <init>: YUV
// parent of YUV.Y: YUV
// parent of YUV.U: YUV
// parent of YUV.V: YUV
// parent of YUV: YUV
// parent of YUV: INVARIANT YUV
// parent of INVARIANT YUV: Array<INVARIANT YUV>
// parent of Array<INVARIANT YUV>: Array<YUV>
// parent of Array<YUV>: values
// parent of values: YUV
// parent of String: String
// parent of String: value
// parent of value: valueOf
// parent of YUV: YUV
// parent of YUV: valueOf
// parent of valueOf: YUV
// parent of YUV: YUV
// parent of YUV: INVARIANT YUV
// parent of INVARIANT YUV: EnumEntries<INVARIANT YUV>
// parent of EnumEntries<INVARIANT YUV>: EnumEntries<YUV>
// parent of EnumEntries<YUV>: entries
// parent of YUV: YUV
// parent of YUV: INVARIANT YUV
// parent of INVARIANT YUV: EnumEntries<INVARIANT YUV>
// parent of EnumEntries<INVARIANT YUV>: EnumEntries<YUV>
// parent of EnumEntries<YUV>: entries.getter()
// parent of entries.getter(): entries
// parent of entries: YUV
// parent of HSV: (HSV..HSV?)
// parent of (HSV..HSV?): INVARIANT (HSV..HSV?)
// parent of INVARIANT (HSV..HSV?): Enum<INVARIANT (HSV..HSV?)>
// parent of Enum<INVARIANT (HSV..HSV?)>: Enum<(HSV..HSV?)>
// parent of Enum<(HSV..HSV?)>: HSV
// parent of HSV: File: HSV.class
// parent of HSV: HSV
// parent of HSV: <init>
// parent of <init>: HSV
// parent of HSV.H: HSV
// parent of HSV.S: HSV
// parent of HSV.V: HSV
// parent of Array<HSV>: values
// parent of values: HSV
// parent of String: value
// parent of value: valueOf
// parent of HSV: valueOf
// parent of valueOf: HSV
// parent of HSV: HSV
// parent of HSV: INVARIANT HSV
// parent of INVARIANT HSV: EnumEntries<INVARIANT HSV>
// parent of EnumEntries<INVARIANT HSV>: EnumEntries<HSV>
// parent of EnumEntries<HSV>: entries
// parent of entries: HSV
// END


// MODULE: lib
// FILE: YUV.kt
enum class YUV {
    Y, U, V
}

// FILE: HSV.java
enum HSV {
    H, S, V
}

// MODULE: main(lib)

// FILE: a.kt
annotation class Anno(val param: String = "")

typealias Alias<T> = Map<Int, T>

@Anno
val topProp : List<Int>? = null

@Anno
fun <T> topFun() : T? {
    return null
}

interface ITF

@Anno
class topClass: ITF {
    fun memberFun(i: Int) = 1
    class InnerClass<P> {
        fun innerFun(p: P) = 1
    }

    val a: Int = 1
    var b: String
        get() = "1"
}

enum class CMYK {
    C, M, Y, K
}

// FILE: Bnno.kt
package p

annotation class Bnno

// FILE: B.java
@Anno
@p.Bnno
public class B<T> implements ITF {
    private T t;
    public int foo(T t, int i) {
        return 1;
    }
}

enum RGB {
    R, G, B
}
