## Introduction

KSP multiple round processing allows processing to happen multiple times, with every time including newly generated files from previous rounds.


#### Changes to your processor



*   To provide multiple round processing, `SymbolProcessor.process()` function will return a list of deferred symbols `List<KSAnnotated>`
*   Use `KSAnnotated.validate()` to filter invalid symbols to be deferred to next round.
*   Sample code to defer invalid symbols with validation check.

```
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


#### Termination condition

After applying multiple round processing, the termination condition will be until no new files are generated. If, when the termination condition is met, there are still deferred symbols not processed, KSP will log an error message for every processor with unprocessed deferred symbols.


#### Files accessible at each round

Both newly generated files and existing files are accessible via `Resolver`, there are 2 APIs provided for accessing files, `Resolver.getAllFiles()` and `Resolver.getNewFiles`. `getAllFiles` will return a combined list of both existing files and newly generated files, while `getNewFiles` only returns newly generated files.


#### Changes to getSymbolsAnnotatedWith

To avoid unnecessary reprocessing of symbols, `getSymbolsAnnotatedWith` will only return symbols found in newly generated files


#### Processor instantiating

Processor instance will only be created once, which means you can store information into the processor to be used for later rounds.


#### Information consistent cross rounds

All KSP symbols will not be reusable cross rounds, due to the resolution result can potentially change based on what was generated last round. However, since KSP does not allow modifying existing code, information such as the string value for a name of a symbol should still be reusable. To summarize, processors can keep information from previous round on their own, but need to bear in mind that they might be invalid in future rounds.

#### Error Handling
In case of an error (defined by processor calling `KSPLogger.error()`), the round where error occurs will be the last round of processing. All processors will invoke `onError()` method, there will be no `finish()` method invoked in case of an error.

Note that even though an error has occured, other processors will still continue processing until all processors have finished processing for that round. That means error handling happen after processing is done.

A default implementation for `onError()` will be provided as part of `SymbolProcessor` interface, which is a no-op implementation, you can also override it and therefore write your own error handling logic.
## Advanced


#### Write your own validation logic

Default validation logic provided by KSP checks all reachable symbols inside the enclosing scope of the symbol that is being checked. This might not suit your use case. You can reference `KSValidateVisitor` and write your own validation logic by providing a custom `predicate` lambda which is used by `KSValidateVisitor` to filter out symbols that need to be checked.
