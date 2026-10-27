package com.syaru.mekpreview.client;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class MaterialHudLayoutTest {
    @Test void displaysMoreThanFiveRowsWhenThereIsRoom() {
        var view = MaterialHudLayout.fit(400, Collections.nCopies(10, 18), 0);
        assertEquals(0, view.first()); assertEquals(10, view.end());
        assertEquals(180, view.height()); assertFalse(view.scrollable());
    }
    @Test void growsToAvailableHeightAndReservesScrollFooter() {
        var view = MaterialHudLayout.fit(400, Collections.nCopies(40, 18), 0);
        assertTrue(view.scrollable());
        assertEquals(400 - 54 - 90 - 13 - 12, view.height());
        assertTrue(view.end() > 5);
    }
    @Test void lastRowIsReachableAndFullyVisible() {
        var heights = Collections.nCopies(40, 18);
        var view = MaterialHudLayout.fit(267, heights, Integer.MAX_VALUE);
        assertEquals(40, view.end()); assertEquals(view.contentHeight(), view.offset() + view.height());
    }
    @Test void everyRowCanBeReachedIncludingWrappedAndOversizedRows() {
        var heights = List.of(18, 31, 18, 400, 18, 22, 18, 18, 18);
        var seen = new HashSet<Integer>();
        for (int offset = 0; offset <= 700; offset += 8) {
            var view = MaterialHudLayout.fit(267, heights, offset);
            for (int i = view.first(); i < view.end(); i++) seen.add(i);
            assertTrue(view.offset() + view.height() <= view.contentHeight());
        }
        assertEquals(heights.size(), seen.size());
    }
    @Test void resizingOrShorterListClampsOldScrollOffset() {
        var view = MaterialHudLayout.fit(600, Collections.nCopies(8, 18), 9999);
        assertEquals(0, view.offset()); assertEquals(8, view.end()); assertFalse(view.scrollable());
    }
    @Test void negativeScrollClampsAtBeginning() {
        assertEquals(0, MaterialHudLayout.fit(267, Collections.nCopies(20, 18), -100).offset());
    }
    @Test void emptyListIsNotScrollable() {
        var view = MaterialHudLayout.fit(267, List.of(), 99);
        assertEquals(0, view.first()); assertEquals(0, view.end()); assertEquals(0, view.height()); assertFalse(view.scrollable());
    }
    @Test void tinyWindowNeverHasNegativeGeometry() {
        var view = MaterialHudLayout.fit(80, List.of(18, 30), 999);
        assertEquals(0, view.height()); assertEquals(view.contentHeight(), view.offset());
        assertEquals(view.first(), view.end());
    }
    @Test void standardRowsAreNotCutIntoSliversAtViewportEdges() {
        var view = MaterialHudLayout.fit(267, Collections.nCopies(24, 18), Integer.MAX_VALUE);
        assertTrue(view.first() * 18 >= view.offset());
        assertTrue(view.end() * 18 <= view.offset() + view.height());
        assertEquals(24, view.end());
    }
}
