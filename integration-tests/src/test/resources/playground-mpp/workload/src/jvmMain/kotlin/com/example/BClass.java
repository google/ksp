package com.example;

import com.example.annotation.Builder;
import hello.HELLO;
import java.util.List;

@Builder
public class BClass {
    public HELLO hello;
    public HELLO helloFun(){
        return null;
    }
    public List<HELLO> list = null;
}