// WITH_RUNTIME
// TEST PROCESSOR: TypeComposureProcessor
// EXPECTED:
// C<*> ?= C<*> : true
// C<*> ?= C<Int> : true
// C<*> ?= C<Number> : true
// C<*> ?= C<in Int> : true
// C<*> ?= C<in Number> : true
// C<*> ?= C<out Int> : true
// C<*> ?= C<out Number> : true
// C<Int> ?= C<*> : false
// C<Int> ?= C<Int> : true
// C<Int> ?= C<Number> : false
// C<Int> ?= C<in Int> : false
// C<Int> ?= C<in Number> : false
// C<Int> ?= C<out Int> : false
// C<Int> ?= C<out Number> : false
// C<Number> ?= C<*> : false
// C<Number> ?= C<Int> : false
// C<Number> ?= C<Number> : true
// C<Number> ?= C<in Int> : false
// C<Number> ?= C<in Number> : false
// C<Number> ?= C<out Int> : false
// C<Number> ?= C<out Number> : false
// C<in Int> ?= C<*> : false
// C<in Int> ?= C<Int> : true
// C<in Int> ?= C<Number> : true
// C<in Int> ?= C<in Int> : true
// C<in Int> ?= C<in Number> : true
// C<in Int> ?= C<out Int> : false
// C<in Int> ?= C<out Number> : false
// C<in Number> ?= C<*> : false
// C<in Number> ?= C<Int> : false
// C<in Number> ?= C<Number> : true
// C<in Number> ?= C<in Int> : false
// C<in Number> ?= C<in Number> : true
// C<in Number> ?= C<out Int> : false
// C<in Number> ?= C<out Number> : false
// C<out Int> ?= C<*> : false
// C<out Int> ?= C<Int> : true
// C<out Int> ?= C<Number> : false
// C<out Int> ?= C<in Int> : false
// C<out Int> ?= C<in Number> : false
// C<out Int> ?= C<out Int> : true
// C<out Int> ?= C<out Number> : false
// C<out Number> ?= C<*> : false
// C<out Number> ?= C<Int> : true
// C<out Number> ?= C<Number> : true
// C<out Number> ?= C<in Int> : false
// C<out Number> ?= C<in Number> : false
// C<out Number> ?= C<out Int> : true
// C<out Number> ?= C<out Number> : true
// END

open class C<T>

val a: Int = 0
val b: Number = 0
val c: C<Int> = C<Int>()
