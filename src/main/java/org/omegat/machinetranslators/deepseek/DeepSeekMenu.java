package org.omegat.machinetranslators.deepseek;

import java.awt.Desktop;
import java.io.File;
import java.util.ResourceBundle;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;

import org.omegat.core.Core;
import org.omegat.util.Log;

/**
 * Top-level "DeepSeek" menu in the OmegaT menu bar — home for additional
 * plugin functions that don't belong in the MT engine configuration dialog.
 * Currently holds the raw-response log controls; future functions can be
 * added here.
 */
final class DeepSeekMenu {

    private static final String BUNDLE_BASENAME = "org.omegat.machinetranslators.deepseek.Bundle";
    private static final ResourceBundle BUNDLE = ResourceBundle.getBundle(BUNDLE_BASENAME);

    private static JMenu registeredMenu;

    private DeepSeekMenu() {
    }

    /**
     * Adds the DeepSeek menu to the main window's menu bar (on the EDT).
     * Failures are logged and never prevent the plugin from loading.
     */
    static void register() {
        SwingUtilities.invokeLater(() -> {
            try {
                JMenuBar menuBar = Core.getMainWindow().getApplicationFrame().getJMenuBar();
                if (menuBar == null) {
                    return;
                }

                JMenu menu = new JMenu(BUNDLE.getString("MT_ENGINE_DEEPSEEK_MENU_LABEL"));

                JCheckBoxMenuItem rawLogToggle = new JCheckBoxMenuItem(
                        BUNDLE.getString("MT_ENGINE_DEEPSEEK_MENU_RAW_LOG_TOGGLE"));
                rawLogToggle.setSelected(RawResponseLogger.isEnabled());
                rawLogToggle.setToolTipText(
                        BUNDLE.getString("MT_ENGINE_DEEPSEEK_MENU_RAW_LOG_TOGGLE_TOOLTIP"));
                rawLogToggle.addActionListener(e -> {
                    boolean enabled = rawLogToggle.isSelected();
                    RawResponseLogger.setEnabled(enabled);
                    showStatus(enabled ? "DeepSeek raw log: ON" : "DeepSeek raw log: OFF");
                });
                menu.add(rawLogToggle);

                menu.addSeparator();

                JMenuItem openItem = new JMenuItem(
                        BUNDLE.getString("MT_ENGINE_DEEPSEEK_MENU_RAW_LOG_OPEN"));
                openItem.addActionListener(e -> openLogFile());
                menu.add(openItem);

                JMenuItem clearItem = new JMenuItem(
                        BUNDLE.getString("MT_ENGINE_DEEPSEEK_MENU_RAW_LOG_CLEAR"));
                clearItem.addActionListener(e -> {
                    RawResponseLogger.clear();
                    showStatus("DeepSeek raw log cleared");
                });
                menu.add(clearItem);

                menuBar.add(menu);
                menuBar.revalidate();
                registeredMenu = menu;
            } catch (Exception e) {
                Log.log(e);
            }
        });
    }

    /**
     * Removes the DeepSeek menu from the main window's menu bar (on the EDT).
     */
    static void unregister() {
        SwingUtilities.invokeLater(() -> {
            try {
                if (registeredMenu == null) {
                    return;
                }
                JMenuBar menuBar = Core.getMainWindow().getApplicationFrame().getJMenuBar();
                if (menuBar != null) {
                    menuBar.remove(registeredMenu);
                    menuBar.revalidate();
                    menuBar.repaint();
                }
                registeredMenu = null;
            } catch (Exception e) {
                Log.log(e);
            }
        });
    }

    private static void openLogFile() {
        try {
            File logFile = RawResponseLogger.getLogFile();
            if (!logFile.isFile()) {
                showStatus(BUNDLE.getString("MT_ENGINE_DEEPSEEK_RAW_LOG_NOT_FOUND"));
                return;
            }
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(logFile);
            } else {
                showStatus(logFile.getAbsolutePath());
            }
        } catch (Exception e) {
            Log.log(e);
        }
    }

    private static void showStatus(String message) {
        try {
            Core.getMainWindow().showProgressMessage(message);
        } catch (Exception ignored) {
            // Status bar may not be available (e.g. console mode)
        }
    }
}
