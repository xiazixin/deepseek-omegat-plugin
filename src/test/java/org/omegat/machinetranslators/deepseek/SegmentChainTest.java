package org.omegat.machinetranslators.deepseek;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

/**
 * Tests the ordered chaining rules: in-order growth only, gaps stall the
 * chain, backfill after the gap is filled, prefix rendering on backward
 * jumps, frozen entries, and cap reset.
 */
public class SegmentChainTest {

    /** Scripted lookup backed by mutable maps (sources and "stored" translations). */
    private static final class ScriptedLookup implements SegmentChain.Lookup {
        final Map<Integer, String> sources = new HashMap<>();
        final Map<Integer, String> translations = new HashMap<>();

        @Override
        public String source(int pos) {
            return sources.get(pos);
        }

        @Override
        public String translation(int pos) {
            return translations.get(pos);
        }
    }

    private static final SegmentChain.LineRenderer RENDERER =
        (pos, src, trg) -> "seg " + (pos + 1) + " : " + src + "  →  " + trg;

    private static ScriptedLookup fullLookup(int count) {
        ScriptedLookup lookup = new ScriptedLookup();
        for (int i = 0; i < count; i++) {
            lookup.sources.put(i, "s" + i);
            lookup.translations.put(i, "t" + i);
        }
        return lookup;
    }

    @Test
    public void growsSequentiallyInOrder() {
        SegmentChain chain = new SegmentChain(0);
        ScriptedLookup lookup = fullLookup(10);

        chain.update(0, lookup, RENDERER);
        assertEquals("", chain.render(0));

        chain.update(1, lookup, RENDERER);
        assertEquals("\n\nPrevious segments\n"
                + "seg 1 : s0  →  t0\n"
                + "\nCurrent segment: segment 2 below", chain.render(1));

        chain.update(2, lookup, RENDERER);
        chain.update(3, lookup, RENDERER);
        assertEquals("\n\nPrevious segments\n"
                + "seg 1 : s0  →  t0\n"
                + "seg 2 : s1  →  t1\n"
                + "seg 3 : s2  →  t2\n"
                + "\nCurrent segment: segment 4 below", chain.render(3));
    }

    @Test
    public void gapStallsChainEvenWhenLaterSegmentsHaveTranslations() {
        SegmentChain chain = new SegmentChain(0);
        ScriptedLookup lookup = fullLookup(10);
        // Position 3 (segment 4) has no stored translation — an untranslated gap
        lookup.translations.remove(3);

        chain.update(0, lookup, RENDERER);
        chain.update(1, lookup, RENDERER);
        chain.update(2, lookup, RENDERER);

        // Jump to position 4 (segment 5): chain kept, extended only up to the gap
        chain.update(4, lookup, RENDERER);
        assertEquals("\n\nPrevious segments\n"
                + "seg 1 : s0  →  t0\n"
                + "seg 2 : s1  →  t1\n"
                + "seg 3 : s2  →  t2\n"
                + "\nCurrent segment: segment 5 below", chain.render(4));

        // Position 4 (segment 5) is translated but can NOT join: the gap at
        // position 3 blocks it, even though position 4 has a translation
        chain.update(5, lookup, RENDERER);
        assertEquals("\n\nPrevious segments\n"
                + "seg 1 : s0  →  t0\n"
                + "seg 2 : s1  →  t1\n"
                + "seg 3 : s2  →  t2\n"
                + "\nCurrent segment: segment 6 below", chain.render(5));
    }

    @Test
    public void backfillsAfterGapSegmentIsTranslated() {
        SegmentChain chain = new SegmentChain(0);
        ScriptedLookup lookup = fullLookup(10);
        lookup.translations.remove(3);

        chain.update(0, lookup, RENDERER);
        chain.update(1, lookup, RENDERER);
        chain.update(2, lookup, RENDERER);
        chain.update(4, lookup, RENDERER);
        chain.update(5, lookup, RENDERER);

        // User goes back and stores a translation for the gap (segment 4),
        // then requests segment 5 again: positions 3 and 4 backfill in order
        lookup.translations.put(3, "t3");
        chain.update(4, lookup, RENDERER);
        assertEquals("\n\nPrevious segments\n"
                + "seg 1 : s0  →  t0\n"
                + "seg 2 : s1  →  t1\n"
                + "seg 3 : s2  →  t2\n"
                + "seg 4 : s3  →  t3\n"
                + "\nCurrent segment: segment 5 below", chain.render(4));

        chain.update(5, lookup, RENDERER);
        assertEquals("\n\nPrevious segments\n"
                + "seg 1 : s0  →  t0\n"
                + "seg 2 : s1  →  t1\n"
                + "seg 3 : s2  →  t2\n"
                + "seg 4 : s3  →  t3\n"
                + "seg 5 : s4  →  t4\n"
                + "\nCurrent segment: segment 6 below", chain.render(5));
    }

    @Test
    public void backwardJumpRendersPrefixOnly() {
        SegmentChain chain = new SegmentChain(0);
        ScriptedLookup lookup = fullLookup(10);

        for (int pos = 0; pos <= 5; pos++) {
            chain.update(pos, lookup, RENDERER);
        }

        // Jump back to position 1 (segment 2): only earlier entries render
        assertEquals("\n\nPrevious segments\n"
                + "seg 1 : s0  →  t0\n"
                + "\nCurrent segment: segment 2 below", chain.render(1));
    }

    @Test
    public void entriesAreFrozenAtAppendTime() {
        SegmentChain chain = new SegmentChain(0);
        ScriptedLookup lookup = fullLookup(10);

        chain.update(0, lookup, RENDERER);
        chain.update(1, lookup, RENDERER);

        // User edits the stored translation of position 0 after it was chained
        lookup.translations.put(0, "EDITED");
        chain.update(2, lookup, RENDERER);

        // The chain still shows the version frozen at append time
        assertEquals("\n\nPrevious segments\n"
                + "seg 1 : s0  →  t0\n"
                + "seg 2 : s1  →  t1\n"
                + "\nCurrent segment: segment 3 below", chain.render(2));
    }

    @Test
    public void capResetRegrowsFromCurrentPosition() {
        SegmentChain chain = new SegmentChain(2);
        ScriptedLookup lookup = fullLookup(10);

        chain.update(0, lookup, RENDERER);
        chain.update(1, lookup, RENDERER);
        chain.update(2, lookup, RENDERER);

        // Third append would exceed the cap: the chain resets (one cache
        // break) and regrows from the current position instead of sliding
        chain.update(3, lookup, RENDERER);
        assertEquals("", chain.render(3));

        chain.update(4, lookup, RENDERER);
        assertEquals("\n\nPrevious segments\n"
                + "seg 4 : s3  →  t3\n"
                + "\nCurrent segment: segment 5 below", chain.render(4));
    }

    @Test
    public void emptyChainRendersEmpty() {
        SegmentChain chain = new SegmentChain(0);
        ScriptedLookup lookup = fullLookup(10);

        assertEquals("", chain.render(5));

        chain.update(0, lookup, RENDERER);
        assertEquals("", chain.render(0));

        // After reset the chain starts over
        chain.update(1, lookup, RENDERER);
        chain.reset();
        assertEquals("", chain.render(2));
    }
}
