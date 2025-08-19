package com.sap.flowdeconstruct.ui.components;

import com.sap.flowdeconstruct.model.FlowConnection;
import com.sap.flowdeconstruct.model.FlowDiagram;
import com.sap.flowdeconstruct.model.FlowNode;
import com.sap.flowdeconstruct.ui.dialogs.ConnectionDialog;

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
        // Calculate connection points using side midpoints based on relative positions
        Point2D.Double fromPt = getAnchorPoint(fromNode, toNode);
        Point2D.Double toPt = getAnchorPoint(toNode, fromNode);
        
        // Draw base line
        g2d.drawLine((int) fromPt.x, (int) fromPt.y, (int) toPt.x, (int) toPt.y);
        
        // Draw arrowheads based on direction style
        FlowConnection.DirectionStyle dir = connection.getDirectionStyle();
        if (dir == null) dir = FlowConnection.DirectionStyle.FROM_TO;
        switch (dir) {
            case FROM_TO:
                drawArrowHead(g2d, fromPt.x, fromPt.y, toPt.x, toPt.y);
                break;
            case TO_FROM:
                drawArrowHead(g2d, toPt.x, toPt.y, fromPt.x, fromPt.y);
                break;
            case BIDIRECTIONAL:
                drawArrowHead(g2d, fromPt.x, fromPt.y, toPt.x, toPt.y);
                drawArrowHead(g2d, toPt.x, toPt.y, fromPt.x, fromPt.y);
                break;
            case NONE:
                // no arrowheads
                break;
        }
        
        // Draw protocol label at the midpoint if provided
        String protocol = connection.getProtocol();
        if (protocol != null && !protocol.isEmpty()) {
            double midX = (fromPt.x + toPt.x) / 2.0;
            double midY = (fromPt.y + toPt.y) / 2.0;
            drawProtocolLabel(g2d, protocol, (int) midX, (int) midY);
        }
    }
    
    private void drawArrowHead(Graphics2D g2d, double fromX, double fromY, double toX, double toY) {
        double angle = Math.atan2(toY - fromY, toX - fromX);
        double arrowLength = 8;
        double arrowAngle = Math.PI / 6;
        
        double x1 = toX - arrowLength * Math.cos(angle - arrowAngle);
        double y1 = toY - arrowLength * Math.sin(angle - arrowAngle);
        double x2 = toX - arrowLength * Math.cos(angle + arrowAngle);
        double y2 = toY - arrowLength * Math.sin(angle + arrowAngle);
        
        g2d.drawLine((int) toX, (int) toY, (int) x1, (int) y1);
        g2d.drawLine((int) toX, (int) toY, (int) x2, (int) y2);
    }

    // Helper to choose best side midpoint as anchor based on relative vector
    private Point2D.Double getAnchorPoint(FlowNode node, FlowNode other) {
        double centerX = node.getX() + NODE_WIDTH / 2.0;
        double centerY = node.getY() + NODE_HEIGHT / 2.0;
        double dx = (other.getX() + NODE_WIDTH / 2.0) - centerX;
        double dy = (other.getY() + NODE_HEIGHT / 2.0) - centerY;
        if (Math.abs(dx) > Math.abs(dy)) {
            // Connect horizontally
            if (dx > 0) {
                // Right side
                return new Point2D.Double(node.getX() + NODE_WIDTH, centerY);
            } else {
                // Left side
                return new Point2D.Double(node.getX(), centerY);
            }
        } else {
            // Connect vertically
            if (dy > 0) {
                // Bottom side
                return new Point2D.Double(centerX, node.getY() + NODE_HEIGHT);
            } else {
                // Top side
                return new Point2D.Double(centerX, node.getY());
            }
        }
    }

    private void drawProtocolLabel(Graphics2D g2d, String text, int x, int y) {
        Font original = g2d.getFont();
        g2d.setFont(MONO_FONT.deriveFont(11f));
        FontMetrics fm = g2d.getFontMetrics();
        int paddingX = 6;
        int paddingY = 3;
        int textW = fm.stringWidth(text);
        int textH = fm.getAscent();
        int boxW = textW + paddingX * 2;
        int boxH = textH + paddingY * 2;
        int boxX = x - boxW / 2;
        int boxY = y - boxH / 2;
        
        // Background box
        g2d.setColor(new Color(0x3a, 0x3a, 0x3a));
        g2d.fillRect(boxX, boxY, boxW, boxH);
        // Border
        g2d.setColor(NODE_SELECTED_COLOR.darker());
        g2d.drawRect(boxX, boxY, boxW, boxH);
        // Text
        g2d.setColor(TEXT_COLOR);
        g2d.drawString(text, boxX + paddingX, boxY + paddingY + fm.getAscent() - fm.getDescent());
        g2d.setFont(original);
    }

    // Distance from point P to segment AB
    private double distancePointToSegment(Point2D.Double p, Point2D.Double a, Point2D.Double b) {
        double vx = b.x - a.x, vy = b.y - a.y;
        double wx = p.x - a.x, wy = p.y - a.y;
        double c1 = vx * wx + vy * wy;
        if (c1 <= 0) return p.distance(a);
        double c2 = vx * vx + vy * vy;
        if (c2 <= c1) return p.distance(b);
        double t = c1 / c2;
        double projX = a.x + t * vx;
        double projY = a.y + t * vy;
        double dx = p.x - projX, dy = p.y - projY;
        return Math.hypot(dx, dy);
    }

    private FlowConnection findConnectionAt(Point2D.Double worldPos) {
        if (flowDiagram == null) return null;
        double tolerance = 6.0 / Math.max(zoomLevel, 0.0001); // keep ~6px tolerance irrespective of zoom
        FlowConnection closest = null;
        double best = Double.MAX_VALUE;
        for (FlowConnection c : flowDiagram.getConnections()) {
            FlowNode from = flowDiagram.findNodeById(c.getFromNodeId());
            FlowNode to = flowDiagram.findNodeById(c.getToNodeId());
            if (from == null || to == null) continue;
            Point2D.Double a = getAnchorPoint(from, to);
            Point2D.Double b = getAnchorPoint(to, from);
            double d = distancePointToSegment(worldPos, a, b);
            if (d < tolerance && d < best) {
                best = d;
                closest = c;
            }
        }
        return closest;
    }
    
    @Override
    public void mouseClicked(MouseEvent e) {
        if (flowDiagram == null) return;
        
        Point2D.Double worldPos = screenToWorld(e.getPoint());
        FlowNode clickedNode = findNodeAt(worldPos);
        
        if (editingNode != null && clickedNode != editingNode) {
            System.out.println("FlowCanvas.mouseClicked: Auto-saving current edit due to click elsewhere");
            finishEditingNode();
        }
        
        if (clickedNode != null) {
            flowDiagram.selectNode(clickedNode);
            
            if (e.getClickCount() == 2) {
                startEditingNode(clickedNode);
            }
        } else {
            // Try clicking on a connection
            FlowConnection clickedConn = findConnectionAt(worldPos);
            if (clickedConn != null) {
                // Open connection edit dialog
                Window w = SwingUtilities.getWindowAncestor(this);
                Frame parent = (w instanceof Frame) ? (Frame) w : null;
                ConnectionDialog dialog = new ConnectionDialog(parent, clickedConn);
                dialog.setVisible(true);
                if (dialog.isConfirmed()) {
                    clickedConn.setDirectionStyle(dialog.getSelectedDirectionStyle());
                    clickedConn.setProtocol(dialog.getProtocol());
                    repaint();
                }
            } else {
                // Click on empty space - deselect
                flowDiagram.selectNode(null);
            }
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
    
        if (clickedNode != null) {
            // Se estava editando outro nó, finalizar a edição antes de mudar a seleção
            if (editingNode != null && clickedNode != editingNode) {
                finishEditingNode();
            }
            // Seleciona o nó clicado (mesmo que ainda não estivesse selecionado)
            if (flowDiagram != null) {
                flowDiagram.selectNode(clickedNode);
            }
            // Permite arrastar imediatamente após pressionar sobre o nó
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
    
    // Methods required by MainWindow
    public boolean isEditingNode() {
        return editingNode != null;
    }

    public void startEditingSelectedNode() {
        if (flowDiagram != null && flowDiagram.getSelectedNode() != null) {
            startEditingNode(flowDiagram.getSelectedNode());
        }
    }

    private void startEditingNode(FlowNode node) {
        if (node == null) return;
        finishEditingNode(); // Finish any current editing
        editingNode = node;
        editingText = node.getText();
        repaint();
    }

    public void finishEditingNode() {
        if (editingNode != null) {
            editingNode.setText(editingText);
            editingNode = null;
            editingText = "";
            repaint();
        }
    }

    public void createNode() {
        if (flowDiagram == null) return;
        
        FlowNode selectedNode = flowDiagram.getSelectedNode();
        FlowNode newNode;
        
        if (selectedNode != null) {
            // Create new node and connect to selected node
            newNode = flowDiagram.addNode("New Node", 
                (int)selectedNode.getX() + NODE_SPACING_X,
                (int)selectedNode.getY());
            flowDiagram.addConnection(selectedNode, newNode);
        } else {
            // Create first node or new isolated node
            newNode = flowDiagram.addNode("New Node");
        }
        
        flowDiagram.selectNode(newNode);
        startEditingNode(newNode);
        repaint();
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
        
        // Determine node state
        boolean isSelected = node.isSelected();
        boolean isEditing = (editingNode == node);
        
        // Choose colors based on state
        Color bgColor = NODE_COLOR;
        if (isEditing) {
            bgColor = NODE_EDITING_COLOR;
        } else if (isSelected) {
            bgColor = NODE_SELECTED_COLOR;
        }
        
        // Draw node background
        g2d.setColor(bgColor);
        g2d.fillRect(x, y, NODE_WIDTH, NODE_HEIGHT);
        
        // Draw border
        g2d.setColor(isSelected ? NODE_SELECTED_COLOR.brighter() : CONNECTION_COLOR);
        g2d.drawRect(x, y, NODE_WIDTH, NODE_HEIGHT);
        
        // Draw text
        g2d.setColor(TEXT_COLOR);
        g2d.setFont(MONO_FONT);
        FontMetrics fm = g2d.getFontMetrics();
        
        String text = isEditing ? editingText : node.getText();
        if (text == null) text = "";
        
        // Simple text centering
        int textX = x + (NODE_WIDTH - fm.stringWidth(text)) / 2;
        int textY = y + (NODE_HEIGHT + fm.getAscent()) / 2 - fm.getDescent();
        
        g2d.drawString(text, textX, textY);
        
        // Draw subflow indicator if node has subflow
        if (node.hasSubFlow()) {
            g2d.setColor(SUBFLOW_INDICATOR_COLOR);
            g2d.fillOval(x + NODE_WIDTH - 12, y + 4, 8, 8);
        }
        
        // Draw notes indicator if node has notes
        if (node.hasNotes()) {
            g2d.setColor(TEXT_COLOR.darker());
            g2d.fillRect(x + NODE_WIDTH - 8, y + NODE_HEIGHT - 8, 4, 4);
        }
        
        // Draw editing cursor if this node is being edited
        if (isEditing) {
            long time = System.currentTimeMillis();
            if ((time / 500) % 2 == 0) { // Blink every 500ms
                int cursorX = textX + fm.stringWidth(editingText);
                g2d.drawLine(cursorX, textY - fm.getAscent(), cursorX, textY);
            }
        }
    }
}