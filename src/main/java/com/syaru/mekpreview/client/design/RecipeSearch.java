package com.syaru.mekpreview.client.design;

import java.text.Normalizer;
import java.util.*;
import java.util.stream.IntStream;

public final class RecipeSearch {
    private RecipeSearch() {}
    public static String normalize(String text) { return Normalizer.normalize(text, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT).strip(); }
    public static List<Integer> filter(List<String> documents, String query) {
        var words = normalize(query).split("\\s+");
        return IntStream.range(0, documents.size()).filter(i -> {
            String text = normalize(documents.get(i));
            return Arrays.stream(words).allMatch(text::contains);
        }).boxed().toList();
    }
}
