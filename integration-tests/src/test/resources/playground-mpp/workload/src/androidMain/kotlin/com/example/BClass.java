package com.example;

import com.example.annotation.Builder;

import java.util.List;

import hello.HELLO;

@Builder
public class BClass {
    public HELLO hello;
    public HELLO helloFun(){
        return null;
    }
    public List<HELLO> list = null;
}