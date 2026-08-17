/*
 * Copyright 2024 Google LLC
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
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

// TEST PROCESSOR: GetSymbolsFromAnnotationProcessor
// EXPECTED:
// ==== Anno inDepth = false ====
// <init>:KSFunctionDeclaration
// Bar:KSClassDeclaration
// Baz:KSClassDeclaration
// Burp:KSClassDeclaration
// Flux:KSTypeAlias
// Foo:KSClassDeclaration
// RGB.B:KSClassDeclaration
// constructorParameterFoo:KSPropertyDeclaration
// constructorParameterFoo:KSValueParameter
// functionFoo:KSFunctionDeclaration
// p1:KSValueParameter
// param:KSValueParameter
// propertyFoo:KSPropertyDeclaration
// ==== Anno inDepth = true ====
// <init>:KSFunctionDeclaration
// Bar:KSClassDeclaration
// Baz:KSClassDeclaration
// Burp:KSClassDeclaration
// Flux:KSTypeAlias
// Foo:KSClassDeclaration
// RGB.B:KSClassDeclaration
// constructorParameterFoo:KSPropertyDeclaration
// constructorParameterFoo:KSValueParameter
// functionFoo:KSFunctionDeclaration
// local:KSPropertyDeclaration
// p1:KSValueParameter
// param:KSValueParameter
// propertyFoo:KSPropertyDeclaration
// ==== Bnno inDepth = false ====
// <init>:KSFunctionDeclaration
// File: Foo.kt:KSFile
// p2:KSValueParameter
// propertyFoo.getter():KSPropertyAccessorImpl
// ==== Bnno inDepth = true ====
// <init>:KSFunctionDeclaration
// File: Foo.kt:KSFile
// p2:KSValueParameter
// propertyFoo.getter():KSPropertyAccessorImpl
// ==== A1 inDepth = false ====
// <init>:KSFunctionDeclaration
// Bar:KSClassDeclaration
// Baz:KSClassDeclaration
// Burp:KSClassDeclaration
// Flux:KSTypeAlias
// Foo:KSClassDeclaration
// RGB.B:KSClassDeclaration
// constructorParameterFoo:KSPropertyDeclaration
// constructorParameterFoo:KSValueParameter
// functionFoo:KSFunctionDeclaration
// p1:KSValueParameter
// param:KSValueParameter
// propertyFoo:KSPropertyDeclaration
// ==== A1 inDepth = true ====
// <init>:KSFunctionDeclaration
// Bar:KSClassDeclaration
// Baz:KSClassDeclaration
// Burp:KSClassDeclaration
// Flux:KSTypeAlias
// Foo:KSClassDeclaration
// RGB.B:KSClassDeclaration
// constructorParameterFoo:KSPropertyDeclaration
// constructorParameterFoo:KSValueParameter
// functionFoo:KSFunctionDeclaration
// local:KSPropertyDeclaration
// p1:KSValueParameter
// param:KSValueParameter
// propertyFoo:KSPropertyDeclaration
// ==== A2 inDepth = false ====
// <init>:KSFunctionDeclaration
// Bar:KSClassDeclaration
// Baz:KSClassDeclaration
// Burp:KSClassDeclaration
// Flux:KSTypeAlias
// Foo:KSClassDeclaration
// RGB.B:KSClassDeclaration
// constructorParameterFoo:KSPropertyDeclaration
// constructorParameterFoo:KSValueParameter
// functionFoo:KSFunctionDeclaration
// p1:KSValueParameter
// param:KSValueParameter
// propertyFoo:KSPropertyDeclaration
// ==== A2 inDepth = true ====
// <init>:KSFunctionDeclaration
// Bar:KSClassDeclaration
// Baz:KSClassDeclaration
// Burp:KSClassDeclaration
// Flux:KSTypeAlias
// Foo:KSClassDeclaration
// RGB.B:KSClassDeclaration
// constructorParameterFoo:KSPropertyDeclaration
// constructorParameterFoo:KSValueParameter
// functionFoo:KSFunctionDeclaration
// local:KSPropertyDeclaration
// p1:KSValueParameter
// param:KSValueParameter
// propertyFoo:KSPropertyDeclaration
// ==== Cnno inDepth = true ====
// constructorParameterFoo:KSValueParameter
// value:KSValueParameter
// x:KSPropertyDeclaration
// x:KSValueParameter
// ==== MyNestedAnnotation inDepth = false ====
// END
// MODULE: lib
//FILE: annotaitons_in_lib.kt
annotation class Anno
annotation class Bnno
annotation class Cnno

//FILE: aliases_in_lib.kt
typealias A1 = Anno
typealias A2 = A1

// MODULE: main(lib)
//FILE: Foo.kt
@file:Bnno

import Anno
import Anno as A3

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

@A3
class Burp

@Anno
typealias Flux = String

enum class RGB{
    R,
    G,
    @Anno
    B
}
