package org.omegat.machinetranslators.deepseek;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.Window;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.TreeMap;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.omegat.core.Core;
import org.omegat.core.data.SourceTextEntry;
import org.omegat.core.data.TMXEntry;
import org.omegat.core.machinetranslators.BaseCachedTranslate;
import org.omegat.core.machinetranslators.BaseTranslate;
import org.omegat.core.machinetranslators.MachineTranslateError;
import org.omegat.gui.exttrans.MTConfigDialog;
import org.omegat.util.HttpConnectionUtils;
import org.omegat.util.Language;
import org.omegat.util.Log;
import org.omegat.util.Preferences;

@SuppressWarnings("unused")
public class DeepSeekTranslate extends BaseCachedTranslate {

    public static final String ALLOW_DEEPSEEK_TRANSLATE = "allow_deepseek_translate";
    public static final String PROPERTY_API_KEY = "deepseek.api.key";
    public static final String PROPERTY_MODEL = "deepseek.api.model";
    public static final String PROPERTY_URL = "deepseek.api.url";
    public static final String PROPERTY_TEMPERATURE = "deepseek.api.temperature";
    public static final String PROPERTY_DYNAMIC_TEMPERATURE = "deepseek.api.dynamic_temperature";
    public static final String PROPERTY_GLOSSARY_MODE = "deepseek.api.glossary.mode";

    /** Glossary disabled */
    public static final int GLOSSARY_MODE_NONE = 0;
    /** Glossary as reference — AI uses judgment, does not blindly follow */
    public static final int GLOSSARY_MODE_REFERENCE = 1;
    /** Glossary as strict rules — AI must use the exact glossary translation */
    public static final int GLOSSARY_MODE_STRICT = 2;

    private static final int GLOSSARY_MODE_DEFAULT = GLOSSARY_MODE_NONE;
    private static final int GLOSSARY_MAX_ENTRIES = 20;

    /** Number of surrounding segments (above/below) to send as context to the AI */
    public static final String PROPERTY_CONTEXT_SEGMENTS = "deepseek.api.context_segments";
    private static final int CONTEXT_SEGMENTS_DEFAULT = 0;
    private static final int CONTEXT_SEGMENTS_MAX = 3;

    /** Max characters per context segment before truncation */
    public static final String PROPERTY_CONTEXT_TRUNCATION = "deepseek.api.context_truncation";
    private static final int CONTEXT_TRUNCATION_DEFAULT = 400;
    private static final int[] CONTEXT_TRUNCATION_OPTIONS = { 200, 400, 600, 800, 1000, 0 };

    private static final String MODEL_DEEPSEEK_V4_PRO = "deepseek-v4-pro";
    private static final String MODEL_DEEPSEEK_V4_FLASH = "deepseek-v4-flash";
    private static final String[] AVAILABLE_MODELS = { MODEL_DEEPSEEK_V4_PRO, MODEL_DEEPSEEK_V4_FLASH };
    private static final String DEFAULT_MODEL = MODEL_DEEPSEEK_V4_FLASH;
    private static final String DEFAULT_URL = "https://api.deepseek.com";
    private static final double DEFAULT_TEMPERATURE = 0.3;
    private static final int TEMPERATURE_MIN = 0;
    private static final int TEMPERATURE_MAX = 20;
    private static final int TEMPERATURE_DEFAULT_SLIDER = 3;
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
    protected String translate(Language sLang, Language tLang, String text) throws Exception {
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
        translated = cleanSpacesAroundTags(translated, text);

        // Cache this translation so future segments can reference it for continuity
        if (translated != null && !translated.isEmpty()) {
            translationCache.put(text, translated);
        }

        return translated;
    }

    @Override
    public void showConfigurationUI(Window parent) {
        // Replace the default text field with a combo box for model selection
        JComboBox<String> modelComboBox = new JComboBox<>(AVAILABLE_MODELS);
        modelComboBox.setSelectedItem(getModel());

        boolean dynamicTemp = isDynamicTemperature();

        // Glossary mode combo box (created before dialog for onConfirm capture)
        int glossaryMode = getGlossaryMode();
        JComboBox<String> glossaryComboBox = new JComboBox<>(new String[] {
            BUNDLE.getString("MT_ENGINE_DEEPSEEK_GLOSSARY_MODE_NONE"),
            BUNDLE.getString("MT_ENGINE_DEEPSEEK_GLOSSARY_MODE_REFERENCE"),
            BUNDLE.getString("MT_ENGINE_DEEPSEEK_GLOSSARY_MODE_STRICT")
        });
        glossaryComboBox.setSelectedIndex(glossaryMode);

        // Context segments combo box (0-3 surrounding segments)
        int contextSegments = getContextSegments();
        String[] contextOptions = new String[CONTEXT_SEGMENTS_MAX + 1];
        for (int i = 0; i <= CONTEXT_SEGMENTS_MAX; i++) {
            contextOptions[i] = String.valueOf(i);
        }
        JComboBox<String> contextComboBox = new JComboBox<>(contextOptions);
        contextComboBox.setSelectedIndex(Math.min(contextSegments, CONTEXT_SEGMENTS_MAX));

        // Context truncation combo box (character limit per context segment)
        int truncation = getContextTruncation();
        String[] truncationOptions = new String[CONTEXT_TRUNCATION_OPTIONS.length];
        for (int i = 0; i < CONTEXT_TRUNCATION_OPTIONS.length - 1; i++) {
            truncationOptions[i] = String.valueOf(CONTEXT_TRUNCATION_OPTIONS[i]);
        }
        truncationOptions[CONTEXT_TRUNCATION_OPTIONS.length - 1] =
            BUNDLE.getString("MT_ENGINE_DEEPSEEK_CONTEXT_TRUNCATION_NOLIMIT");
        JComboBox<String> truncationComboBox = new JComboBox<>(truncationOptions);
        truncationComboBox.setSelectedIndex(truncationToIndex(truncation));

        // Slider sub-panel (label + slider)
        JPanel sliderPanel = new JPanel(new BorderLayout(5, 0));
        JLabel tempLabel = new JLabel(BUNDLE.getString("MT_ENGINE_DEEPSEEK_TEMPERATURE_LABEL"));

        // Temperature slider: 0-20 represents 0.0-2.0 in 0.1 steps
        JSlider temperatureSlider = new JSlider(TEMPERATURE_MIN, TEMPERATURE_MAX,
                temperatureToSlider(getTemperature()));
        temperatureSlider.setMajorTickSpacing(5);
        temperatureSlider.setMinorTickSpacing(1);
        temperatureSlider.setPaintTicks(true);
        temperatureSlider.setPaintLabels(true);
        temperatureSlider.setSnapToTicks(true);
        @SuppressWarnings("UseOfObsoleteCollectionType")
        Dictionary<Integer, JLabel> tempLabels = new Hashtable<>();
        tempLabels.put(0, new JLabel("0.0"));
        tempLabels.put(5, new JLabel("0.5"));
        tempLabels.put(10, new JLabel("1.0"));
        tempLabels.put(15, new JLabel("1.5"));
        tempLabels.put(20, new JLabel("2.0"));
        temperatureSlider.setLabelTable(tempLabels);

        sliderPanel.add(tempLabel, BorderLayout.WEST);
        sliderPanel.add(temperatureSlider, BorderLayout.CENTER);
        tempLabel.setEnabled(!dynamicTemp);
        temperatureSlider.setEnabled(!dynamicTemp);

        // Dynamic temperature checkbox
        JCheckBox dynamicCheckBox = new JCheckBox(
                BUNDLE.getString("MT_ENGINE_DEEPSEEK_DYNAMIC_TEMPERATURE_LABEL"));
        dynamicCheckBox.setSelected(dynamicTemp);
        dynamicCheckBox.addActionListener(e -> {
            boolean enabled = !dynamicCheckBox.isSelected();
            tempLabel.setEnabled(enabled);
            temperatureSlider.setEnabled(enabled);
        });

        MTConfigDialog dialog = new MTConfigDialog(parent, getName()) {
            @Override
            protected void onConfirm() {
                boolean temporary = panel.temporaryCheckBox.isSelected();
                String apiKey = panel.valueField1.getText().trim();
                String model = modelComboBox.getSelectedItem().toString();
                double temperature = sliderToTemperature(temperatureSlider.getValue());
                boolean dynamic = dynamicCheckBox.isSelected();
                int glossaryModeIdx = glossaryComboBox.getSelectedIndex();
                int contextSegmentsVal = contextComboBox.getSelectedIndex();
                int truncationIdx = truncationComboBox.getSelectedIndex();
                int truncationVal = CONTEXT_TRUNCATION_OPTIONS[truncationIdx];

                setCredential(PROPERTY_API_KEY, apiKey, temporary);
                Preferences.setPreference(PROPERTY_MODEL, model);
                Preferences.setPreference(PROPERTY_TEMPERATURE, String.valueOf(temperature));
                Preferences.setPreference(PROPERTY_DYNAMIC_TEMPERATURE, dynamic);
                Preferences.setPreference(PROPERTY_GLOSSARY_MODE, glossaryModeIdx);
                Preferences.setPreference(PROPERTY_CONTEXT_SEGMENTS, contextSegmentsVal);
                Preferences.setPreference(PROPERTY_CONTEXT_TRUNCATION, truncationVal);
                clearCache();
            }
        };

        dialog.panel.valueLabel1.setText(BUNDLE.getString("MT_ENGINE_DEEPSEEK_API_KEY_LABEL"));
        dialog.panel.valueField1.setText(getApiKey());
        dialog.panel.temporaryCheckBox.setSelected(isCredentialStoredTemporarily(PROPERTY_API_KEY));

        dialog.panel.valueLabel2.setText(BUNDLE.getString("MT_ENGINE_DEEPSEEK_MODEL_LABEL"));

        // valueField2 lives inside credentialsPanel which uses GridBagLayout
        java.awt.Container credentialsPanel = dialog.panel.valueField2.getParent();
        GridBagConstraints gbc = ((java.awt.GridBagLayout) credentialsPanel.getLayout())
                .getConstraints(dialog.panel.valueField2);
        credentialsPanel.remove(dialog.panel.valueField2);
        credentialsPanel.add(modelComboBox, gbc);
        credentialsPanel.revalidate();
        credentialsPanel.repaint();

        // Add temperature panel below credentials
        JPanel temperaturePanel = new JPanel(new BorderLayout(5, 0));
        temperaturePanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        temperaturePanel.add(dynamicCheckBox, BorderLayout.NORTH);
        temperaturePanel.add(sliderPanel, BorderLayout.CENTER);
        dialog.panel.itemsPanel.add(temperaturePanel);

        // Glossary mode panel
        JPanel glossaryPanel = new JPanel(new BorderLayout(5, 0));
        glossaryPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        JLabel glossaryLabel = new JLabel(BUNDLE.getString("MT_ENGINE_DEEPSEEK_GLOSSARY_MODE_LABEL"));
        glossaryPanel.add(glossaryLabel, BorderLayout.NORTH);
        glossaryPanel.add(glossaryComboBox, BorderLayout.CENTER);
        dialog.panel.itemsPanel.add(glossaryPanel);

        // Context segments panel
        JPanel contextPanel = new JPanel(new BorderLayout(5, 0));
        contextPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        JLabel contextLabel = new JLabel(BUNDLE.getString("MT_ENGINE_DEEPSEEK_CONTEXT_LABEL"));
        contextPanel.add(contextLabel, BorderLayout.NORTH);
        contextPanel.add(contextComboBox, BorderLayout.CENTER);
        dialog.panel.itemsPanel.add(contextPanel);

        // Context truncation panel
        JPanel truncationPanel = new JPanel(new BorderLayout(5, 0));
        truncationPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        JLabel truncationLabel = new JLabel(BUNDLE.getString("MT_ENGINE_DEEPSEEK_CONTEXT_TRUNCATION_LABEL"));
        truncationPanel.add(truncationLabel, BorderLayout.NORTH);
        truncationPanel.add(truncationComboBox, BorderLayout.CENTER);
        dialog.panel.itemsPanel.add(truncationPanel);

        dialog.show();
    }

    protected String createJsonRequest(Language sLang, Language tLang, String text) throws JsonProcessingException {
        Map<String, Object> request = new TreeMap<>();
        request.put("messages", createMessages(sLang, tLang, text));
        request.put("model", getModel());
        request.put("stream", false);
        if (!isDynamicTemperature()) {
            request.put("temperature", getTemperature());
        }

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

    private double getTemperature() {
        String temp = Preferences.getPreferenceDefault(PROPERTY_TEMPERATURE,
                String.valueOf(DEFAULT_TEMPERATURE));
        try {
            return Double.parseDouble(temp);
        } catch (NumberFormatException e) {
            Log.log(e);
            return DEFAULT_TEMPERATURE;
        }
    }

    private boolean isDynamicTemperature() {
        return Preferences.isPreference(PROPERTY_DYNAMIC_TEMPERATURE);
    }

    private int getGlossaryMode() {
        return Preferences.getPreferenceDefault(PROPERTY_GLOSSARY_MODE, GLOSSARY_MODE_DEFAULT);
    }

    private int getContextSegments() {
        return Preferences.getPreferenceDefault(PROPERTY_CONTEXT_SEGMENTS, CONTEXT_SEGMENTS_DEFAULT);
    }

    private int getContextTruncation() {
        return Preferences.getPreferenceDefault(PROPERTY_CONTEXT_TRUNCATION, CONTEXT_TRUNCATION_DEFAULT);
    }

    private static int truncationToIndex(int value) {
        for (int i = 0; i < CONTEXT_TRUNCATION_OPTIONS.length; i++) {
            if (CONTEXT_TRUNCATION_OPTIONS[i] == value) return i;
        }
        // Default to 400
        for (int i = 0; i < CONTEXT_TRUNCATION_OPTIONS.length; i++) {
            if (CONTEXT_TRUNCATION_OPTIONS[i] == CONTEXT_TRUNCATION_DEFAULT) return i;
        }
        return 1;
    }

    /** Tracks sequential translation position for efficient context lookups */
    private int contextLastPosition = -1;
    private String contextLastProjectPath = null;
    private List<String> contextCachedSources = null;

    /** Caches this plugin's own translation output as a fallback for context continuity. */
    private final Map<String, String> translationCache = new LinkedHashMap<String, String>(128, 0.75f, true) {
        private static final int MAX_ENTRIES = 512;
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
            return size() > MAX_ENTRIES;
        }
    };

    /** Cached map of source text → stored translation from OmegaT's project data.
     *  This reflects the user's actual (possibly edited) translations, rebuilt
     *  when the project cache is refreshed. */
    private Map<String, String> contextStoredTranslations = null;

    private static int temperatureToSlider(double temperature) {
        return (int) Math.round(temperature * 10.0);
    }

    private static double sliderToTemperature(int sliderValue) {
        return sliderValue / 10.0;
    }

    private List<Map<String, String>> createMessages(Language sLang, Language tLang, String text) {
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(message("system", buildSystemPrompt(sLang, tLang, text)));
        messages.add(message("user", text));
        return messages;
    }

    private static Map<String, String> message(String role, String content) {
        Map<String, String> message = new TreeMap<>();
        message.put("role", role);
        message.put("content", content);
        return message;
    }

    private String buildSystemPrompt(Language sLang, Language tLang, String text) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are a professional translation engine for OmegaT. Translate from ")
            .append(describeLanguage(sLang)).append(" to ").append(describeLanguage(tLang))
            .append(". Preserve tags, placeholders, and line breaks. Return only the translated text.");

        // Context segments (surrounding text for continuity)
        int contextCount = getContextSegments();
        if (contextCount > 0) {
            int pos = findSourcePosition(text);
            if (pos >= 0) {
                String ctx = getContextText(pos, contextCount);
                if (!ctx.isEmpty()) {
                    prompt.append(ctx);
                }
            }
        }

        int glossaryMode = getGlossaryMode();
        if (glossaryMode != GLOSSARY_MODE_NONE) {
            List<GlossaryEntry> matching = findMatchingEntries(text);
            if (!matching.isEmpty()) {
                prompt.append(formatGlossaryPrompt(matching, glossaryMode));
            }
        }

        return prompt.toString();
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

    // -------------------------------------------------------------------------
    // Glossary support
    // -------------------------------------------------------------------------

    /**
     * Immutable glossary entry: source term → target term with optional comment.
     */
    private static class GlossaryEntry {
        final String source;
        final String target;
        final String comment;

        GlossaryEntry(String source, String target, String comment) {
            this.source = source;
            this.target = target;
            this.comment = comment != null ? comment : "";
        }
    }

    /**
     * Reads all glossary entries from the OmegaT project's glossary folder.
     * Returns an empty list when no project is open or no glossary files exist.
     */
    private static List<GlossaryEntry> readGlossaryEntries() {
        List<GlossaryEntry> entries = new ArrayList<>();
        try {
            if (Core.getProject() == null) return entries;
            String glossaryRoot = Core.getProject().getProjectProperties().getGlossaryRoot();
            if (glossaryRoot == null || glossaryRoot.isEmpty()) return entries;

            File glossaryDir = new File(glossaryRoot);
            if (!glossaryDir.isDirectory()) return entries;

            File[] files = glossaryDir.listFiles((dir, name) ->
                name.endsWith(".txt") || name.endsWith(".csv")
                    || name.endsWith(".tab") || name.endsWith(".utf8"));
            if (files == null) return entries;

            for (File file : files) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (line.isEmpty() || line.startsWith("#")) continue;
                        String[] parts = line.split("\t", 3);
                        if (parts.length >= 2) {
                            String source = parts[0].trim();
                            String target = parts[1].trim();
                            String comment = parts.length >= 3 ? parts[2].trim() : "";
                            if (!source.isEmpty() && !target.isEmpty()) {
                                entries.add(new GlossaryEntry(source, target, comment));
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.log(e);
        }
        return entries;
    }

    /**
     * Finds glossary entries whose source term appears in the given text.
     * Entries are sorted by source length descending (more specific first)
     * and limited to {@link #GLOSSARY_MAX_ENTRIES}.
     */
    private static List<GlossaryEntry> findMatchingEntries(String text) {
        List<GlossaryEntry> all = readGlossaryEntries();
        List<GlossaryEntry> matching = new ArrayList<>();
        String lowerText = text.toLowerCase();
        for (GlossaryEntry entry : all) {
            if (lowerText.contains(entry.source.toLowerCase())) {
                matching.add(entry);
            }
        }
        // Sort by source length descending — longer (more specific) matches first
        matching.sort((a, b) -> Integer.compare(b.source.length(), a.source.length()));
        if (matching.size() > GLOSSARY_MAX_ENTRIES) {
            matching = matching.subList(0, GLOSSARY_MAX_ENTRIES);
        }
        return matching;
    }

    /**
     * Formats matching glossary entries into a bullet list for the system prompt.
     */
    private static String formatGlossaryPrompt(List<GlossaryEntry> entries, int mode) {
        if (entries.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        if (mode == GLOSSARY_MODE_STRICT) {
            sb.append("\n\nStrict glossary — you MUST use these exact translations:\n");
        } else {
            sb.append("\n\nReference glossary — use judgment. Do NOT apply entries blindly "
                + "(e.g., if glossary has \"金色 → gold color\" and the text contains "
                + "\"白金色\", still translate \"白金色\" as \"platinum color\"):\n");
        }

        for (GlossaryEntry e : entries) {
            sb.append("- ").append(e.source).append(" → ").append(e.target);
            if (!e.comment.isEmpty()) {
                sb.append("  [").append(e.comment).append("]");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    // -------------------------------------------------------------------------
    // Context segments support (surrounding segment text for better translations)
    // -------------------------------------------------------------------------

    /**
     * Builds a cached list of all source texts from the current project,
     * in file order. Returns empty list when no project is open.
     */
    private List<String> getCachedAllSources() {
        try {
            if (Core.getProject() == null) return new ArrayList<>();
            String projectPath = Core.getProject().getProjectProperties().getProjectRoot();
            // Invalidate cache when project changes
            if (!projectPath.equals(contextLastProjectPath) || contextCachedSources == null) {
                contextCachedSources = new ArrayList<>();
                contextStoredTranslations = new TreeMap<>();
                List<SourceTextEntry> entries = Core.getProject().getAllEntries();
                if (entries != null) {
                    for (SourceTextEntry entry : entries) {
                        String src = entry.getSrcText();
                        if (src != null) {
                            contextCachedSources.add(src);
                            // Read the user's actual (possibly edited) translation from OmegaT
                            try {
                                TMXEntry tmx = Core.getProject().getTranslationInfo(entry);
                                if (tmx != null && tmx.isTranslated() && tmx.translation != null) {
                                    contextStoredTranslations.put(src, tmx.translation);
                                }
                            } catch (Exception ignored) {
                                // Translation lookup is best-effort; skip on API mismatch
                            }
                        }
                    }
                }
                contextLastProjectPath = projectPath;
                contextLastPosition = -1;
                translationCache.clear();
            }
        } catch (Exception e) {
            Log.log(e);
            return new ArrayList<>();
        }
        return contextCachedSources;
    }

    /**
     * Finds the position of {@code sourceText} in the project's ordered entry list.
     * Uses sequential tracking for efficiency — searches forward from the last
     * found position (since OmegaT typically translates segments in order).
     */
    private int findSourcePosition(String sourceText) {
        List<String> sources = getCachedAllSources();
        if (sources.isEmpty()) return -1;

        // Search forward from last position + 1 (sequential translation)
        int start = Math.max(0, contextLastPosition + 1);
        for (int i = start; i < sources.size(); i++) {
            if (sourceText.equals(sources.get(i))) {
                contextLastPosition = i;
                return i;
            }
        }
        // Fallback: search from beginning (for out-of-order or first-segment access)
        for (int i = 0; i < start; i++) {
            if (sourceText.equals(sources.get(i))) {
                contextLastPosition = i;
                return i;
            }
        }
        return -1; // not found (e.g., external text, untranslatable segment)
    }

    /**
     * Returns context segment texts surrounding the given position.
     * @param position the position of the current segment
     * @param count   number of segments to fetch in each direction
     * @return a formatted string of surrounding segments (empty if none)
     */
    private String getContextText(int position, int count) {
        if (count <= 0 || position < 0) return "";
        List<String> sources = getCachedAllSources();
        int truncLen = getContextTruncation();

        StringBuilder sb = new StringBuilder();
        sb.append("\n\nSurrounding context for reference (DO NOT translate these — only the current segment):");

        boolean hasAbove = false;
        int aboveStart = Math.max(0, position - count);
        for (int i = aboveStart; i < position; i++) {
            String src = sources.get(i).replace('\n', ' ').replace('\r', ' ');
            if (truncLen > 0 && src.length() > truncLen) {
                src = src.substring(0, truncLen) + "...";
            }
            if (!hasAbove) {
                sb.append("\n[Above] ");
                hasAbove = true;
            }
            sb.append(src);
            // Show the user's actual stored translation (from OmegaT project data) first,
            // falling back to this plugin's own cached output if no stored translation exists.
            String storedTrg = contextStoredTranslations != null
                ? contextStoredTranslations.get(sources.get(i)) : null;
            if (storedTrg == null) {
                storedTrg = translationCache.get(sources.get(i));
            }
            if (storedTrg != null) {
                String trgSnippet = storedTrg.replace('\n', ' ').replace('\r', ' ');
                if (truncLen > 0 && trgSnippet.length() > truncLen) {
                    trgSnippet = trgSnippet.substring(0, truncLen) + "...";
                }
                sb.append("  →  ").append(trgSnippet);
            }
            sb.append(" /// ");
        }
        if (hasAbove) {
            sb.setLength(sb.length() - 5); // trim trailing " /// "
        }

        boolean hasBelow = false;
        int belowEnd = Math.min(sources.size(), position + count + 1);
        for (int i = position + 1; i < belowEnd; i++) {
            String t = sources.get(i).replace('\n', ' ').replace('\r', ' ');
            if (truncLen > 0 && t.length() > truncLen) {
                t = t.substring(0, truncLen) + "...";
            }
            if (!hasBelow) {
                sb.append("\n[Below] ");
                hasBelow = true;
            }
            sb.append(t).append(" /// ");
        }
        if (hasBelow) {
            sb.setLength(sb.length() - 5);
        }

        return (hasAbove || hasBelow) ? sb.toString() : "";
    }
}
