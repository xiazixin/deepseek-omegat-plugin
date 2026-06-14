package org.omegat.machinetranslators.deepseek;

import static org.junit.Assert.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import org.omegat.core.machinetranslators.MachineTranslateError;
import org.omegat.util.Language;
import org.omegat.util.Preferences;

public class DeepSeekTranslateTest {

    @Test
    public void createJsonRequest() throws Exception {
        Preferences.setPreference(DeepSeekTranslate.ALLOW_DEEPSEEK_TRANSLATE, true);
        Preferences.setPreference(DeepSeekTranslate.PROPERTY_MODEL, "deepseek-v4-flash");

        DeepSeekTranslate translate = new DeepSeekTranslate();
        String json = translate.createJsonRequest(new Language("EN"), new Language("DE"), "Hello world");

        String expected = "{"
                + "\"messages\":["
                + "{\"content\":\"You are a professional translation engine for OmegaT. Translate from EN to DE. Preserve tags, placeholders, and line breaks. Return only the translated text.\",\"role\":\"system\"},"
                + "{\"content\":\"Hello world\",\"role\":\"user\"}],"
                + "\"model\":\"deepseek-v4-flash\","
                + "\"stream\":false,"
                + "\"temperature\":0.0}";

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
}
