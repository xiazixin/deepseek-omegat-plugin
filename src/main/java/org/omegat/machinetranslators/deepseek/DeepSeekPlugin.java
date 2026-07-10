package org.omegat.machinetranslators.deepseek;

import java.awt.KeyboardFocusManager;
import java.awt.event.KeyEvent;

import javax.swing.SwingUtilities;
import javax.swing.Timer;

import org.omegat.core.Core;
import org.omegat.core.data.SourceTextEntry;
import org.omegat.gui.editor.IEditor;
import org.omegat.util.Log;
import org.omegat.util.Preferences;

public final class DeepSeekPlugin {

    private static java.awt.KeyEventDispatcher hotkeyDispatcher;
    private static Timer indicatorTimer;

    private DeepSeekPlugin() {
    }

    public static void loadPlugins() {
        Core.registerMachineTranslationClass(DeepSeekTranslate.class);
        registerHotkey();
        // Restore indicator if auto-mode was left on from a previous session
        if (DeepSeekTranslate.isAutoActive()) {
            startIndicator();
        }
    }

    public static void unloadPlugins() {
        if (hotkeyDispatcher != null) {
            KeyboardFocusManager.getCurrentKeyboardFocusManager()
                    .removeKeyEventDispatcher(hotkeyDispatcher);
            hotkeyDispatcher = null;
        }
        stopIndicator();
    }

    /**
     * Registers a global hotkey (Ctrl+Shift+M) that toggles the
     * auto-insert / auto-confirm feature on and off.
     * <p>
     * Tracks key-release state to ignore OS auto-repeat events —
     * without this guard, holding the keys causes rapid oscillation
     * that floods the EDT and makes the editor unresponsive.
     */
    private static void registerHotkey() {
        hotkeyDispatcher = new java.awt.KeyEventDispatcher() {
            private boolean mMKeyReleased = true;
            private long lastToggleMs = 0;

            @Override
            public boolean dispatchKeyEvent(KeyEvent e) {
                // Track M-key release so we ignore auto-repeat KEY_PRESSED events
                if (e.getID() == KeyEvent.KEY_RELEASED
                        && e.getKeyCode() == KeyEvent.VK_M) {
                    mMKeyReleased = true;
                    return false;
                }
                if (e.getID() == KeyEvent.KEY_PRESSED
                        && e.isControlDown()
                        && e.isShiftDown()
                        && !e.isAltDown()
                        && e.getKeyCode() == KeyEvent.VK_M) {
                    // Ignore OS auto-repeat: only react on first press after a release
                    if (!mMKeyReleased) {
                        return true; // consume but don't act
                    }
                    // Debounce: minimum 400ms between toggles (belt-and-suspenders)
                    long now = System.currentTimeMillis();
                    if (now - lastToggleMs < 400) {
                        return true;
                    }
                    mMKeyReleased = false;
                    lastToggleMs = now;
                    toggleAutoInsert();
                    return true;
                }
                return false;
            }
        };
        KeyboardFocusManager.getCurrentKeyboardFocusManager()
                .addKeyEventDispatcher(hotkeyDispatcher);
    }

    private static void toggleAutoInsert() {
        boolean current = Preferences.isPreference(DeepSeekTranslate.PROPERTY_AUTO_ACTIVE);
        boolean newState = !current;
        Preferences.setPreference(DeepSeekTranslate.PROPERTY_AUTO_ACTIVE, newState);
        String msg = newState ? "⚡ DeepSeek Auto: ON" : "DeepSeek Auto: OFF";
        Log.log(msg);
        try {
            Core.getMainWindow().showProgressMessage(msg);
            if (newState) {
                Core.getMainWindow().showLengthMessage("⚡ AUTO");
                startIndicator();
                retriggerCurrentSegment();
            } else {
                Core.getMainWindow().showLengthMessage("");
                stopIndicator();
            }
        } catch (Exception ignored) {
            // Status bar may not be available (e.g. console mode)
        }
    }

    /**
     * When auto-mode is toggled ON, the current segment may already have a
     * translation in the MT pane (OmegaT auto-fetches MT on navigation).
     * We check the shared translation cache — if a cached result exists,
     * it is inserted directly.  If auto-confirm is enabled, a throttled
     * advance to the next untranslated segment is also scheduled.
     */
    private static void retriggerCurrentSegment() {
        SwingUtilities.invokeLater(() -> {
            try {
                IEditor editor = Core.getEditor();
                if (editor == null) return;
                SourceTextEntry entry = editor.getCurrentEntry();
                if (entry == null) return;
                String trans = editor.getCurrentTranslation();
                if (trans != null && !trans.trim().isEmpty()) return;

                // Only insert from cache — never call activateEntry()
                String sourceText = entry.getSrcText();
                String cached = DeepSeekTranslate.lastTranslationBySource.get(sourceText);
                if (cached != null && !cached.isEmpty()) {
                    editor.replaceEditText(cached, "DeepSeek");
                    if (DeepSeekTranslate.isAutoConfirmEnabled()) {
                        DeepSeekTranslate.scheduleAdvance(editor, entry);
                    }
                }
            } catch (Exception ignored) { }
        });
    }

    /**
     * Starts a repeating timer that refreshes the "⚡ AUTO" indicator
     * in the status bar, so it persists even when OmegaT overwrites it.
     */
    static void startIndicator() {
        stopIndicator();
        indicatorTimer = new Timer(1500, e -> {
            try {
                if (DeepSeekTranslate.isAutoActive()) {
                    Core.getMainWindow().showLengthMessage("⚡ AUTO");
                }
            } catch (Exception ignored) { }
        });
        indicatorTimer.setRepeats(true);
        indicatorTimer.start();
    }

    /**
     * Stops the repeating indicator timer and clears the display.
     */
    static void stopIndicator() {
        if (indicatorTimer != null) {
            indicatorTimer.stop();
            indicatorTimer = null;
        }
    }
}
