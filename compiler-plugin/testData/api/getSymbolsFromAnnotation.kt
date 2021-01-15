// TEST PROCESSOR: GetSymbolsFromAnnotationProcessor
// EXPECTED:
// ==== Anno superficial====
// Foo
// propertyFoo
// functionFoo
// constructorParameterFoo
// Foo
// ==== Anno in depth ====
// Foo
// propertyFoo
// functionFoo
// local
// constructorParameterFoo
// Foo
// ==== Bnno superficial====
// File: Foo.kt
// Foo
// propertyFoo.getter()
// ==== Bnno in depth ====
// File: Foo.kt
// Foo
// propertyFoo.getter()
// END
//FILE: annotations.kt
annotation class Anno
annotation class Bnno

//FILE: Foo.kt
@file:Bnno

@Anno
class Foo @Anno constructor(@Anno val constructorParameterFoo: Int){
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

//FILE: JavaSource.java
@Anno
class JavaClass {
    @Anno
    int javaProp;
    @Anno
    fun javaFun() {
    }
}