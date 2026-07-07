package org.omegat.machinetranslators.deepseek;

import java.awt.KeyboardFocusManager;
import java.awt.Window;
import java.awt.event.KeyEvent;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
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
    private static JMenu deepSeekMenu;
    private static JCheckBoxMenuItem autoInsertMenuItem;
    private static JCheckBoxMenuItem autoConfirmMenuItem;
    private static JCheckBoxMenuItem autoGlossaryMenuItem;
    private static JCheckBoxMenuItem selfReviewMenuItem;

    private DeepSeekPlugin() {
    }

    public static void loadPlugins() {
        Core.registerMachineTranslationClass(DeepSeekTranslate.class);
        registerDeepSeekMenu();
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
        // Remove the DeepSeek Function menu from the menu bar
        SwingUtilities.invokeLater(() -> {
            try {
                JMenuBar menuBar = (JMenuBar) Core.getMainWindow().getMainMenu();
                if (menuBar != null && deepSeekMenu != null) {
                    menuBar.remove(deepSeekMenu);
                    menuBar.revalidate();
                    menuBar.repaint();
                    deepSeekMenu = null;
                }
            } catch (Exception ignored) { }
        });
    }

    /**
     * Registers a "DeepSeek Function" dropdown menu in OmegaT's menu bar.
     * Provides quick access to all plugin features without opening settings.
     */
    private static void registerDeepSeekMenu() {
        SwingUtilities.invokeLater(() -> {
            try {
                JMenuBar menuBar = (JMenuBar) Core.getMainWindow().getMainMenu();
                if (menuBar == null) return;

                // Avoid duplicate registration
                for (int i = 0; i < menuBar.getMenuCount(); i++) {
                    JMenu existing = menuBar.getMenu(i);
                    if (existing != null && "DeepSeek Function".equals(existing.getText())) {
                        deepSeekMenu = existing;
                        return;
                    }
                }

                deepSeekMenu = new JMenu("DeepSeek Function");

                // ── ⚡ Toggle Auto Mode ──
                JMenuItem toggleAutoItem = new JMenuItem("⚡ Toggle Auto Mode");
                toggleAutoItem.setToolTipText("Ctrl+Shift+M — Toggle auto-insert mode on/off");
                toggleAutoItem.addActionListener(e -> toggleAutoInsert());
                deepSeekMenu.add(toggleAutoItem);

                deepSeekMenu.addSeparator();

                // ── Auto-Insert (checkbox) ──
                autoInsertMenuItem = new JCheckBoxMenuItem("Auto-Insert");
                autoInsertMenuItem.setSelected(DeepSeekTranslate.isAutoInsert());
                autoInsertMenuItem.setToolTipText("Automatically fill target segment with translation");
                autoInsertMenuItem.addActionListener(e -> {
                    Preferences.setPreference(DeepSeekTranslate.PROPERTY_AUTO_INSERT,
                            autoInsertMenuItem.isSelected());
                    // Auto-confirm depends on auto-insert
                    if (!autoInsertMenuItem.isSelected()) {
                        autoConfirmMenuItem.setSelected(false);
                        autoConfirmMenuItem.setEnabled(false);
                        Preferences.setPreference(DeepSeekTranslate.PROPERTY_AUTO_CONFIRM, false);
                    } else {
                        autoConfirmMenuItem.setEnabled(true);
                    }
                });
                deepSeekMenu.add(autoInsertMenuItem);

                // ── Auto-Confirm (checkbox) ──
                autoConfirmMenuItem = new JCheckBoxMenuItem("Auto-Confirm");
                autoConfirmMenuItem.setSelected(DeepSeekTranslate.isAutoConfirm());
                autoConfirmMenuItem.setEnabled(DeepSeekTranslate.isAutoInsert());
                autoConfirmMenuItem.setToolTipText("Also commit translation and advance to next segment");
                autoConfirmMenuItem.addActionListener(e -> {
                    Preferences.setPreference(DeepSeekTranslate.PROPERTY_AUTO_CONFIRM,
                            autoConfirmMenuItem.isSelected());
                });
                deepSeekMenu.add(autoConfirmMenuItem);

                deepSeekMenu.addSeparator();

                // ── Auto-Glossary (checkbox) ──
                autoGlossaryMenuItem = new JCheckBoxMenuItem("Auto-Glossary");
                autoGlossaryMenuItem.setSelected(DeepSeekTranslate.isAutoGlossary());
                autoGlossaryMenuItem.setToolTipText("AI suggests terminology pairs during translation");
                autoGlossaryMenuItem.addActionListener(e -> {
                    Preferences.setPreference(DeepSeekTranslate.PROPERTY_AUTO_GLOSSARY,
                            autoGlossaryMenuItem.isSelected());
                });
                deepSeekMenu.add(autoGlossaryMenuItem);

                // ── Self-Review (checkbox) ──
                selfReviewMenuItem = new JCheckBoxMenuItem("Self-Review Agent");
                selfReviewMenuItem.setSelected(DeepSeekTranslate.isSelfReview());
                selfReviewMenuItem.setToolTipText("Second API pass checks quality and corrects errors");
                selfReviewMenuItem.addActionListener(e -> {
                    Preferences.setPreference(DeepSeekTranslate.PROPERTY_SELF_REVIEW,
                            selfReviewMenuItem.isSelected());
                });
                deepSeekMenu.add(selfReviewMenuItem);

                deepSeekMenu.addSeparator();

                // ── Configure DeepSeek... ──
                JMenuItem configureItem = new JMenuItem("Configure DeepSeek...");
                configureItem.addActionListener(e -> {
                    try {
                        Window parent = javax.swing.SwingUtilities.getWindowAncestor(menuBar);
                        if (parent != null) {
                            new DeepSeekTranslate().showConfigurationUI(parent);
                            // Refresh menu checkboxes after settings dialog closes
                            refreshMenuState();
                        }
                    } catch (Exception ex) {
                        Log.log(ex);
                    }
                });
                deepSeekMenu.add(configureItem);

                // ── About ──
                JMenuItem aboutItem = new JMenuItem("About DeepSeek Plugin");
                aboutItem.addActionListener(e -> {
                    javax.swing.JOptionPane.showMessageDialog(
                            menuBar.getParent(),
                            "DeepSeek OmegaT Plugin v1.5.1\n"
                            + "Adds DeepSeek AI as a machine translation engine.\n\n"
                            + "Features: Auto-mode, Glossary, Context Segments,\n"
                            + "Auto-Glossary, Self-Review Agent",
                            "About DeepSeek Plugin",
                            javax.swing.JOptionPane.INFORMATION_MESSAGE);
                });
                deepSeekMenu.add(aboutItem);

                // Insert before "Help" menu if it exists, otherwise at the end
                int helpIndex = -1;
                for (int i = 0; i < menuBar.getMenuCount(); i++) {
                    JMenu m = menuBar.getMenu(i);
                    if (m != null && "Help".equals(m.getText())) {
                        helpIndex = i;
                        break;
                    }
                }
                if (helpIndex >= 0) {
                    menuBar.add(deepSeekMenu, helpIndex);
                } else {
                    menuBar.add(deepSeekMenu);
                }
                menuBar.revalidate();
                menuBar.repaint();
            } catch (Exception ignored) {
                // Menu bar may not be available (e.g. console mode / headless)
            }
        });
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
     */
    private static void registerHotkey() {
        hotkeyDispatcher = new java.awt.KeyEventDispatcher() {
            @Override
            public boolean dispatchKeyEvent(KeyEvent e) {
                if (e.getID() == KeyEvent.KEY_PRESSED
                        && e.isControlDown()
                        && e.isShiftDown()
                        && !e.isAltDown()
                        && e.getKeyCode() == KeyEvent.VK_M) {
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
                // Trigger translation for the current segment if already loaded
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
                // Re-activate to trigger MT re-fetch; the entry listener
                // will see this as the same lastAutoEntryNum and ignore it
                DeepSeekTranslate.lastAutoEntryNum = entry.entryNum();
                DeepSeekTranslate.expectingAutoActivation = false;
                editor.activateEntry();
            } catch (Exception ignored) { }
        });
    }

    /**
     * Updates the DeepSeek Function menu checkbox states to reflect
     * current preference values (called after settings dialog closes).
     */
    static void refreshMenuState() {
        SwingUtilities.invokeLater(() -> {
            if (autoInsertMenuItem != null) {
                autoInsertMenuItem.setSelected(DeepSeekTranslate.isAutoInsert());
            }
            if (autoConfirmMenuItem != null) {
                boolean autoInsert = DeepSeekTranslate.isAutoInsert();
                autoConfirmMenuItem.setSelected(autoInsert && DeepSeekTranslate.isAutoConfirm());
                autoConfirmMenuItem.setEnabled(autoInsert);
            }
            if (autoGlossaryMenuItem != null) {
                autoGlossaryMenuItem.setSelected(DeepSeekTranslate.isAutoGlossary());
            }
            if (selfReviewMenuItem != null) {
                selfReviewMenuItem.setSelected(DeepSeekTranslate.isSelfReview());
            }
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
