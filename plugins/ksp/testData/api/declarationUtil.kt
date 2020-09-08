// TEST PROCESSOR: DeclarationUtilProcessor
// FORMAT: <name>: isInternal: isLocal: isPrivate: isProtected: isPublic: isOpen
// EXPECTED:
// Cls: true: false: false: false: false: false
// <simple name: Cls>: false: false: false: false: true: false
// <simple name: aaa>: false: true: false: false: false: false
// Cls.prop: false: false: false: false: true: true
// Cls.protectedProp: false: false: false: true: false: true
// Cls.abstractITFFun: false: false: false: false: true: true
// Cls.pri: false: false: true: false: false: false
// Cls.b: false: false: false: false: true: true
// ITF: false: false: false: false: true: true
// ITF.prop: false: false: false: false: true: true
// ITF.protectedProp: false: false: false: true: false: true
// ITF.b: false: false: false: false: true: true
// ITF.abstractITFFun: false: false: false: false: true: true
// ITF.nonAbstractITFFun: false: false: false: false: true: true
// <simple name: aa>: false: true: false: false: false: false
// END
// FILE: a.kt
internal class Cls(override val b: Int) : ITF {
    constructor() {
        val aaa = 2
        Cls(aaa)
    }
    override val prop: Int = 2

    override val protectedProp: Int = 2

    override fun abstractITFFun(): Int {
        return 2
    }

    private val pri: Int = 3
}

interface ITF {
    val prop: Int

    protected val protectedProp: Int

    val b: Int = 1

    fun abstractITFFun(): Int

    fun nonAbstractITFFun(): Int {
        val aa = "local"
        return 1
    }
}