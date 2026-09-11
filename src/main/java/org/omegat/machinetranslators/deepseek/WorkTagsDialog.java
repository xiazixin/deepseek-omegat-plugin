package org.omegat.machinetranslators.deepseek;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.GridLayout;
import java.awt.Window;
import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ResourceBundle;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.omegat.core.Core;
import org.omegat.util.Log;

/**
 * Popup for editing the work tags (source author/title/tags, generic labels,
 * and free-form tags). "Create / update tags" writes them to
 * {@code deepseek_tags.txt} in the project folder.
 */
final class WorkTagsDialog {

    private static final String BUNDLE_BASENAME = "org.omegat.machinetranslators.deepseek.Bundle";
    private static final ResourceBundle BUNDLE = ResourceBundle.getBundle(BUNDLE_BASENAME);

    private WorkTagsDialog() {
    }

    static void showDialog(Window parent) {
        File tagsFile = WorkTags.getTagsFile();
        if (tagsFile == null) {
            JOptionPane.showMessageDialog(parent,
                    BUNDLE.getString("MT_ENGINE_DEEPSEEK_TAGS_NO_PROJECT"),
                    BUNDLE.getString("MT_ENGINE_DEEPSEEK_TAGS_DIALOG_TITLE"),
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        Map<String, String> values = WorkTags.load(tagsFile);
        JTextField authorField = new JTextField(values.getOrDefault("author", ""), 24);
        JTextField titleField = new JTextField(values.getOrDefault("title", ""), 24);
        JTextField tagsField = new JTextField(values.getOrDefault("tags", ""), 24);
        JTextField labelsField = new JTextField(values.getOrDefault("labels", ""), 24);
        JTextField customField = new JTextField(values.getOrDefault("custom", ""), 24);

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        form.add(new JLabel(BUNDLE.getString("MT_ENGINE_DEEPSEEK_TAGS_AUTHOR")));
        form.add(authorField);
        form.add(new JLabel(BUNDLE.getString("MT_ENGINE_DEEPSEEK_TAGS_TITLE")));
        form.add(titleField);
        form.add(new JLabel(BUNDLE.getString("MT_ENGINE_DEEPSEEK_TAGS_TAGS")));
        form.add(tagsField);
        form.add(new JLabel(BUNDLE.getString("MT_ENGINE_DEEPSEEK_TAGS_LABELS")));
        form.add(labelsField);
        form.add(new JLabel(BUNDLE.getString("MT_ENGINE_DEEPSEEK_TAGS_CUSTOM")));
        form.add(customField);

        JDialog dialog = new JDialog(parent,
                BUNDLE.getString("MT_ENGINE_DEEPSEEK_TAGS_DIALOG_TITLE"),
                Dialog.ModalityType.APPLICATION_MODAL);

        JButton saveButton = new JButton(BUNDLE.getString("MT_ENGINE_DEEPSEEK_TAGS_SAVE"));
        saveButton.addActionListener(e -> {
            Map<String, String> toSave = new LinkedHashMap<>();
            toSave.put("author", authorField.getText().trim());
            toSave.put("title", titleField.getText().trim());
            toSave.put("tags", tagsField.getText().trim());
            toSave.put("labels", labelsField.getText().trim());
            toSave.put("custom", customField.getText().trim());
            try {
                WorkTags.save(tagsFile, toSave);
                dialog.dispose();
                showStatus(BUNDLE.getString("MT_ENGINE_DEEPSEEK_TAGS_SAVED"));
            } catch (Exception ex) {
                Log.log(ex);
            }
        });

        JButton cancelButton = new JButton(BUNDLE.getString("MT_ENGINE_DEEPSEEK_TAGS_CANCEL"));
        cancelButton.addActionListener(e -> dialog.dispose());

        JPanel buttons = new JPanel();
        buttons.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        buttons.add(saveButton);
        buttons.add(cancelButton);

        dialog.getContentPane().add(form, BorderLayout.CENTER);
        dialog.getContentPane().add(buttons, BorderLayout.SOUTH);
        dialog.pack();
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
    }

    private static void showStatus(String message) {
        try {
            Core.getMainWindow().showProgressMessage(message);
        } catch (Exception ignored) {
            // Status bar may not be available (e.g. console mode)
        }
    }
}
