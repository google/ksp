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
// Bar
// Baz
// ==== Anno in depth ====
// Foo
// propertyFoo
// functionFoo
// p1
// local
// constructorParameterFoo
// <init>
// param
// Bar
// Baz
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
// ==== A1 superficial====
// Foo
// propertyFoo
// functionFoo
// p1
// constructorParameterFoo
// <init>
// param
// Bar
// Baz
// ==== A1 in depth ====
// Foo
// propertyFoo
// functionFoo
// p1
// local
// constructorParameterFoo
// <init>
// param
// Bar
// Baz
// ==== A2 superficial====
// Foo
// propertyFoo
// functionFoo
// p1
// constructorParameterFoo
// <init>
// param
// Bar
// Baz
// ==== A2 in depth ====
// Foo
// propertyFoo
// functionFoo
// p1
// local
// constructorParameterFoo
// <init>
// param
// Bar
// Baz
// END
//FILE: annotations.kt
annotation class Anno
annotation class Bnno
typealias A1 = Anno
typealias A2 = A1

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

@A1
class Bar

@A2
class Baz
