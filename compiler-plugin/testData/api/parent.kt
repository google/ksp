// TEST PROCESSOR: ParentProcessor
// EXPECTED:
// parent of File: a.kt: null
// parent of Anno: File: a.kt
// parent of Anno: synthetic constructor for Anno
// parent of synthetic constructor for Anno: Anno
// parent of Int: Int
// parent of Int: INVARIANT Int
// parent of INVARIANT Int: Map
// parent of T: T
// parent of T: INVARIANT T
// parent of INVARIANT T: Map
// parent of Map: Map
// parent of Map: Alias
// parent of T: Alias
// parent of Alias: File: a.kt
// parent of Int: Int
// parent of Int: INVARIANT Int
// parent of INVARIANT Int: List
// parent of List: List
// parent of List: topProp
// parent of Int: Int
// parent of Int: INVARIANT Int
// parent of INVARIANT Int: List<Int>
// parent of List<Int>: List<Int>
// parent of List<Int>: topProp.getter()
// parent of topProp.getter(): topProp
// parent of Anno: Anno
// parent of Anno: @Anno
// parent of @Anno: topProp
// parent of topProp: File: a.kt
// parent of T: T
// parent of T: topFun
// parent of T: topFun
// parent of Anno: Anno
// parent of Anno: @Anno
// parent of @Anno: topFun
// parent of topFun: File: a.kt
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
// parent of Int: memberFun
// parent of memberFun: topClass
// parent of P: InnerClass
// parent of InnerClass: topClass
// parent of P: P
// parent of P: p
// parent of p: innerFun
// parent of Int: innerFun
// parent of innerFun: InnerClass
// parent of InnerClass<*>: synthetic constructor for InnerClass
// parent of synthetic constructor for InnerClass: InnerClass
// parent of Int: Int
// parent of Int: a
// parent of Int: Int
// parent of Int: a.getter()
// parent of a.getter(): a
// parent of a: topClass
// parent of String: String
// parent of String: b
// parent of b.getter(): b
// parent of String: String
// parent of String: <set-?>
// parent of <set-?>: b.setter()
// parent of b.setter(): b
// parent of b: topClass
// parent of topClass: synthetic constructor for topClass
// parent of synthetic constructor for topClass: topClass
// parent of CMYK: File: a.kt
// parent of C: CMYK
// parent of C: synthetic constructor for C
// parent of synthetic constructor for C: C
// parent of M: CMYK
// parent of M: synthetic constructor for M
// parent of synthetic constructor for M: M
// parent of Y: CMYK
// parent of Y: synthetic constructor for Y
// parent of synthetic constructor for Y: Y
// parent of K: CMYK
// parent of K: synthetic constructor for K
// parent of synthetic constructor for K: K
// parent of CMYK: synthetic constructor for CMYK
// parent of synthetic constructor for CMYK: CMYK
// parent of File: Bnno.kt: null
// parent of Bnno: File: Bnno.kt
// parent of Bnno: synthetic constructor for Bnno
// parent of synthetic constructor for Bnno: Bnno
// parent of File: B.java: null
// parent of Object: Object
// parent of Object: B
// parent of ITF: ITF
// parent of ITF: B
// parent of T: B
// parent of Anno: Anno
// parent of Anno: @Anno
// parent of @Anno: B
// parent of p: Bnno
// parent of p.Bnno: Bnno
// parent of Bnno: @Bnno
// parent of @Bnno: B
// parent of B: File: B.java
// parent of T: T
// parent of T: t
// parent of t: B
// parent of T: T
// parent of T: t
// parent of t: foo
// parent of Int: Int
// parent of Int: i
// parent of i: foo
// parent of Int: Int
// parent of Int: foo
// parent of foo: B
// parent of B<*>: synthetic constructor for B
// parent of synthetic constructor for B: B
// parent of RGB: RGB
// parent of RGB: INVARIANT RGB
// parent of INVARIANT RGB: Enum<RGB>
// parent of Enum<RGB>: Enum<RGB>
// parent of Enum<RGB>: RGB
// parent of RGB: File: B.java
// parent of R: RGB
// parent of G: RGB
// parent of B: RGB
// parent of RGB: RGB
// parent of RGB: INVARIANT RGB
// parent of INVARIANT RGB: Array<(RGB..RGB?)>
// parent of Array<(RGB..RGB?)>: Array<(RGB..RGB?)>
// parent of Array<(RGB..RGB?)>: values
// parent of values: RGB
// parent of java: String
// parent of lang: String
// parent of String: String
// parent of String: name
// parent of name: valueOf
// parent of RGB: RGB
// parent of RGB: valueOf
// parent of valueOf: RGB
// parent of RGB: synthetic constructor for RGB
// parent of synthetic constructor for RGB: RGB
// parent of YUV: YUV
// parent of YUV: INVARIANT YUV
// parent of INVARIANT YUV: Enum<YUV>
// parent of Enum<YUV>: Enum<YUV>
// parent of Enum<YUV>: YUV
// parent of YUV: null
// parent of YUV: YUV
// parent of YUV: Y
// parent of Y: YUV
// parent of YUV: YUV
// parent of YUV: U
// parent of U: YUV
// parent of YUV: YUV
// parent of YUV: V
// parent of V: YUV
// parent of String: String
// parent of String: value
// parent of value: valueOf
// parent of YUV: YUV
// parent of YUV: valueOf
// parent of valueOf: YUV
// parent of YUV: YUV
// parent of YUV: INVARIANT YUV
// parent of INVARIANT YUV: Array<YUV>
// parent of Array<YUV>: Array<YUV>
// parent of Array<YUV>: values
// parent of values: YUV
// parent of YUV: YUV
// parent of YUV: <init>
// parent of <init>: YUV
// parent of HSV: HSV
// parent of HSV: INVARIANT HSV
// parent of INVARIANT HSV: Enum<(HSV..HSV?)>
// parent of Enum<(HSV..HSV?)>: Enum<(HSV..HSV?)>
// parent of Enum<(HSV..HSV?)>: HSV
// parent of HSV: null
// parent of HSV: HSV
// parent of HSV: H
// parent of H: HSV
// parent of HSV: HSV
// parent of HSV: S
// parent of S: HSV
// parent of HSV: HSV
// parent of HSV: V
// parent of V: HSV
// parent of String: String
// parent of String: value
// parent of value: valueOf
// parent of HSV: HSV
// parent of HSV: valueOf
// parent of valueOf: HSV
// parent of HSV: HSV
// parent of HSV: INVARIANT HSV
// parent of INVARIANT HSV: Array<HSV>
// parent of Array<HSV>: Array<HSV>
// parent of Array<HSV>: values
// parent of values: HSV
// parent of HSV: HSV
// parent of HSV: <init>
// parent of <init>: HSV
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
annotation class Anno

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
