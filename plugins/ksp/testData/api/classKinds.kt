// WITH_RUNTIME
// TEST PROCESSOR: ClassKindsProcessor
// EXPECTED:
// JA: ANNOTATION_CLASS
// JC: CLASS
// JE: ENUM_CLASS
// JI: INTERFACE
// KA: ANNOTATION_CLASS
// KC: CLASS
// KE.ENTRY: ENUM_ENTRY
// KE: ENUM_CLASS
// KI: INTERFACE
// KO: OBJECT
// kotlin.Annotation: INTERFACE
// kotlin.Any: CLASS
// kotlin.Deprecated: ANNOTATION_CLASS
// kotlin.DeprecationLevel.WARNING: ENUM_ENTRY
// kotlin.DeprecationLevel: ENUM_CLASS
// kotlin.Double.Companion: OBJECT
// END

// FILE: K.kt
class KC
interface KI
annotation class KA
object KO
enum class KE {
    ENTRY
}

// FILE: J.java
class JC {}
interface JI {}
@interface JA {}
enum JE {}
