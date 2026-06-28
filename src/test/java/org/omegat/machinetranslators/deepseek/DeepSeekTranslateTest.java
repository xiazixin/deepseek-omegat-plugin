package org.omegat.machinetranslators.deepseek;

import static org.junit.Assert.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.BeforeClass;
import org.junit.Test;

import org.omegat.core.machinetranslators.MachineTranslateError;
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

        DeepSeekTranslate translate = new DeepSeekTranslate();
        String json = translate.createJsonRequest(new Language("EN"), new Language("DE"), "Hello world");

        String expected = "{"
            + "\"messages\":["
            + "{\"content\":\"You are a professional translation engine for OmegaT. Translate from en to de. Preserve tags, placeholders, and line breaks. Return only the translated text.\",\"role\":\"system\"},"
                + "{\"content\":\"Hello world\",\"role\":\"user\"}],"
                + "\"model\":\"deepseek-v4-flash\","
                + "\"stream\":false,"
                + "\"temperature\":0.3}";

        ObjectMapper mapper = new ObjectMapper();
        assertEquals(mapper.readTree(expected), mapper.readTree(json));
    }

    @Test
    public void createJsonRequestDynamicTemperature() throws Exception {
        Preferences.setPreference(DeepSeekTranslate.ALLOW_DEEPSEEK_TRANSLATE, true);
        Preferences.setPreference(DeepSeekTranslate.PROPERTY_MODEL, "deepseek-v4-flash");
        Preferences.setPreference(DeepSeekTranslate.PROPERTY_TEMPERATURE, "0.3");
        Preferences.setPreference(DeepSeekTranslate.PROPERTY_DYNAMIC_TEMPERATURE, true);

        DeepSeekTranslate translate = new DeepSeekTranslate();
        String json = translate.createJsonRequest(new Language("EN"), new Language("DE"), "Hello world");

        String expected = "{"
            + "\"messages\":["
            + "{\"content\":\"You are a professional translation engine for OmegaT. Translate from en to de. Preserve tags, placeholders, and line breaks. Return only the translated text.\",\"role\":\"system\"},"
                + "{\"content\":\"Hello world\",\"role\":\"user\"}],"
                + "\"model\":\"deepseek-v4-flash\","
                + "\"stream\":false}";

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
                + "\"temperature\":0.3}";

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
                + "\"temperature\":0.3}";

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
                + "\"temperature\":0.3}";

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
                + "\"temperature\":0.3}";

        ObjectMapper mapper = new ObjectMapper();
        assertEquals(mapper.readTree(expected), mapper.readTree(json));
    }
}
