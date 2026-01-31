// TEST PROCESSOR: TypeAliasProcessor
// EXPECTED:
// nestedAlias : NestedAlias = String = (expanded) String
// nestedClass : Nested = (expanded) Nested
// param w.o. asMemberOf: NestedAlias = String = (expanded) String
// param with asMemberOf: NestedAlias = String = (expanded) String
// param: C.NestedAlias: NestedAlias = String = (expanded) String
// END

class C {
    typealias NestedAlias = String
    class Nested
}

val nestedAlias: C.NestedAlias = ""
val nestedClass: C.Nested = C.Nested()

class Subject(val param: C.NestedAlias)
