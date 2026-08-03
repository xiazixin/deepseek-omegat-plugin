package org.omegat.machinetranslators.deepseek;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class RequestFormatterTest {

    @Test
    public void rendersNewlinesAndSections() {
        String json = "{\"messages\":["
                + "{\"content\":\"Line one.\\nLine two.\",\"role\":\"system\"},"
                + "{\"content\":\"Hello world\",\"role\":\"user\"}],"
                + "\"model\":\"deepseek-v4-flash\",\"stream\":false,\"temperature\":0.3}";

        String formatted = RequestFormatter.format(json);

        assertTrue(formatted.contains("model: deepseek-v4-flash"));
        assertTrue(formatted.contains("stream: false"));
        assertTrue(formatted.contains("temperature: 0.3"));
        // Escaped \n in the JSON content must appear as a real line break
        assertTrue(formatted.contains("=== system ===\nLine one.\nLine two."));
        assertTrue(formatted.contains("=== user ===\nHello world"));
    }

    @Test
    public void fallsBackToRawBodyWhenNotParseable() {
        assertEquals("not json", RequestFormatter.format("not json"));
    }
}
