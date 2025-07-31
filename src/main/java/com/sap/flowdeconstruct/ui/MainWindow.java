package com.sap.flowdeconstruct.ui;

import com.sap.flowdeconstruct.core.ProjectManager;
import com.sap.flowdeconstruct.model.FlowDiagram;
import com.sap.flowdeconstruct.model.FlowNode;
import com.sap.flowdeconstruct.ui.components.FlowCanvas;
import com.sap.flowdeconstruct.ui.dialogs.ExportDialog;
import com.sap.flowdeconstruct.ui.dialogs.NoteDialog;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.JButton;
import java.util.Stack;

import com.sap.flowdeconstruct.ui.components.FlowCanvas;

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
            "Ctrl+Enter - Drill down to subflow",
            "Ctrl+N     - Add note to selected node",
            "Ctrl+E     - Export flow",
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
        // Make sure the window can receive key events
        setFocusable(true);
        addKeyListener(this);
        
        // Add global key event dispatcher to handle keys regardless of focus
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(e -> {
            if (e.getID() == KeyEvent.KEY_PRESSED && isActive()) {
                handleGlobalKeyPress(e);
                return true; // Consume the event
            }
            return false;
        });
        
        // Canvas is already configured with KeyListener in its constructor
        // Give initial focus to canvas
        SwingUtilities.invokeLater(() -> {
            System.out.println("MainWindow: Requesting focus for canvas");
            canvas.requestFocusInWindow();
            canvas.requestFocus();
            
            // Also ensure the main window has focus
            this.requestFocus();
            this.toFront();
            
            System.out.println("MainWindow: Canvas focusable: " + canvas.isFocusable());
            System.out.println("MainWindow: Canvas has focus: " + canvas.hasFocus());
        });
    }
    
    private void handleGlobalKeyPress(KeyEvent e) {
        if (currentFlow == null) {
            return;
        }
        
        int keyCode = e.getKeyCode();
        boolean ctrl = e.isControlDown();
        boolean shift = e.isShiftDown();
        
        // Handle help overlay first - multiple ways to open help
        if (keyCode == KeyEvent.VK_SLASH || keyCode == KeyEvent.VK_F1 || keyCode == KeyEvent.VK_HELP ||
            (keyCode == KeyEvent.VK_SLASH && shift) || keyCode == KeyEvent.VK_H) {
            System.out.println("MainWindow.handleGlobalKeyPress: Help key detected");
            toggleHelpOverlay();
            return;
        }
        
        if (helpVisible && keyCode == KeyEvent.VK_ESCAPE) {
            hideHelpOverlay();
            return;
        }
        
        if (helpVisible) {
            return; // Don't process other keys when help is visible
        }
        
        switch (keyCode) {
            case KeyEvent.VK_TAB:
                System.out.println("MainWindow.handleGlobalKeyPress: TAB key detected");
                if (!e.isShiftDown()) {
                    createNode();
                }
                break;
                
            case KeyEvent.VK_ENTER:
                if (ctrl) {
                    drillDownToSubflow();
                } else if (currentFlow != null && currentFlow.getSelectedNode() != null) {
                    createNode();
                }
                break;
                
            case KeyEvent.VK_N:
                if (ctrl) {
                    addNoteToSelectedNode();
                } else {
                    createNode();
                }
                break;
                
            case KeyEvent.VK_E:
                if (ctrl) {
                    exportFlow();
                } else if (currentFlow != null && currentFlow.getSelectedNode() != null) {
                    currentFlow.getSelectedNode().setEditing(true);
                    canvas.repaint();
                }
                break;
                
            case KeyEvent.VK_ESCAPE:
                if (currentFlow != null && currentFlow.getSelectedNode() != null && currentFlow.getSelectedNode().isEditing()) {
                    currentFlow.getSelectedNode().setEditing(false);
                    canvas.repaint();
                } else {
                    handleEscape();
                }
                break;
                
            case KeyEvent.VK_UP:
            case KeyEvent.VK_DOWN:
            case KeyEvent.VK_LEFT:
            case KeyEvent.VK_RIGHT:
                navigateNodes(keyCode);
                break;
        }
    }
    
    private void setupProjectListener() {
        System.out.println("MainWindow: Setting up project listener");
        projectManager.addStateListener((event, oldValue, newValue) -> {
            System.out.println("MainWindow: Project event received: " + event);
            SwingUtilities.invokeLater(() -> {
                switch (event) {
                    case "currentProjectChanged":
                        System.out.println("MainWindow: Current project changed event");
                        if (newValue instanceof FlowDiagram) {
                            setCurrentFlow((FlowDiagram) newValue);
                        }
                        break;
                    case "projectCreated":
                        System.out.println("MainWindow: Project created event");
                        if (newValue instanceof FlowDiagram) {
                            setCurrentFlow((FlowDiagram) newValue);
                            navigationStack.clear();
                            updateBreadcrumb();
                        }
                        break;
                    case "projectLoaded":
                        System.out.println("MainWindow: Project loaded event");
                        if (newValue instanceof FlowDiagram) {
                            setCurrentFlow((FlowDiagram) newValue);
                            navigationStack.clear();
                            updateBreadcrumb();
                        }
                        break;
                        
                    case "projectClosed":
                        System.out.println("MainWindow: Project closed event");
                        setCurrentFlow(null);
                        break;
                }
            });
        });
    }
    
    private void setCurrentFlow(FlowDiagram flow) {
        System.out.println("Setting current flow: " + (flow != null ? flow.getName() : "null"));
        this.currentFlow = flow;
        
        if (canvas == null) {
            System.out.println("ERROR: canvas is null");
        } else {
            System.out.println("Setting flow diagram on canvas");
            canvas.setFlowDiagram(flow);
        }
        
        updateBreadcrumb();
        
        // Request focus to ensure key events are received
        SwingUtilities.invokeLater(() -> {
            if (canvas != null) {
                System.out.println("Requesting focus for canvas");
                canvas.requestFocusInWindow();
            }
        });
    }
    
    private void updateBreadcrumb() {
        StringBuilder breadcrumb = new StringBuilder("Main Flow");
        
        for (FlowDiagram flow : navigationStack) {
            breadcrumb.append(" > ").append(flow.getName());
        }
        
        if (currentFlow != null && !navigationStack.isEmpty()) {
            breadcrumb.append(" > ").append(currentFlow.getName());
        }
        
        breadcrumbLabel.setText(breadcrumb.toString());
    }
    
    // Keyboard event handling
    @Override
    public void keyPressed(KeyEvent e) {
        System.out.println("MainWindow.keyPressed: Key=" + KeyEvent.getKeyText(e.getKeyCode()) + 
                          ", Ctrl=" + e.isControlDown() + ", Shift=" + e.isShiftDown());
        
        if (currentFlow == null) {
            System.out.println("MainWindow.keyPressed: currentFlow is null, ignoring key event");
            return;
        }
        
        int keyCode = e.getKeyCode();
        boolean ctrl = e.isControlDown();
        boolean shift = e.isShiftDown();
        
        // Handle help overlay first
        if (keyCode == KeyEvent.VK_SLASH && shift) { // ? key
            System.out.println("MainWindow.keyPressed: Help key detected");
            toggleHelpOverlay();
            return;
        }
        
        if (helpVisible && keyCode == KeyEvent.VK_ESCAPE) {
            hideHelpOverlay();
            return;
        }
        
        if (helpVisible) {
            return; // Don't process other keys when help is visible
        }
        
        System.out.println("MainWindow.keyPressed: Processing keyCode=" + keyCode + " (" + KeyEvent.getKeyText(keyCode) + ")");
        
        switch (keyCode) {
            case KeyEvent.VK_TAB:
                System.out.println("MainWindow.keyPressed: TAB key detected");
                if (!e.isShiftDown()) {
                    System.out.println("MainWindow.keyPressed: Calling createNode()");
                    createNode();
                }
                e.consume(); // Prevent default TAB behavior
                break;
                
            case 0: // Handle unknown keyCode 0 - often Tab key on some systems
                System.out.println("MainWindow.keyPressed: Unknown keyCode 0 detected - treating as TAB");
                if (!e.isShiftDown()) {
                    System.out.println("MainWindow.keyPressed: Calling createNode() for keyCode 0");
                    createNode();
                }
                e.consume();
                break;
                
            case KeyEvent.VK_ENTER:
                System.out.println("MainWindow.keyPressed: ENTER key detected");
                if (ctrl) {
                    System.out.println("MainWindow.keyPressed: Ctrl+Enter - drilling down");
                    drillDownToSubflow();
                } else if (currentFlow != null && currentFlow.getSelectedNode() != null) {
                    System.out.println("MainWindow.keyPressed: Enter - creating node");
                    createNode();
                }
                break;
                
            case KeyEvent.VK_N:
                System.out.println("MainWindow.keyPressed: N key detected");
                if (ctrl) {
                    System.out.println("MainWindow.keyPressed: Ctrl+N - adding note");
                    addNoteToSelectedNode();
                } else {
                    System.out.println("MainWindow.keyPressed: N - creating node");
                    createNode();
                }
                break;
                
            case KeyEvent.VK_E:
                System.out.println("MainWindow.keyPressed: E key detected");
                if (ctrl) {
                    System.out.println("MainWindow.keyPressed: Ctrl+E - exporting");
                    exportFlow();
                } else if (currentFlow != null && currentFlow.getSelectedNode() != null) {
                    System.out.println("MainWindow.keyPressed: E - editing node");
                    currentFlow.getSelectedNode().setEditing(true);
                    canvas.repaint();
                }
                break;
                
            case KeyEvent.VK_ESCAPE:
                System.out.println("MainWindow.keyPressed: ESCAPE key detected");
                if (currentFlow != null && currentFlow.getSelectedNode() != null && currentFlow.getSelectedNode().isEditing()) {
                    currentFlow.getSelectedNode().setEditing(false);
                    canvas.repaint();
                } else {
                    handleEscape();
                }
                break;
                
            case KeyEvent.VK_UP:
                System.out.println("MainWindow.keyPressed: UP key detected");
                navigateNodes(keyCode);
                break;
                
            case KeyEvent.VK_DOWN:
                System.out.println("MainWindow.keyPressed: DOWN key detected");
                navigateNodes(keyCode);
                break;
                
            case KeyEvent.VK_LEFT:
                System.out.println("MainWindow.keyPressed: LEFT key detected");
                navigateNodes(keyCode);
                break;
                
            case KeyEvent.VK_RIGHT:
                System.out.println("MainWindow.keyPressed: RIGHT key detected");
                navigateNodes(keyCode);
                break;
                
            case KeyEvent.VK_SLASH:
                if (shift) {
                    System.out.println("MainWindow.keyPressed: ? key detected (Shift+/)");
                    toggleHelpOverlay();
                }
                break;
                
            default:
                System.out.println("MainWindow.keyPressed: Unhandled key: " + KeyEvent.getKeyText(keyCode) + " (keyCode=" + keyCode + ")");
                break;
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
    
    // Action methods
    private void createNode() {
        System.out.println("MainWindow.createNode() called");
        if (canvas == null) {
            System.out.println("ERROR: canvas is null");
            return;
        }
        if (currentFlow == null) {
            System.out.println("ERROR: currentFlow is null");
            return;
        }
        System.out.println("Calling canvas.createNode()");
        canvas.createNode();
    }
    
    private void drillDownToSubflow() {
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
        ExportDialog dialog = new ExportDialog(this);
        dialog.setVisible(true);
        
        if (dialog.isConfirmed()) {
            // Export logic would go here
            // For now, just show a message
            JOptionPane.showMessageDialog(this, 
                "Export functionality will be implemented in the next phase.", 
                "Export", 
                JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    private void handleEscape() {
        if (helpVisible) {
            hideHelpOverlay();
        } else if (!navigationStack.isEmpty()) {
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
}