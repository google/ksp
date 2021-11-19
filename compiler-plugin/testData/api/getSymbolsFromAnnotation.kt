// TEST PROCESSOR: GetSymbolsFromAnnotationProcessor
// EXPECTED:
// ==== Anno superficial====
// Foo:KSClassDeclaration
// <init>:KSFunctionDeclaration
// constructorParameterFoo:KSValueParameter
// param:KSValueParameter
// constructorParameterFoo:KSPropertyDeclaration
// propertyFoo:KSPropertyDeclaration
// functionFoo:KSFunctionDeclaration
// p1:KSValueParameter
// Bar:KSClassDeclaration
// Baz:KSClassDeclaration
// ==== Anno in depth ====
// Foo:KSClassDeclaration
// <init>:KSFunctionDeclaration
// constructorParameterFoo:KSValueParameter
// param:KSValueParameter
// constructorParameterFoo:KSPropertyDeclaration
// propertyFoo:KSPropertyDeclaration
// functionFoo:KSFunctionDeclaration
// p1:KSValueParameter
// local:KSPropertyDeclaration
// Bar:KSClassDeclaration
// Baz:KSClassDeclaration
// ==== Bnno superficial====
// File: Foo.kt:KSFile
// <init>:KSFunctionDeclaration
// propertyFoo.getter():KSPropertyAccessorImpl
// p2:KSValueParameter
// ==== Bnno in depth ====
// File: Foo.kt:KSFile
// <init>:KSFunctionDeclaration
// propertyFoo.getter():KSPropertyAccessorImpl
// p2:KSValueParameter
// ==== A1 superficial====
// Foo:KSClassDeclaration
// <init>:KSFunctionDeclaration
// constructorParameterFoo:KSValueParameter
// param:KSValueParameter
// constructorParameterFoo:KSPropertyDeclaration
// propertyFoo:KSPropertyDeclaration
// functionFoo:KSFunctionDeclaration
// p1:KSValueParameter
// Bar:KSClassDeclaration
// Baz:KSClassDeclaration
// ==== A1 in depth ====
// Foo:KSClassDeclaration
// <init>:KSFunctionDeclaration
// constructorParameterFoo:KSValueParameter
// param:KSValueParameter
// constructorParameterFoo:KSPropertyDeclaration
// propertyFoo:KSPropertyDeclaration
// functionFoo:KSFunctionDeclaration
// p1:KSValueParameter
// local:KSPropertyDeclaration
// Bar:KSClassDeclaration
// Baz:KSClassDeclaration
// ==== A2 superficial====
// Foo:KSClassDeclaration
// <init>:KSFunctionDeclaration
// constructorParameterFoo:KSValueParameter
// param:KSValueParameter
// constructorParameterFoo:KSPropertyDeclaration
// propertyFoo:KSPropertyDeclaration
// functionFoo:KSFunctionDeclaration
// p1:KSValueParameter
// Bar:KSClassDeclaration
// Baz:KSClassDeclaration
// ==== A2 in depth ====
// Foo:KSClassDeclaration
// <init>:KSFunctionDeclaration
// constructorParameterFoo:KSValueParameter
// param:KSValueParameter
// constructorParameterFoo:KSPropertyDeclaration
// propertyFoo:KSPropertyDeclaration
// functionFoo:KSFunctionDeclaration
// p1:KSValueParameter
// local:KSPropertyDeclaration
// Bar:KSClassDeclaration
// Baz:KSClassDeclaration
// ==== Cnno in depth ====
// constructorParameterFoo:KSValueParameter
// <set-?>:KSValueParameter
// x:KSValueParameter
// x:KSPropertyDeclaration
// END
//FILE: annotations.kt
annotation class Anno
annotation class Bnno
annotation class Cnno
typealias A1 = Anno
typealias A2 = A1

//FILE: Foo.kt
@file:Bnno

@Anno
class Foo @Anno constructor(@Anno @param:Cnno val constructorParameterFoo: Int, @Anno param: Int){
    @Bnno constructor() {

    }

    @Anno
    val propertyFoo: String
    @Bnno get() = TODO()

    @Anno
    fun functionFoo(@Anno p1: Int, @Bnno p2: Int) {
        @Anno val local = 1
    }

    @setparam:Cnno
    var a = 1
}

class C(@Cnno val x: Int)

@A1
class Bar

@A2
class Baz
