package org.omegat.machinetranslators.deepseek;

import java.awt.Window;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.TreeMap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import org.omegat.core.machinetranslators.BaseCachedTranslate;
import org.omegat.core.machinetranslators.BaseTranslate;
import org.omegat.core.machinetranslators.MachineTranslateError;
import org.omegat.gui.exttrans.MTConfigDialog;
import org.omegat.util.HttpConnectionUtils;
import org.omegat.util.Language;
import org.omegat.util.Log;
import org.omegat.util.Preferences;

@NullMarked
@SuppressWarnings("unused")
public class DeepSeekTranslate extends BaseCachedTranslate {

    public static final String ALLOW_DEEPSEEK_TRANSLATE = "allow_deepseek_translate";
    public static final String PROPERTY_API_KEY = "deepseek.api.key";
    public static final String PROPERTY_MODEL = "deepseek.api.model";
    public static final String PROPERTY_URL = "deepseek.api.url";

    private static final String DEFAULT_MODEL = "deepseek-v4-flash";
    private static final String DEFAULT_URL = "https://api.deepseek.com";
    private static final String CHAT_COMPLETIONS_PATH = "/chat/completions";
    private static final String BUNDLE_BASENAME = "org.omegat.machinetranslators.deepseek.Bundle";
    private static final ResourceBundle BUNDLE = ResourceBundle.getBundle(BUNDLE_BASENAME);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public DeepSeekTranslate() {
        super();
    }

    @Override
    protected String getPreferenceName() {
        return ALLOW_DEEPSEEK_TRANSLATE;
    }

    @Override
    public String getName() {
        return BUNDLE.getString("MT_ENGINE_DEEPSEEK");
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    @Override
    protected @Nullable String translate(Language sLang, Language tLang, String text) throws Exception {
        String apiKey = getApiKey();
        if (apiKey.isEmpty()) {
            throw new MachineTranslateError(BUNDLE.getString("MT_ENGINE_DEEPSEEK_API_KEY_NOTFOUND"));
        }

        String request = createJsonRequest(sLang, tLang, text);
        Map<String, String> headers = new TreeMap<>();
        headers.put("Authorization", "Bearer " + apiKey);

        String response;
        try {
            response = HttpConnectionUtils.postJSON(getBaseUrl() + CHAT_COMPLETIONS_PATH, request, headers);
        } catch (HttpConnectionUtils.ResponseError e) {
            throw new MachineTranslateError(extractErrorMessage(e.body));
        }

        if (response == null) {
            return null;
        }

        String translated = extractTranslation(response);
        translated = BaseTranslate.unescapeHTML(translated);
        return cleanSpacesAroundTags(translated, text);
    }

    @Override
    public void showConfigurationUI(Window parent) {
        MTConfigDialog dialog = new MTConfigDialog(parent, getName()) {
            @Override
            protected void onConfirm() {
                boolean temporary = panel.temporaryCheckBox.isSelected();
                String apiKey = panel.valueField1.getText().trim();
                String model = panel.valueField2.getText().trim();

                setCredential(PROPERTY_API_KEY, apiKey, temporary);
                Preferences.setPreference(PROPERTY_MODEL, model);
                clearCache();
            }
        };

        dialog.panel.valueLabel1.setText(BUNDLE.getString("MT_ENGINE_DEEPSEEK_API_KEY_LABEL"));
        dialog.panel.valueField1.setText(getApiKey());
        dialog.panel.temporaryCheckBox.setSelected(isCredentialStoredTemporarily(PROPERTY_API_KEY));

        dialog.panel.valueLabel2.setText(BUNDLE.getString("MT_ENGINE_DEEPSEEK_MODEL_LABEL"));
        dialog.panel.valueField2.setText(getModel());

        dialog.show();
    }

    protected String createJsonRequest(Language sLang, Language tLang, String text) throws JsonProcessingException {
        Map<String, Object> request = new TreeMap<>();
        request.put("messages", createMessages(sLang, tLang, text));
        request.put("model", getModel());
        request.put("stream", false);
        request.put("temperature", 0.0d);

        return MAPPER.writeValueAsString(request);
    }

    protected String extractTranslation(String json) throws MachineTranslateError {
        try {
            JsonNode root = MAPPER.readTree(json);

            JsonNode error = root.get("error");
            if (error != null && !error.isNull()) {
                String message = error.path("message").asText("");
                if (message.isEmpty()) {
                    message = error.toString();
                }
                throw new MachineTranslateError(message);
            }

            JsonNode choices = root.get("choices");
            if (choices != null && choices.isArray() && !choices.isEmpty()) {
                JsonNode firstChoice = choices.get(0);
                JsonNode messageNode = firstChoice.get("message");
                if (messageNode != null) {
                    String content = messageNode.path("content").asText("");
                    if (!content.isEmpty()) {
                        return content;
                    }
                }

                String text = firstChoice.path("text").asText("");
                if (!text.isEmpty()) {
                    return text;
                }
            }
        } catch (MachineTranslateError e) {
            throw e;
        } catch (Exception e) {
            Log.logErrorRB(e, "MT_JSON_ERROR");
            throw new MachineTranslateError(BUNDLE.getString("MT_JSON_ERROR"));
        }

        throw new MachineTranslateError(BUNDLE.getString("MT_ENGINE_DEEPSEEK_BAD_RESPONSE"));
    }

    private String getApiKey() {
        return System.getProperty(PROPERTY_API_KEY, getCredential(PROPERTY_API_KEY));
    }

    private String getModel() {
        String model = System.getProperty(PROPERTY_MODEL, Preferences.getPreferenceDefault(PROPERTY_MODEL, DEFAULT_MODEL));
        return model.isEmpty() ? DEFAULT_MODEL : model;
    }

    private String getBaseUrl() {
        String baseUrl = System.getProperty(PROPERTY_URL, Preferences.getPreferenceDefault(PROPERTY_URL, DEFAULT_URL));
        return baseUrl.isEmpty() ? DEFAULT_URL : baseUrl;
    }

    private static List<Map<String, String>> createMessages(Language sLang, Language tLang, String text) {
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(message("system", buildSystemPrompt(sLang, tLang)));
        messages.add(message("user", text));
        return messages;
    }

    private static Map<String, String> message(String role, String content) {
        Map<String, String> message = new TreeMap<>();
        message.put("role", role);
        message.put("content", content);
        return message;
    }

    private static String buildSystemPrompt(Language sLang, Language tLang) {
        return "You are a professional translation engine for OmegaT. Translate from "
                + describeLanguage(sLang) + " to " + describeLanguage(tLang)
                + ". Preserve tags, placeholders, and line breaks. Return only the translated text.";
    }

    private static String describeLanguage(Language language) {
        String locale = language.getLocaleCode();
        if (locale != null && !locale.isEmpty()) {
            return locale.replace('_', '-');
        }
        return language.getLanguageCode();
    }

    private static String extractErrorMessage(String json) {
        try {
            JsonNode root = MAPPER.readTree(json);
            JsonNode error = root.get("error");
            if (error != null) {
                String message = error.path("message").asText("");
                if (!message.isEmpty()) {
                    return message;
                }
            }
        } catch (Exception e) {
            Log.log(e);
        }
        return BUNDLE.getString("MT_ENGINE_DEEPSEEK_BAD_RESPONSE");
    }
}
