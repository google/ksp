/*
 * Copyright 2020 Google LLC
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
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

// WITH_RUNTIME
// TEST PROCESSOR: SuperTypesProcessor
// EXPECTED:
// KotlinInterfaceInLib: kotlin.Any
// KotlinInterfaceInLibWithSuper: KotlinInterfaceInLib
// AbstractKotlinClassInLib: kotlin.Any
// AbstractKotlinClassInLibWithSuperClass: AbstractKotlinClassInLib
// AbstractKotlinClassInLibWithSuperInterface: KotlinInterfaceInLib
// KotlinClassInLib: kotlin.Any
// KotlinClassInLibWithSuperAbstract: AbstractKotlinClassInLib
// KotlinClassInLibWithSuperClass: KotlinClassInLib
// KotlinClassInLibWithSuperInterface: KotlinInterfaceInLib
// JavaInterfaceInLib: kotlin.Any
// JavaInterfaceInLibWithSuper: JavaInterfaceInLib
// AbstractJavaClassInLib: kotlin.Any
// AbstractJavaClassInLibWithSuperInterface: JavaInterfaceInLib
// AbstractJavaClassInLibWithSuperClass: AbstractJavaClassInLib
// JavaClassInLib: kotlin.Any
// JavaClassInLibWithSuperInterface: JavaInterfaceInLib
// JavaClassInLibWithSuperAbstract: AbstractJavaClassInLib
// JavaClassInLibWithSuperClass: JavaClassInLib
// KotlinInterfaceInSource: kotlin.Any
// KotlinInterfaceInSourceWithSuper: KotlinInterfaceInSource
// AbstractKotlinClassInSource: kotlin.Any
// AbstractKotlinClassInSourceWithSuperClass: AbstractKotlinClassInSource
// AbstractKotlinClassInSourceWithSuperInterface: KotlinInterfaceInSource
// KotlinClassInSource: kotlin.Any
// KotlinClassInSourceWithSuperAbstract: AbstractKotlinClassInSource
// KotlinClassInSourceWithSuperClass: KotlinClassInSource
// KotlinClassInSourceWithSuperInterface: KotlinInterfaceInSource
// JavaInterfaceInSource: kotlin.Any
// JavaInterfaceInSourceWithSuper: JavaInterfaceInSource
// AbstractJavaClassInSource: kotlin.Any
// AbstractJavaClassInSourceWithSuperInterface: JavaInterfaceInSource
// AbstractJavaClassInSourceWithSuperClass: AbstractJavaClassInSource
// JavaClassInSource: kotlin.Any
// JavaClassInSourceWithSuperInterface: JavaInterfaceInSource
// JavaClassInSourceWithSuperAbstract: AbstractJavaClassInSource
// JavaClassInSourceWithSuperClass: JavaClassInSource
// END

// MODULE: lib
// FILE: KotlinLib.kt
interface KotlinInterfaceInLib
interface KotlinInterfaceInLibWithSuper : KotlinInterfaceInLib

abstract class AbstractKotlinClassInLib
abstract class AbstractKotlinClassInLibWithSuperClass : AbstractKotlinClassInLib()
abstract class AbstractKotlinClassInLibWithSuperInterface : KotlinInterfaceInLib

open class KotlinClassInLib
open class KotlinClassInLibWithSuperAbstract : AbstractKotlinClassInLib()
open class KotlinClassInLibWithSuperClass : KotlinClassInLib()
open class KotlinClassInLibWithSuperInterface : KotlinInterfaceInLib

// FILE: JavaLib.java
interface JavaInterfaceInLib {}
interface JavaInterfaceInLibWithSuper extends JavaInterfaceInLib {}

abstract class AbstractJavaClassInLib {}
abstract class AbstractJavaClassInLibWithSuperInterface implements JavaInterfaceInLib {}
abstract class AbstractJavaClassInLibWithSuperClass extends AbstractJavaClassInLib {}

class JavaClassInLib {}
class JavaClassInLibWithSuperInterface implements JavaInterfaceInLib {}
class JavaClassInLibWithSuperAbstract extends AbstractJavaClassInLib {}
class JavaClassInLibWithSuperClass extends JavaClassInLib {}

// MODULE: main(lib)
// FILE: KotlinSource.kt
interface KotlinInterfaceInSource
interface KotlinInterfaceInSourceWithSuper : KotlinInterfaceInSource

abstract class AbstractKotlinClassInSource
abstract class AbstractKotlinClassInSourceWithSuperClass : AbstractKotlinClassInSource()
abstract class AbstractKotlinClassInSourceWithSuperInterface : KotlinInterfaceInSource

open class KotlinClassInSource
open class KotlinClassInSourceWithSuperAbstract : AbstractKotlinClassInSource()
open class KotlinClassInSourceWithSuperClass : KotlinClassInSource()
open class KotlinClassInSourceWithSuperInterface : KotlinInterfaceInSource

// FILE: JavaSource.java
interface JavaInterfaceInSource {}
interface JavaInterfaceInSourceWithSuper extends JavaInterfaceInSource {}

abstract class AbstractJavaClassInSource {}
abstract class AbstractJavaClassInSourceWithSuperInterface implements JavaInterfaceInSource {}
abstract class AbstractJavaClassInSourceWithSuperClass extends AbstractJavaClassInSource {}

class JavaClassInSource {}
class JavaClassInSourceWithSuperInterface implements JavaInterfaceInSource {}
class JavaClassInSourceWithSuperAbstract extends AbstractJavaClassInSource {}
class JavaClassInSourceWithSuperClass extends JavaClassInSource {}

