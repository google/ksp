/*
 * Copyright 2020 Google LLC
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
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


package com.google.devtools.ksp.symbol

/**
 * An expression that represents calling a method with chaining api.
 *
 * Note that this expression is only valid if the number of chains called > 2,
 * otherwise, it should be [KSCallExpression]
 *
 * @author RinOrz
 */
interface KSChainCallsExpression : KSExpression {

    /**
     * The call chains of this expression.
     *
     * For example, in `a().b().c()`, the arguments is "[a(), b(), c()]"
     */
    val chains: List<KSExpression>
}