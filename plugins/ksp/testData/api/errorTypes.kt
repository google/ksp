// WITH_RUNTIME
// TEST PROCESSOR: ErrorTypeProcessor
// EXPECTED:
// ERROR TYPE
// kotlin.collections.Map
// kotlin.String
// ERROR TYPE
// END
// FILE: a.kt
class C {
    val errorAtTop = mutableMapOf<String, NonExistType>()
    val errorInComponent: Map<String, NonExistType>
}