// TEST PROCESSOR: ResolveJavaTypeProcessor
// EXPECTED:
// kotlin.Int
// kotlin.String?
// kotlin.IntArray?
// C.T?
// C.PFun.P?
// kotlin.collections.MutableList<out kotlin.collections.MutableSet<kotlin.Double?>?>?
// kotlin.collections.MutableList<in kotlin.collections.MutableList<out kotlin.Double?>?>?
// Bar?
// kotlin.Array<Bar?>?
// END
// FILE: a.kt
annotation class Test
@Test
class Foo<P>: C<P>() {

}

class Bar

// FILE: C.java
import java.util.List;
import java.util.Set;

public class C<T> {
    public int intFun() {}

    public String strFun() {}

    public int[] intArrayFun() {}

    public T TFoo() {}

    public <P> P PFun() {}

    public List<? extends Set<Double>> extendsSetFun() {}

    public List<? super List<? extends Double>> extendsListTFun() {}

    public Bar BarFun() {}

    public Bar[] BarArryFun() {}
}