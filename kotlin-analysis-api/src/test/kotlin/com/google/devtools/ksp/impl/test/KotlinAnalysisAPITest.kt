package com.google.devtools.ksp.impl.test

import com.google.devtools.ksp.impl.main
import org.junit.Test

class KotlinAnalysisAPITest {

    @Test
    fun testHello() {
        main(arrayOf("testData/api"))
    }
}
