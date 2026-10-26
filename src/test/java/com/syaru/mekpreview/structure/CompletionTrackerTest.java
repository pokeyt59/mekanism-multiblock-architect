package com.syaru.mekpreview.structure;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CompletionTrackerTest {
    @Test void notCompleteWhileFollowingOrEmptyOrPartial() {
        var tracker = new CompletionTracker();
        assertFalse(tracker.update(false, 3, 3));
        assertFalse(tracker.update(true, 0, 0));
        assertFalse(tracker.update(true, 3, 2));
    }
    @Test void notifiesOnlyOnceUntilBrokenOrReset() {
        var tracker = new CompletionTracker();
        assertTrue(tracker.update(true, 3, 3)); assertFalse(tracker.update(true, 3, 3));
        assertFalse(tracker.update(true, 3, 2)); assertTrue(tracker.update(true, 3, 3));
        tracker.reset(); assertTrue(tracker.update(true, 3, 3));
    }
    @Test void switchingToFollowResetsCompletion() {
        var tracker = new CompletionTracker();
        assertTrue(tracker.update(true, 3, 3)); assertFalse(tracker.update(false, 3, 3));
        assertTrue(tracker.update(true, 3, 3));
    }
}
