// WITH_RUNTIME
// TEST PROCESSOR: DeclarationOrderProcessor
// EXPECTED:
// lib.KotlinClass
// b:Ljava/lang/String;
// a:Ljava/lang/String;
// c:Ljava/lang/String;
// isB:Ljava/lang/String;
// isA:Ljava/lang/String;
// isC:Ljava/lang/String;
// noBackingB:Ljava/lang/String;
// noBackingA:Ljava/lang/String;
// noBackingC:Ljava/lang/String;
// noBackingVarB:Ljava/lang/String;
// noBackingVarA:Ljava/lang/String;
// noBackingVarC:Ljava/lang/String;
// overloaded:(Ljava/lang/String;)Ljava/lang/String;
// overloaded:(I)Ljava/lang/String;
// overloaded:()Ljava/lang/String;
// overloaded:(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
// lib.JavaClass
// b:Ljava/lang/String;
// a:Ljava/lang/String;
// c:Ljava/lang/String;
// overloaded:(Ljava/lang/String;)V
// overloaded:(I)V
// overloaded:()V
// overloaded:(Ljava/lang/String;Ljava/lang/String;)V
// KotlinClass
// b:Ljava/lang/String;
// a:Ljava/lang/String;
// c:Ljava/lang/String;
// isB:Ljava/lang/String;
// isA:Ljava/lang/String;
// isC:Ljava/lang/String;
// noBackingB:Ljava/lang/String;
// noBackingA:Ljava/lang/String;
// noBackingC:Ljava/lang/String;
// noBackingVarB:Ljava/lang/String;
// noBackingVarA:Ljava/lang/String;
// noBackingVarC:Ljava/lang/String;
// overloaded:(Ljava/lang/String;)Ljava/lang/String;
// overloaded:(I)Ljava/lang/String;
// overloaded:()Ljava/lang/String;
// overloaded:(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
// JavaClass
// b:Ljava/lang/String;
// a:Ljava/lang/String;
// c:Ljava/lang/String;
// overloaded:(Ljava/lang/String;)V
// overloaded:(I)V
// overloaded:()V
// overloaded:(Ljava/lang/String;Ljava/lang/String;)V
// END
// MODULE: module1
// FILE: lib/KotlinClass.kt
package lib;
class KotlinClass {
    val b: String = TODO()
    val a: String = TODO()
    val c: String = TODO()
    val isB:String = TODO()
    val isA:String = TODO()
    val isC:String = TODO()
    val noBackingB: String
        get() = ""
    val noBackingA: String
        get() = ""
    val noBackingC: String
        get() = ""
    var noBackingVarB: String
        get() = ""
        set(value) {}
    var noBackingVarA: String
        get() = ""
        set(value) {}
    var noBackingVarC: String
        get() = ""
        set(value) {}
    fun overloaded(x:String): String = TODO()
    fun overloaded(x:Int): String = TODO()
    fun overloaded(): String = TODO()
    fun overloaded(x:String, y:String): String = TODO()
}
// FILE: lib/JavaClass.java
package lib;
public class JavaClass {
    // notice the non alphabetic order, which is triggering the problem
    String b = "";
    String a = "";
    String c = "";
    void overloaded(String x) {}
    void overloaded(int x) {}
    void overloaded() {}
    void overloaded(String x, String y) {}
}
// MODULE: main(module1)
// FILE: main.kt
class KotlinClass {
    val b: String? = TODO()
    val a: String = TODO()
    val c: String? = TODO()
    val isB:String = TODO()
    val isA:String = TODO()
    val isC:String = TODO()
    val noBackingB: String
        get() = ""
    val noBackingA: String
        get() = ""
    val noBackingC: String
        get() = ""
    var noBackingVarB: String
        get() = ""
        set(value) {}
    var noBackingVarA: String
        get() = ""
        set(value) {}
    var noBackingVarC: String
        get() = ""
        set(value) {}
    fun overloaded(x:String): String = TODO()
    fun overloaded(x:Int): String = TODO()
    fun overloaded(): String = TODO()
    fun overloaded(x:String, y:String): String = TODO()
}
// FILE: JavaClass.java
public class JavaClass {
    String b = "";
    String a = "";
    String c = "";
    void overloaded(String x) {}
    void overloaded(int x) {}
    void overloaded() {}
    void overloaded(String x, String y) {}
}
