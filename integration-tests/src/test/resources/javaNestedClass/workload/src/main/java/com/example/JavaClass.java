package com.example;

public class JavaClass {

    public int b2;

    public ENUM e;

    public enum ENUM {
        R,G,B
    }

    void inject(InjectionTarget t) {}

    class InjectionTarget {}

    static final class NestedClass {
        static String provideString() {
            return "str";
        }
    }
}
