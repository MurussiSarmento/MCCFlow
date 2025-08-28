package com.sap.flowdeconstruct.ui.dialogs;

import com.sap.flowdeconstruct.i18n.I18n;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Locale;

/**
 * Settings dialog for configuring application preferences including language selection
 */
public class SettingsDialog extends JDialog {
    
    // Design constants from design.md
    private static final Color BACKGROUND_COLOR = new Color(0x2d, 0x2d, 0x2d);
    private static final Color TEXT_COLOR = new Color(0xcc, 0xcc, 0xcc);
    private static final Font MONO_FONT = new Font(Font.MONOSPACED, Font.PLAIN, 12);
    
    private JComboBox<LanguageItem> languageComboBox;
    private JButton okButton;
    private JButton cancelButton;
    
    // Language options
    private static class LanguageItem {
        final Locale locale;
        final String displayName;
        
        LanguageItem(Locale locale, String displayName) {
            this.locale = locale;
            this.displayName = displayName;
        }
        
        @Override
        public String toString() {
            return displayName;
        }
    }
    
    public SettingsDialog(Frame parent) {
        super(parent, true); // Modal dialog
        
        initializeDialog();
        createComponents();
        layoutComponents();
        setupEventHandlers();
        loadCurrentSettings();
        
        pack();
        setLocationRelativeTo(parent);
    }
    
    private void initializeDialog() {
        setTitle(I18n.t("settings.title"));
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);
        
        // Apply dark theme
        getContentPane().setBackground(BACKGROUND_COLOR);
    }
    
    private void createComponents() {
        // Language selection components
        JLabel languageLabel = new JLabel(I18n.t("settings.language") + ":");
        languageLabel.setForeground(TEXT_COLOR);
        languageLabel.setFont(MONO_FONT);
        
        languageComboBox = new JComboBox<>();
        languageComboBox.setFont(MONO_FONT);
        languageComboBox.setBackground(new Color(0x3a, 0x3a, 0x3a));
        languageComboBox.setForeground(TEXT_COLOR);
        
        // Add language options
        languageComboBox.addItem(new LanguageItem(Locale.ENGLISH, I18n.t("settings.lang.en")));
        languageComboBox.addItem(new LanguageItem(new Locale("pt", "BR"), I18n.t("settings.lang.pt_br")));
        languageComboBox.addItem(new LanguageItem(new Locale("es", "ES"), I18n.t("settings.lang.es")));
        
        // Buttons
        okButton = createButton(I18n.t("dialog.ok"));
        cancelButton = createButton(I18n.t("dialog.cancel"));
        
        // Set preferred size for combo box
        languageComboBox.setPreferredSize(new Dimension(200, 25));
    }
    
    private JButton createButton(String text) {
        JButton button = new JButton(text);
        button.setFont(MONO_FONT);
        button.setBackground(new Color(0x4a, 0x4a, 0x4a));
        button.setForeground(TEXT_COLOR);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setPreferredSize(new Dimension(80, 30));
        
        // Hover effect
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(new Color(0x5a, 0x5a, 0x5a));
            }
            
            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(new Color(0x4a, 0x4a, 0x4a));
            }
        });
        
        return button;
    }
    
    private void layoutComponents() {
        setLayout(new BorderLayout(10, 10));
        
        // Main panel with padding
        JPanel mainPanel = new JPanel();
        mainPanel.setBackground(BACKGROUND_COLOR);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 10, 20));
        mainPanel.setLayout(new GridBagLayout());
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        // Language selection
        JLabel languageLabel = new JLabel(I18n.t("settings.language") + ":");
        languageLabel.setForeground(TEXT_COLOR);
        languageLabel.setFont(MONO_FONT);
        
        gbc.gridx = 0;
        gbc.gridy = 0;
        mainPanel.add(languageLabel, gbc);
        
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        mainPanel.add(languageComboBox, gbc);
        
        add(mainPanel, BorderLayout.CENTER);
        
        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        buttonPanel.setBackground(BACKGROUND_COLOR);
        buttonPanel.add(cancelButton);
        buttonPanel.add(okButton);
        
        add(buttonPanel, BorderLayout.SOUTH);
    }
    
    private void setupEventHandlers() {
        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                applySettings();
                dispose();
            }
        });
        
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
        
        // Close on Escape
        KeyStroke escapeKeyStroke = KeyStroke.getKeyStroke("ESCAPE");
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(escapeKeyStroke, "ESCAPE");
        getRootPane().getActionMap().put("ESCAPE", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
        
        // Default button
        getRootPane().setDefaultButton(okButton);
    }
    
    private void loadCurrentSettings() {
        Locale currentLocale = I18n.getLocale();
        
        // Find and select the current language in the combo box
        for (int i = 0; i < languageComboBox.getItemCount(); i++) {
            LanguageItem item = languageComboBox.getItemAt(i);
            if (item.locale.getLanguage().equals(currentLocale.getLanguage()) &&
                item.locale.getCountry().equals(currentLocale.getCountry())) {
                languageComboBox.setSelectedIndex(i);
                break;
            } else if (item.locale.getLanguage().equals(currentLocale.getLanguage()) &&
                       currentLocale.getCountry().isEmpty()) {
                languageComboBox.setSelectedIndex(i);
                break;
            }
        }
    }
    
    private void applySettings() {
        // Apply language setting
        LanguageItem selectedLanguage = (LanguageItem) languageComboBox.getSelectedItem();
        if (selectedLanguage != null) {
            I18n.setLocale(selectedLanguage.locale);
        }
    }
}