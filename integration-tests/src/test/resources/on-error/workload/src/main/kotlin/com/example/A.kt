package com.example

@RequiresOptIn
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class MyOptIn

@OptIn(MyOptIn::class)
fun main() {
    print("hello world")
}
