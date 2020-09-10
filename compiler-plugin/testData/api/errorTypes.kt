// WITH_RUNTIME
// TEST PROCESSOR: ErrorTypeProcessor
// EXPECTED:
// ERROR TYPE
// kotlin.collections.Map
// kotlin.String
// ERROR TYPE
// errorInComponent is assignable from errorAtTop: false
// errorInComponent is assignable from class C: false
// Any is assignable from errorInComponent: false
// class C is assignable from errorInComponent: false
// Any is assignable from class C: true
// END
// FILE: a.kt
class C {
    val errorAtTop = mutableMapOf<String, NonExistType>()
    val errorInComponent: Map<String, NonExistType>
}