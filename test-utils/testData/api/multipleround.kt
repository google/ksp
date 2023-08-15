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

// TEST PROCESSOR: MultipleroundProcessor
// EXPECTED:
// Round 0:
// K : <Error>, <Error>, <Error>, <Error>, <Error>, <Error>
// J : <Error>, <Error>, <Error>, <Error>, <Error>, <Error>
// +J.java, +K.kt
// Round 1:
// K : I0, <Error>, <Error>, <Error>, <Error>, <Error>
// J : I0, <Error>, <Error>, <Error>, <Error>, <Error>
// +I0.kt, J.java, K.kt
// Round 2:
// K : I0, I1, <Error>, <Error>, <Error>, <Error>
// J : I0, I1, <Error>, <Error>, <Error>, <Error>
// +I1.java, I0.kt, J.java, K.kt
// Round 3:
// K : I0, I1, I2, <Error>, <Error>, <Error>
// J : I0, I1, I2, <Error>, <Error>, <Error>
// +I2.kt, I0.kt, I1.java, J.java, K.kt
// Round 4:
// K : I0, I1, I2, I3, <Error>, <Error>
// J : I0, I1, I2, I3, <Error>, <Error>
// +I3.java, I0.kt, I1.java, I2.kt, J.java, K.kt
// Round 5:
// K : I0, I1, I2, I3, I4, <Error>
// J : I0, I1, I2, I3, I4, <Error>
// +I4.kt, I0.kt, I1.java, I2.kt, I3.java, J.java, K.kt
// Round 6:
// K : I0, I1, I2, I3, I4, I5
// J : I0, I1, I2, I3, I4, I5
// +I5.java, I0.kt, I1.java, I2.kt, I3.java, I4.kt, J.java, K.kt
// END

// FILE: K.kt
import com.example.*

class K : I0, I1, I2, I3, I4, I5

// FILE: J.java
import com.example.*;

class J implements I0, I1, I2, I3, I4, I5 {

}
