// TEST PROCESSOR: TypeAliasProcessor
// EXPECTED:
// nestedAlias : <ERROR TYPE: C.NestedAlias> = (expanded) <ERROR TYPE: C.NestedAlias>
// nestedClass : Nested = (expanded) Nested
// param w.o. asMemberOf: <ERROR TYPE: C.NestedAlias> = (expanded) <ERROR TYPE: C.NestedAlias>
// param with asMemberOf: <ERROR TYPE: C.NestedAlias> = (expanded) <ERROR TYPE: C.NestedAlias>
// nestedAliasparam: null: <ERROR TYPE: C.NestedAlias> = (expanded) <ERROR TYPE: C.NestedAlias>
// END

class C {
    typealias NestedAlias = String
    class Nested
}

val nestedAlias: C.NestedAlias = ""
val nestedClass: C.Nested = C.Nested()

class Subject(val nestedAliasparam: C.NestedAlias)
