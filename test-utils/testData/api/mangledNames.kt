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
// TEST PROCESSOR: MangledNamesProcessor
// EXPECTED:
// JavaEnum -> declarations
// JavaEnum.VAL1 -> declarations
// JavaEnum.VAL2 -> declarations
// values -> values
// valueOf -> valueOf
// <init> -> <init>
// JavaInput -> declarations
// javaFunction -> javaFunction
// staticJavaFunction -> staticJavaFunction
// getX -> getX
// getY -> getY
// setY -> setY
// mainPackage.Foo -> declarations
// get-normalProp -> getNormalProp
// set-normalProp -> setNormalProp
// get-inlineProp -> getInlineProp-HRn7Rpw
// set-inlineProp -> setInlineProp-E03SJzc
// get-internalProp -> getInternalProp$mainModule
// set-internalProp -> setInternalProp$mainModule
// get-internalInlineProp -> getInternalInlineProp-HRn7Rpw$mainModule
// set-internalInlineProp -> setInternalInlineProp-E03SJzc$mainModule
// get-jvmNameProp -> explicitGetterName
// set-jvmNameProp -> explicitSetterName
// normalFun -> normalFun
// hasJvmName -> explicitJvmName
// inlineReceivingFun -> inlineReceivingFun-E03SJzc
// inlineReturningFun -> inlineReturningFun-HRn7Rpw
// internalInlineReceivingFun -> internalInlineReceivingFun-E03SJzc$mainModule
// internalInlineReturningFun -> internalInlineReturningFun-HRn7Rpw$mainModule
// mainPackage.AbstractKotlinClass -> declarations
// get-abstractVar -> getAbstractVar
// set-abstractVar -> setAbstractVar
// get-abstractVal -> getAbstractVal
// get-internalAbstractVar -> getInternalAbstractVar$mainModule
// set-internalAbstractVar -> setInternalAbstractVar$mainModule
// get-internalAbstractVal -> getInternalAbstractVal$mainModule
// set-internalAbstractVal -> setInternalAbstractVal$mainModule
// fileLevelInternalFun -> fileLevelInternalFun
// fileLevelInlineReceivingFun -> fileLevelInlineReceivingFun-E03SJzc
// fileLevelInlineReturningFun -> fileLevelInlineReturningFun
// fileLevelInternalInlineReceivingFun -> fileLevelInternalInlineReceivingFun-E03SJzc
// fileLevelInternalInlineReturningFun -> fileLevelInternalInlineReturningFun
// mainPackage.MyInterface -> declarations
// get-x -> getX
// get-y -> getY
// set-y -> setY
// libPackage.Foo -> declarations
// get-inlineProp -> getInlineProp-b_MPbnQ
// set-inlineProp -> setInlineProp-mQ73O9w
// get-internalInlineProp -> getInternalInlineProp-b_MPbnQ$lib
// set-internalInlineProp -> setInternalInlineProp-mQ73O9w$lib
// get-internalProp -> getInternalProp$lib
// set-internalProp -> setInternalProp$lib
// get-jvmNameProp -> explicitGetterName
// set-jvmNameProp -> explicitSetterName
// get-normalProp -> getNormalProp
// set-normalProp -> setNormalProp
// hasJvmName -> explicitJvmName
// inlineReceivingFun -> inlineReceivingFun-mQ73O9w
// inlineReturningFun -> inlineReturningFun-b_MPbnQ
// internalInlineReceivingFun -> internalInlineReceivingFun-mQ73O9w$lib
// internalInlineReturningFun -> internalInlineReturningFun-b_MPbnQ$lib
// normalFun -> normalFun
// <init> -> <init>
// libPackage.AbstractKotlinClass -> declarations
// get-abstractVal -> getAbstractVal
// get-abstractVar -> getAbstractVar
// set-abstractVar -> setAbstractVar
// get-internalAbstractVal -> getInternalAbstractVal$lib
// set-internalAbstractVal -> setInternalAbstractVal$lib
// get-internalAbstractVar -> getInternalAbstractVar$lib
// set-internalAbstractVar -> setInternalAbstractVar$lib
// libPackage.MyInterface -> declarations
// get-x -> getX
// get-y -> getY
// set-y -> setY
// END
// MODULE: lib
// FILE: input.kt
/**
 * control group
 */
package libPackage;
inline class Inline1(val value:String)
class Foo {
    var normalProp:String = TODO()
    var inlineProp: Inline1 = TODO()
    internal var internalProp: String = TODO()
    internal var internalInlineProp: Inline1 = TODO()
    @get:JvmName("explicitGetterName")
    @set:JvmName("explicitSetterName")
    var jvmNameProp:String
    fun normalFun() {}
    @JvmName("explicitJvmName")
    fun hasJvmName() {}
    fun inlineReceivingFun(value: Inline1) {}
    fun inlineReturningFun(): Inline1 = TODO()
    internal fun internalInlineReceivingFun(value: Inline1) {}
    internal fun internalInlineReturningFun(): Inline1 = TODO()
}

abstract class AbstractKotlinClass {
    abstract var abstractVar:String
    abstract val abstractVal:String
    internal abstract var internalAbstractVar:String
    internal abstract var internalAbstractVal:String
}

interface MyInterface {
    val x:Int
    var y:Int
}
// MODULE: mainModule(lib)
// FILE: input.kt
package mainPackage;
inline class Inline1(val value:String)
class Foo {
    var normalProp:String = TODO()
    var inlineProp: Inline1 = TODO()
    internal var internalProp: String = TODO()
    internal var internalInlineProp: Inline1 = TODO()
    @get:JvmName("explicitGetterName")
    @set:JvmName("explicitSetterName")
    var jvmNameProp:String
    fun normalFun() {}
    @JvmName("explicitJvmName")
    fun hasJvmName() {}
    fun inlineReceivingFun(value: Inline1) {}
    fun inlineReturningFun(): Inline1 = TODO()
    internal fun internalInlineReceivingFun(value: Inline1) {}
    internal fun internalInlineReturningFun(): Inline1 = TODO()
}

abstract class AbstractKotlinClass {
    abstract var abstractVar:String
    abstract val abstractVal:String
    internal abstract var internalAbstractVar:String
    internal abstract var internalAbstractVal:String
}

internal fun fileLevelInternalFun(): Unit = TODO()
fun fileLevelInlineReceivingFun(inline1: Inline1): Unit = TODO()
fun fileLevelInlineReturningFun(): Inline1 = TODO()
fun fileLevelInternalInlineReceivingFun(inline1: Inline1): Unit = TODO()
fun fileLevelInternalInlineReturningFun(): Inline1 = TODO()

interface MyInterface {
    val x:Int
    var y:Int
}

// FILE: JavaInput.java
import mainPackage.MyInterface;

class JavaInput implements MyInterface {
    String javaField;
    String javaFunction() {}
    static String staticJavaField;
    static void staticJavaFunction() {}
    public int getX() {
        return 1;
    }
    public int getY() {
        return 1;
    }
    public void setY(int value) {
    }
}

// FILE: JavaEnum.java
public enum JavaEnum {
    VAL1,
    VAL2;
}
