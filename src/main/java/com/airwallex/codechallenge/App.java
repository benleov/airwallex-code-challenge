package com.airwallex.codechallenge;

import com.airwallex.codechallenge.input.Mapper;

import java.util.stream.Collectors;

public class App {

    public static void main(String[] args) {
        Mapper mapper = new Mapper();
        new AlertProcessor().start(mapper.read(args[0]).collect(Collectors.toList()));
    }
}
