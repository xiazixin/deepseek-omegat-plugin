package org.omegat.machinetranslators.deepseek;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

import org.omegat.util.Log;
import org.omegat.util.Preferences;
import org.omegat.util.StaticUtils;

/**
 * Dedicated log for raw DeepSeek responses.
 * <p>
 * When enabled from the DeepSeek top menu, every raw response body received
 * from the DeepSeek API is appended verbatim to {@code deepseek_raw.log} in
 * the OmegaT configuration folder. The file contains ONLY raw DeepSeek
 * responses — no timestamps, prompts, or other noise.
 */
final class RawResponseLogger {

    static final String PROPERTY_RAW_LOG = "deepseek.api.raw_log";
    private static final String LOG_FILE_NAME = "deepseek_raw.log";

    private RawResponseLogger() {
    }

    static boolean isEnabled() {
        return Preferences.isPreference(PROPERTY_RAW_LOG);
    }

    static void setEnabled(boolean enabled) {
        Preferences.setPreference(PROPERTY_RAW_LOG, enabled);
    }

    /**
     * Appends the raw response to the log file, but only when raw logging is
     * enabled in the DeepSeek menu. Failures are logged to the OmegaT log and
     * never interrupt translation.
     */
    static void log(String rawResponse) {
        if (!isEnabled() || rawResponse == null || rawResponse.isEmpty()) {
            return;
        }
        try {
            appendRaw(getLogFile(), rawResponse);
        } catch (Exception e) {
            Log.log(e);
        }
    }

    /**
     * Appends exactly the raw value plus a trailing newline (to separate
     * entries) — nothing else is written.
     */
    static synchronized void appendRaw(File file, String rawResponse) throws Exception {
        try (PrintWriter writer = new PrintWriter(
                new OutputStreamWriter(new FileOutputStream(file, true), StandardCharsets.UTF_8))) {
            writer.print(rawResponse);
            writer.print('\n');
        }
    }

    static File getLogFile() {
        String configDir = StaticUtils.getConfigDir();
        if (configDir == null || configDir.isEmpty()) {
            configDir = System.getProperty("user.home") + File.separator;
        }
        return new File(configDir, LOG_FILE_NAME);
    }

    /**
     * Empties the log file (the file itself is kept).
     */
    static synchronized void clear() {
        try {
            new FileOutputStream(getLogFile()).close();
        } catch (Exception e) {
            Log.log(e);
        }
    }
}
