package com.example.bbmoviesearch.utils;

import java.util.ArrayList;
import java.util.List;

public class EmbeddingUtils {

    private EmbeddingUtils() {
    }

    public static List<Float> convertToFloatList(float[] floats) {
        List<Float> list = new ArrayList<>(floats.length);
        for (float f : floats) list.add(f);
        return list;
    }
}
