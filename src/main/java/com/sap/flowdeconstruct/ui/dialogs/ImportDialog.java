package com.sap.flowdeconstruct.ui.dialogs;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import com.sap.flowdeconstruct.model.FlowDiagram;
import com.sap.flowdeconstruct.importer.MarkdownImporter;
import com.sap.flowdeconstruct.ui.components.FlowCanvas;
import java.io.File;
import java.io.IOException;

public class ImportDialog extends JDialog implements KeyListener {

    private static final Color BACKGROUND_COLOR = new Color(0x2d, 0x2d, 0x2d);
    private static final Color PANEL_COLOR = new Color(0x3a, 0x3a, 0x3a);
    private static final Color TEXT_COLOR = new Color(0xcc, 0xcc, 0xcc);
    private static final Color ACCENT_COLOR = new Color(0x5f, 0x9e, 0xa0);
    private static final Font MONO_FONT = new Font(Font.MONOSPACED, Font.PLAIN, 12);

    private JTextField filePathField;
    private FlowCanvas previewCanvas;
    private boolean confirmed = false;

    public ImportDialog(Frame parent) {
        super(parent, "Import Flow from Markdown", true);
        initializeDialog();
        setupComponents();
        setupKeyboardHandling();
    }

    private void initializeDialog() {
        setSize(500, 400);
        setLocationRelativeTo(getOwner());
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        getContentPane().setBackground(BACKGROUND_COLOR);
    }

    private void setupComponents() {
        setLayout(new BorderLayout());

        // Title panel
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        titlePanel.setBackground(BACKGROUND_COLOR);
        titlePanel.setBorder(BorderFactory.createEmptyBorder(12, 16, 8, 16));

        JLabel titleLabel = new JLabel("Import Flow Diagram");
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

        // Preview section
        panel.add(createPreviewSection());

        return panel;
    }

    private JPanel createFilePathSection() {
        JPanel section = new JPanel(new BorderLayout());
        section.setBackground(BACKGROUND_COLOR);
    
        JLabel label = new JLabel("Import from:");
        label.setForeground(TEXT_COLOR);
        label.setFont(MONO_FONT);
        section.add(label, BorderLayout.NORTH);
    
        JPanel pathPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pathPanel.setBackground(BACKGROUND_COLOR);
        pathPanel.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
    
        filePathField = new JTextField();
        filePathField.setPreferredSize(new Dimension(300, 30)); // Adjust width as needed
        filePathField.setBackground(PANEL_COLOR);
        filePathField.setForeground(TEXT_COLOR);
        filePathField.setFont(MONO_FONT);
        filePathField.setCaretColor(TEXT_COLOR);
        filePathField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ACCENT_COLOR.darker(), 1),
            BorderFactory.createEmptyBorder(6, 8, 6, 8)
        ));
        filePathField.setText("");
    
        JButton browseButton = createStyledButton("Browse...");
        browseButton.addActionListener(e -> browseForFile());
    
        pathPanel.add(filePathField);
        pathPanel.add(Box.createHorizontalStrut(8));
        pathPanel.add(browseButton);
    
        section.add(pathPanel, BorderLayout.CENTER);
        return section;
    }

    private JPanel createPreviewSection() {
        JPanel section = new JPanel(new BorderLayout());
        section.setBackground(BACKGROUND_COLOR);

        JLabel label = new JLabel("Preview:");
        label.setForeground(TEXT_COLOR);
        label.setFont(MONO_FONT);
        section.add(label, BorderLayout.NORTH);

        previewCanvas = new FlowCanvas();
        previewCanvas.setPreferredSize(new Dimension(450, 150));
        previewCanvas.setBackground(PANEL_COLOR);
        previewCanvas.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ACCENT_COLOR.darker(), 1),
            BorderFactory.createEmptyBorder(6, 8, 6, 8)
        ));

        JScrollPane scrollPane = new JScrollPane(previewCanvas);
        scrollPane.setPreferredSize(new Dimension(450, 150));
        section.add(scrollPane, BorderLayout.CENTER);
        return section;
    }

    private JButton createStyledButton(String text) {
        JButton button = new JButton(text);
        button.setBackground(ACCENT_COLOR);
        button.setForeground(TEXT_COLOR);
        button.setFont(MONO_FONT);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ACCENT_COLOR.darker(), 1),
            BorderFactory.createEmptyBorder(6, 12, 6, 12)
        ));
        return button;
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panel.setBackground(BACKGROUND_COLOR);
        panel.setBorder(BorderFactory.createEmptyBorder(8, 16, 16, 16));

        JButton cancelButton = createStyledButton("Cancel (Esc)");
        cancelButton.addActionListener(e -> {
            confirmed = false;
            dispose();
        });

        JButton importButton = createStyledButton("Import (Ctrl+Enter)");
        importButton.addActionListener(e -> {
            confirmed = true;
            dispose();
        });

        panel.add(cancelButton);
        panel.add(Box.createHorizontalStrut(8));
        panel.add(importButton);
        return panel;
    }

    private void browseForFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Markdown files", "md"));

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            filePathField.setText(selectedFile.getAbsolutePath());
            updatePreview(selectedFile.getAbsolutePath());
        }
    }

    private void updatePreview(String filePath) {
        try {
            MarkdownImporter importer = new MarkdownImporter();
            FlowDiagram previewDiagram = importer.importFlow(filePath);
            previewCanvas.setFlowDiagram(previewDiagram);
            previewCanvas.repaint();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error loading preview: " + e.getMessage(), "Preview Error", JOptionPane.ERROR_MESSAGE);
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
    public void keyTyped(KeyEvent e) {}

    @Override
    public void keyReleased(KeyEvent e) {}

    public boolean isConfirmed() {
        return confirmed;
    }

    public String getFilePath() {
        return filePathField.getText();
    }
}