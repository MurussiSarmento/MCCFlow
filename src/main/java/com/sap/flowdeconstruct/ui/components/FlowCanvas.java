package com.sap.flowdeconstruct.ui.components;

import com.sap.flowdeconstruct.model.FlowConnection;
import com.sap.flowdeconstruct.model.FlowDiagram;
import com.sap.flowdeconstruct.model.FlowNode;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.List;

/**
 * Canvas component for rendering and interacting with flow diagrams
 * Implements the visual design specified in design.md
 */
public class FlowCanvas extends JPanel implements MouseListener, MouseMotionListener, KeyListener, FocusListener {
    
    // Design constants from design.md
    private static final Color BACKGROUND_COLOR = new Color(0x2d, 0x2d, 0x2d);
    private static final Color NODE_COLOR = new Color(0x3a, 0x3a, 0x3a);
    private static final Color NODE_SELECTED_COLOR = new Color(0x5f, 0x9e, 0xa0);
    private static final Color NODE_EDITING_COLOR = new Color(0x4a, 0x4a, 0x4a);
    private static final Color TEXT_COLOR = new Color(0xcc, 0xcc, 0xcc);
    private static final Color CONNECTION_COLOR = new Color(0x66, 0x66, 0x66);
    private static final Color SUBFLOW_INDICATOR_COLOR = new Color(0x5f, 0x9e, 0xa0);
    
    private static final Font MONO_FONT = new Font(Font.MONOSPACED, Font.PLAIN, 12);
    private static final int NODE_WIDTH = 120;
    private static final int NODE_HEIGHT = 40;
    private static final int NODE_SPACING_X = 160;
    private static final int NODE_SPACING_Y = 80;
    private static final int CANVAS_MARGIN = 50;
    
    private FlowDiagram flowDiagram;
    private FlowNode editingNode;
    private String editingText = "";
    
    // Canvas state
    private Point2D.Double viewOffset = new Point2D.Double(0, 0);
    private double zoomLevel = 1.0;
    private Point lastMousePos;
    private boolean dragging = false;
    private FlowNode draggingNode = null;
    
    public FlowCanvas() {
        setBackground(BACKGROUND_COLOR);
        setPreferredSize(new Dimension(2000, 1500)); // Large canvas for infinite feel
        
        addMouseListener(this);
        addMouseMotionListener(this);
        addKeyListener(this);
        addFocusListener(this);
        
        // Enable focus for keyboard events
        setFocusable(true);
        setRequestFocusEnabled(true);
        
        // Request focus when clicked
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                requestFocusInWindow();
            }
        });
    }
    
    public void setFlowDiagram(FlowDiagram diagram) {
        System.out.println("FlowCanvas.setFlowDiagram: Setting diagram: " + 
                          (diagram != null ? diagram.getName() : "null"));
        this.flowDiagram = diagram;
        
        if (diagram != null) {
            System.out.println("FlowCanvas.setFlowDiagram: Diagram has " + diagram.getNodes().size() + " nodes");
            // Auto-layout if nodes don't have positions
            autoLayoutNodes();
            
            // Add listener for diagram changes
            diagram.addStateListener((d, event, oldValue, newValue) -> {
                System.out.println("FlowCanvas: Diagram event: " + event);
                SwingUtilities.invokeLater(() -> {
                    if ("nodeAdded".equals(event) || "nodeRemoved".equals(event)) {
                        autoLayoutNodes();
                    }
                    // Don't repaint during text editing to avoid interrupting the editing process
                    if (editingNode == null || !"nodeModified".equals(event)) {
                        repaint();
                    }
                });
            });
        } else {
            System.out.println("FlowCanvas.setFlowDiagram: Diagram is null");
        }
        
        // Force component to be visible and focusable
        setVisible(true);
        setFocusable(true);
        
        // Request focus in multiple ways
        requestFocus();
        requestFocusInWindow();
        
        // Force complete repaint
        invalidate();
        revalidate();
        System.out.println("FlowCanvas.setFlowDiagram: Calling repaint()");
        repaint();
        
        // Request focus again after a short delay
        SwingUtilities.invokeLater(() -> {
            requestFocusInWindow();
            System.out.println("FlowCanvas.setFlowDiagram: Requested focus again");
        });
    }
    
    private void autoLayoutNodes() {
        if (flowDiagram == null || flowDiagram.getNodes().isEmpty()) {
            return;
        }
        
        List<FlowNode> nodes = flowDiagram.getNodes();
        
        // Simple grid layout
        int nodesPerRow = Math.max(1, (int) Math.ceil(Math.sqrt(nodes.size())));
        
        for (int i = 0; i < nodes.size(); i++) {
            FlowNode node = nodes.get(i);
            
            int row = i / nodesPerRow;
            int col = i % nodesPerRow;
            
            double x = CANVAS_MARGIN + col * NODE_SPACING_X;
            double y = CANVAS_MARGIN + row * NODE_SPACING_Y;
            
            node.setPosition((int)x, (int)y);
        }
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        System.out.println("FlowCanvas.paintComponent: Called. FlowDiagram=" + 
                          (flowDiagram != null ? "not null" : "null"));
        
        if (flowDiagram == null) {
            System.out.println("FlowCanvas.paintComponent: No diagram, showing welcome screen");
            paintWelcomeScreen(g);
            return;
        }
        
        System.out.println("FlowCanvas.paintComponent: Drawing " + flowDiagram.getNodes().size() + " nodes");
        
        Graphics2D g2d = (Graphics2D) g.create();
        
        // Enable antialiasing
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        
        // Apply zoom and pan transformations
        AffineTransform transform = new AffineTransform();
        transform.translate(viewOffset.x, viewOffset.y);
        transform.scale(zoomLevel, zoomLevel);
        g2d.setTransform(transform);
        
        // Draw connections first (behind nodes)
        drawConnections(g2d);
        
        // Draw nodes
        drawNodes(g2d);
        
        g2d.dispose();
    }
    
    private void paintWelcomeScreen(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        
        g2d.setColor(TEXT_COLOR.darker());
        g2d.setFont(MONO_FONT.deriveFont(16f));
        
        String message = "Press Tab to create your first node";
        FontMetrics fm = g2d.getFontMetrics();
        int x = (getWidth() - fm.stringWidth(message)) / 2;
        int y = getHeight() / 2;
        
        g2d.drawString(message, x, y);
    }
    
    private void drawConnections(Graphics2D g2d) {
        if (flowDiagram == null) return;
        
        g2d.setColor(CONNECTION_COLOR);
        g2d.setStroke(new BasicStroke(2f));
        
        for (FlowConnection connection : flowDiagram.getConnections()) {
            FlowNode fromNode = flowDiagram.findNodeById(connection.getFromNodeId());
            FlowNode toNode = flowDiagram.findNodeById(connection.getToNodeId());
            
            if (fromNode != null && toNode != null) {
                drawConnection(g2d, fromNode, toNode, connection);
            }
        }
    }
    
    private void drawConnection(Graphics2D g2d, FlowNode fromNode, FlowNode toNode, FlowConnection connection) {
        // Calculate connection points
        double fromX = fromNode.getX() + NODE_WIDTH / 2;
        double fromY = fromNode.getY() + NODE_HEIGHT;
        double toX = toNode.getX() + NODE_WIDTH / 2;
        double toY = toNode.getY();
        
        // Draw line
        g2d.drawLine((int) fromX, (int) fromY, (int) toX, (int) toY);
        
        // Draw arrow head
        drawArrowHead(g2d, fromX, fromY, toX, toY);
    }
    
    private void drawArrowHead(Graphics2D g2d, double fromX, double fromY, double toX, double toY) {
        double angle = Math.atan2(toY - fromY, toX - fromX);
        double arrowLength = 8;
        double arrowAngle = Math.PI / 6;
        
        // Calculate arrow points
        double x1 = toX - arrowLength * Math.cos(angle - arrowAngle);
        double y1 = toY - arrowLength * Math.sin(angle - arrowAngle);
        double x2 = toX - arrowLength * Math.cos(angle + arrowAngle);
        double y2 = toY - arrowLength * Math.sin(angle + arrowAngle);
        
        // Draw arrow
        g2d.drawLine((int) toX, (int) toY, (int) x1, (int) y1);
        g2d.drawLine((int) toX, (int) toY, (int) x2, (int) y2);
    }
    
    private void drawNodes(Graphics2D g2d) {
        if (flowDiagram == null) return;
        
        for (FlowNode node : flowDiagram.getNodes()) {
            drawNode(g2d, node);
        }
    }
    
    private void drawNode(Graphics2D g2d, FlowNode node) {
        int x = (int) node.getX();
        int y = (int) node.getY();
        
        // Determine node color based on state
        Color nodeColor = NODE_COLOR;
        if (node == editingNode) {
            nodeColor = NODE_EDITING_COLOR;
        } else if (node.isSelected()) {
            nodeColor = NODE_SELECTED_COLOR;
        }
        
        // Draw node background
        g2d.setColor(nodeColor);
        g2d.fillRoundRect(x, y, NODE_WIDTH, NODE_HEIGHT, 8, 8);
        
        // Draw node border
        g2d.setColor(nodeColor.brighter());
        g2d.setStroke(new BasicStroke(1f));
        g2d.drawRoundRect(x, y, NODE_WIDTH, NODE_HEIGHT, 8, 8);
        
        // Draw subflow indicator if present
        if (node.hasSubFlow()) {
            g2d.setColor(SUBFLOW_INDICATOR_COLOR);
            g2d.fillOval(x + NODE_WIDTH - 12, y + 4, 8, 8);
        }
        
        // Draw node text
        g2d.setColor(TEXT_COLOR);
        g2d.setFont(MONO_FONT);
        
        String text = node == editingNode ? editingText : node.getText();
        if (text == null || text.isEmpty()) {
            // Only show "New Node" placeholder when not editing
            text = node == editingNode ? "" : "New Node";
        }
        
        // Center text in node
        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(text);
        int textHeight = fm.getHeight();
        
        int textX = x + (NODE_WIDTH - textWidth) / 2;
        int textY = y + (NODE_HEIGHT + textHeight) / 2 - fm.getDescent();
        
        g2d.drawString(text, textX, textY);
        
        // Draw note indicator if present
        if (node.getNotes() != null && !node.getNotes().trim().isEmpty()) {
            g2d.setColor(TEXT_COLOR.darker());
            g2d.setFont(MONO_FONT.deriveFont(8f));
            g2d.drawString("N", x + 4, y + NODE_HEIGHT - 4);
        }
    }
    
    // Public methods for interaction
    public void createNode() {
        // Save current editing before creating new node
        if (editingNode != null) {
            System.out.println("FlowCanvas.createNode: Auto-saving current edit before creating new node");
            finishEditingNode();
        }
        
        System.out.println("createNode() called");
        
        if (flowDiagram == null) {
            System.out.println("ERROR: flowDiagram is null");
            return;
        }
        
        System.out.println("Creating new node in diagram: " + flowDiagram.getName());
        
        FlowNode newNode = new FlowNode();
        
        // Position new node
        FlowNode selectedNode = flowDiagram.getSelectedNode();
        if (selectedNode != null) {
            System.out.println("Positioning below selected node: " + selectedNode.getText());
            // Position below selected node
            newNode.setPosition((int)selectedNode.getX(), (int)(selectedNode.getY() + NODE_SPACING_Y));
            
            // Create connection from selected to new node
            flowDiagram.addConnection(selectedNode, newNode);
        } else {
            System.out.println("No selected node, positioning at origin");
            // Position at origin if no selection
            newNode.setPosition(CANVAS_MARGIN, CANVAS_MARGIN);
        }
        
        // Add to diagram
        flowDiagram.addNode(newNode);
        System.out.println("Node added to diagram. Total nodes: " + flowDiagram.getNodes().size());
        
        // Select and start editing the new node
        flowDiagram.selectNode(newNode);
        startEditingNode(newNode);
        
        // Force repaint with multiple methods
        invalidate();
        revalidate();
        repaint();
        System.out.println("Canvas repainted with invalidate/revalidate");
    }
    
    public void navigateNodes(int direction) {
        // Save current editing before navigating
        if (editingNode != null) {
            System.out.println("FlowCanvas.navigateNodes: Auto-saving current edit before navigation");
            finishEditingNode();
        }
        
        if (flowDiagram == null || flowDiagram.getNodes().isEmpty()) {
            System.out.println("FlowCanvas.navigateNodes: No flow or empty nodes");
            return;
        }
        
        FlowNode currentSelected = flowDiagram.getSelectedNode();
        List<FlowNode> nodes = flowDiagram.getNodes();
        
        if (currentSelected == null) {
            System.out.println("FlowCanvas.navigateNodes: No selected node, selecting first");
            // Select first node
            flowDiagram.selectNode(nodes.get(0));
            repaint();
            return;
        }
        
        System.out.println("FlowCanvas.navigateNodes: Finding next from " + currentSelected.getText() + " in direction " + direction);
        
        // Find next node based on direction
        FlowNode nextNode = findNextNode(currentSelected, direction);
        if (nextNode != null) {
            System.out.println("FlowCanvas.navigateNodes: Selected " + nextNode.getText());
            flowDiagram.selectNode(nextNode);
            repaint();
        } else {
            System.out.println("FlowCanvas.navigateNodes: No next node found");
        }
    }
    
    public void startEditingSelectedNode() {
        if (flowDiagram == null) {
            System.out.println("FlowCanvas.startEditingSelectedNode: No flow diagram");
            return;
        }
        
        FlowNode selectedNode = flowDiagram.getSelectedNode();
        if (selectedNode == null) {
            System.out.println("FlowCanvas.startEditingSelectedNode: No selected node");
            return;
        }
        
        System.out.println("FlowCanvas.startEditingSelectedNode: Starting to edit " + selectedNode.getText());
        startEditingNode(selectedNode);
    }
    
    public boolean isEditingNode() {
        return editingNode != null;
    }
    
    private FlowNode findNextNode(FlowNode current, int direction) {
        if (flowDiagram == null) return null;
        
        List<FlowNode> nodes = flowDiagram.getNodes();
        FlowNode closest = null;
        double minDistance = Double.MAX_VALUE;
        
        double currentX = current.getX();
        double currentY = current.getY();
        
        for (FlowNode node : nodes) {
            if (node == current) continue;
            
            double nodeX = node.getX();
            double nodeY = node.getY();
            
            boolean validDirection = false;
            double distance = 0;
            
            switch (direction) {
                case KeyEvent.VK_UP:
                    validDirection = nodeY < currentY;
                    distance = Math.sqrt(Math.pow(nodeX - currentX, 2) + Math.pow(nodeY - currentY, 2));
                    break;
                case KeyEvent.VK_DOWN:
                    validDirection = nodeY > currentY;
                    distance = Math.sqrt(Math.pow(nodeX - currentX, 2) + Math.pow(nodeY - currentY, 2));
                    break;
                case KeyEvent.VK_LEFT:
                    validDirection = nodeX < currentX;
                    distance = Math.sqrt(Math.pow(nodeX - currentX, 2) + Math.pow(nodeY - currentY, 2));
                    break;
                case KeyEvent.VK_RIGHT:
                    validDirection = nodeX > currentX;
                    distance = Math.sqrt(Math.pow(nodeX - currentX, 2) + Math.pow(nodeY - currentY, 2));
                    break;
            }
            
            if (validDirection && distance < minDistance) {
                minDistance = distance;
                closest = node;
            }
        }
        
        return closest;
    }
    
    private void startEditingNode(FlowNode node) {
        System.out.println("FlowCanvas.startEditingNode: Starting edit for node: '" + node.getText() + "'");
        editingNode = node;
        // Start with current node text to allow editing existing text
        editingText = node.getText() != null ? node.getText() : "";
        System.out.println("FlowCanvas.startEditingNode: editingText initialized to: '" + editingText + "'");
        
        // Add key listener for text editing
        requestFocusInWindow();
    }
    
    public void finishEditingNode() {
        if (editingNode != null) {
            System.out.println("FlowCanvas.finishEditingNode: Finishing edit for node with text: '" + editingText + "'");
            // If user leaves text empty, set it to empty string (not "New Node")
            if (editingText.trim().isEmpty()) {
                System.out.println("FlowCanvas.finishEditingNode: Setting empty text");
                editingNode.setText("");
            } else {
                System.out.println("FlowCanvas.finishEditingNode: Setting text to: '" + editingText + "'");
                editingNode.setText(editingText);
            }
            editingNode = null;
            editingText = "";
            repaint();
        }
    }
    
    // Mouse event handlers
    @Override
    public void mouseClicked(MouseEvent e) {
        if (flowDiagram == null) return;
        
        // Transform mouse coordinates
        Point2D.Double worldPos = screenToWorld(e.getPoint());
        
        // Find clicked node
        FlowNode clickedNode = findNodeAt(worldPos);
        
        // Auto-save if clicking on different node or empty space during editing
        if (editingNode != null && clickedNode != editingNode) {
            System.out.println("FlowCanvas.mouseClicked: Auto-saving current edit due to click elsewhere");
            finishEditingNode();
        }
        
        if (clickedNode != null) {
            flowDiagram.selectNode(clickedNode);
            
            if (e.getClickCount() == 2) {
                // Double-click to edit
                startEditingNode(clickedNode);
            }
        } else {
            // Click on empty space - deselect
            flowDiagram.selectNode(null);
        }
        
        repaint();
        requestFocusInWindow();
    }
    
    // In mousePressed
    @Override
    public void mousePressed(MouseEvent e) {
        lastMousePos = e.getPoint();
        Point2D.Double worldPos = screenToWorld(e.getPoint());
        FlowNode clickedNode = findNodeAt(worldPos);
        if (clickedNode != null && clickedNode.isSelected()) {
            draggingNode = clickedNode;
        } else {
            draggingNode = null;
        }
        requestFocusInWindow();
    }
    
    // In mouseDragged
    @Override
    public void mouseDragged(MouseEvent e) {
        if (lastMousePos == null) return;
        
        if (draggingNode != null) {
            // Calculate movement delta in screen coordinates
            int screenDx = e.getX() - lastMousePos.x;
            int screenDy = e.getY() - lastMousePos.y;
            
            // Convert to world coordinates (accounting for zoom)
            int worldDx = (int)(screenDx / zoomLevel);
            int worldDy = (int)(screenDy / zoomLevel);
            
            // Apply movement to the node
            int newX = (int)draggingNode.getX() + worldDx;
            int newY = (int)draggingNode.getY() + worldDy;
            
            // Apply canvas bounds
            newX = Math.max(10, Math.min(newX, 2000 - 150));
            newY = Math.max(10, Math.min(newY, 1500 - 80));
            
            if (!wouldOverlap(draggingNode, newX, newY)) {
                draggingNode.setPosition(newX, newY);
                repaint();
            }
        } else {
            // Pan the view
            int dx = e.getX() - lastMousePos.x;
            int dy = e.getY() - lastMousePos.y;
            viewOffset.x += dx;
            viewOffset.y += dy;
            repaint();
        }
        
        lastMousePos = e.getPoint();
        dragging = true;
    }
    
    // In mouseReleased
    @Override
    public void mouseReleased(MouseEvent e) {
        dragging = false;
        draggingNode = null;
    }
    
    // New method
    public boolean wouldOverlap(FlowNode movingNode, int newX, int newY) {
        for (FlowNode node : flowDiagram.getNodes()) {
            if (node == movingNode) continue;
            int minDistance = 10; // Minimum distance between nodes
            boolean overlapX = Math.abs(newX - node.getX()) < NODE_WIDTH + minDistance;
            boolean overlapY = Math.abs(newY - node.getY()) < NODE_HEIGHT + minDistance;
            if (overlapX && overlapY) return true;
        }
        return false;
    }
    
    @Override
    public void mouseEntered(MouseEvent e) {
        // Not used
    }
    
    @Override
    public void mouseExited(MouseEvent e) {
        // Not used
    }
    
    @Override
    public void mouseMoved(MouseEvent e) {
        // Not used
    }
    
    // Helper methods
    private Point2D.Double screenToWorld(Point screenPoint) {
        double worldX = (screenPoint.x - viewOffset.x) / zoomLevel;
        double worldY = (screenPoint.y - viewOffset.y) / zoomLevel;
        return new Point2D.Double(worldX, worldY);
    }
    
    private FlowNode findNodeAt(Point2D.Double worldPos) {
        if (flowDiagram == null) return null;
        
        for (FlowNode node : flowDiagram.getNodes()) {
            double x = node.getX();
            double y = node.getY();
            
            if (worldPos.x >= x && worldPos.x <= x + NODE_WIDTH &&
                worldPos.y >= y && worldPos.y <= y + NODE_HEIGHT) {
                return node;
            }
        }
        
        return null;
    }
    
    // Handle text input for editing
    public void handleKeyTyped(char keyChar) {
        if (editingNode != null) {
            System.out.println("FlowCanvas.handleKeyTyped: Processing key '" + keyChar + "' (code: " + (int)keyChar + ")");
            if (keyChar == '\b') { // Backspace
                if (!editingText.isEmpty()) {
                    editingText = editingText.substring(0, editingText.length() - 1);
                    System.out.println("FlowCanvas.handleKeyTyped: After backspace, editingText: '" + editingText + "'");
                }
            } else if (keyChar == '\n' || keyChar == '\r') { // Enter
                System.out.println("FlowCanvas.handleKeyTyped: Enter pressed, finishing edit");
                finishEditingNode();
            } else if (Character.isISOControl(keyChar)) {
                // Ignore other control characters
                System.out.println("FlowCanvas.handleKeyTyped: Ignoring control character");
                return;
            } else {
                editingText += keyChar;
                System.out.println("FlowCanvas.handleKeyTyped: After adding char, editingText: '" + editingText + "'");
            }
            repaint();
        }
    }
    
    public void handleEscapeKey() {
        if (editingNode != null) {
            // Cancel editing - node text remains unchanged
            editingNode = null;
            editingText = "";
            repaint();
        }
    }
    
    // KeyListener implementation
    @Override
    public void keyPressed(KeyEvent e) {
        // Only handle text editing when actively editing a node
        if (editingNode != null) {
            // Let text editing handle the key
            return;
        }
        
        // Otherwise, delegate to parent window for navigation and other keys
        Container parent = getParent();
        while (parent != null) {
            if (parent instanceof KeyListener) {
                ((KeyListener) parent).keyPressed(e);
                return;
            }
            parent = parent.getParent();
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
        if (editingNode != null) {
            handleKeyTyped(e.getKeyChar());
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        // Not used
    }
    
    // FocusListener implementation
    @Override
    public void focusGained(FocusEvent e) {
        // Not used - no special action needed when gaining focus
        System.out.println("FlowCanvas.focusGained: Canvas gained focus");
    }
    
    @Override
    public void focusLost(FocusEvent e) {
        // Auto-save when losing focus during editing
        if (editingNode != null) {
            System.out.println("FlowCanvas.focusLost: Auto-saving current edit due to focus loss");
            finishEditingNode();
        }
    }
}