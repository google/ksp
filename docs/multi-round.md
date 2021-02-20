## Introduction

KSP supports *multiple round processing*, or processing files over multiple rounds, with output from previous rounds being used as additional input for subsequent rounds.


#### Changes to your processor

To use multiple round processing, the `SymbolProcessor.process()` function needs to return a list of deferred symbols (`List<KSAnnotated>`) for invalid symbols. Use `KSAnnotated.validate()` to filter invalid symbols to be deferred to the next round.

The following sample code shows how to defer invalid symbols by using a validation check:

```kotlin
override fun process(resolver: Resolver): List<KSAnnotated> {
    val symbols = resolver.getSymbolsWithAnnotation("com.example.annotation.Builder")
    val ret = symbols.filter { !it.validate() }
    symbols
        .filter { it is KSClassDeclaration && it.validate() }
        .map { it.accept(BuilderVisitor(), Unit) }
    return ret
}
```




## Multiple round behavior

#### Defer symbols to the next round

Processors can defer the processing of certain symbols to the next round. When you defer a symbol, you're waiting for other processors to provide additional information, and you can continue deferring the symbol as much as needed. Once the other processors provide the required information, the processor can then process the symbol.

As an example, a processor that creates a builder for an annotated class might require all parameter types of its constructors to be valid (i.e. resolves to a concrete type). In the first round, <this happens>. Then in the second round, <this happens>.

#### Validate symbols
A convenient way to decide if a symbol should be deferred is through validation. A processor should know which information is necessary to properly process the symbol.
Note that validation usually requires resolution which can be expensive, so we recommend checking only what is required.
Continuing with the previous example, an ideal validation for the builder processor checks only whether all resolved parameter types of the constructors of annotated symbols contain `isError == false`.

KSP provides a default validation utility. For more information, see the [Advanced](#advanced) section.

#### Termination condition

Multiple round processing terminates when a full round of processing generates no new files. If unprocessed deferred symbols still exist when the termination condition is met, KSP logs an error message for each processor with unprocessed deferred symbols.


#### Files accessible at each round

Both newly generated files and existing files are accessible through a `Resolver`. KSP provides two APIs for accessing files: `Resolver.getAllFiles()` and `Resolver.getNewFiles`. `getAllFiles` returns a combined list of both existing files and newly generated files, while `getNewFiles` returns only newly generated files.


#### Changes to getSymbolsAnnotatedWith

To avoid unnecessary reprocessing of symbols, `getSymbolsAnnotatedWith` returns only those symbols found in newly generated files.


#### Processor instantiating

A processor instance is created only once, which means you can store information in the processor object to be used for later rounds.


#### Information consistent cross rounds

All KSP symbols will not be reusable across multiple rounds, as the resolution result can potentially change based on what was generated in a previous round. However, since KSP does not allow modifying existing code, some information such as the string value for a symbol name should still be reusable. To summarize, processors can store information from previous rounds but need to bear in mind that this information might be invalid in future rounds.

#### Error and Exception Handling
When an error(defined by processor calling `KSPLogger.error()`) or exception occurs, processing stops after the current round completes. All processors will call the `onError()` method and will **not** call the `finish()` method.

Note that even though an error has occurred, other processors continue processing normally for that round. This means that error handling occurs after processing has completed for the round.

 Upon Exceptions, KSP will try to distinguish the exceptions from KSP and exceptions from processors.
 Exceptions will result in a termination of processing immediately and be logged as an error in KSPLogger.
 Exceptions from KSP should be reported to KSP developers for further investigation.
 At the end of the round where exceptions or errors happened, all processors will invoke onError() function to do
 their own error handling.

KSP provides a default no-op implementation for `onError()` as part of the `SymbolProcessor` interface. You can override this method to provide your own error handling logic.
## Advanced


#### Default behavior for validation

The default validation logic provided by KSP validates all directly reachable symbols inside the enclosing scope of the symbol that is being validated.
Default validation  checks whether references in the enclosed scope are resolvable to a concrete type but does not recursively dive into the referenced types to perform validation.

#### Write your own validation logic

Default validation behavior might not be suitable for all cases. You can reference `KSValidateVisitor` and write your own validation logic by providing a custom `predicate` lambda, which is then used by `KSValidateVisitor` to filter out symbols that need to be checked.

