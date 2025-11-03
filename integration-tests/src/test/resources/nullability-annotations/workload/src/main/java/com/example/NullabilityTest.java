package com.example;

// Unrecognized by Kotlin compiler. Should not affect nullability at all.
@interface Nullable {}

// Unrecognized by Kotlin compiler. Should not affect nullability at all.
@interface NotNull {}

interface NullabilityTest {
  String s0();

  @javax.annotation.Nullable
  String s1();

  @javax.annotation.Nonnull
  String s2();

  @org.jetbrains.annotations.Nullable
  String s3();

  @org.jetbrains.annotations.NotNull
  String s4();

  @org.checkerframework.checker.nullness.qual.Nullable
  String s5();

  @org.checkerframework.checker.nullness.qual.NonNull
  String s6();

  @Nullable
  String s7();

  @NotNull
  String s8();
}