package com.example

annotation class MyAnnotation

// https://github.com/google/ksp/issues/632
@MyAnnotation
@ExperimentalMultiplatform
class ToBeValidated {
    // https://github.com/google/ksp/issues/574
    val ToBeInferred = listOf("string")
}
