package com.sap.flowdeconstruct.ui.dialogs;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.KeyEvent;

import com.sap.flowdeconstruct.i18n.I18n;

public class TextStyleDialog extends JDialog {
    // Dark theme constants (consistent with other dialogs)
    private static final Color BACKGROUND_COLOR = new Color(0x2d, 0x2d, 0x2d);
    private static final Color PANEL_COLOR = new Color(0x3a, 0x3a, 0x3a);
    private static final Color TEXT_COLOR = new Color(0xcc, 0xcc, 0xcc);
    private static final Color ACCENT_COLOR = new Color(0x5f, 0x9e, 0xa0);
    private static final Font MONO_FONT = new Font(Font.MONOSPACED, Font.PLAIN, 12);

    private boolean confirmed = false;

    private final JPanel colorPreview;
    private Color selectedColor;
    private final JComboBox<String> familyCombo;
    private final JSpinner sizeSpinner;
    private final JCheckBox boldCheck;
    private final JCheckBox italicCheck;

    // Preview components
    private JLabel previewLabel;

    public TextStyleDialog(Frame owner, Color initialColor, String initialFamily, int initialSize, int initialStyle) {
        super(owner, I18n.t("textstyle.title"), true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        getContentPane().setBackground(BACKGROUND_COLOR);

        // Title
        JLabel title = new JLabel(I18n.t("textstyle.header"));
        title.setForeground(TEXT_COLOR);
        title.setFont(MONO_FONT.deriveFont(Font.BOLD, 14f));
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        titlePanel.setBackground(BACKGROUND_COLOR);
        titlePanel.setBorder(BorderFactory.createEmptyBorder(12, 16, 8, 16));
        titlePanel.add(title);

        // Content panel
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(BACKGROUND_COLOR);
        content.setBorder(BorderFactory.createEmptyBorder(0, 16, 12, 16));

        // Color section
        JPanel colorRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        colorRow.setBackground(BACKGROUND_COLOR);
        JLabel colorLabel = new JLabel(I18n.t("textstyle.color"));
        colorLabel.setForeground(TEXT_COLOR);
        colorLabel.setFont(MONO_FONT);
        JButton colorBtn = createStyledButton(I18n.t("textstyle.chooseColor"));
        selectedColor = initialColor != null ? initialColor : new Color(0xcc, 0xcc, 0xcc);
        colorPreview = new JPanel();
        colorPreview.setPreferredSize(new Dimension(28, 18));
        colorPreview.setBackground(selectedColor);
        colorPreview.setBorder(BorderFactory.createLineBorder(ACCENT_COLOR.darker(), 1));
        colorBtn.addActionListener(e -> chooseColor());
        colorRow.add(colorLabel);
        colorRow.add(Box.createHorizontalStrut(8));
        colorRow.add(colorPreview);
        colorRow.add(Box.createHorizontalStrut(8));
        colorRow.add(colorBtn);

        // Font family
        JPanel familyRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        familyRow.setBackground(BACKGROUND_COLOR);
        JLabel familyLabel = new JLabel(I18n.t("textstyle.font"));
        familyLabel.setForeground(TEXT_COLOR);
        familyLabel.setFont(MONO_FONT);
        String[] families = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
        familyCombo = new JComboBox<>(families);
        familyCombo.setBackground(PANEL_COLOR);
        familyCombo.setForeground(TEXT_COLOR);
        familyCombo.setFont(MONO_FONT);
        familyCombo.setPrototypeDisplayValue("XXXXXXXXXXXXXXX");
        if (initialFamily != null) {
            familyCombo.setSelectedItem(initialFamily);
        }
        familyRow.add(familyLabel);
        familyRow.add(Box.createHorizontalStrut(8));
        familyRow.add(familyCombo);

        // Size and style
        JPanel styleRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        styleRow.setBackground(BACKGROUND_COLOR);
        JLabel sizeLabel = new JLabel(I18n.t("textstyle.size"));
        sizeLabel.setForeground(TEXT_COLOR);
        sizeLabel.setFont(MONO_FONT);
        sizeSpinner = new JSpinner(new SpinnerNumberModel(Math.max(6, Math.min(96, initialSize > 0 ? initialSize : 12)), 6, 96, 1));
        sizeSpinner.setFont(MONO_FONT);
        JComponent editor = sizeSpinner.getEditor();
        if (editor instanceof JSpinner.DefaultEditor) {
            ((JSpinner.DefaultEditor) editor).getTextField().setBackground(PANEL_COLOR);
            ((JSpinner.DefaultEditor) editor).getTextField().setForeground(TEXT_COLOR);
            ((JSpinner.DefaultEditor) editor).getTextField().setCaretColor(TEXT_COLOR);
        }
        boldCheck = new JCheckBox(I18n.t("textstyle.bold"));
        boldCheck.setBackground(BACKGROUND_COLOR);
        boldCheck.setForeground(TEXT_COLOR);
        boldCheck.setFont(MONO_FONT);
        italicCheck = new JCheckBox(I18n.t("textstyle.italic"));
        italicCheck.setBackground(BACKGROUND_COLOR);
        italicCheck.setForeground(TEXT_COLOR);
        italicCheck.setFont(MONO_FONT);
        boldCheck.setSelected((initialStyle & Font.BOLD) != 0);
        italicCheck.setSelected((initialStyle & Font.ITALIC) != 0);
        styleRow.add(sizeLabel);
        styleRow.add(Box.createHorizontalStrut(8));
        styleRow.add(sizeSpinner);
        styleRow.add(Box.createHorizontalStrut(16));
        styleRow.add(boldCheck);
        styleRow.add(Box.createHorizontalStrut(8));
        styleRow.add(italicCheck);

        // Preview section
        JPanel previewRow = new JPanel(new BorderLayout());
        previewRow.setBackground(BACKGROUND_COLOR);
        JLabel previewTitle = new JLabel(I18n.t("textstyle.preview"));
        previewTitle.setForeground(TEXT_COLOR);
        previewTitle.setFont(MONO_FONT);
        JPanel previewPanel = new JPanel(new BorderLayout());
        previewPanel.setBackground(PANEL_COLOR);
        previewPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ACCENT_COLOR.darker(), 1),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)
        ));
        previewLabel = new JLabel(I18n.t("textstyle.sample"));
        previewLabel.setForeground(selectedColor);
        previewPanel.add(previewLabel, BorderLayout.CENTER);
        previewRow.add(previewTitle, BorderLayout.NORTH);
        previewRow.add(previewPanel, BorderLayout.CENTER);

        // Buttons
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.setBackground(BACKGROUND_COLOR);
        buttons.setBorder(BorderFactory.createEmptyBorder(8, 16, 16, 16));
        JButton cancel = createStyledButton(I18n.t("dialog.cancel"));
        JButton ok = createStyledButton(I18n.t("dialog.ok"));
        cancel.addActionListener(e -> { confirmed = false; dispose(); });
        ok.addActionListener(e -> { confirmed = true; dispose(); });
        buttons.add(cancel);
        buttons.add(Box.createHorizontalStrut(8));
        buttons.add(ok);

        // Assemble
        content.add(colorRow);
        content.add(Box.createVerticalStrut(8));
        content.add(familyRow);
        content.add(Box.createVerticalStrut(8));
        content.add(styleRow);
        content.add(Box.createVerticalStrut(12));
        content.add(previewRow);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BACKGROUND_COLOR);
        root.add(titlePanel, BorderLayout.NORTH);
        root.add(content, BorderLayout.CENTER);
        root.add(buttons, BorderLayout.SOUTH);
        setContentPane(root);
        pack();
        setSize(560, Math.max(360, getHeight()));
        setLocationRelativeTo(getOwner());

        // ESC and Enter shortcuts
        getRootPane().registerKeyboardAction(e -> { confirmed = false; dispose(); },
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        getRootPane().setDefaultButton(ok);

        // Listeners to update preview live
        familyCombo.addActionListener(e -> updatePreview());
        sizeSpinner.addChangeListener(new ChangeListener() {
            @Override public void stateChanged(ChangeEvent e) { updatePreview(); }
        });
        boldCheck.addActionListener(e -> updatePreview());
        italicCheck.addActionListener(e -> updatePreview());

        // Initial preview
        updatePreview();
    }

    private void chooseColor() {
        Color c = JColorChooser.showDialog(getOwner(), I18n.t("textstyle.color.dialog.title"), selectedColor);
        if (c != null) {
            selectedColor = c;
            colorPreview.setBackground(c);
            colorPreview.repaint();
            updatePreview();
        }
    }

    private void updatePreview() {
        try {
            String fam = (String) familyCombo.getSelectedItem();
            int sz = (int) sizeSpinner.getValue();
            int style = Font.PLAIN;
            if (boldCheck.isSelected()) style |= Font.BOLD;
            if (italicCheck.isSelected()) style |= Font.ITALIC;
            Font f = new Font(fam != null && !fam.trim().isEmpty() ? fam : Font.MONOSPACED, style, Math.max(6, Math.min(96, sz)));
            previewLabel.setFont(f);
            previewLabel.setForeground(selectedColor != null ? selectedColor : TEXT_COLOR);
            previewLabel.revalidate();
            previewLabel.repaint();
        } catch (Exception ignored) {
            // In case of invalid font, fallback silently
            previewLabel.setFont(MONO_FONT);
            previewLabel.setForeground(selectedColor != null ? selectedColor : TEXT_COLOR);
        }
    }

    private JButton createStyledButton(String text) {
        JButton button = new JButton(text);
        button.setBackground(PANEL_COLOR);
        button.setForeground(TEXT_COLOR);
        button.setFont(MONO_FONT);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ACCENT_COLOR.darker(), 1),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)
        ));
        button.setFocusPainted(false);
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) { button.setBackground(ACCENT_COLOR.darker()); }
            @Override public void mouseExited(java.awt.event.MouseEvent e) { button.setBackground(PANEL_COLOR); }
        });
        return button;
    }

    public boolean isConfirmed() { return confirmed; }
    public Color getSelectedColor() { return selectedColor; }
    public String getSelectedFamily() { return (String) familyCombo.getSelectedItem(); }
    public int getSelectedSize() { return (int) sizeSpinner.getValue(); }
    public int getSelectedStyle() {
        int style = Font.PLAIN;
        if (boldCheck.isSelected()) style |= Font.BOLD;
        if (italicCheck.isSelected()) style |= Font.ITALIC;
        return style;
    }
}