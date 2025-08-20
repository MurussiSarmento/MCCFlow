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
import java.util.ArrayList;

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
    private static final String DEFAULT_NODE_TEXT = "New Node";
    
    private FlowDiagram flowDiagram;
    private FlowNode editingNode;
    private String editingText = "";
    
    // Canvas state
    private Point2D.Double viewOffset = new Point2D.Double(0, 0);
    private double zoomLevel = 1.0;
    private Point lastMousePos;
    private boolean dragging = false;
    private FlowNode draggingNode = null;
    private FlowConnection selectedConnection = null;
    
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
            // Auto-layout only if all nodes are unpositioned (at 0,0)
            if (allNodesUnpositioned()) {
                autoLayoutNodes();
            }
            
            // Add listener for diagram changes
            diagram.addStateListener((d, event, oldValue, newValue) -> {
                System.out.println("FlowCanvas: Diagram event: " + event);
                SwingUtilities.invokeLater(() -> {
                    // Avoid resetting user-arranged positions. Only auto-layout if all nodes are still unpositioned.
                    if (("nodeAdded".equals(event) || "nodeRemoved".equals(event)) && allNodesUnpositioned()) {
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
    
    // Returns true if there is at least one node and all nodes are still at the origin (0,0)
    private boolean allNodesUnpositioned() {
        if (flowDiagram == null) return false;
        List<FlowNode> nodes = flowDiagram.getNodes();
        if (nodes.isEmpty()) return false;
        for (FlowNode n : nodes) {
            if (!(n.getX() == 0 && n.getY() == 0)) {
                return false;
            }
        }
        return true;
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
        
        for (FlowConnection connection : flowDiagram.getConnections()) {
            FlowNode fromNode = flowDiagram.findNodeById(connection.getFromNodeId());
            FlowNode toNode = flowDiagram.findNodeById(connection.getToNodeId());
            if (fromNode != null && toNode != null) {
                drawConnection(g2d, fromNode, toNode, connection);
            }
        }
    }
    
    private void drawConnection(Graphics2D g2d, FlowNode fromNode, FlowNode toNode, FlowConnection connection) {
        Point2D.Double fromPoint = getAnchorPoint(fromNode, toNode);
        Point2D.Double toPoint = getAnchorPoint(toNode, fromNode);
        
        boolean isSelected = (connection == selectedConnection);
        // Draw main line with customizable color
        Color baseLineColor = parseHexColor(connection.getLineColorHex(), CONNECTION_COLOR);
        Color lineColor = isSelected ? SUBFLOW_INDICATOR_COLOR : baseLineColor;
        g2d.setColor(lineColor);
        g2d.setStroke(new BasicStroke(isSelected ? 3f : 2f));
        g2d.drawLine((int) fromPoint.x, (int) fromPoint.y, (int) toPoint.x, (int) toPoint.y);
        
        // Draw arrowhead(s) according to direction style with customizable color
        Color baseArrowColor = parseHexColor(connection.getArrowColorHex(), baseLineColor);
        Color arrowColor = isSelected ? SUBFLOW_INDICATOR_COLOR : baseArrowColor;
        FlowConnection.DirectionStyle ds = connection.getDirectionStyle();
        if (ds != null) {
            switch (ds) {
                case FROM_TO:
                    g2d.setColor(arrowColor);
                    drawArrowHead(g2d, fromPoint.x, fromPoint.y, toPoint.x, toPoint.y);
                    break;
                case TO_FROM:
                    g2d.setColor(arrowColor);
                    drawArrowHead(g2d, toPoint.x, toPoint.y, fromPoint.x, fromPoint.y);
                    break;
                case BIDIRECTIONAL:
                    g2d.setColor(arrowColor);
                    drawArrowHead(g2d, fromPoint.x, fromPoint.y, toPoint.x, toPoint.y);
                    drawArrowHead(g2d, toPoint.x, toPoint.y, fromPoint.x, fromPoint.y);
                    break;
                case NONE:
                    // no arrowheads
                    break;
            }
        }
        
        // Draw protocol label if exists
        if (connection.getProtocol() != null && !connection.getProtocol().trim().isEmpty()) {
            int labelX = (int) ((fromPoint.x + toPoint.x) / 2);
            int labelY = (int) ((fromPoint.y + toPoint.y) / 2) - 5;
            drawProtocolLabel(g2d, connection.getProtocol(), labelX, labelY);
        }
    }
    
    private void drawArrowHead(Graphics2D g2d, double fromX, double fromY, double toX, double toY) {
        double dx = toX - fromX;
        double dy = toY - fromY;
        double angle = Math.atan2(dy, dx);
        int size = 8;
        
        int x1 = (int) (toX - size * Math.cos(angle - Math.PI / 6));
        int y1 = (int) (toY - size * Math.sin(angle - Math.PI / 6));
        int x2 = (int) (toX - size * Math.cos(angle + Math.PI / 6));
        int y2 = (int) (toY - size * Math.sin(angle + Math.PI / 6));
        
        Polygon arrowHead = new Polygon();
        arrowHead.addPoint((int) toX, (int) toY);
        arrowHead.addPoint(x1, y1);
        arrowHead.addPoint(x2, y2);
        
        g2d.fillPolygon(arrowHead);
    }

    private Point2D.Double getAnchorPoint(FlowNode node, FlowNode other) {
        double x = node.getX();
        double y = node.getY();
        double otherX = other.getX();
        
        // If the other node is to the right, use the right edge, otherwise use the left edge
        double anchorX = otherX > x ? x + NODE_WIDTH : x;
        double anchorY = y + NODE_HEIGHT / 2.0;
        
        return new Point2D.Double(anchorX, anchorY);
    }

    private void drawProtocolLabel(Graphics2D g2d, String text, int x, int y) {
        g2d.setColor(TEXT_COLOR);
        g2d.setFont(MONO_FONT);
        
        FontMetrics fm = g2d.getFontMetrics();
        int width = fm.stringWidth(text) + 8;
        int height = fm.getHeight();
        
        // Background
        g2d.setColor(new Color(0x3a, 0x3a, 0x3a));
        g2d.fillRoundRect(x - width / 2, y - height + 3, width, height, 8, 8);
        
        // Border
        g2d.setColor(new Color(0x5f, 0x9e, 0xa0));
        g2d.drawRoundRect(x - width / 2, y - height + 3, width, height, 8, 8);
        
        // Text
        g2d.setColor(TEXT_COLOR);
        g2d.drawString(text, x - width / 2 + 4, y);
    }

    private double distancePointToSegment(Point2D.Double p, Point2D.Double a, Point2D.Double b) {
        double abx = b.x - a.x;
        double aby = b.y - a.y;
        double apx = p.x - a.x;
        double apy = p.y - a.y;
        double ab2 = abx * abx + aby * aby;
        double ap_ab = apx * abx + apy * aby;
        double t = Math.max(0, Math.min(1, ap_ab / ab2));
        double closestX = a.x + abx * t;
        double closestY = a.y + aby * t;
        double dx = p.x - closestX;
        double dy = p.y - closestY;
        return Math.sqrt(dx * dx + dy * dy);
    }

    private FlowConnection findConnectionAt(Point2D.Double worldPos) {
        if (flowDiagram == null) return null;
        
        for (FlowConnection connection : flowDiagram.getConnections()) {
            FlowNode fromNode = flowDiagram.findNodeById(connection.getFromNodeId());
            FlowNode toNode = flowDiagram.findNodeById(connection.getToNodeId());
            if (fromNode == null || toNode == null) continue;
            
            Point2D.Double fromPoint = getAnchorPoint(fromNode, toNode);
            Point2D.Double toPoint = getAnchorPoint(toNode, fromNode);
            
            double distance = distancePointToSegment(worldPos, fromPoint, toPoint);
            if (distance < 6.0) {
                return connection;
            }
        }
        
        return null;
    }
    
    @Override
    public void mouseClicked(MouseEvent e) {
        requestFocusInWindow();
        if (flowDiagram == null) return;
        
        Point2D.Double worldPos = screenToWorld(e.getPoint());
        FlowNode clickedNode = findNodeAt(worldPos);
        FlowConnection clickedConnection = null;
        if (clickedNode == null) {
            clickedConnection = findConnectionAt(worldPos);
        }
        
        if (clickedNode != null) {
            selectedConnection = null; // clear connection selection when clicking a node
            if (editingNode != null && clickedNode != editingNode) {
                // Finish editing previous node when clicking a different node
                finishEditingNode();
            } else if (e.getClickCount() == 2) {
                // Double-click to edit
                startEditingNode(clickedNode);
            } else {
                // Single-click to select
                flowDiagram.selectNode(clickedNode);
                repaint();
            }
        } else if (clickedConnection != null) {
            // Selecting a connection
            flowDiagram.selectNode(null); // clear node selection
            selectedConnection = clickedConnection;
            if (e.getClickCount() == 2) {
                openConnectionDialog(clickedConnection);
            }
            repaint();
        } else {
            // Clicked empty space: finish editing if any and clear selections
            if (editingNode != null) {
                finishEditingNode();
            }
            selectedConnection = null;
            flowDiagram.selectNode(null);
            repaint();
        }
    }
    
    @Override
    public void mousePressed(MouseEvent e) {
        requestFocusInWindow();
        if (flowDiagram == null) return;
        
        // Show node context menu first if applicable
        if (maybeShowNodePopup(e)) return;
        // Show connection context menu if applicable
        if (maybeShowConnectionPopup(e)) return;
        
        Point2D.Double worldPos = screenToWorld(e.getPoint());
        FlowNode clickedNode = findNodeAt(worldPos);
        if (clickedNode != null) {
            dragging = true;
            draggingNode = clickedNode;
            lastMousePos = e.getPoint();
        } else {
            // Start panning
            lastMousePos = e.getPoint();
        }
    }
    
    @Override
    public void mouseDragged(MouseEvent e) {
        if (flowDiagram == null) return;
        
        if (dragging && draggingNode != null) {
            Point2D.Double worldPos = screenToWorld(e.getPoint());
            int newX = (int) (worldPos.x - NODE_WIDTH / 2);
            int newY = (int) (worldPos.y - NODE_HEIGHT / 2);
            
            // Prevent overlapping too tightly
            if (!wouldOverlap(draggingNode, newX, newY)) {
                draggingNode.setPosition(newX, newY);
                repaint();
            }
        } else if (lastMousePos != null) {
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
        // Show node context menu first if applicable
        if (maybeShowNodePopup(e)) {
            dragging = false;
            draggingNode = null;
            return;
        }
        // Show connection context menu if applicable
        if (maybeShowConnectionPopup(e)) {
            dragging = false;
            draggingNode = null;
            return;
        }
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
    
    @Override
    public void focusGained(FocusEvent e) {
        // Optional: could show focus state
    }
    
    @Override
    public void focusLost(FocusEvent e) {
        // Optional: could hide focus state
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
        String currentText = node.getText();
        editingText = DEFAULT_NODE_TEXT.equals(currentText) ? "" : currentText;
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
            newNode = flowDiagram.addNode(DEFAULT_NODE_TEXT, 
                (int)selectedNode.getX() + NODE_SPACING_X,
                (int)selectedNode.getY());
            flowDiagram.addConnection(selectedNode, newNode);
        } else {
            // Create first node or new isolated node with a sensible position
            List<FlowNode> nodes = flowDiagram.getNodes();
            if (nodes.isEmpty()) {
                newNode = flowDiagram.addNode(DEFAULT_NODE_TEXT, CANVAS_MARGIN, CANVAS_MARGIN);
            } else {
                int maxX = Integer.MIN_VALUE;
                int minY = Integer.MAX_VALUE;
                for (FlowNode n : nodes) {
                    maxX = Math.max(maxX, (int) n.getX());
                    minY = Math.min(minY, (int) n.getY());
                }
                newNode = flowDiagram.addNode(DEFAULT_NODE_TEXT, maxX + NODE_SPACING_X, minY);
            }
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
        
        boolean isSelected = flowDiagram.getSelectedNode() == node;
        boolean isEditing = (editingNode == node);
        
        // Colors
        Color fill = parseHexColor(node.getFillColorHex(), NODE_COLOR);
        if (isEditing) {
            fill = NODE_EDITING_COLOR;
        }
        Color border = isSelected ? SUBFLOW_INDICATOR_COLOR : parseHexColor(node.getBorderColorHex(), CONNECTION_COLOR);
        
        // Draw shape based on node shape
        FlowNode.NodeShape shape = node.getShape();
        g2d.setColor(fill);
        switch (shape) {
            case RECTANGLE:
                g2d.fillRoundRect(x, y, NODE_WIDTH, NODE_HEIGHT, 12, 12);
                g2d.setColor(border);
                g2d.drawRoundRect(x, y, NODE_WIDTH, NODE_HEIGHT, 12, 12);
                break;
            case SQUARE: {
                int side = Math.min(NODE_WIDTH, NODE_HEIGHT);
                g2d.fillRect(x, y, side, side);
                g2d.setColor(border);
                g2d.drawRect(x, y, side, side);
                break;
            }
            case OVAL:
                g2d.fillOval(x, y, NODE_WIDTH, NODE_HEIGHT);
                g2d.setColor(border);
                g2d.drawOval(x, y, NODE_WIDTH, NODE_HEIGHT);
                break;
            case CIRCLE: {
                int diameter = Math.min(NODE_WIDTH, NODE_HEIGHT);
                int cx = x + (NODE_WIDTH - diameter) / 2;
                int cy = y + (NODE_HEIGHT - diameter) / 2;
                g2d.fillOval(cx, cy, diameter, diameter);
                g2d.setColor(border);
                g2d.drawOval(cx, cy, diameter, diameter);
                break;
            }
            case DIAMOND: {
                int cx = x + NODE_WIDTH / 2;
                int cy = y + NODE_HEIGHT / 2;
                Polygon p = new Polygon(
                    new int[]{cx, x + NODE_WIDTH, cx, x},
                    new int[]{y, cy, y + NODE_HEIGHT, cy},
                    4
                );
                g2d.fillPolygon(p);
                g2d.setColor(border);
                g2d.drawPolygon(p);
                break;
            }
            default:
                g2d.fillRoundRect(x, y, NODE_WIDTH, NODE_HEIGHT, 12, 12);
                g2d.setColor(border);
                g2d.drawRoundRect(x, y, NODE_WIDTH, NODE_HEIGHT, 12, 12);
        }
        
        // Node text
        g2d.setColor(TEXT_COLOR);
        g2d.setFont(MONO_FONT);
        
        String text = isEditing ? editingText : node.getText();
        FontMetrics fm = g2d.getFontMetrics();
        
        // Determine bounds for centering text based on shape actually drawn
        int boundsX = x;
        int boundsY = y;
        int boundsW = NODE_WIDTH;
        int boundsH = NODE_HEIGHT;
        switch (shape) {
            case SQUARE: {
                int side = Math.min(NODE_WIDTH, NODE_HEIGHT);
                boundsW = side;
                boundsH = side;
                break;
            }
            case CIRCLE: {
                int diameter = Math.min(NODE_WIDTH, NODE_HEIGHT);
                boundsW = diameter;
                boundsH = diameter;
                boundsX = x + (NODE_WIDTH - diameter) / 2;
                boundsY = y + (NODE_HEIGHT - diameter) / 2;
                break;
            }
            default:
                // RECTANGLE, OVAL, DIAMOND already use NODE_WIDTH x NODE_HEIGHT
                break;
        }
        
        int textX = boundsX + (boundsW - fm.stringWidth(text)) / 2;
        int textY = boundsY + (boundsH + fm.getAscent() - fm.getDescent()) / 2;
        g2d.drawString(text, textX, textY);
        
        // Subflow indicator
        if (node.hasSubFlow()) {
            int indicatorSize = 8;
            int indicatorX = x + NODE_WIDTH - indicatorSize - 6;
            int indicatorY = y + NODE_HEIGHT - indicatorSize - 6;
            g2d.setColor(SUBFLOW_INDICATOR_COLOR);
            g2d.fillOval(indicatorX, indicatorY, indicatorSize, indicatorSize);
        }
    }

    // ---------- Connection editing helpers ----------
    private void openConnectionDialog(FlowConnection connection) {
        Window window = SwingUtilities.getWindowAncestor(this);
        Frame owner = (window instanceof Frame) ? (Frame) window : null;
        ConnectionDialog dialog = new ConnectionDialog(owner, connection);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
        if (dialog.isConfirmed()) {
            connection.setDirectionStyle(dialog.getSelectedDirectionStyle());
            connection.setProtocol(dialog.getProtocol());
            // trigger diagram modified time and listeners
            if (flowDiagram != null) {
                List<FlowConnection> updated = new ArrayList<>(flowDiagram.getConnections());
                flowDiagram.setConnections(updated);
            }
            repaint();
        }
    }

    private void reverseDirection(FlowConnection connection) {
        FlowConnection.DirectionStyle ds = connection.getDirectionStyle();
        if (ds == FlowConnection.DirectionStyle.FROM_TO) {
            connection.setDirectionStyle(FlowConnection.DirectionStyle.TO_FROM);
        } else if (ds == FlowConnection.DirectionStyle.TO_FROM) {
            connection.setDirectionStyle(FlowConnection.DirectionStyle.FROM_TO);
        } else {
            // keep as is for BIDIRECTIONAL and NONE
        }
        if (flowDiagram != null) {
            List<FlowConnection> updated = new ArrayList<>(flowDiagram.getConnections());
            flowDiagram.setConnections(updated);
        }
        repaint();
    }

    private void showConnectionPopup(int x, int y, FlowConnection connection) {
        JPopupMenu popup = new JPopupMenu();
        JMenuItem edit = new JMenuItem("Edit Connection...");
        edit.addActionListener(ev -> openConnectionDialog(connection));
        popup.add(edit);

        JMenuItem reverse = new JMenuItem("Reverse Direction");
        reverse.addActionListener(ev -> reverseDirection(connection));
        popup.add(reverse);

        popup.addSeparator();
        JMenuItem dirFromTo = new JMenuItem("Direction: FROM → TO");
        dirFromTo.addActionListener(ev -> {
            connection.setDirectionStyle(FlowConnection.DirectionStyle.FROM_TO);
            if (flowDiagram != null) {
                List<FlowConnection> updated = new ArrayList<>(flowDiagram.getConnections());
                flowDiagram.setConnections(updated);
            }
            repaint();
        });
        popup.add(dirFromTo);

        JMenuItem dirToFrom = new JMenuItem("Direction: TO → FROM");
        dirToFrom.addActionListener(ev -> {
            connection.setDirectionStyle(FlowConnection.DirectionStyle.TO_FROM);
            if (flowDiagram != null) {
                List<FlowConnection> updated = new ArrayList<>(flowDiagram.getConnections());
                flowDiagram.setConnections(updated);
            }
            repaint();
        });
        popup.add(dirToFrom);

        JMenuItem dirBi = new JMenuItem("Direction: Bidirectional");
        dirBi.addActionListener(ev -> {
            connection.setDirectionStyle(FlowConnection.DirectionStyle.BIDIRECTIONAL);
            if (flowDiagram != null) {
                List<FlowConnection> updated = new ArrayList<>(flowDiagram.getConnections());
                flowDiagram.setConnections(updated);
            }
            repaint();
        });
        popup.add(dirBi);

        JMenuItem dirNone = new JMenuItem("Direction: None");
        dirNone.addActionListener(ev -> {
            connection.setDirectionStyle(FlowConnection.DirectionStyle.NONE);
            if (flowDiagram != null) {
                List<FlowConnection> updated = new ArrayList<>(flowDiagram.getConnections());
                flowDiagram.setConnections(updated);
            }
            repaint();
        });
        popup.add(dirNone);

        popup.addSeparator();
        JMenuItem lineColorItem = new JMenuItem("Change Line Color...");
        lineColorItem.addActionListener(ev -> {
            Color initial = parseHexColor(connection.getLineColorHex(), CONNECTION_COLOR);
            Color chosen = chooseColor("Select Line Color", initial);
            if (chosen != null) {
                connection.setLineColorHex(colorToHex(chosen));
                if (flowDiagram != null) {
                    List<FlowConnection> updated = new ArrayList<>(flowDiagram.getConnections());
                    flowDiagram.setConnections(updated);
                }
                repaint();
            }
        });
        popup.add(lineColorItem);

        JMenuItem arrowColorItem = new JMenuItem("Change Arrow Color...");
        arrowColorItem.addActionListener(ev -> {
            Color initial = parseHexColor(connection.getArrowColorHex(), CONNECTION_COLOR);
            Color chosen = chooseColor("Select Arrow Color", initial);
            if (chosen != null) {
                connection.setArrowColorHex(colorToHex(chosen));
                if (flowDiagram != null) {
                    List<FlowConnection> updated = new ArrayList<>(flowDiagram.getConnections());
                    flowDiagram.setConnections(updated);
                }
                repaint();
            }
        });
        popup.add(arrowColorItem);

        popup.show(this, x, y);
    }

    private boolean maybeShowConnectionPopup(MouseEvent e) {
        if (e.isPopupTrigger() || SwingUtilities.isRightMouseButton(e)) {
            Point2D.Double worldPos = screenToWorld(e.getPoint());
            FlowConnection conn = findConnectionAt(worldPos);
            if (conn != null) {
                selectedConnection = conn;
                showConnectionPopup(e.getX(), e.getY(), conn);
                return true;
            }
        }
        return false;
    }
    
    // Keyboard navigation between nodes (used by MainWindow)
    public void navigateNodes(int keyCode) {
        if (flowDiagram == null) return;
        java.util.List<FlowNode> nodes = flowDiagram.getNodes();
        if (nodes.isEmpty()) return;
    
        FlowNode current = flowDiagram.getSelectedNode();
        if (current == null) {
            flowDiagram.selectNode(nodes.get(0));
            repaint();
            return;
        }
    
        double cx = current.getX() + NODE_WIDTH / 2.0;
        double cy = current.getY() + NODE_HEIGHT / 2.0;
    
        FlowNode best = null;
        double bestDist = Double.MAX_VALUE;
        FlowNode fallback = null;
        double fallbackDist = Double.MAX_VALUE;
    
        for (FlowNode n : nodes) {
            if (n == current) continue;
            double nx = n.getX() + NODE_WIDTH / 2.0;
            double ny = n.getY() + NODE_HEIGHT / 2.0;
            double dx = nx - cx;
            double dy = ny - cy;
            double dist = Math.hypot(dx, dy);
    
            boolean inDirection = false;
            switch (keyCode) {
                case KeyEvent.VK_RIGHT:
                    inDirection = dx > 0;
                    break;
                case KeyEvent.VK_LEFT:
                    inDirection = dx < 0;
                    break;
                case KeyEvent.VK_UP:
                    inDirection = dy < 0;
                    break;
                case KeyEvent.VK_DOWN:
                    inDirection = dy > 0;
                    break;
                default:
                    return; // unsupported key
            }
    
            if (inDirection) {
                if (dist < bestDist) {
                    bestDist = dist;
                    best = n;
                }
            }
            // Always compute nearest for fallback
            if (dist < fallbackDist) {
                fallbackDist = dist;
                fallback = n;
            }
        }
    
        FlowNode target = (best != null) ? best : fallback;
        if (target != null) {
            flowDiagram.selectNode(target);
            repaint();
        }
    }

// Node popup for color and shape
private void showNodePopup(int x, int y, FlowNode node) {
    JPopupMenu popup = new JPopupMenu();

    JMenuItem fillColorItem = new JMenuItem("Mudar cor do preenchimento...");
    fillColorItem.addActionListener(ev -> {
        Color initial = parseHexColor(node.getFillColorHex(), NODE_COLOR);
        Color chosen = chooseColor("Selecione a cor de preenchimento", initial);
        if (chosen != null) {
            node.setFillColorHex(colorToHex(chosen));
            repaint();
        }
    });
    popup.add(fillColorItem);

    JMenuItem borderColorItem = new JMenuItem("Mudar cor da borda...");
    borderColorItem.addActionListener(ev -> {
        Color initial = parseHexColor(node.getBorderColorHex(), CONNECTION_COLOR);
        Color chosen = chooseColor("Selecione a cor da borda", initial);
        if (chosen != null) {
            node.setBorderColorHex(colorToHex(chosen));
            repaint();
        }
    });
    popup.add(borderColorItem);

    popup.addSeparator();
    JMenu shapeMenu = new JMenu("Forma");
    for (FlowNode.NodeShape s : FlowNode.NodeShape.values()) {
        JMenuItem item = new JMenuItem(s.name());
        item.addActionListener(ev -> {
            node.setShape(s);
            repaint();
        });
        shapeMenu.add(item);
    }
    popup.add(shapeMenu);

    JMenuItem resetColors = new JMenuItem("Resetar cores");
    resetColors.addActionListener(ev -> {
        node.setFillColorHex("#3a3a3a");
        node.setBorderColorHex("#666666");
        repaint();
    });
    popup.add(resetColors);

    popup.show(this, x, y);
}

private boolean maybeShowNodePopup(MouseEvent e) {
    if (e.isPopupTrigger() || SwingUtilities.isRightMouseButton(e)) {
        Point2D.Double worldPos = screenToWorld(e.getPoint());
        FlowNode node = findNodeAt(worldPos);
        if (node != null) {
            flowDiagram.selectNode(node);
            showNodePopup(e.getX(), e.getY(), node);
            return true;
        }
    }
    return false;
}

// Helpers for color handling
private Color parseHexColor(String hex, Color fallback) {
    try {
        if (hex == null || hex.isEmpty()) return fallback;
        return Color.decode(hex);
    } catch (Exception ex) {
        return fallback;
    }
}

private String colorToHex(Color c) {
    if (c == null) return null;
    return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
    }

private Color chooseColor(String title, Color initial) {
    return JColorChooser.showDialog(this, title, initial);
}
}