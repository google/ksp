// WITH_RUNTIME
// TEST PROCESSOR: ConstPropertiesProcessor
// EXPECTED:
// insideCompanionConstCompiled
// insideCompanionConstSource
// insideObjectConstCompiled
// insideObjectConstSource
// topLevelConstCompiled
// topLevelConstSource
// END
// MODULE: lib
// FILE: compiledProperties.kt
package foo.compiled

const val topLevelConstCompiled: String = "hello"
val topLevelCompiled: String = "hello"
val topLevelDelegatedCompiled by lazy { "hello" }
var topLevelVarCompiled: String = "hello"
val topLevelCustomGetterCompiled: String get() = "hello"
object TestObject {
    const val insideObjectConstCompiled: Boolean = true
    val insideObjectCompiled: String = "hello"
    val insideObjectDelegatedCompiled by lazy { "hello" }
    var insideVarObjectCompiled: String = "hello"
    val insideObjectCustomGetterCompiled: String get() = "hello"
}
interface Foo {
    val abstractCompiled: Long
    val abstractWithDefaultCompiled: Long get() = 100L
    companion object {
        const val insideCompanionConstCompiled: Int = 34
    }
}

// MODULE: main(lib)
// FILE: sourceProperties.kt
package foo.source

const val topLevelConstSource: String = "hello"
val topLevelSource: String = "hello"
val topLevelDelegatedSource by lazy { "hello" }
var topLevelVarSource: String = "hello"
val topLevelCustomGetterSource: String get() = "hello"
object TestObject {
    const val insideObjectConstSource: Boolean = true
    val insideObjectSource: String = "hello"
    val insideObjectDelegatedSource by lazy { "hello" }
    var insideVarObjectSource: String = "hello"
    val insideObjectCustomGetterSource: String get() = "hello"
}
interface Foo {
    val abstractSource: Long
    val abstractWithDefaultSource: Long get() = 100L
    companion object {
        const val insideCompanionConstSource: Int = 34
    }
}
