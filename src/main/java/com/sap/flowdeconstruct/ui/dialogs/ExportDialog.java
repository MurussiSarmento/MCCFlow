package com.sap.flowdeconstruct.ui.dialogs;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;

/**
 * Dialog for exporting flow diagrams to PDF or Markdown
 * Follows the dark theme design from design.md
 */
public class ExportDialog extends JDialog implements KeyListener {
    
    // Design constants
    private static final Color BACKGROUND_COLOR = new Color(0x2d, 0x2d, 0x2d);
    private static final Color PANEL_COLOR = new Color(0x3a, 0x3a, 0x3a);
    private static final Color TEXT_COLOR = new Color(0xcc, 0xcc, 0xcc);
    private static final Color ACCENT_COLOR = new Color(0x5f, 0x9e, 0xa0);
    private static final Font MONO_FONT = new Font(Font.MONOSPACED, Font.PLAIN, 12);
    
    public enum ExportFormat {
        PDF("PDF Document", ".pdf"),
        MARKDOWN("Markdown", ".md");
        
        private final String displayName;
        private final String extension;
        
        ExportFormat(String displayName, String extension) {
            this.displayName = displayName;
            this.extension = extension;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public String getExtension() {
            return extension;
        }
    }
    
    private JTextField filePathField;
    private JComboBox<ExportFormat> formatComboBox;
    private JCheckBox includeNotesCheckBox;
    private JCheckBox includeSubflowsCheckBox;
    private boolean confirmed = false;
    
    public ExportDialog(Frame parent) {
        super(parent, "Export Flow", true);
        
        initializeDialog();
        setupComponents();
        setupKeyboardHandling();
    }
    
    private void initializeDialog() {
        setSize(500, 300);
        setLocationRelativeTo(getParent());
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        
        // Apply dark theme
        getContentPane().setBackground(BACKGROUND_COLOR);
    }
    
    private void setupComponents() {
        setLayout(new BorderLayout());
        
        // Title panel
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        titlePanel.setBackground(BACKGROUND_COLOR);
        titlePanel.setBorder(BorderFactory.createEmptyBorder(12, 16, 8, 16));
        
        JLabel titleLabel = new JLabel("Export Flow Diagram");
        titleLabel.setForeground(TEXT_COLOR);
        titleLabel.setFont(MONO_FONT.deriveFont(Font.BOLD, 14f));
        titlePanel.add(titleLabel);
        
        add(titlePanel, BorderLayout.NORTH);
        
        // Main content panel
        JPanel contentPanel = createContentPanel();
        add(contentPanel, BorderLayout.CENTER);
        
        // Button panel
        JPanel buttonPanel = createButtonPanel();
        add(buttonPanel, BorderLayout.SOUTH);
    }
    
    private JPanel createContentPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(BACKGROUND_COLOR);
        panel.setBorder(BorderFactory.createEmptyBorder(0, 16, 12, 16));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        
        // File path section
        panel.add(createFilePathSection());
        panel.add(Box.createVerticalStrut(16));
        
        // Format section
        panel.add(createFormatSection());
        panel.add(Box.createVerticalStrut(16));
        
        // Options section
        panel.add(createOptionsSection());
        
        return panel;
    }
    
    private JPanel createFilePathSection() {
        JPanel section = new JPanel(new BorderLayout());
        section.setBackground(BACKGROUND_COLOR);
        
        JLabel label = new JLabel("Export to:");
        label.setForeground(TEXT_COLOR);
        label.setFont(MONO_FONT);
        section.add(label, BorderLayout.NORTH);
        
        JPanel pathPanel = new JPanel(new BorderLayout());
        pathPanel.setBackground(BACKGROUND_COLOR);
        pathPanel.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
        
        filePathField = new JTextField();
        filePathField.setBackground(PANEL_COLOR);
        filePathField.setForeground(TEXT_COLOR);
        filePathField.setFont(MONO_FONT);
        filePathField.setCaretColor(TEXT_COLOR);
        filePathField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ACCENT_COLOR.darker(), 1),
            BorderFactory.createEmptyBorder(6, 8, 6, 8)
        ));
        filePathField.setText(System.getProperty("user.home") + File.separator + "flow_diagram.pdf");
        
        JButton browseButton = createStyledButton("Browse...");
        browseButton.addActionListener(e -> browseForFile());
        
        pathPanel.add(filePathField, BorderLayout.CENTER);
        pathPanel.add(Box.createHorizontalStrut(8), BorderLayout.LINE_END);
        pathPanel.add(browseButton, BorderLayout.EAST);
        
        section.add(pathPanel, BorderLayout.CENTER);
        
        return section;
    }
    
    private JPanel createFormatSection() {
        JPanel section = new JPanel(new BorderLayout());
        section.setBackground(BACKGROUND_COLOR);
        
        JLabel label = new JLabel("Format:");
        label.setForeground(TEXT_COLOR);
        label.setFont(MONO_FONT);
        section.add(label, BorderLayout.NORTH);
        
        formatComboBox = new JComboBox<>(ExportFormat.values());
        formatComboBox.setBackground(PANEL_COLOR);
        formatComboBox.setForeground(TEXT_COLOR);
        formatComboBox.setFont(MONO_FONT);
        formatComboBox.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
        formatComboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof ExportFormat) {
                    setText(((ExportFormat) value).getDisplayName());
                }
                setBackground(isSelected ? ACCENT_COLOR.darker() : PANEL_COLOR);
                setForeground(TEXT_COLOR);
                return this;
            }
        });
        
        formatComboBox.addActionListener(e -> updateFileExtension());
        
        section.add(formatComboBox, BorderLayout.CENTER);
        
        return section;
    }
    
    private JPanel createOptionsSection() {
        JPanel section = new JPanel();
        section.setBackground(BACKGROUND_COLOR);
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
        
        JLabel label = new JLabel("Options:");
        label.setForeground(TEXT_COLOR);
        label.setFont(MONO_FONT);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        section.add(label);
        
        section.add(Box.createVerticalStrut(8));
        
        includeNotesCheckBox = createStyledCheckBox("Include node notes");
        includeNotesCheckBox.setSelected(true);
        includeNotesCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        section.add(includeNotesCheckBox);
        
        section.add(Box.createVerticalStrut(4));
        
        includeSubflowsCheckBox = createStyledCheckBox("Include subflows");
        includeSubflowsCheckBox.setSelected(true);
        includeSubflowsCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        section.add(includeSubflowsCheckBox);
        
        return section;
    }
    
    private JCheckBox createStyledCheckBox(String text) {
        JCheckBox checkBox = new JCheckBox(text);
        checkBox.setBackground(BACKGROUND_COLOR);
        checkBox.setForeground(TEXT_COLOR);
        checkBox.setFont(MONO_FONT);
        checkBox.setFocusPainted(false);
        return checkBox;
    }
    
    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panel.setBackground(BACKGROUND_COLOR);
        panel.setBorder(BorderFactory.createEmptyBorder(8, 16, 16, 16));
        
        // Cancel button
        JButton cancelButton = createStyledButton("Cancel (Esc)");
        cancelButton.addActionListener(e -> {
            confirmed = false;
            dispose();
        });
        
        // Export button
        JButton exportButton = createStyledButton("Export (Ctrl+Enter)");
        exportButton.addActionListener(e -> {
            confirmed = true;
            dispose();
        });
        
        panel.add(cancelButton);
        panel.add(Box.createHorizontalStrut(8));
        panel.add(exportButton);
        
        return panel;
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
        
        // Hover effect
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                button.setBackground(ACCENT_COLOR.darker());
            }
            
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                button.setBackground(PANEL_COLOR);
            }
        });
        
        return button;
    }
    
    private void browseForFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        
        ExportFormat selectedFormat = (ExportFormat) formatComboBox.getSelectedItem();
        if (selectedFormat != null) {
            fileChooser.setSelectedFile(new File("flow_diagram" + selectedFormat.getExtension()));
        }
        
        int result = fileChooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            filePathField.setText(selectedFile.getAbsolutePath());
        }
    }
    
    private void updateFileExtension() {
        ExportFormat selectedFormat = (ExportFormat) formatComboBox.getSelectedItem();
        if (selectedFormat != null) {
            String currentPath = filePathField.getText();
            
            // Remove existing extension
            int lastDotIndex = currentPath.lastIndexOf('.');
            if (lastDotIndex > 0) {
                currentPath = currentPath.substring(0, lastDotIndex);
            }
            
            // Add new extension
            filePathField.setText(currentPath + selectedFormat.getExtension());
        }
    }
    
    private void setupKeyboardHandling() {
        addKeyListener(this);
        setFocusable(true);
    }
    
    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            confirmed = false;
            dispose();
        } else if (e.getKeyCode() == KeyEvent.VK_ENTER && e.isControlDown()) {
            confirmed = true;
            dispose();
        }
    }
    
    @Override
    public void keyTyped(KeyEvent e) {
        // Not used
    }
    
    @Override
    public void keyReleased(KeyEvent e) {
        // Not used
    }
    
    // Public methods
    public boolean isConfirmed() {
        return confirmed;
    }
    
    public String getFilePath() {
        return filePathField.getText();
    }
    
    public ExportFormat getSelectedFormat() {
        return (ExportFormat) formatComboBox.getSelectedItem();
    }
    
    public boolean isIncludeNotes() {
        return includeNotesCheckBox.isSelected();
    }
    
    public boolean isIncludeSubflows() {
        return includeSubflowsCheckBox.isSelected();
    }
}