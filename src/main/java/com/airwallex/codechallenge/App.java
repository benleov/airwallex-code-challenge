package com.airwallex.codechallenge;

import com.airwallex.codechallenge.input.Reader;

import java.util.stream.Collectors;

public class App {

    public static void main(String[] args) {
        Reader reader = new Reader();
        new AlertProcessor().start(reader.read(args[0]).collect(Collectors.toList()));
    }
}
