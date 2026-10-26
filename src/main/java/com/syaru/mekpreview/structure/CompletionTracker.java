package com.syaru.mekpreview.structure;

/** An edge, not a repeating completion message; the full plan is checked, not its visible slice. */
public final class CompletionTracker {
    private boolean complete;
    public boolean update(boolean anchored, int total, int correct) {
        boolean now = anchored && total > 0 && correct == total;
        boolean changed = now && !complete;
        complete = now; return changed;
    }
    public void reset() { complete = false; }
}
