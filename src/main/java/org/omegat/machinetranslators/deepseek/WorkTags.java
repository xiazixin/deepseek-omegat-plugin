package org.omegat.machinetranslators.deepseek;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.omegat.core.Core;
import org.omegat.util.Log;

/**
 * Work tags — user-provided metadata identifying the source work (author,
 * title, tags, labels) so the AI can match tone, genre, and terminology.
 * <p>
 * Stored as {@code deepseek_tags.txt} in the OmegaT project folder, one
 * {@code key: value} line per field, UTF-8. Missing file or no open project
 * simply means "no tags" — the feature is silently skipped.
 */
final class WorkTags {

    static final String FILE_NAME = "deepseek_tags.txt";

    /** Tag keys in display/save order. */
    static final String[] KEYS = { "author", "title", "tags", "labels", "custom" };
    private static final Set<String> KEY_SET = new LinkedHashSet<>(Arrays.asList(KEYS));

    private WorkTags() {
    }

    /**
     * Returns the tags file inside the current project's folder, or null when
     * no project is open.
     */
    static File getTagsFile() {
        try {
            if (Core.getProject() == null) {
                return null;
            }
            String projectRoot = Core.getProject().getProjectProperties().getProjectRoot();
            if (projectRoot == null || projectRoot.isEmpty()) {
                return null;
            }
            return new File(projectRoot, FILE_NAME);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Reads the tags file. Returns an empty map when the file does not exist.
     */
    static Map<String, String> load(File file) {
        Map<String, String> values = new LinkedHashMap<>();
        if (file == null || !file.isFile()) {
            return values;
        }
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                int sep = line.indexOf(':');
                if (sep <= 0) {
                    continue;
                }
                String key = line.substring(0, sep).trim().toLowerCase();
                String value = line.substring(sep + 1).trim();
                if (KEY_SET.contains(key) && !value.isEmpty()) {
                    values.put(key, value);
                }
            }
        } catch (Exception e) {
            Log.log(e);
        }
        return values;
    }

    /**
     * Writes the tags file (overwrites — this is both "create" and "update").
     * Only non-empty fields are written.
     */
    static void save(File file, Map<String, String> values) throws Exception {
        try (PrintWriter writer = new PrintWriter(
                new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
            writer.println("# DeepSeek work tags — identifies the source work to the AI");
            for (String key : KEYS) {
                String value = values.getOrDefault(key, "").trim();
                if (!value.isEmpty()) {
                    writer.println(key + ": " + value);
                }
            }
        }
    }

    /**
     * Formats the tags as a system-prompt section, or "" when no tags are set.
     */
    static String formatForPrompt(Map<String, String> values) {
        StringBuilder fields = new StringBuilder();
        appendField(fields, "Author", values.get("author"));
        appendField(fields, "Title", values.get("title"));
        appendField(fields, "Tags", values.get("tags"));
        appendField(fields, "Labels", values.get("labels"));
        appendField(fields, "Additional tags", values.get("custom"));
        if (fields.length() == 0) {
            return "";
        }
        return "\n\nWork being translated (use this to match tone, genre, and terminology):\n"
                + fields;
    }

    private static void appendField(StringBuilder sb, String label, String value) {
        if (value != null && !value.trim().isEmpty()) {
            sb.append("- ").append(label).append(": ").append(value.trim()).append('\n');
        }
    }
}
