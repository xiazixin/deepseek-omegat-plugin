package org.omegat.machinetranslators.deepseek;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;

public class WorkTagsTest {

    @Test
    public void saveLoadRoundTrip() throws Exception {
        File temp = File.createTempFile("deepseek_tags_test", ".txt");
        temp.deleteOnExit();

        Map<String, String> values = new LinkedHashMap<>();
        values.put("author", "猫腻");
        values.put("title", "大道朝天");
        values.put("tags", "玄幻, 修真");
        values.put("labels", "webnovel");
        values.put("custom", "grim tone");
        WorkTags.save(temp, values);

        assertEquals(values, WorkTags.load(temp));
    }

    @Test
    public void loadSkipsEmptyAndUnknownKeys() throws Exception {
        File temp = File.createTempFile("deepseek_tags_test", ".txt");
        temp.deleteOnExit();

        Map<String, String> values = new LinkedHashMap<>();
        values.put("author", "Somebody");
        values.put("title", "  ");
        WorkTags.save(temp, values);

        Map<String, String> loaded = WorkTags.load(temp);
        assertEquals(1, loaded.size());
        assertEquals("Somebody", loaded.get("author"));
    }

    @Test
    public void loadMissingFileReturnsEmpty() {
        assertTrue(WorkTags.load(new File("does_not_exist_12345.txt")).isEmpty());
        assertTrue(WorkTags.load(null).isEmpty());
    }

    @Test
    public void formatForPromptRendersOnlySetFields() {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("title", "大道朝天");
        values.put("tags", "玄幻");

        String formatted = WorkTags.formatForPrompt(values);
        assertTrue(formatted.contains("Work being translated"));
        assertTrue(formatted.contains("- Title: 大道朝天"));
        assertTrue(formatted.contains("- Tags: 玄幻"));
        assertTrue(!formatted.contains("Author"));
    }

    @Test
    public void formatForPromptEmptyWhenNoTags() {
        assertEquals("", WorkTags.formatForPrompt(new LinkedHashMap<>()));
    }
}
