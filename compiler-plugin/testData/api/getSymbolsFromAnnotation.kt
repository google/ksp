// TEST PROCESSOR: GetSymbolsFromAnnotationProcessor
// EXPECTED:
// ==== Anno superficial====
// Foo
// propertyFoo
// functionFoo
// p1
// constructorParameterFoo
// <init>
// param
// ==== Anno in depth ====
// Foo
// propertyFoo
// functionFoo
// p1
// local
// constructorParameterFoo
// <init>
// param
// ==== Bnno superficial====
// File: Foo.kt
// <init>
// propertyFoo.getter()
// p2
// ==== Bnno in depth ====
// File: Foo.kt
// <init>
// propertyFoo.getter()
// p2
// END
//FILE: annotations.kt
annotation class Anno
annotation class Bnno

//FILE: Foo.kt
@file:Bnno

@Anno
class Foo @Anno constructor(@Anno val constructorParameterFoo: Int, @Anno param: Int){
    @Bnno constructor() {

    }

    @Anno
    val propertyFoo: String
    @Bnno get() = TODO()

    @Anno
    fun functionFoo(@Anno p1: Int, @Bnno p2: Int) {
        @Anno val local = 1
    }
}
