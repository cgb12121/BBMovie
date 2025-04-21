package com.example.bbmovie.testdata;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class TestDataLoaderSample {

    @Autowired
    private TestDataLoader testDataLoader;

    @Test
    void loadTestData() {
        testDataLoader.loadTestData();
    }

    @Test
    void cleanTestData() {
        testDataLoader.cleanTestData();
    }
}