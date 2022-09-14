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

// WITH_RUNTIME
// TEST PROCESSOR: RawTypesProcessor
// EXPECTED:
// barRaw
// barRawField
// barRawGet
// barRawGetCompiled
// fooRaw
// fooRawField
// fooRawGet
// fooRawGetCompiled
// p4
// p5
// END
// MODULE: lib
// FILE: dummy.kt
// FILE: Api.java
public interface Api {}

// FILE: Foo.java
public class Foo<T> {}

// FILE: Bar.java
public class Bar<T extends Api> {}

// FILE: UsageCompiled.java
public abstract class UsageCompiled {
    public static void usage(
        Foo<?> fooWildcardCompiled,
        Foo<? super Api> fooWildcardExCompiled,
        Bar<?> barWildcardCompiled,
        Bar<? extends Api> barWildcardExCompiled,
        Foo fooRawCompiled,
        Bar barRawCompiled
    ) {}

    public abstract Foo<?> fooWildcardGetCompiled();
    public abstract Foo<? super Api> fooWildcardExGetCompiled();
    public abstract Bar<?> barWildcardGetCompiled();
    public abstract Bar<? extends Api> barWildcardExGetCompiled();
    public abstract Foo fooRawGetCompiled();
    public abstract Bar barRawGetCompiled();
}

// MODULE: main(lib)
// FILE: usage.kt
fun usage(
    fooStar: Foo<*>,
    fooIn: Foo<in Api>,
    fooOut: Foo<out Api>,
    barStar: Bar<*>,
) = Unit

val fooStarProp: Foo<*>
val fooInProp: Foo<in Api>
val fooOutProp: Foo<out Api>
val barStarProp: Bar<*>

// FILE: Usage.java
public abstract class Usage {
    public Foo<?> fooWildcardField;
    public Foo<? super Api> fooWildcardExField;
    public Bar<?> barWildcardField;
    public Bar<? extends Api> barWildcardExField;
    public Foo fooRawField;
    public Bar barRawField;

    public static void usage(
        Foo<?> fooWildcard,
        Foo<? super Api> fooWildcardEx,
        Bar<?> barWildcard,
        Bar<? extends Api> barWildcardEx,
        Foo fooRaw,
        Bar barRaw
    ) {}

    public abstract Foo<?> fooWildcardGet();
    public abstract Foo<? super Api> fooWildcardExGet();
    public abstract Bar<?> barWildcardGet();
    public abstract Bar<? extends Api> barWildcardExGet();
    public abstract Foo fooRawGet();
    public abstract Bar barRawGet();
}
