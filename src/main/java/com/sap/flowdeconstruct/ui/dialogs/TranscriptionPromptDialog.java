package com.sap.flowdeconstruct.ui.dialogs;

import com.sap.flowdeconstruct.ai.PromptBuilder;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class TranscriptionPromptDialog extends JDialog implements KeyListener {

    // Design constants (match other dialogs)
    private static final Color BACKGROUND_COLOR = new Color(0x2d, 0x2d, 0x2d);
    private static final Color PANEL_COLOR = new Color(0x3a, 0x3a, 0x3a);
    private static final Color TEXT_COLOR = new Color(0xcc, 0xcc, 0xcc);
    private static final Color ACCENT_COLOR = new Color(0x5f, 0x9e, 0xa0);
    private static final Font MONO_FONT = new Font(Font.MONOSPACED, Font.PLAIN, 12);

    private final JTextArea transcriptionArea;
    private final JTextArea promptPreviewArea;
    private final PromptBuilder promptBuilder;

    public TranscriptionPromptDialog(Frame parent) {
        super(parent, "Generate Prompt from Transcription", true);

        this.promptBuilder = new PromptBuilder();

        setSize(800, 600);
        setLocationRelativeTo(getParent());
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        getContentPane().setBackground(BACKGROUND_COLOR);
        setLayout(new BorderLayout());

        // Title
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        titlePanel.setBackground(BACKGROUND_COLOR);
        titlePanel.setBorder(BorderFactory.createEmptyBorder(12, 16, 8, 16));
        JLabel titleLabel = new JLabel("Transcription → Prompt (Markdown Schema)");
        titleLabel.setForeground(TEXT_COLOR);
        titleLabel.setFont(MONO_FONT.deriveFont(Font.BOLD, 14f));
        titlePanel.add(titleLabel);
        add(titlePanel, BorderLayout.NORTH);

        // Split pane for transcription and preview
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setBackground(BACKGROUND_COLOR);
        splitPane.setDividerLocation(280);
        splitPane.setResizeWeight(0.5);

        // Transcription panel
        JPanel transcriptionPanel = new JPanel(new BorderLayout());
        transcriptionPanel.setBackground(BACKGROUND_COLOR);
        transcriptionPanel.setBorder(BorderFactory.createEmptyBorder(0, 16, 8, 16));
        JLabel transcriptionLabel = new JLabel("Transcription (paste or type here):");
        transcriptionLabel.setForeground(TEXT_COLOR);
        transcriptionLabel.setFont(MONO_FONT);
        transcriptionPanel.add(transcriptionLabel, BorderLayout.NORTH);

        transcriptionArea = new JTextArea();
        transcriptionArea.setLineWrap(true);
        transcriptionArea.setWrapStyleWord(true);
        transcriptionArea.setBackground(PANEL_COLOR);
        transcriptionArea.setForeground(TEXT_COLOR);
        transcriptionArea.setFont(MONO_FONT);
        transcriptionArea.setCaretColor(TEXT_COLOR);
        transcriptionArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ACCENT_COLOR.darker(), 1),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));

        JScrollPane transcriptionScroll = new JScrollPane(transcriptionArea);
        transcriptionScroll.setBackground(PANEL_COLOR);
        transcriptionPanel.add(transcriptionScroll, BorderLayout.CENTER);
        splitPane.setTopComponent(transcriptionPanel);

        // Prompt preview panel
        JPanel previewPanel = new JPanel(new BorderLayout());
        previewPanel.setBackground(BACKGROUND_COLOR);
        previewPanel.setBorder(BorderFactory.createEmptyBorder(0, 16, 16, 16));
        JLabel previewLabel = new JLabel("Prompt Preview (read-only):");
        previewLabel.setForeground(TEXT_COLOR);
        previewLabel.setFont(MONO_FONT);
        previewPanel.add(previewLabel, BorderLayout.NORTH);

        promptPreviewArea = new JTextArea();
        promptPreviewArea.setEditable(false);
        promptPreviewArea.setLineWrap(true);
        promptPreviewArea.setWrapStyleWord(true);
        promptPreviewArea.setBackground(PANEL_COLOR);
        promptPreviewArea.setForeground(TEXT_COLOR);
        promptPreviewArea.setFont(MONO_FONT);
        promptPreviewArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ACCENT_COLOR.darker(), 1),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));

        JScrollPane previewScroll = new JScrollPane(promptPreviewArea);
        previewScroll.setBackground(PANEL_COLOR);
        previewPanel.add(previewScroll, BorderLayout.CENTER);
        splitPane.setBottomComponent(previewPanel);

        add(splitPane, BorderLayout.CENTER);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(BACKGROUND_COLOR);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(8, 16, 16, 16));

        JButton copyButton = createStyledButton("Copy Prompt (Ctrl+C)");
        copyButton.addActionListener(e -> copyPromptToClipboard());
        JButton closeButton = createStyledButton("Close (Esc)");
        closeButton.addActionListener(e -> dispose());

        buttonPanel.add(copyButton);
        buttonPanel.add(Box.createHorizontalStrut(8));
        buttonPanel.add(closeButton);
        add(buttonPanel, BorderLayout.SOUTH);

        // Live update of prompt preview
        transcriptionArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { updatePrompt(); }
            @Override public void removeUpdate(DocumentEvent e) { updatePrompt(); }
            @Override public void changedUpdate(DocumentEvent e) { updatePrompt(); }
        });

        setupKeyboardHandling();
        updatePrompt(); // initialize
    }

    private void setupKeyboardHandling() {
        addKeyListener(this);
        setFocusable(true);
        transcriptionArea.addKeyListener(this);
        promptPreviewArea.addKeyListener(this);
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
        return button;
    }

    private void updatePrompt() {
        String transcription = transcriptionArea.getText();
        String prompt = promptBuilder.buildPrompt(transcription);
        promptPreviewArea.setText(prompt);
        promptPreviewArea.setCaretPosition(0);
    }

    private void copyPromptToClipboard() {
        String prompt = promptPreviewArea.getText();
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(new StringSelection(prompt), null);
        JOptionPane.showMessageDialog(this, "Prompt copied to clipboard!", "Copied", JOptionPane.INFORMATION_MESSAGE);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            dispose();
        } else if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_C) {
            copyPromptToClipboard();
        }
    }

    @Override public void keyTyped(KeyEvent e) { }
    @Override public void keyReleased(KeyEvent e) { }
}