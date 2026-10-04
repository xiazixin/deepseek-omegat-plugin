package org.omegat.machinetranslators.deepseek;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.BeforeClass;
import org.junit.Test;

import org.omegat.core.Core;
import org.omegat.core.data.EntryKey;
import org.omegat.core.data.SourceTextEntry;
import org.omegat.core.machinetranslators.MachineTranslateError;
import org.omegat.gui.editor.IEditor;
import org.omegat.util.Language;
import org.omegat.util.Preferences;

public class DeepSeekTranslateTest {

    @BeforeClass
    public static void initPreferences() {
        Preferences.init();
    }

    @Test
    public void createJsonRequest() throws Exception {
        Preferences.setPreference(DeepSeekTranslate.ALLOW_DEEPSEEK_TRANSLATE, true);
        Preferences.setPreference(DeepSeekTranslate.PROPERTY_MODEL, "deepseek-v4-flash");
        Preferences.setPreference(DeepSeekTranslate.PROPERTY_TEMPERATURE, "0.3");
        Preferences.setPreference(DeepSeekTranslate.PROPERTY_DYNAMIC_TEMPERATURE, false);
        Preferences.setPreference(DeepSeekTranslate.PROPERTY_AUTO_GLOSSARY, false);
        Preferences.setPreference(DeepSeekTranslate.PROPERTY_SELF_REVIEW, false);
        Preferences.setPreference(DeepSeekTranslate.PROPERTY_REASONING_EFFORT, 0);

        DeepSeekTranslate translate = new DeepSeekTranslate();
        String json = translate.createJsonRequest(new Language("EN"), new Language("DE"), "Hello world");

        String expected = "{"
            + "\"messages\":["
            + "{\"content\":\"You are a professional translation engine for OmegaT. Translate from en to de. Preserve tags, placeholders, and line breaks. Return only the translated text.\",\"role\":\"system\"},"
                + "{\"content\":\"Hello world\",\"role\":\"user\"}],"
                + "\"model\":\"deepseek-v4-flash\","
                + "\"stream\":false,"
                + "\"temperature\":0.3,"
                + "\"reasoning\":{\"effort\":\"none\"}}";

        ObjectMapper mapper = new ObjectMapper();
        assertEquals(mapper.readTree(expected), mapper.readTree(json));
    }

    @Test
    public void createJsonRequestDynamicTemperature() throws Exception {
        Preferences.setPreference(DeepSeekTranslate.ALLOW_DEEPSEEK_TRANSLATE, true);
        Preferences.setPreference(DeepSeekTranslate.PROPERTY_MODEL, "deepseek-v4-flash");
        Preferences.setPreference(DeepSeekTranslate.PROPERTY_TEMPERATURE, "0.3");
        Preferences.setPreference(DeepSeekTranslate.PROPERTY_DYNAMIC_TEMPERATURE, true);
        Preferences.setPreference(DeepSeekTranslate.PROPERTY_AUTO_GLOSSARY, false);
        Preferences.setPreference(DeepSeekTranslate.PROPERTY_SELF_REVIEW, false);
        Preferences.setPreference(DeepSeekTranslate.PROPERTY_REASONING_EFFORT, 0);

        DeepSeekTranslate translate = new DeepSeekTranslate();
        String json = translate.createJsonRequest(new Language("EN"), new Language("DE"), "Hello world");

        String expected = "{"
            + "\"messages\":["
            + "{\"content\":\"You are a professional translation engine for OmegaT. Translate from en to de. Preserve tags, placeholders, and line breaks. Return only the translated text.\",\"role\":\"system\"},"
                + "{\"content\":\"Hello world\",\"role\":\"user\"}],"
                + "\"model\":\"deepseek-v4-flash\","
                + "\"stream\":false,"
                + "\"reasoning\":{\"effort\":\"none\"}}";

        ObjectMapper mapper = new ObjectMapper();
        assertEquals(mapper.readTree(expected), mapper.readTree(json));
    }

    @Test
    public void extractTranslation() throws MachineTranslateError {
        DeepSeekTranslate translate = new DeepSeekTranslate();
        String json = "{\"choices\":[{\"message\":{\"content\":\"Hallo Welt\"}}]}";

        assertEquals("Hallo Welt", translate.extractTranslation(json));
    }

    @Test(expected = MachineTranslateError.class)
    public void extractTranslationMissingChoiceFails() throws MachineTranslateError {
        DeepSeekTranslate translate = new DeepSeekTranslate();
        translate.extractTranslation("{\"id\":\"abc\"}");
    }

    @Test
    public void glossaryModeNoneProducesStandardPrompt() throws Exception {
        Preferences.setPreference(DeepSeekTranslate.ALLOW_DEEPSEEK_TRANSLATE, true);
        Preferences.setPreference(DeepSeekTranslate.PROPERTY_MODEL, "deepseek-v4-flash");
        Preferences.setPreference(DeepSeekTranslate.PROPERTY_TEMPERATURE, "0.3");
        Preferences.setPreference(DeepSeekTranslate.PROPERTY_DYNAMIC_TEMPERATURE, false);
        Preferences.setPreference(DeepSeekTranslate.PROPERTY_GLOSSARY_MODE,
                DeepSeekTranslate.GLOSSARY_MODE_NONE);
        Preferences.setPreference(DeepSeekTranslate.PROPERTY_AUTO_GLOSSARY, false);
        Preferences.setPreference(DeepSeekTranslate.PROPERTY_SELF_REVIEW, false);
        Preferences.setPreference(DeepSeekTranslate.PROPERTY_REASONING_EFFORT, 0);

        DeepSeekTranslate translate = new DeepSeekTranslate();
        String json = translate.createJsonRequest(new Language("EN"), new Language("DE"), "Hello world");

        // With glossary mode NONE and no project, the prompt should be unchanged
        String expected = "{"
            + "\"messages\":["
            + "{\"content\":\"You are a professional translation engine for OmegaT. "
                + "Translate from en to de. Preserve tags, placeholders, and line breaks. "
                + "Return only the translated text.\",\"role\":\"system\"},"
                + "{\"content\":\"Hello world\",\"role\":\"user\"}],"
                + "\"model\":\"deepseek-v4-flash\","
                + "\"stream\":false,"
                + "\"temperature\":0.3,"
                + "\"reasoning\":{\"effort\":\"none\"}}";

        ObjectMapper mapper = new ObjectMapper();
        assertEquals(mapper.readTree(expected), mapper.readTree(json));
    }

    @Test
    public void glossaryModeReferenceDoesNotCrashWithoutProject() throws Exception {
        Preferences.setPreference(DeepSeekTranslate.ALLOW_DEEPSEEK_TRANSLATE, true);
        Preferences.setPreference(DeepSeekTranslate.PROPERTY_MODEL, "deepseek-v4-flash");
        Preferences.setPreference(DeepSeekTranslate.PROPERTY_TEMPERATURE, "0.3");
        Preferences.setPreference(DeepSeekTranslate.PROPERTY_DYNAMIC_TEMPERATURE, false);
        Preferences.setPreference(DeepSeekTranslate.PROPERTY_GLOSSARY_MODE,
                DeepSeekTranslate.GLOSSARY_MODE_REFERENCE);
        Preferences.setPreference(DeepSeekTranslate.PROPERTY_AUTO_GLOSSARY, false);
        Preferences.setPreference(DeepSeekTranslate.PROPERTY_SELF_REVIEW, false);
        Preferences.setPreference(DeepSeekTranslate.PROPERTY_REASONING_EFFORT, 0);

        DeepSeekTranslate translate = new DeepSeekTranslate();
        // Should not throw — when no project is open, glossary is simply skipped
        String json = translate.createJsonRequest(new Language("EN"), new Language("DE"), "Hello world");

        // Prompt should be the standard prompt (no glossary entries injected)
        String expected = "{"
            + "\"messages\":["
            + "{\"content\":\"You are a professional translation engine for OmegaT. "
                + "Translate from en to de. Preserve tags, placeholders, and line breaks. "
                + "Return only the translated text.\",\"role\":\"system\"},"
                + "{\"content\":\"Hello world\",\"role\":\"user\"}],"
                + "\"model\":\"deepseek-v4-flash\","
                + "\"stream\":false,"
                + "\"temperature\":0.3,"
                + "\"reasoning\":{\"effort\":\"none\"}}";

        ObjectMapper mapper = new ObjectMapper();
        assertEquals(mapper.readTree(expected), mapper.readTree(json));
    }

    @Test
    public void glossaryModeStrictDoesNotCrashWithoutProject() throws Exception {
        Preferences.setPreference(DeepSeekTranslate.ALLOW_DEEPSEEK_TRANSLATE, true);
        Preferences.setPreference(DeepSeekTranslate.PROPERTY_MODEL, "deepseek-v4-flash");
        Preferences.setPreference(DeepSeekTranslate.PROPERTY_TEMPERATURE, "0.3");
        Preferences.setPreference(DeepSeekTranslate.PROPERTY_DYNAMIC_TEMPERATURE, false);
        Preferences.setPreference(DeepSeekTranslate.PROPERTY_GLOSSARY_MODE,
                DeepSeekTranslate.GLOSSARY_MODE_STRICT);
        Preferences.setPreference(DeepSeekTranslate.PROPERTY_AUTO_GLOSSARY, false);
        Preferences.setPreference(DeepSeekTranslate.PROPERTY_SELF_REVIEW, false);
        Preferences.setPreference(DeepSeekTranslate.PROPERTY_REASONING_EFFORT, 0);

        DeepSeekTranslate translate = new DeepSeekTranslate();
        // Should not throw — when no project is open, glossary is simply skipped
        String json = translate.createJsonRequest(new Language("EN"), new Language("DE"), "Hello world");

        String expected = "{"
            + "\"messages\":["
            + "{\"content\":\"You are a professional translation engine for OmegaT. "
                + "Translate from en to de. Preserve tags, placeholders, and line breaks. "
                + "Return only the translated text.\",\"role\":\"system\"},"
                + "{\"content\":\"Hello world\",\"role\":\"user\"}],"
                + "\"model\":\"deepseek-v4-flash\","
                + "\"stream\":false,"
                + "\"temperature\":0.3,"
                + "\"reasoning\":{\"effort\":\"none\"}}";

        ObjectMapper mapper = new ObjectMapper();
        assertEquals(mapper.readTree(expected), mapper.readTree(json));
    }

    @Test
    public void contextSegmentsEnabledDoesNotCrashWithoutProject() throws Exception {
        Preferences.setPreference(DeepSeekTranslate.ALLOW_DEEPSEEK_TRANSLATE, true);
        Preferences.setPreference(DeepSeekTranslate.PROPERTY_MODEL, "deepseek-v4-flash");
        Preferences.setPreference(DeepSeekTranslate.PROPERTY_TEMPERATURE, "0.3");
        Preferences.setPreference(DeepSeekTranslate.PROPERTY_DYNAMIC_TEMPERATURE, false);
        Preferences.setPreference(DeepSeekTranslate.PROPERTY_CONTEXT_SEGMENTS, 2);
        Preferences.setPreference(DeepSeekTranslate.PROPERTY_AUTO_GLOSSARY, false);
        Preferences.setPreference(DeepSeekTranslate.PROPERTY_SELF_REVIEW, false);
        Preferences.setPreference(DeepSeekTranslate.PROPERTY_REASONING_EFFORT, 0);

        DeepSeekTranslate translate = new DeepSeekTranslate();
        // Should not throw — when no project is open, context is simply skipped
        String json = translate.createJsonRequest(new Language("EN"), new Language("DE"), "Hello world");

        String expected = "{"
            + "\"messages\":["
            + "{\"content\":\"You are a professional translation engine for OmegaT. "
                + "Translate from en to de. Preserve tags, placeholders, and line breaks. "
                + "Return only the translated text.\",\"role\":\"system\"},"
                + "{\"content\":\"Hello world\",\"role\":\"user\"}],"
                + "\"model\":\"deepseek-v4-flash\","
                + "\"stream\":false,"
                + "\"temperature\":0.3,"
                + "\"reasoning\":{\"effort\":\"none\"}}";

        ObjectMapper mapper = new ObjectMapper();
        assertEquals(mapper.readTree(expected), mapper.readTree(json));
    }

    @Test
    public void chainingEnabledDoesNotCrashWithoutProject() throws Exception {
        Preferences.setPreference(DeepSeekTranslate.ALLOW_DEEPSEEK_TRANSLATE, true);
        Preferences.setPreference(DeepSeekTranslate.PROPERTY_MODEL, "deepseek-v4-flash");
        Preferences.setPreference(DeepSeekTranslate.PROPERTY_TEMPERATURE, "0.3");
        Preferences.setPreference(DeepSeekTranslate.PROPERTY_DYNAMIC_TEMPERATURE, false);
        Preferences.setPreference(DeepSeekTranslate.PROPERTY_CONTEXT_SEGMENTS, 2);
        Preferences.setPreference(DeepSeekTranslate.PROPERTY_CONTEXT_CHAINING, true);
        Preferences.setPreference(DeepSeekTranslate.PROPERTY_CONTEXT_CHAIN_LENGTH, 100);
        Preferences.setPreference(DeepSeekTranslate.PROPERTY_AUTO_GLOSSARY, false);
        Preferences.setPreference(DeepSeekTranslate.PROPERTY_SELF_REVIEW, false);
        Preferences.setPreference(DeepSeekTranslate.PROPERTY_REASONING_EFFORT, 0);

        DeepSeekTranslate translate = new DeepSeekTranslate();
        // Should not throw — when no project is open, chaining is simply skipped
        String json = translate.createJsonRequest(new Language("EN"), new Language("DE"), "Hello world");

        String expected = "{"
            + "\"messages\":["
            + "{\"content\":\"You are a professional translation engine for OmegaT. "
                + "Translate from en to de. Preserve tags, placeholders, and line breaks. "
                + "Return only the translated text.\",\"role\":\"system\"},"
                + "{\"content\":\"Hello world\",\"role\":\"user\"}],"
                + "\"model\":\"deepseek-v4-flash\","
                + "\"stream\":false,"
                + "\"temperature\":0.3,"
                + "\"reasoning\":{\"effort\":\"none\"}}";

        ObjectMapper mapper = new ObjectMapper();
        assertEquals(mapper.readTree(expected), mapper.readTree(json));
    }

    @Test
    public void reasoningMaximumSendsMax() throws Exception {
        Preferences.setPreference(DeepSeekTranslate.ALLOW_DEEPSEEK_TRANSLATE, true);
        Preferences.setPreference(DeepSeekTranslate.PROPERTY_MODEL, "deepseek-v4-flash");
        Preferences.setPreference(DeepSeekTranslate.PROPERTY_TEMPERATURE, "0.3");
        Preferences.setPreference(DeepSeekTranslate.PROPERTY_DYNAMIC_TEMPERATURE, false);
        Preferences.setPreference(DeepSeekTranslate.PROPERTY_AUTO_GLOSSARY, false);
        Preferences.setPreference(DeepSeekTranslate.PROPERTY_SELF_REVIEW, false);
        Preferences.setPreference(DeepSeekTranslate.PROPERTY_REASONING_EFFORT, 6);

        DeepSeekTranslate translate = new DeepSeekTranslate();
        String json = translate.createJsonRequest(new Language("EN"), new Language("DE"), "Hello world");

        ObjectMapper mapper = new ObjectMapper();
        assertEquals("max", mapper.readTree(json).get("reasoning").get("effort").asText());
    }

    @Test
    public void reasoningMediumCollapsesToHigh() throws Exception {
        Preferences.setPreference(DeepSeekTranslate.ALLOW_DEEPSEEK_TRANSLATE, true);
        Preferences.setPreference(DeepSeekTranslate.PROPERTY_MODEL, "deepseek-v4-flash");
        Preferences.setPreference(DeepSeekTranslate.PROPERTY_TEMPERATURE, "0.3");
        Preferences.setPreference(DeepSeekTranslate.PROPERTY_DYNAMIC_TEMPERATURE, false);
        Preferences.setPreference(DeepSeekTranslate.PROPERTY_AUTO_GLOSSARY, false);
        Preferences.setPreference(DeepSeekTranslate.PROPERTY_SELF_REVIEW, false);
        Preferences.setPreference(DeepSeekTranslate.PROPERTY_REASONING_EFFORT, 3);

        DeepSeekTranslate translate = new DeepSeekTranslate();
        String json = translate.createJsonRequest(new Language("EN"), new Language("DE"), "Hello world");

        ObjectMapper mapper = new ObjectMapper();
        assertEquals("high", mapper.readTree(json).get("reasoning").get("effort").asText());
    }

    // --- Auto-insert segment identity guard ----------------------------------
    //
    // The deferred auto-insert (SwingUtilities.invokeLater) must only write
    // when the cursor is still on the segment that was translated. These tests
    // inject a fake IEditor into Core and invoke the real private
    // autoInsertTranslation(String, String), moving the fake cursor between
    // calls — the reproduction of the wrong-segment insertion bug.

    private static final String GUARD_SOURCE = "可我的谎言究竟要从何说起呢……？";
    private static final String GUARD_TRANSLATION = "...where should I even begin to tell my lie...?";

    private static SourceTextEntry fakeEntry(int num, String source) {
        return new SourceTextEntry(new EntryKey("file.txt", source, null, null, null, null),
                num, null, null, Collections.emptyList());
    }

    /** Minimal IEditor stub: only the methods autoInsertTranslation touches are real. */
    private static IEditor fakeEditor(SourceTextEntry current, String currentTranslation,
            AtomicReference<String> replaced) {
        return (IEditor) Proxy.newProxyInstance(DeepSeekTranslateTest.class.getClassLoader(),
                new Class<?>[] { IEditor.class }, (proxy, method, args) -> {
                    switch (method.getName()) {
                    case "getCurrentEntry":
                        return current;
                    case "getCurrentEntryNumber":
                        return current.entryNum();
                    case "getCurrentTranslation":
                        return currentTranslation;
                    case "replaceEditText":
                        replaced.set((String) args[0]);
                        return null;
                    default:
                        if (method.getReturnType() == int.class) {
                            return 0;
                        }
                        if (method.getReturnType() == boolean.class) {
                            return false;
                        }
                        return null;
                    }
                });
    }

    private static void setCoreEditor(IEditor editor) throws Exception {
        Field field = Core.class.getDeclaredField("editor");
        field.setAccessible(true);
        field.set(null, editor);
    }

    private static void invokeAutoInsert(DeepSeekTranslate translate, String expectedSource,
            String translation) throws Exception {
        Method method = DeepSeekTranslate.class.getDeclaredMethod(
                "autoInsertTranslation", String.class, String.class);
        method.setAccessible(true);
        method.invoke(translate, expectedSource, translation);
    }

    @Test
    public void autoInsertWritesWhenCursorStillOnTranslatedSegment() throws Exception {
        Preferences.setPreference(DeepSeekTranslate.PROPERTY_AUTO_ACTIVE, false);
        Preferences.setPreference(DeepSeekTranslate.PROPERTY_AUTO_CONFIRM, false);
        AtomicReference<String> replaced = new AtomicReference<>();
        setCoreEditor(fakeEditor(fakeEntry(4, GUARD_SOURCE), "", replaced));
        try {
            invokeAutoInsert(new DeepSeekTranslate(), GUARD_SOURCE, GUARD_TRANSLATION);
            assertEquals(GUARD_TRANSLATION, replaced.get());
        } finally {
            setCoreEditor(null);
        }
    }

    @Test
    public void autoInsertSkippedWhenCursorMovedBeforeInsertRuns() throws Exception {
        Preferences.setPreference(DeepSeekTranslate.PROPERTY_AUTO_ACTIVE, false);
        Preferences.setPreference(DeepSeekTranslate.PROPERTY_AUTO_CONFIRM, false);
        AtomicReference<String> replaced = new AtomicReference<>();
        // Cursor advanced to the NEXT segment before the deferred insert ran
        setCoreEditor(fakeEditor(fakeEntry(5, "大约是我十四岁那一年……"), "", replaced));
        try {
            invokeAutoInsert(new DeepSeekTranslate(), GUARD_SOURCE, GUARD_TRANSLATION);
            assertNull("translation must not be written into a different segment", replaced.get());
        } finally {
            setCoreEditor(null);
        }
    }

    @Test
    public void autoInsertSkippedWhenTargetAlreadyFilled() throws Exception {
        Preferences.setPreference(DeepSeekTranslate.PROPERTY_AUTO_ACTIVE, false);
        Preferences.setPreference(DeepSeekTranslate.PROPERTY_AUTO_CONFIRM, false);
        AtomicReference<String> replaced = new AtomicReference<>();
        setCoreEditor(fakeEditor(fakeEntry(4, GUARD_SOURCE), "existing translation", replaced));
        try {
            invokeAutoInsert(new DeepSeekTranslate(), GUARD_SOURCE, GUARD_TRANSLATION);
            assertNull("existing translations must not be overwritten", replaced.get());
        } finally {
            setCoreEditor(null);
        }
    }
}
