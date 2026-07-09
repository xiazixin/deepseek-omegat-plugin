package org.omegat.machinetranslators.deepseek;

import java.awt.KeyboardFocusManager;
import java.awt.event.KeyEvent;

import javax.swing.SwingUtilities;
import javax.swing.Timer;

import org.omegat.core.Core;
import org.omegat.core.CoreEvents;
import org.omegat.core.data.SourceTextEntry;
import org.omegat.core.events.IEntryEventListener;
import org.omegat.gui.editor.IEditor;
import org.omegat.util.Log;
import org.omegat.util.Preferences;

public final class DeepSeekPlugin {

    private static java.awt.KeyEventDispatcher hotkeyDispatcher;
    private static Timer indicatorTimer;
    private static IEntryEventListener entryListener;

    private DeepSeekPlugin() {
    }

    public static void loadPlugins() {
        Core.registerMachineTranslationClass(DeepSeekTranslate.class);
        registerHotkey();
        registerEntryListener();
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
        if (entryListener != null) {
            CoreEvents.unregisterEntryEventListener(entryListener);
            entryListener = null;
        }
    }

    /**
     * Registers a listener that stops auto-mode when the user manually
     * clicks or navigates to a different segment. Uses entry-number
     * matching: when auto-navigation happens, the expected entry number
     * is recorded; any activation to a different entry is manual.
     */
    private static void registerEntryListener() {
        entryListener = new IEntryEventListener() {
            @Override
            public void onNewFile(String activeFileName) {
                // Only stop if this file change was NOT triggered by auto-navigation
                if (DeepSeekTranslate.isAutoActive()
                        && !DeepSeekTranslate.expectingAutoActivation) {
                    stopAutoMode("file changed");
                }
            }

            @Override
            public void onEntryActivated(SourceTextEntry newEntry) {
                if (!DeepSeekTranslate.isAutoActive()) return;

                if (DeepSeekTranslate.expectingAutoActivation) {
                    // This activation was triggered by auto-navigation —
                    // record the entry number and clear the flag
                    DeepSeekTranslate.lastAutoEntryNum = newEntry.entryNum();
                    DeepSeekTranslate.expectingAutoActivation = false;
                } else if (newEntry.entryNum() != DeepSeekTranslate.lastAutoEntryNum) {
                    // Activation to a different entry than the last auto-navigated one
                    // → user clicked manually
                    stopAutoMode("manual navigation");
                }
                // else: same entry number — OmegaT re-activating the auto-navigated
                // entry (e.g., after translate() completes) — ignore
            }
        };
        CoreEvents.registerEntryEventListener(entryListener);
    }

    /**
     * Stops auto-mode and shows a notification in the status bar.
     */
    private static void stopAutoMode(String reason) {
        Preferences.setPreference(DeepSeekTranslate.PROPERTY_AUTO_ACTIVE, false);
        stopIndicator();
        String msg = "DeepSeek Auto: OFF (" + reason + ")";
        Log.log(msg);
        try {
            Core.getMainWindow().showProgressMessage(msg);
            Core.getMainWindow().showLengthMessage("");
        } catch (Exception ignored) { }
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
     * translation loaded in the MT pane. Re-activate the entry so OmegaT
     * re-fetches the translation, which then gets auto-inserted.
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
                DeepSeekTranslate.lastAutoEntryNum = entry.entryNum();
                DeepSeekTranslate.expectingAutoActivation = false;
                editor.activateEntry();
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
