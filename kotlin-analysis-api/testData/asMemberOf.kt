/*
 * Copyright 2022 Google LLC
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
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
// TEST PROCESSOR: AsMemberOfProcessor
// EXPECTED:
// main.Test: MutableIterator<(String..String?)>
// main.Test: Iterator<(String..String?)>
// main.Test: MutableIterator<(String..String?)>
// lib.Test: MutableIterator<(String..String?)>
// lib.Test: Iterator<(String..String?)>
// lib.Test: MutableIterator<(String..String?)>
// main.TestKt: MutableIterator<String>
// main.TestKt: Iterator<String>
// main.TestKt: MutableIterator<String>
// lib.TestKt: MutableIterator<String>
// lib.TestKt: Iterator<String>
// lib.TestKt: MutableIterator<String>
// Child1!!
// intType: kotlin.Int!!
// baseTypeArg1: kotlin.Int!!
// baseTypeArg2: kotlin.String?
// typePair: kotlin.Pair!!<kotlin.String?, kotlin.Int!!>
// errorType: <ERROR TYPE: NonExistType>!!
// extensionProperty: kotlin.String?
// returnInt: () -> kotlin.Int!!
// returnArg1: () -> kotlin.Int!!
// returnArg1Nullable: () -> kotlin.Int?
// returnArg2: () -> kotlin.String?
// returnArg2Nullable: () -> kotlin.String?
// receiveArgs: (kotlin.Int?, kotlin.Int!!, kotlin.String?) -> kotlin.Unit!!
// receiveArgsPair: (kotlin.Pair!!<kotlin.Int!!, kotlin.String?>, kotlin.Pair?<kotlin.Int?, kotlin.String?>) -> kotlin.Unit!!
// functionArgType: <BaseTypeArg1: kotlin.Any?>(Base.functionArgType.BaseTypeArg1?) -> kotlin.String?
// functionArgTypeWithBounds: <in T: kotlin.Int!!>(Base.functionArgTypeWithBounds.T?) -> kotlin.String?
// extensionFunction: kotlin.Int!!.() -> kotlin.Int?
// Child2!!<no-type>
// intType: kotlin.Int!!
// baseTypeArg1: kotlin.Any!!
// baseTypeArg2: kotlin.Any?
// typePair: kotlin.Pair!!<kotlin.Any?, kotlin.Any!!>
// errorType: <ERROR TYPE: NonExistType>!!
// extensionProperty: kotlin.Any?
// returnInt: () -> kotlin.Int!!
// returnArg1: () -> kotlin.Any!!
// returnArg1Nullable: () -> kotlin.Any?
// returnArg2: () -> kotlin.Any?
// returnArg2Nullable: () -> kotlin.Any?
// receiveArgs: (kotlin.Int?, kotlin.Any!!, kotlin.Any?) -> kotlin.Unit!!
// receiveArgsPair: (kotlin.Pair!!<kotlin.Any!!, kotlin.Any?>, kotlin.Pair?<kotlin.Any?, kotlin.Any?>) -> kotlin.Unit!!
// functionArgType: <BaseTypeArg1: kotlin.Any?>(Base.functionArgType.BaseTypeArg1?) -> kotlin.Any?
// functionArgTypeWithBounds: <in T: kotlin.Any!!>(Base.functionArgTypeWithBounds.T?) -> kotlin.Any?
// extensionFunction: kotlin.Any!!.() -> kotlin.Any?
// Child2!!<kotlin.String!!>
// intType: kotlin.Int!!
// baseTypeArg1: kotlin.String!!
// baseTypeArg2: kotlin.String?
// typePair: kotlin.Pair!!<kotlin.String?, kotlin.String!!>
// errorType: <ERROR TYPE: NonExistType>!!
// extensionProperty: kotlin.String?
// returnInt: () -> kotlin.Int!!
// returnArg1: () -> kotlin.String!!
// returnArg1Nullable: () -> kotlin.String?
// returnArg2: () -> kotlin.String?
// returnArg2Nullable: () -> kotlin.String?
// receiveArgs: (kotlin.Int?, kotlin.String!!, kotlin.String?) -> kotlin.Unit!!
// receiveArgsPair: (kotlin.Pair!!<kotlin.String!!, kotlin.String?>, kotlin.Pair?<kotlin.String?, kotlin.String?>) -> kotlin.Unit!!
// functionArgType: <BaseTypeArg1: kotlin.Any?>(Base.functionArgType.BaseTypeArg1?) -> kotlin.String?
// functionArgTypeWithBounds: <in T: kotlin.String!!>(Base.functionArgTypeWithBounds.T?) -> kotlin.String?
// extensionFunction: kotlin.String!!.() -> kotlin.String?
// NotAChild!!
// intType: java.lang.IllegalArgumentException: NotAChild is not a sub type of the class/interface that contains `intType` (Base)
// baseTypeArg1: java.lang.IllegalArgumentException: NotAChild is not a sub type of the class/interface that contains `baseTypeArg1` (Base)
// baseTypeArg2: java.lang.IllegalArgumentException: NotAChild is not a sub type of the class/interface that contains `baseTypeArg2` (Base)
// typePair: java.lang.IllegalArgumentException: NotAChild is not a sub type of the class/interface that contains `typePair` (Base)
// errorType: java.lang.IllegalArgumentException: NotAChild is not a sub type of the class/interface that contains `errorType` (Base)
// extensionProperty: java.lang.IllegalArgumentException: NotAChild is not a sub type of the class/interface that contains `extensionProperty` (Base)
// returnInt: java.lang.IllegalArgumentException: NotAChild is not a sub type of the class/interface that contains `returnInt` (Base)
// returnArg1: java.lang.IllegalArgumentException: NotAChild is not a sub type of the class/interface that contains `returnArg1` (Base)
// returnArg1Nullable: java.lang.IllegalArgumentException: NotAChild is not a sub type of the class/interface that contains `returnArg1Nullable` (Base)
// returnArg2: java.lang.IllegalArgumentException: NotAChild is not a sub type of the class/interface that contains `returnArg2` (Base)
// returnArg2Nullable: java.lang.IllegalArgumentException: NotAChild is not a sub type of the class/interface that contains `returnArg2Nullable` (Base)
// receiveArgs: java.lang.IllegalArgumentException: NotAChild is not a sub type of the class/interface that contains `receiveArgs` (Base)
// receiveArgsPair: java.lang.IllegalArgumentException: NotAChild is not a sub type of the class/interface that contains `receiveArgsPair` (Base)
// functionArgType: java.lang.IllegalArgumentException: NotAChild is not a sub type of the class/interface that contains `functionArgType` (Base)
// functionArgTypeWithBounds: java.lang.IllegalArgumentException: NotAChild is not a sub type of the class/interface that contains `functionArgTypeWithBounds` (Base)
// extensionFunction: java.lang.IllegalArgumentException: NotAChild is not a sub type of the class/interface that contains `extensionFunction` (Base)
// List#get
// listOfStrings: (kotlin.Int!!) -> kotlin.String!!
// setOfStrings: java.lang.IllegalArgumentException: Set<String> is not a sub type of the class/interface that contains `get` (List)
// Set#contains
// listOfStrings: java.lang.IllegalArgumentException: List<String> is not a sub type of the class/interface that contains `contains` (Set)
// setOfStrings: (kotlin.String!!) -> kotlin.Boolean!!
// JavaChild1!!
// intType: kotlin.Int!!
// typeArg1: kotlin.String
// typeArg2: kotlin.Int
// errorType: (<ERROR TYPE: NonExist>..<ERROR TYPE: NonExist>?)
// returnArg1: () -> kotlin.Int
// receiveArgs: (kotlin.String, kotlin.Int, kotlin.Int!!) -> kotlin.Unit!!
// methodArgType: <BaseTypeArg1: kotlin.Any>(JavaBase.methodArgType.BaseTypeArg1, kotlin.Int) -> kotlin.Unit!!
// methodArgTypeWithBounds: <T: kotlin.String>(JavaBase.methodArgTypeWithBounds.T) -> kotlin.Unit!!
// fileLevelFunction: java.lang.IllegalArgumentException: Cannot call asMemberOf with a function that is not declared in a class or an interface
// fileLevelExtensionFunction: java.lang.IllegalArgumentException: Cannot call asMemberOf with a function that is not declared in a class or an interface
// fileLevelProperty: java.lang.IllegalArgumentException: Cannot call asMemberOf with a property that is not declared in a class or an interface
// errorType: java.lang.IllegalArgumentException: <ERROR TYPE: NonExistingType> is not a sub type of the class/interface that contains `get` (List)
// expected comparison failures
// () -> kotlin.Int!!
// () -> kotlin.Int!!
// (kotlin.Int!!) -> kotlin.Unit!!
// Baz!!<kotlin.Long!!, kotlin.Number!!>
// END
// MODULE: lib
// FILE: Test.java
package lib;
import java.util.List;
class Test {
    List<String> f() {
        throw new RuntimeException("stub");
    }
}
// FILE: TestKt.kt
package lib
class TestKt {
    fun f(): MutableList<String> = TODO()
}
// MODULE: main(lib)
// FILE: Input.kt
open class Base<BaseTypeArg1, BaseTypeArg2> {
    val intType: Int = 0
    val baseTypeArg1: BaseTypeArg1 = TODO()
    val baseTypeArg2: BaseTypeArg2 = TODO()
    val typePair: Pair<BaseTypeArg2, BaseTypeArg1>  = TODO()
    val errorType: NonExistType = TODO()
    fun returnInt():Int = TODO()
    fun returnArg1(): BaseTypeArg1 = TODO()
    fun returnArg1Nullable(): BaseTypeArg1? = TODO()
    fun returnArg2(): BaseTypeArg2 = TODO()
    fun returnArg2Nullable(): BaseTypeArg2? = TODO()
    fun receiveArgs(intArg:Int?, arg1: BaseTypeArg1, arg2:BaseTypeArg2):Unit = TODO()
    fun receiveArgsPair(
        pairs: Pair<BaseTypeArg1, BaseTypeArg2>,
        pairNullable: Pair<BaseTypeArg1?, BaseTypeArg2?>?,
    ):Unit = TODO()
    // intentional type argument name conflict here to ensure it does not get replaced by mistake
    fun <BaseTypeArg1> functionArgType(t:BaseTypeArg1?): BaseTypeArg2 = TODO()
    fun <in T: BaseTypeArg1> functionArgTypeWithBounds(t:T?): BaseTypeArg2 = TODO()
    fun BaseTypeArg1.extensionFunction():BaseTypeArg1? = TODO()
    val BaseTypeArg2.extensionProperty:BaseTypeArg2? = TODO()
}

open class Child1 : Base<Int, String?>() {
}

open class Child2<ChildTypeArg1> : Base<ChildTypeArg1, ChildTypeArg1?>() {
}

class NotAChild
val child2WithString: Child2<String> = TODO()
val listOfStrings: List<String> = TODO()
val setOfStrings: Set<String> = TODO()

fun <T>List<T>.fileLevelExtensionFunction():Unit = TODO()
fun <T>fileLevelFunction():Unit = TODO()
val fileLevelProperty:Int = 3
val errorType: NonExistingType

interface KotlinInterface {
    val x:Int
    var y:Int
}

interface Usage : Foo<Long, Integer> {
    fun foo(param: Foo<Double, Integer>): Foo<String, Integer>
}
interface Foo<V1, V2: Integer> : Bar<Baz<V1, Number>, V2> {}
interface Bar<U1, U2: Integer> : Baz<U1, U2> {}
interface Baz<T1, T2: Number> {
    fun method1(): T1
    fun method2(): T2
}

// FILE: JavaInput.java
class JavaBase<BaseTypeArg1, BaseTypeArg2> {
    int intType;
    BaseTypeArg1 typeArg1;
    BaseTypeArg2 typeArg2;
    NonExist errorType;
    BaseTypeArg2 returnArg1() {
        return null;
    }
    void receiveArgs(BaseTypeArg1 arg1, BaseTypeArg2 arg2, int intArg) {
    }
    <BaseTypeArg1> void methodArgType(BaseTypeArg1 arg1, BaseTypeArg2 arg2) {
    }
    <T extends BaseTypeArg1> void methodArgTypeWithBounds(T arg1) {
    }
}

class JavaChild1 extends JavaBase<String, Integer> {
}

class JavaImpl implements KotlinInterface {
    public int getX() {
        return 1;
    }
    public int getY() {
        return 1;
    }
    public void setY(int value) {
    }
}
// FILE: main/Test.java
package main;
import java.util.List;
class Test {
    List<String> f() {
        throw new RuntimeException("stub");
    }
}
// FILE: TestKt.kt
package main
class TestKt {
    fun f(): MutableList<String> = TODO()
}
