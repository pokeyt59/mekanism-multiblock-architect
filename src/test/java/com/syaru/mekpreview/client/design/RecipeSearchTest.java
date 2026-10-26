package com.syaru.mekpreview.client.design;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class RecipeSearchTest {
    @Test void matchesJapaneseAndMultipleTerms() {
        assertEquals(List.of(1), RecipeSearch.filter(List.of("鉄 インゴット", "チタン インゴット test:titanium"), "チタン インゴット"));
    }
    @Test void normalizesCaseAndFullWidthLatinCharacters() {
        assertEquals(List.of(0), RecipeSearch.filter(List.of("minecraft:iron_ingot", "gold_ingot"), "ＩＲＯＮ"));
    }
    @Test void retainsOriginalRecipeIndices() {
        assertEquals(List.of(0, 2), RecipeSearch.filter(List.of("iron 1", "gold", "iron 2"), "iron"));
    }
    @Test void emptyQueryShowsAllAndMissingTermShowsNone() {
        assertEquals(List.of(0, 1), RecipeSearch.filter(List.of("a", "b"), "  "));
        assertTrue(RecipeSearch.filter(List.of("a", "b"), "missing").isEmpty());
    }
}
