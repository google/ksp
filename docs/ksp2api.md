# KSP2 API Changes

While KSP2 is binary compatible with processors written for KSP1, there are some changes in the return values. The
differences are listed below:

##### Resolve implicit type from function call: val error = mutableMapOf<String, NonExistType>()
* KSP1: The whole type will be an error type due to failed type resolution.
* KSP2: It will successfully resolve the container type, and for the non-existent type in the type argument, it will
        correctly report errors on the specific type argument.

##### Unbounded type parameter
* KSP1: No bounds
* KSP2: An upper bound of `Any?` is always inserted for consistency

##### Resolving references to type aliases in function types and annotations
* KSP1: Expanded to the underlying, non-alias type
* KSP2: Not expanded, like uses in other places

##### Fully qualified names
* KSP1: Constructors have FQN if the constructor is from source, but not if the constructor is from a library.
* KSP2: Constructors do not have FQN

##### Type arguments of inner types
* KSP1: Inner types has arguments from outer types
* KSP2: Inner types has no arguments from outer types

##### Type arguments of star projections
* KSP1: Star projections have type arguments that are expanded to the effective variances according to the declaration
        sites.
* KSP2: No expansion. Star projections have nulls in their type arguments.

##### Variance of Java Array
* KSP1: Java Array has a invariant upper bound
* KSP2: Java Array has a covariant upper bound

##### Type of Enum Entries
* KSP1: Each enum entry has itsown type
* KSP2: All enum entries share the same type with the enclosing enum class

##### Evaluation of Enum Entries in Annotation Arguments
* KSP1: An annotation argument that is an enum entry is evaluated as a `KSType` of the corresponding enum entry
* KSP2: An annotation argument that is an enum entry is evaluated directly as the corresponding `KSClassDeclaration`
        of the enum entry

##### Multi-override scenario
For example
```
interface GrandBaseInterface1 {
  fun foo(): Unit
}

interface GrandBaseInterface2 {
  fun foo(): Unit
}

interface BaseInterface1 : GrandBaseInterface1 {
}

interface BaseInterface2 : GrandBaseInterface2 {
}

class OverrideOrder1 : BaseInterface1, GrandBaseInterface2 {
  override fun foo() = TODO()
}
class OverrideOrder2 : BaseInterface2, GrandBaseInterface1 {
  override fun foo() = TODO()
}
```

* KSP1: Find overridden symbols in BFS order, first super type found on direct super type list that contains overridden 
        symbol is returned. For the example, KSP will say `OverrideOrder1.foo()` overrides `GrandBaseInterface2.foo()`
        and `OverrideOrder2.foo()` overrides `GrandBaseInterface1.foo()`.
* KSP2: DFS order, first super type found overridden symbols (with recursive super type look up) in direct super type 
        list is returned. For the example, KSP will say `OverrideOrder1.foo()` overrides `GrandBaseInterface1.foo()`
        and `OverrideOrder2.foo()` overrides `GrandBaseInterface2.foo()`.
 
##### Java modifier
* KSP1: Transient/volatile fields are final by default
* KSP2: Transient/volatile fields are open by default

##### Type annotations
* KSP1: Type annotations on a type argument is only reflected on the type argument symbol
* KSP2: Type annotations on a type argument now present in the resolved type as well

##### vararg parameters
* KSP1: Considered as an `Array` type
* KSP2: Not considered as an `Array` type

##### Synthesized members of Enums
* KSP1: `values` and `valueOf` are missing if the enum is defined in Kotlin sources
* KSP2: `values` and `valueOf` are always present

##### Synthesized members of data classes
* KSP1: `componentN` and `copy` are missing if the data class is defined in Kotlin sources
* KSP2: `componentN` and `copy` are always present

##### Super type of Enum classes
* KSP1: The super type of Enum classes is `Any`
* KSP2: The super type of Enum classes is `Enum`