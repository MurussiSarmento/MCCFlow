package com.sap.flowdeconstruct.ui.dialogs;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

/**
 * Dialog for adding/editing notes on flow nodes
 * Follows the dark theme design from design.md
 */
public class NoteDialog extends JDialog implements KeyListener {
    
    // Design constants
    private static final Color BACKGROUND_COLOR = new Color(0x2d, 0x2d, 0x2d);
    private static final Color PANEL_COLOR = new Color(0x3a, 0x3a, 0x3a);
    private static final Color TEXT_COLOR = new Color(0xcc, 0xcc, 0xcc);
    private static final Color ACCENT_COLOR = new Color(0x5f, 0x9e, 0xa0);
    private static final Font MONO_FONT = new Font(Font.MONOSPACED, Font.PLAIN, 12);
    
    private JTextArea noteTextArea;
    private boolean confirmed = false;
    
    public NoteDialog(Frame parent, String initialText) {
        super(parent, "Add Note", true);
        
        initializeDialog();
        setupComponents(initialText);
        setupKeyboardHandling();
    }
    
    private void initializeDialog() {
        setSize(400, 300);
        setLocationRelativeTo(getParent());
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        
        // Apply dark theme
        getContentPane().setBackground(BACKGROUND_COLOR);
    }
    
    private void setupComponents(String initialText) {
        setLayout(new BorderLayout());
        
        // Title panel
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        titlePanel.setBackground(BACKGROUND_COLOR);
        titlePanel.setBorder(BorderFactory.createEmptyBorder(12, 16, 8, 16));
        
        JLabel titleLabel = new JLabel("Node Notes");
        titleLabel.setForeground(TEXT_COLOR);
        titleLabel.setFont(MONO_FONT.deriveFont(Font.BOLD, 14f));
        titlePanel.add(titleLabel);
        
        add(titlePanel, BorderLayout.NORTH);
        
        // Text area panel
        JPanel textPanel = new JPanel(new BorderLayout());
        textPanel.setBackground(BACKGROUND_COLOR);
        textPanel.setBorder(BorderFactory.createEmptyBorder(0, 16, 12, 16));
        
        noteTextArea = new JTextArea(initialText != null ? initialText : "");
        noteTextArea.setBackground(PANEL_COLOR);
        noteTextArea.setForeground(TEXT_COLOR);
        noteTextArea.setFont(MONO_FONT);
        noteTextArea.setCaretColor(TEXT_COLOR);
        noteTextArea.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ACCENT_COLOR.darker(), 1),
            BorderFactory.createEmptyBorder(8, 8, 8, 8)
        ));
        noteTextArea.setLineWrap(true);
        noteTextArea.setWrapStyleWord(true);
        noteTextArea.addKeyListener(this);
        
        JScrollPane scrollPane = new JScrollPane(noteTextArea);
        scrollPane.setBackground(PANEL_COLOR);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(PANEL_COLOR);
        
        textPanel.add(scrollPane, BorderLayout.CENTER);
        add(textPanel, BorderLayout.CENTER);
        
        // Button panel
        JPanel buttonPanel = createButtonPanel();
        add(buttonPanel, BorderLayout.SOUTH);
        
        // Focus on text area
        SwingUtilities.invokeLater(() -> noteTextArea.requestFocusInWindow());
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
        
        // Save button
        JButton saveButton = createStyledButton("Save (Ctrl+Enter)");
        saveButton.addActionListener(e -> {
            confirmed = true;
            dispose();
        });
        
        panel.add(cancelButton);
        panel.add(Box.createHorizontalStrut(8));
        panel.add(saveButton);
        
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
    
    private void setupKeyboardHandling() {
        // Add key listener to the dialog itself
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
    
    public String getNoteText() {
        return noteTextArea.getText();
    }
}