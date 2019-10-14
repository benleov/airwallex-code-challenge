package com.airwallex.codechallenge;

import com.airwallex.codechallenge.input.Mapper;

import java.util.stream.Collectors;

public class App {

    public static void main(String[] args) {
        Mapper mapper = new Mapper();


        // this could be enhanced to batch together the largest required window size for
        // any alerter rather than collecting all of them at once, which would reduce heap
        new AlertProcessor().start(mapper.read(args[0]).collect(Collectors.toList()));
    }
}
