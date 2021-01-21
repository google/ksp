# Why KSP

## The problem KSP solves

Compiler plugins are powerful metaprogramming tools that can greatly enhance
how you write code. Compiler plugins call compilers directly as libraries to
analyze and edit input programs. These plugins can also generate output for
various uses. For example, they can generate boilerplate code, and they can
even generate full implementations for specially-marked program elements,
such as `Parcelable`. Plugins have a variety of other uses and can even be used
to implement and fine-tune features that are not provided directly in a
language.

While compiler plugins are powerful, this power comes at a price. To write
even the simplest plugin, you need to have some compiler background
knowledge, as well as a certain level of familiarity with the
implementation details of your specific compiler. Another practical
issue is that plugins are often closely tied to specific compiler
versions, meaning you might need to update your plugin each time you
want to support a newer version of the compiler.

## KSP makes creating lightweight compiler plugins easier

KSP is designed to hide compiler changes, minimizing maintenance
efforts for processors that use it. KSP is designed not to be tied to the
JVM so that it can be adapted to other platforms more easily in the future.
KSP is also designed to minimize build times. For some processors, such as
[Glide](https://github.com/bumptech/glide), KSP reduces full compilation
times by up to 25% when compared to KAPT.

KSP is itself implemented as a compiler plugin. There are prebuilt packages
on Google's Maven repository that you can download and use without having
to build the project yourself.
