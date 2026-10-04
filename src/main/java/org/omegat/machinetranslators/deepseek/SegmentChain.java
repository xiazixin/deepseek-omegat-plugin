package org.omegat.machinetranslators.deepseek;

import java.util.ArrayList;
import java.util.List;

/**
 * Ordered, append-only chain of previously translated segments for the
 * DeepSeek system prompt ("context chaining").
 * <p>
 * The chain grows ONLY in order: a position may join once every position
 * before it (back to the chain's start) has a stored translation. A gap
 * (segment without a stored translation) stalls growth until it is filled —
 * the chain is never extended across a gap, and jumping to a later segment
 * keeps the existing chain but cannot extend it.
 * <p>
 * KV-cache friendliness is the design driver: entries are rendered once at
 * append time (frozen) and only ever appended, so the prompt prefix stays
 * byte-stable across requests. Rendering clips to entries before the current
 * position, so a backward jump renders a prefix of the chain. When the entry
 * cap is reached the whole chain resets and regrows from the current
 * position — one cache break per overflow instead of a sliding window that
 * would break the cache on every request.
 */
final class SegmentChain {

    /** Provides segment data for a position in the project's ordered entry list. */
    interface Lookup {
        /** Source text at the position, or null if out of range. */
        String source(int pos);

        /** Stored (committed) translation at the position, or null when absent. */
        String translation(int pos);
    }

    /** Renders one frozen chain line from a segment's data. */
    interface LineRenderer {
        String render(int pos, String src, String trg);
    }

    private static final class Entry {
        final int pos;
        final String line;

        Entry(int pos, String line) {
            this.pos = pos;
            this.line = line;
        }
    }

    private final int maxEntries;
    private final List<Entry> entries = new ArrayList<>();
    /** Next position eligible to join the chain; -1 = not started yet. */
    private int nextPos = -1;

    SegmentChain(int maxEntries) {
        this.maxEntries = Math.max(0, maxEntries);
    }

    /**
     * Grows the chain toward {@code currentPos}, appending every contiguous
     * position that has a stored translation. Must be called once per
     * translation request, before {@link #render(int)}.
     */
    synchronized void update(int currentPos, Lookup lookup, LineRenderer renderer) {
        if (nextPos < 0) {
            nextPos = currentPos;
        }
        while (nextPos < currentPos) {
            String trg = lookup.translation(nextPos);
            if (trg == null || trg.isEmpty()) {
                break; // Gap: not stored/committed yet — chain waits here
            }
            if (maxEntries > 0 && entries.size() >= maxEntries) {
                // Cap reached: reset and regrow from the current position
                entries.clear();
                nextPos = currentPos;
                break;
            }
            String src = lookup.source(nextPos);
            if (src == null) {
                break;
            }
            entries.add(new Entry(nextPos, renderer.render(nextPos, src, trg)));
            nextPos++;
        }
    }

    /**
     * Renders the "Previous segments" block for a request at
     * {@code currentPos}, using only entries before it (a prefix of the
     * chain). Returns an empty string when there is nothing to show.
     */
    synchronized String render(int currentPos) {
        StringBuilder sb = new StringBuilder();
        for (Entry e : entries) {
            if (e.pos >= currentPos) {
                break; // Entries are ordered; nothing later can qualify
            }
            sb.append(e.line).append('\n');
        }
        if (sb.length() == 0) {
            return "";
        }
        return "\n\nPrevious segments\n" + sb
            + "\nCurrent segment: segment " + (currentPos + 1) + " below";
    }

    /** Clears the chain (project change, settings change). */
    synchronized void reset() {
        entries.clear();
        nextPos = -1;
    }
}
