package com.bbmovie;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.annotations.QuarkusMain;

@QuarkusMain
public class WatchlistApplication {
    public static void main(String[] args) {
        Quarkus.run(args);
    }
}
