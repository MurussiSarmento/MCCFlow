package com.sap.flowdeconstruct.ui;

import com.sap.flowdeconstruct.core.ProjectManager;
import com.sap.flowdeconstruct.model.FlowDiagram;
import com.sap.flowdeconstruct.model.FlowNode;
import com.sap.flowdeconstruct.ui.components.FlowCanvas;
import com.sap.flowdeconstruct.ui.dialogs.ExportDialog;
import com.sap.flowdeconstruct.ui.dialogs.NoteDialog;
import com.sap.flowdeconstruct.ui.dialogs.TranscriptionPromptDialog;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.JButton;
import java.util.Stack;

import com.sap.flowdeconstruct.ui.components.FlowCanvas;
import com.sap.flowdeconstruct.export.MarkdownExporter;
import com.sap.flowdeconstruct.importer.MarkdownImporter;
import com.sap.flowdeconstruct.ui.dialogs.ImportDialog;

/**
 * Main application window containing the flow canvas and all UI components
 * Implements keyboard-first interaction as specified in PRD
 */
public class MainWindow extends JFrame implements KeyListener {
    
    // Design constants from design.md
    private static final Color BACKGROUND_COLOR = new Color(0x2d, 0x2d, 0x2d);
    private static final Color TEXT_COLOR = new Color(0xcc, 0xcc, 0xcc);
    private static final Font MONO_FONT = new Font(Font.MONOSPACED, Font.PLAIN, 12);
    
    private final ProjectManager projectManager;
    private FlowCanvas canvas;
    private JLabel breadcrumbLabel;
    private JLabel helpHintLabel;
    private JPanel helpOverlay;
    private boolean helpVisible = false;
    
    // Navigation state
    private Stack<FlowDiagram> navigationStack;
    private FlowDiagram currentFlow;
    private boolean moveMode = false;
    
    public MainWindow(ProjectManager projectManager) {
        this.projectManager = projectManager;
        this.navigationStack = new Stack<>();
        
        System.out.println("MainWindow: Initializing...");
        initializeWindow();
        initializeComponents();
        setupKeyboardHandling();
        setupProjectListener();
        
        // Load current project if available
        System.out.println("MainWindow: Checking for current project...");
        if (projectManager.getCurrentProject() != null) {
            System.out.println("MainWindow: Found existing project, setting current flow");
            setCurrentFlow(projectManager.getCurrentProject());
        } else {
            System.out.println("MainWindow: No current project found");
        }
        System.out.println("MainWindow: Initialization complete");
    }
    
    private void initializeWindow() {
        setTitle("FlowDeconstruct - Ultra-fast flow mapping");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(1200, 800);
        setMinimumSize(new Dimension(800, 600));
        setLocationRelativeTo(null);
        
        // Apply dark theme
        getContentPane().setBackground(BACKGROUND_COLOR);
        
        // Window close behavior - minimize to tray
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                setVisible(false);
            }
        });
    }
    
    private void initializeComponents() {
        System.out.println("MainWindow: Initializing components...");
        setLayout(new BorderLayout());
        
        // Create and set menu bar
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        JMenuItem saveMdItem = new JMenuItem("Save as Markdown");
        saveMdItem.addActionListener(e -> saveAsMarkdown());
        JMenuItem loadMdItem = new JMenuItem("Load from Markdown");
        loadMdItem.addActionListener(e -> importFlow());
        fileMenu.add(saveMdItem);
        fileMenu.add(loadMdItem);
        menuBar.add(fileMenu);
        setJMenuBar(menuBar);
        
        // Top bar with breadcrumb and help hint
        JPanel topBar = createTopBar();
        System.out.println("MainWindow: Top bar created");
        
        // Toolbar with action buttons
        JPanel toolbar = createToolbar();
        System.out.println("MainWindow: Toolbar created with " + toolbar.getComponentCount() + " components");
        
        // Combine top bar and toolbar
        JPanel northPanel = new JPanel(new BorderLayout());
        northPanel.add(topBar, BorderLayout.NORTH);
        northPanel.add(toolbar, BorderLayout.SOUTH);
        add(northPanel, BorderLayout.NORTH);
        
        // Main canvas
        canvas = new FlowCanvas();
        canvas.setBackground(BACKGROUND_COLOR);
        System.out.println("MainWindow: Canvas created");
        
        // Set current flow on canvas if it exists
        if (currentFlow != null) {
            System.out.println("MainWindow: Setting existing currentFlow on newly created canvas");
            canvas.setFlowDiagram(currentFlow);
        }
        
        JScrollPane scrollPane = new JScrollPane(canvas);
        scrollPane.setBackground(BACKGROUND_COLOR);
        scrollPane.getViewport().setBackground(BACKGROUND_COLOR);
        scrollPane.setBorder(null);
        add(scrollPane, BorderLayout.CENTER);
        System.out.println("MainWindow: ScrollPane added to center");
        
        // Help overlay (initially hidden)
        helpOverlay = createHelpOverlay();
        
        // Add help overlay to glass pane for overlay functionality
        JPanel glassPane = new JPanel();
        glassPane.setOpaque(false);
        glassPane.setLayout(null);
        glassPane.add(helpOverlay);
        setGlassPane(glassPane);
        helpOverlay.setVisible(false);
        
        // Ensure components are visible
        northPanel.setVisible(true);
        topBar.setVisible(true);
        toolbar.setVisible(true);
        scrollPane.setVisible(true);
        canvas.setVisible(true);
        
        System.out.println("MainWindow: All components initialized and made visible");
        System.out.println("MainWindow: North panel components: " + northPanel.getComponentCount());
        System.out.println("MainWindow: Toolbar buttons: " + toolbar.getComponentCount());
        System.out.println("MainWindow: Canvas visible: " + canvas.isVisible());
        System.out.println("MainWindow: ScrollPane visible: " + scrollPane.isVisible());
    }
    
    private JPanel createTopBar() {
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(BACKGROUND_COLOR);
        topBar.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        
        // Breadcrumb on the left
        breadcrumbLabel = new JLabel("Main Flow");
        breadcrumbLabel.setForeground(TEXT_COLOR);
        breadcrumbLabel.setFont(MONO_FONT);
        topBar.add(breadcrumbLabel, BorderLayout.WEST);
        
        // Help hint on the right
        helpHintLabel = new JLabel("Press ? for shortcuts");
        helpHintLabel.setForeground(TEXT_COLOR.darker());
        helpHintLabel.setFont(MONO_FONT.deriveFont(Font.ITALIC));
        topBar.add(helpHintLabel, BorderLayout.EAST);
        
        return topBar;
    }
    
    private JPanel createToolbar() {
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        toolbar.setBackground(BACKGROUND_COLOR.darker());
        toolbar.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        
        // Create Node button
        JButton createNodeBtn = createToolbarButton("+ Node", "Create new node (Tab)", e -> createNode());
        toolbar.add(createNodeBtn);
        
        // Drill Down button
        JButton drillDownBtn = createToolbarButton("↓ Subflow", "Drill down to subflow (Ctrl+Enter)", e -> drillDownToSubflow());
        toolbar.add(drillDownBtn);
        
        // Add Note button
        JButton addNoteBtn = createToolbarButton("Note", "Add note to selected node (Ctrl+N)", e -> addNoteToSelectedNode());
        toolbar.add(addNoteBtn);
        
        // Separator
        toolbar.add(createSeparator());
        
        // Export button
        JButton exportBtn = createToolbarButton("Export", "Export flow (Ctrl+E)", e -> exportFlow());
        toolbar.add(exportBtn);
        
        // Prompt from Transcription button
        JButton promptBtn = createToolbarButton("Prompt", "Generate Markdown prompt from transcription (Ctrl+G)", e -> openTranscriptionPromptDialog());
        toolbar.add(promptBtn);
        
        // Back button
        JButton backBtn = createToolbarButton("< Back", "Go back (Esc)", e -> handleEscape());
        toolbar.add(backBtn);
        
        // Separator
        toolbar.add(createSeparator());
        
        // Help button
        JButton helpBtn = createToolbarButton("? Help", "Show keyboard shortcuts (?)", e -> toggleHelpOverlay());
        toolbar.add(helpBtn);
        
        return toolbar;
    }
    
    private JButton createToolbarButton(String text, String tooltip, ActionListener action) {
        JButton button = new JButton(text);
        button.setToolTipText(tooltip);
        button.setBackground(new Color(0x3a, 0x3a, 0x3a));
        button.setForeground(TEXT_COLOR);
        button.setFont(MONO_FONT.deriveFont(10f));
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0x5f, 0x9e, 0xa0).darker(), 1),
            BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));
        button.setFocusPainted(false);
        button.addActionListener(action);
        
        // Hover effect
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(new Color(0x5f, 0x9e, 0xa0).darker());
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(new Color(0x3a, 0x3a, 0x3a));
            }
        });
        
        return button;
    }
    
    private Component createSeparator() {
        JPanel separator = new JPanel();
        separator.setPreferredSize(new Dimension(1, 20));
        separator.setBackground(new Color(0x5f, 0x9e, 0xa0).darker());
        return separator;
    }
    
    private JPanel createHelpOverlay() {
        JPanel overlay = new JPanel();
        overlay.setBackground(new Color(0, 0, 0, 180)); // Semi-transparent black
        overlay.setLayout(new BorderLayout());
        
        // Help content panel
        JPanel helpPanel = new JPanel();
        helpPanel.setBackground(new Color(0x3a, 0x3a, 0x3a));
        helpPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0x5f, 0x9e, 0xa0), 1),
            BorderFactory.createEmptyBorder(16, 20, 16, 20)
        ));
        
        helpPanel.setLayout(new BoxLayout(helpPanel, BoxLayout.Y_AXIS));
        
        // Title
        JLabel titleLabel = new JLabel("FlowDeconstruct Shortcuts");
        titleLabel.setForeground(TEXT_COLOR);
        titleLabel.setFont(MONO_FONT.deriveFont(Font.BOLD, 14f));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        helpPanel.add(titleLabel);
        
        helpPanel.add(Box.createVerticalStrut(12));
        
        // Shortcuts
        String[] shortcuts = {
            "Tab        - Create connected node",
            "Shift+Tab  - Create isolated node",
            "Enter      - Edit selected node",
            "Type text  - Auto-edit selected node",
            "Ctrl+Enter - Drill down to subflow",
            "Ctrl+N     - Add note to selected node",
            "Ctrl+E     - Export flow",
            "Ctrl+G     - Generate prompt from transcription",
            "Arrow Keys - Navigate nodes",
            "Esc        - Go back / Cancel",
            "?          - Show this help"
        };
        
        for (String shortcut : shortcuts) {
            JLabel shortcutLabel = new JLabel(shortcut);
            shortcutLabel.setForeground(TEXT_COLOR);
            shortcutLabel.setFont(MONO_FONT);
            shortcutLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            helpPanel.add(shortcutLabel);
            helpPanel.add(Box.createVerticalStrut(4));
        }
        
        // Position help panel in bottom-right
        JPanel positioningPanel = new JPanel(new BorderLayout());
        positioningPanel.setOpaque(false);
        positioningPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 20));
        positioningPanel.add(helpPanel, BorderLayout.EAST);
        
        overlay.add(positioningPanel, BorderLayout.SOUTH);
        
        return overlay;
    }
    
    private void setupKeyboardHandling() {
        // Permite que a janela receba eventos de teclado e registra este KeyListener
        setFocusable(true);
        addKeyListener(this);

        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(e -> {
            if (e.getID() == KeyEvent.KEY_PRESSED) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_TAB:
                        if (e.isShiftDown()) {
                            // Shift+Tab: criar nó isolado
                            if (canvas != null) {
                                System.out.println("MainWindow.keyPressed: Calling createIsolatedNode() (Shift+Tab)");
                                canvas.createIsolatedNode();
                                return true;
                            }
                        } else {
                            createNode();
                            return true;
                        }
                        break;
                    case KeyEvent.VK_ESCAPE:
                        if (canvas != null) canvas.handleEscapeKey();
                        return true;
                }
            }
            return false;
        });

        // Garantir foco inicial no canvas
        SwingUtilities.invokeLater(() -> {
            if (canvas != null) {
                System.out.println("MainWindow: Requesting focus for canvas");
                canvas.requestFocusInWindow();
                canvas.requestFocus();
            }
            this.requestFocus();
            this.toFront();
        });
    }
    
    private void createNode() {
        System.out.println("MainWindow.createNode() called");
        // Mantém comportamento atual
        if (canvas != null) {
            System.out.println("Calling canvas.createNode()");
            canvas.createNode();
        }
    }
    
    private void startEditingSelectedNode() {
        System.out.println("MainWindow.startEditingSelectedNode() called");
        if (canvas == null) {
            System.out.println("ERROR: canvas is null");
            return;
        }
        if (currentFlow == null || currentFlow.getSelectedNode() == null) {
            System.out.println("ERROR: no selected node");
            return;
        }
        if (canvas.isEditingNode()) {
            System.out.println("MainWindow.startEditingSelectedNode: Already editing, skipping");
            return;
        }
        System.out.println("Starting to edit selected node: " + currentFlow.getSelectedNode().getText());
        canvas.startEditingSelectedNode();
    }
    
    private void drillDownToSubflow() {
        // Auto-save any current editing before drilling down
        if (canvas != null && canvas.isEditingNode()) {
            canvas.finishEditingNode();
        }
        
        FlowNode selectedNode = currentFlow.getSelectedNode();
        if (selectedNode != null) {
            if (!selectedNode.hasSubFlow()) {
                selectedNode.createSubFlow();
            }
            
            // Navigate to subflow
            navigationStack.push(currentFlow);
            setCurrentFlow(selectedNode.getSubFlow());
        }
    }
    
    private void addNoteToSelectedNode() {
        // Auto-save any current editing before opening note dialog
        if (canvas != null && canvas.isEditingNode()) {
            canvas.finishEditingNode();
        }
        
        FlowNode selectedNode = currentFlow.getSelectedNode();
        if (selectedNode != null) {
            NoteDialog dialog = new NoteDialog(this, selectedNode.getNotes());
            dialog.setVisible(true);
            
            if (dialog.isConfirmed()) {
                selectedNode.setNotes(dialog.getNoteText());
                canvas.repaint();
            }
        }
    }
    
    private void exportFlow() {
        // Auto-save any current editing before opening export dialog
        if (canvas != null && canvas.isEditingNode()) {
            canvas.finishEditingNode();
        }
        
        ExportDialog dialog = new ExportDialog(this);
        dialog.setVisible(true);
        
        if (dialog.isConfirmed()) {
            String filePath = dialog.getFilePath();
            boolean includeNotes = dialog.isIncludeNotes();
            boolean includeSubflows = dialog.isIncludeSubflows();
            if (filePath.endsWith(".md")) {
                try {
                    // Use ProjectManager.saveToMarkdown for consistency
                    projectManager.saveToMarkdown(filePath, includeNotes, includeSubflows);
                    JOptionPane.showMessageDialog(this, "Flow exported to Markdown successfully!", "Export Success", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Error exporting to Markdown: " + ex.getMessage(), "Export Error", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                // Handle other formats like PDF here if implemented
                JOptionPane.showMessageDialog(this, "Export format not supported yet.", "Export", JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }
    
    private void handleEscape() {
        if (helpVisible) {
            hideHelpOverlay();
        } else if (!navigationStack.isEmpty()) {
            // Auto-save any current editing before going back
            if (canvas != null && canvas.isEditingNode()) {
                canvas.finishEditingNode();
            }
            
            // Go back to parent flow
            FlowDiagram parentFlow = navigationStack.pop();
            setCurrentFlow(parentFlow);
        }
    }
    
    private void navigateNodes(int direction) {
        if (canvas != null) {
            canvas.navigateNodes(direction);
        }
    }
    
    private void moveSelectedNode(int dx, int dy) {
        FlowNode node = currentFlow.getSelectedNode();
        int newX = (int)node.getX() + dx;
        int newY = (int)node.getY() + dy;
        if (!canvas.wouldOverlap(node, newX, newY)) {
            node.setPosition(newX, newY);
            canvas.repaint();
        }
    }
    
    private void toggleHelpOverlay() {
        if (helpVisible) {
            hideHelpOverlay();
        } else {
            showHelpOverlay();
        }
    }
    
    private void showHelpOverlay() {
        helpOverlay.setVisible(true);
        helpVisible = true;
        
        // Position overlay to cover the entire window
        helpOverlay.setBounds(0, 0, getWidth(), getHeight());
        
        // Bring to front
        getLayeredPane().setComponentZOrder(helpOverlay, 0);
    }
    
    private void hideHelpOverlay() {
        helpOverlay.setVisible(false);
        helpVisible = false;
        
        // Return focus to canvas
        canvas.requestFocusInWindow();
    }
    
    // Public methods for external access
    public void createNewProject() {
        projectManager.createNewProject();
    }
    
    public void exitApplication() {
        // Save current project
        projectManager.saveCurrentProject();
        
        // Exit
        System.exit(0);
    }
    
    private void importFlow() {
        // Auto-save any current editing before importing
        if (canvas != null && canvas.isEditingNode()) {
            canvas.finishEditingNode();
        }
        
        ImportDialog dialog = new ImportDialog(this);
        dialog.setVisible(true);
        
        if (dialog.isConfirmed()) {
            String filePath = dialog.getFilePath();
            try {
                FlowDiagram importedFlow = projectManager.loadFromMarkdown(filePath);
                setCurrentFlow(importedFlow);
                JOptionPane.showMessageDialog(this, "Flow imported from Markdown successfully!", "Import Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error importing from Markdown: " + ex.getMessage(), "Import Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void saveAsMarkdown() {
        if (currentFlow == null) {
            JOptionPane.showMessageDialog(this, "No flow to save!", "Save Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save as Markdown");
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Markdown files (*.md)", "md"));
        
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            String filePath = fileChooser.getSelectedFile().getPath();
            if (!filePath.endsWith(".md")) {
                filePath += ".md";
            }
            
            try {
                projectManager.saveToMarkdown(filePath, true, true); // Default to include notes and subflows
                JOptionPane.showMessageDialog(this, "Flow saved as Markdown successfully!", "Save Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error saving as Markdown: " + ex.getMessage(), "Save Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible) {
            // Ensure canvas has focus when window becomes visible
            SwingUtilities.invokeLater(() -> {
                canvas.requestFocusInWindow();
            });
        }
    }
    
    private void openTranscriptionPromptDialog() {
        // Auto-save any current editing before opening dialog
        if (canvas != null && canvas.isEditingNode()) {
            canvas.finishEditingNode();
        }
        TranscriptionPromptDialog dialog = new TranscriptionPromptDialog(this);
        dialog.setVisible(true);
    }
    
    // Keyboard event handling (invocado pelo FlowCanvas via parent KeyListener)
    @Override
    public void keyPressed(KeyEvent e) {
        if (currentFlow == null) {
            return;
        }
    
        int keyCode = e.getKeyCode();
        boolean ctrl = e.isControlDown();
        boolean shift = e.isShiftDown();
    
        // Ajuda (Shift + / -> ?) e F1
        if ((keyCode == KeyEvent.VK_SLASH && shift) || keyCode == KeyEvent.VK_F1 || keyCode == KeyEvent.VK_HELP) {
            toggleHelpOverlay();
            return;
        }
        if (helpVisible) {
            if (keyCode == KeyEvent.VK_ESCAPE) hideHelpOverlay();
            return; // não processa outras teclas quando ajuda está visível
        }
    
        switch (keyCode) {
            case KeyEvent.VK_TAB:
                if (shift) {
                    if (canvas != null) canvas.createIsolatedNode();
                } else {
                    createNode();
                }
                e.consume();
                break;
    
            case KeyEvent.VK_ENTER:
                if (canvas != null && canvas.isEditingNode()) {
                    // Deixe o canvas finalizar a edição via handleKeyTyped('\n')
                    canvas.handleKeyTyped('\n');
                } else if (ctrl) {
                    drillDownToSubflow();
                } else if (currentFlow.getSelectedNode() != null && !canvas.isEditingNode()) {
                    // Enter inicia edição (sem Ctrl)
                    startEditingSelectedNode();
                }
                break;
    
            case KeyEvent.VK_N:
                if (ctrl) {
                    addNoteToSelectedNode();
                }
                break;
    
            case KeyEvent.VK_E:
                if (ctrl) {
                    exportFlow();
                } else if (currentFlow.getSelectedNode() != null && canvas != null && !canvas.isEditingNode()) {
                    startEditingSelectedNode();
                }
                break;
    
            case KeyEvent.VK_G:
                if (ctrl) {
                    openTranscriptionPromptDialog();
                }
                break;
    
            case KeyEvent.VK_DELETE:
                if (canvas != null && !canvas.isEditingNode() && currentFlow.getSelectedNode() != null) {
                    currentFlow.removeNode(currentFlow.getSelectedNode());
                    canvas.repaint();
                }
                break;
    
            case KeyEvent.VK_ESCAPE:
                if (canvas != null && canvas.isEditingNode()) {
                    canvas.handleEscapeKey();
                } else {
                    handleEscape();
                }
                break;
    
            case KeyEvent.VK_M:
                if (ctrl) {
                    moveMode = !moveMode;
                }
                break;
    
            case KeyEvent.VK_UP:
            case KeyEvent.VK_DOWN:
            case KeyEvent.VK_LEFT:
            case KeyEvent.VK_RIGHT:
                if (moveMode && shift && currentFlow.getSelectedNode() != null && canvas != null && !canvas.isEditingNode()) {
                    int dx = 0, dy = 0;
                    switch (keyCode) {
                        case KeyEvent.VK_UP: dy = -10; break;
                        case KeyEvent.VK_DOWN: dy = 10; break;
                        case KeyEvent.VK_LEFT: dx = -10; break;
                        case KeyEvent.VK_RIGHT: dx = 10; break;
                    }
                    moveSelectedNode(dx, dy);
                } else {
                    navigateNodes(keyCode);
                }
                break;
            default:
                // outros atalhos não tratados aqui
                break;
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
        if (currentFlow != null && canvas != null) {
            if (currentFlow.getSelectedNode() != null && !canvas.isEditingNode()) {
                char keyChar = e.getKeyChar();
                if (!Character.isISOControl(keyChar)) {
                    startEditingSelectedNode();
                    canvas.handleKeyTyped(keyChar);
                }
            } else if (canvas.isEditingNode()) {
                char keyChar = e.getKeyChar();
                if (!Character.isISOControl(keyChar)) {
                    canvas.handleKeyTyped(keyChar);
                }
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        // not used
    }
 
    private void setupProjectListener() {
        // Escuta eventos do ProjectManager para manter a UI sincronizada com o projeto atual
        projectManager.addStateListener((event, oldValue, newValue) -> {
            System.out.println("MainWindow: ProjectManager event: " + event);
            if ("currentProjectChanged".equals(event)
                    || "projectLoaded".equals(event)
                    || "projectCreated".equals(event)) {
                FlowDiagram flow = (newValue instanceof FlowDiagram)
                        ? (FlowDiagram) newValue
                        : projectManager.getCurrentProject();
                SwingUtilities.invokeLater(() -> setCurrentFlow(flow));
            } else if ("projectModified".equals(event) || "nodeModified".equals(event)) {
                if (canvas != null) {
                    SwingUtilities.invokeLater(() -> canvas.repaint());
                }
            }
        });
    }

    private void setCurrentFlow(FlowDiagram flow) {
        System.out.println("MainWindow.setCurrentFlow: " + (flow != null ? flow.getName() : "null"));
        this.currentFlow = flow;

        // Atualiza canvas
        if (canvas != null) {
            canvas.setFlowDiagram(flow);
            canvas.requestFocusInWindow();
        }

        // Atualiza breadcrumb
        if (breadcrumbLabel != null) {
            String label = (flow != null && flow.getName() != null && !flow.getName().isEmpty())
                    ? flow.getName()
                    : "Main Flow";
            breadcrumbLabel.setText(label);
        }
    }
}