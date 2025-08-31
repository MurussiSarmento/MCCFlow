package com.sap.flowdeconstruct.ui.components;

import com.sap.flowdeconstruct.model.FlowConnection;
import com.sap.flowdeconstruct.model.FlowDiagram;
import com.sap.flowdeconstruct.model.FlowNode;
import com.sap.flowdeconstruct.model.TimelineEvent;
import com.sap.flowdeconstruct.ui.dialogs.ConnectionDialog;
import com.sap.flowdeconstruct.ui.dialogs.TextStyleDialog;
import com.sap.flowdeconstruct.i18n.I18n;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.List;
import java.util.ArrayList;
import java.util.Date;
import java.text.SimpleDateFormat;

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
    private static final SimpleDateFormat TIMESTAMP_FMT = new SimpleDateFormat("dd/MM/yyyy HH:mm");
    private static final Font MONO_FONT = new Font(Font.MONOSPACED, Font.PLAIN, 12);
    private static final int NODE_WIDTH = 120;
    private static final int NODE_HEIGHT = 40;
    private static final int NODE_SPACING_X = 160;
    private static final int NODE_SPACING_Y = 80;
    private static final int CANVAS_MARGIN = 50;
    private static String getDefaultNodeText() {
        return I18n.t("canvas.default.node");
    }
    
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
    // Novo: estado para criação de conexão manual
    private FlowNode connectStartNode = null;
    private Point2D.Double connectMouseWorld = null;
    
    // Timeline state
    public enum Mode { FLOW_ONLY, TIMELINE_ONLY, BOTH }
    private Mode mode = Mode.FLOW_ONLY;
    private static final int TIMELINE_HEIGHT = 120;
    private static final int TIMELINE_PADDING = 16;
    private static final int TIMELINE_TRACK_HEIGHT = 6;
    private static final int TIMELINE_EVENT_RADIUS = 7;
    private static final Color TIMELINE_BG = new Color(0x24, 0x24, 0x24);
    private static final Color TIMELINE_TRACK = new Color(0x55, 0x55, 0x55);
    private static final Color TIMELINE_EVENT = new Color(0x5f, 0x9e, 0xa0);

    // Interação com eventos da timeline
    private TimelineEvent draggingEvent = null;

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

    public void setMode(Mode m) {
        if (m != null) {
            this.mode = m;
            revalidate();
            repaint();
        }
    }

    public Mode getMode() {
        return mode;
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
        
        if (mode == Mode.TIMELINE_ONLY || mode == Mode.BOTH) {
            // timeline ocupa uma faixa fixa na parte inferior
            paintTimelineBackground(g);
        }
        
        if (mode == Mode.TIMELINE_ONLY) {
            paintTimeline((Graphics2D) g);
            return;
        }
        
        if (flowDiagram == null) {
            paintWelcomeScreen(g);
            return;
        }
        
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
        
        if (mode == Mode.BOTH) {
            paintTimeline((Graphics2D) g);
        }
    }

    private Rectangle getTimelineBounds() {
        int h = TIMELINE_HEIGHT;
        Rectangle vr = getVisibleRect();
        int y = vr.y + vr.height - h;
        return new Rectangle(vr.x, y, vr.width, h);
    }

    private void paintTimelineBackground(Graphics g) {
        Rectangle r = getTimelineBounds();
        g.setColor(TIMELINE_BG);
        g.fillRect(r.x, r.y, r.width, r.height);
    }

    private void paintTimeline(Graphics2D g2d) {
        if (flowDiagram == null) return;
        Rectangle r = getTimelineBounds();
        int trackY = r.y + TIMELINE_PADDING + (TIMELINE_HEIGHT / 2) - (TIMELINE_TRACK_HEIGHT / 2);
        int trackX = r.x + TIMELINE_PADDING;
        int trackW = r.width - 2 * TIMELINE_PADDING;

        // Track
        g2d.setColor(TIMELINE_TRACK);
        g2d.fillRoundRect(trackX, trackY, trackW, TIMELINE_TRACK_HEIGHT, TIMELINE_TRACK_HEIGHT, TIMELINE_TRACK_HEIGHT);

        // Events
        List<TimelineEvent> events = flowDiagram.getTimelineEvents();
        for (TimelineEvent ev : events) {
            int ex = trackX + (int) Math.round(ev.getPosition() * trackW);
            int ey = trackY + TIMELINE_TRACK_HEIGHT / 2;
            // point
            g2d.setColor(TIMELINE_EVENT);
            g2d.fillOval(ex - TIMELINE_EVENT_RADIUS, ey - TIMELINE_EVENT_RADIUS, TIMELINE_EVENT_RADIUS * 2, TIMELINE_EVENT_RADIUS * 2);
            // label
            g2d.setFont(MONO_FONT);
            g2d.setColor(TEXT_COLOR);
            String label = ev.getLabel();
            Date ts = ev.getTimestamp();
            String labelText = (ts != null ? (TIMESTAMP_FMT.format(ts) + " - ") : "") + (label != null ? label : "");
            int strW = g2d.getFontMetrics().stringWidth(labelText);
            int lx = Math.max(trackX, Math.min(ex - strW / 2, trackX + trackW - strW));
            int ly = ey - TIMELINE_EVENT_RADIUS - 6;
            g2d.drawString(labelText, lx, ly);
        }

        // Hint
        g2d.setFont(MONO_FONT.deriveFont(10f));
        g2d.setColor(TEXT_COLOR.darker());
        g2d.drawString(I18n.t("canvas.timeline.hint"), trackX, trackY + TIMELINE_TRACK_HEIGHT + 18);
    }

    private TimelineEvent findTimelineEventAt(Point p) {
        Rectangle r = getTimelineBounds();
        int trackY = r.y + TIMELINE_PADDING + (TIMELINE_HEIGHT / 2) - (TIMELINE_TRACK_HEIGHT / 2);
        int trackX = r.x + TIMELINE_PADDING;
        int trackW = r.width - 2 * TIMELINE_PADDING;
        int ey = trackY + TIMELINE_TRACK_HEIGHT / 2;
        List<TimelineEvent> events = flowDiagram != null ? flowDiagram.getTimelineEvents() : new ArrayList<>();
        for (TimelineEvent ev : events) {
            int ex = trackX + (int) Math.round(ev.getPosition() * trackW);
            int dx = p.x - ex;
            int dy = p.y - ey;
            if (dx * dx + dy * dy <= (TIMELINE_EVENT_RADIUS + 2) * (TIMELINE_EVENT_RADIUS + 2)) {
                return ev;
            }
        }
        return null;
    }

    private double positionFromPointOnTrack(Point p) {
        Rectangle r = getTimelineBounds();
        int trackX = r.x + TIMELINE_PADDING;
        int trackW = r.width - 2 * TIMELINE_PADDING;
        double pos = (p.x - trackX) / (double) trackW;
        return Math.max(0.0, Math.min(1.0, pos));
    }

    private boolean isOnTimelineTrack(Point p) {
        Rectangle r = getTimelineBounds();
        int trackY = r.y + TIMELINE_PADDING + (TIMELINE_HEIGHT / 2) - (TIMELINE_TRACK_HEIGHT / 2);
        int trackX = r.x + TIMELINE_PADDING;
        int trackW = r.width - 2 * TIMELINE_PADDING;
        Rectangle track = new Rectangle(trackX, trackY - 10, trackW, TIMELINE_TRACK_HEIGHT + 20);
        return track.contains(p);
    }

    private void paintWelcomeScreen(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        
        g2d.setColor(TEXT_COLOR.darker());
        g2d.setFont(MONO_FONT.deriveFont(16f));
        
        String message = I18n.t("canvas.welcome.hint");
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
            if (fromNode == null || toNode == null) continue;
            
            drawConnection(g2d, fromNode, toNode, connection);
        }
        
        // Pré-visualização de conexão (modo conectar)
        if (connectStartNode != null && connectMouseWorld != null) {
            // Determina um ponto de ancoragem na borda do nó de origem em direção ao mouse
            int x = (int) connectStartNode.getX();
            int y = (int) connectStartNode.getY();
            int w = connectStartNode.getWidth();
            int h = connectStartNode.getHeight();
            double cx = x + w / 2.0;
            double cy = y + h / 2.0;
            double dx = connectMouseWorld.x - cx;
            double dy = connectMouseWorld.y - cy;
            Point2D.Double fromPt;
            if (Math.abs(dx) > Math.abs(dy)) {
                fromPt = new Point2D.Double(dx >= 0 ? x + w : x, cy);
            } else {
                fromPt = new Point2D.Double(cx, dy >= 0 ? y + h : y);
            }
            // Desenha linha tracejada com seta
            Stroke oldStroke = g2d.getStroke();
            g2d.setColor(SUBFLOW_INDICATOR_COLOR);
            g2d.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0f, new float[]{6f, 6f}, 0f));
            g2d.drawLine((int) fromPt.x, (int) fromPt.y, (int) connectMouseWorld.x, (int) connectMouseWorld.y);
            // seta
            drawArrowHead(g2d, fromPt.x, fromPt.y, connectMouseWorld.x, connectMouseWorld.y);
            g2d.setStroke(oldStroke);
        }
    }

    // --- Added helpers for drawing and hit-testing connections ---
    private void drawConnection(Graphics2D g2d, FlowNode fromNode, FlowNode toNode, FlowConnection connection) {
        // Determine anchor points on node borders towards each other
        Point2D.Double toCenter = new Point2D.Double(toNode.getX() + toNode.getWidth() / 2.0, toNode.getY() + toNode.getHeight() / 2.0);
        Point2D.Double fromCenter = new Point2D.Double(fromNode.getX() + fromNode.getWidth() / 2.0, fromNode.getY() + fromNode.getHeight() / 2.0);
        Point2D.Double fromPt = anchorPointTowards(fromNode, toCenter.x, toCenter.y);
        Point2D.Double toPt = anchorPointTowards(toNode, fromCenter.x, fromCenter.y);

        // Colors and stroke
        Color lineColor = parseHexColor(connection.getLineColorHex(), CONNECTION_COLOR);
        Color arrowColor = parseHexColor(connection.getArrowColorHex(), lineColor);
        Stroke old = g2d.getStroke();
        g2d.setStroke(new BasicStroke(2f));
        g2d.setColor(lineColor);
        g2d.drawLine((int) fromPt.x, (int) fromPt.y, (int) toPt.x, (int) toPt.y);

        // Arrowheads according to direction style
        g2d.setColor(arrowColor);
        FlowConnection.DirectionStyle ds = connection.getDirectionStyle();
        if (ds == FlowConnection.DirectionStyle.FROM_TO) {
            drawArrowHead(g2d, fromPt.x, fromPt.y, toPt.x, toPt.y);
        } else if (ds == FlowConnection.DirectionStyle.TO_FROM) {
            drawArrowHead(g2d, toPt.x, toPt.y, fromPt.x, fromPt.y);
        } else if (ds == FlowConnection.DirectionStyle.BIDIRECTIONAL) {
            drawArrowHead(g2d, fromPt.x, fromPt.y, toPt.x, toPt.y);
            drawArrowHead(g2d, toPt.x, toPt.y, fromPt.x, fromPt.y);
        }
        g2d.setStroke(old);
    }

    private void drawArrowHead(Graphics2D g2d, double x1, double y1, double x2, double y2) {
        // Draw a filled triangle arrow head at (x2,y2), pointing from (x1,y1) -> (x2,y2)
        double angle = Math.atan2(y2 - y1, x2 - x1);
        int size = 10;
        int xA = (int) x2;
        int yA = (int) y2;
        int xB = (int) (x2 - size * Math.cos(angle - Math.PI / 6));
        int yB = (int) (y2 - size * Math.sin(angle - Math.PI / 6));
        int xC = (int) (x2 - size * Math.cos(angle + Math.PI / 6));
        int yC = (int) (y2 - size * Math.sin(angle + Math.PI / 6));
        g2d.fillPolygon(new int[]{xA, xB, xC}, new int[]{yA, yB, yC}, 3);
    }

    private FlowConnection findConnectionAt(Point2D.Double worldPos) {
        if (flowDiagram == null) return null;
        double threshold = 6.0; // pixels in world space
        FlowConnection best = null;
        double bestDist = Double.MAX_VALUE;
        for (FlowConnection connection : flowDiagram.getConnections()) {
            FlowNode fromNode = flowDiagram.findNodeById(connection.getFromNodeId());
            FlowNode toNode = flowDiagram.findNodeById(connection.getToNodeId());
            if (fromNode == null || toNode == null) continue;
            Point2D.Double toCenter = new Point2D.Double(toNode.getX() + toNode.getWidth() / 2.0, toNode.getY() + toNode.getHeight() / 2.0);
            Point2D.Double fromCenter = new Point2D.Double(fromNode.getX() + fromNode.getWidth() / 2.0, fromNode.getY() + fromNode.getHeight() / 2.0);
            Point2D.Double fromPt = anchorPointTowards(fromNode, toCenter.x, toCenter.y);
            Point2D.Double toPt = anchorPointTowards(toNode, fromCenter.x, fromCenter.y);

            double d = distancePointToSegment(worldPos.x, worldPos.y, fromPt.x, fromPt.y, toPt.x, toPt.y);
            if (d < threshold && d < bestDist) {
                bestDist = d;
                best = connection;
            }
        }
        return best;
    }

    private Point2D.Double anchorPointTowards(FlowNode node, double tx, double ty) {
        int x = (int) node.getX();
        int y = (int) node.getY();
        int w = node.getWidth();
        int h = node.getHeight();
        double cx = x + w / 2.0;
        double cy = y + h / 2.0;
        double dx = tx - cx;
        double dy = ty - cy;
        if (Math.abs(dx) > Math.abs(dy)) {
            return new Point2D.Double(dx >= 0 ? x + w : x, cy);
        } else {
            return new Point2D.Double(cx, dy >= 0 ? y + h : y);
        }
    }

    private double distancePointToSegment(double px, double py, double x1, double y1, double x2, double y2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        if (dx == 0 && dy == 0) {
            // It's a point not a segment.
            dx = px - x1;
            dy = py - y1;
            return Math.sqrt(dx * dx + dy * dy);
        }
        double t = ((px - x1) * dx + (py - y1) * dy) / (dx * dx + dy * dy);
        t = Math.max(0, Math.min(1, t));
        double projX = x1 + t * dx;
        double projY = y1 + t * dy;
        double ddx = px - projX;
        double ddy = py - projY;
        return Math.sqrt(ddx * ddx + ddy * ddy);
    }
@Override
    public void mouseClicked(MouseEvent e) {
        // Timeline interactions have priority when in timeline region
        if (mode != Mode.FLOW_ONLY) {
            if (isOnTimelineTrack(e.getPoint())) {
                TimelineEvent hit = (flowDiagram != null) ? findTimelineEventAt(e.getPoint()) : null;

                // Editar evento com duplo clique (botão esquerdo)
                if (hit != null && SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
                    promptEditTimelineEvent(hit);
                    return;
                }

                // Editar evento com botão direito
                if (hit != null && SwingUtilities.isRightMouseButton(e)) {
                    promptEditTimelineEvent(hit);
                    return;
                }

                // Criar novo evento ao clicar na faixa (botão esquerdo) e já perguntar o nome/data
                if (hit == null && flowDiagram != null && SwingUtilities.isLeftMouseButton(e)) {
                    double pos = positionFromPointOnTrack(e.getPoint());
                    TimelineEvent ev = flowDiagram.addTimelineEvent(I18n.t("canvas.timeline.event.default"), pos);
                    // Definir timestamp inicial com base na posição relativa entre vizinhos
                    Date ts = computeTimestampForPosition(pos, ev);
                    flowDiagram.updateTimelineEvent(ev, null, null, ts, true);
                    // Prompt para editar label e data/hora
                    promptEditTimelineEvent(ev);
                    repaint();
                    return;
                }
            }
        }
        // existing behavior below
        requestFocusInWindow();
        if (flowDiagram == null) return;
        
        // Se estamos no modo de conexão, um clique define o nó alvo ou cancela
        if (connectStartNode != null) {
            Point2D.Double worldPos = screenToWorld(e.getPoint());
            FlowNode target = findNodeAt(worldPos);
            if (target != null && target != connectStartNode) {
                flowDiagram.addConnection(connectStartNode, target);
            }
            // Finaliza o modo de conexão (seja conectando ou cancelando)
            connectStartNode = null;
            connectMouseWorld = null;
            repaint();
            return;
        }
        
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
        
        // Timeline drag start has priority
        if (mode != Mode.FLOW_ONLY && isOnTimelineTrack(e.getPoint()) && SwingUtilities.isLeftMouseButton(e)) {
            TimelineEvent ev = findTimelineEventAt(e.getPoint());
            if (ev != null) {
                draggingEvent = ev;
                lastMousePos = e.getPoint();
                return;
            }
        }
        
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
        
        // Dragging timeline event
        if (draggingEvent != null) {
            double pos = positionFromPointOnTrack(e.getPoint());
            flowDiagram.updateTimelineEvent(draggingEvent, null, pos, false);
            repaint();
            lastMousePos = e.getPoint();
            return;
        }
        
        if (dragging && draggingNode != null) {
            Point2D.Double worldPos = screenToWorld(e.getPoint());
            int newX = (int) (worldPos.x - draggingNode.getWidth() / 2.0);
            int newY = (int) (worldPos.y - draggingNode.getHeight() / 2.0);
            
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
        
        // Atualiza posição do mouse em coordenadas do mundo para a pré-visualização de conexão
        if (connectStartNode != null) {
            connectMouseWorld = screenToWorld(e.getPoint());
            repaint();
        }
        
        lastMousePos = e.getPoint();
        dragging = true;
    }
    
    // In mouseReleased
    @Override
    public void mouseReleased(MouseEvent e) {
        // Finish timeline drag if any
        if (draggingEvent != null) {
            // Ajusta timestamp conforme nova posição e normaliza
            double pos = draggingEvent.getPosition();
            Date ts = computeTimestampForPosition(pos, draggingEvent);
            flowDiagram.updateTimelineEvent(draggingEvent, null, null, ts, true);
            draggingEvent = null;
            dragging = false;
            draggingNode = null;
            repaint();
            return;
        }
        
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
            int mw = movingNode.getWidth();
            int mh = movingNode.getHeight();
            int ow = node.getWidth();
            int oh = node.getHeight();
            Rectangle rMoving = new Rectangle(newX - minDistance / 2, newY - minDistance / 2, mw + minDistance, mh + minDistance);
            Rectangle rOther = new Rectangle((int) node.getX() - minDistance / 2, (int) node.getY() - minDistance / 2, ow + minDistance, oh + minDistance);
            if (rMoving.intersects(rOther)) return true;
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
            
            if (worldPos.x >= x && worldPos.x <= x + node.getWidth() &&
                worldPos.y >= y && worldPos.y <= y + node.getHeight()) {
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
        } else if (connectStartNode != null) {
            // Cancelar modo de conexão
            connectStartNode = null;
            connectMouseWorld = null;
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
        editingText = getDefaultNodeText().equals(currentText) ? "" : currentText;
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
            newNode = flowDiagram.addNode(getDefaultNodeText(), 
                (int)selectedNode.getX() + NODE_SPACING_X,
                (int)selectedNode.getY());
            flowDiagram.addConnection(selectedNode, newNode);
        } else {
            // Create first node or new isolated node with a sensible position
            List<FlowNode> nodes = flowDiagram.getNodes();
            if (nodes.isEmpty()) {
                newNode = flowDiagram.addNode(getDefaultNodeText(), CANVAS_MARGIN, CANVAS_MARGIN);
            } else {
                int maxX = Integer.MIN_VALUE;
                int minY = Integer.MAX_VALUE;
                for (FlowNode n : nodes) {
                    maxX = Math.max(maxX, (int) n.getX());
                    minY = Math.min(minY, (int) n.getY());
                }
                newNode = flowDiagram.addNode(getDefaultNodeText(), maxX + NODE_SPACING_X, minY);
            }
        }
        
        flowDiagram.selectNode(newNode);
        startEditingNode(newNode);
        repaint();
    }
    
    // Novo: criar nó isolado (nunca cria conexão automática)
    public void createIsolatedNode() {
        if (flowDiagram == null) return;
        FlowNode selectedNode = flowDiagram.getSelectedNode();
        FlowNode newNode;

        List<FlowNode> nodes = flowDiagram.getNodes();
        if (nodes.isEmpty()) {
            newNode = flowDiagram.addNode(getDefaultNodeText(), CANVAS_MARGIN, CANVAS_MARGIN);
        } else if (selectedNode != null) {
            newNode = flowDiagram.addNode(getDefaultNodeText(),
                    (int) selectedNode.getX() + NODE_SPACING_X,
                    (int) selectedNode.getY());
        } else {
            int maxX = Integer.MIN_VALUE;
            int minY = Integer.MAX_VALUE;
            for (FlowNode n : nodes) {
                maxX = Math.max(maxX, (int) n.getX());
                minY = Math.min(minY, (int) n.getY());
            }
            newNode = flowDiagram.addNode(getDefaultNodeText(), maxX + NODE_SPACING_X, minY);
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
        int w = node.getWidth();
        int h = node.getHeight();
        
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
                g2d.fillRoundRect(x, y, w, h, 12, 12);
                g2d.setColor(border);
                g2d.drawRoundRect(x, y, w, h, 12, 12);
                break;
            case SQUARE: {
                int side = Math.min(w, h);
                g2d.fillRect(x, y, side, side);
                g2d.setColor(border);
                g2d.drawRect(x, y, side, side);
                break;
            }
            case OVAL:
                g2d.fillOval(x, y, w, h);
                g2d.setColor(border);
                g2d.drawOval(x, y, w, h);
                break;
            case CIRCLE: {
                int diameter = Math.min(w, h);
                int cx = x + (w - diameter) / 2;
                int cy = y + (h - diameter) / 2;
                g2d.fillOval(cx, cy, diameter, diameter);
                g2d.setColor(border);
                g2d.drawOval(cx, cy, diameter, diameter);
                break;
            }
            case DIAMOND: {
                int cx = x + w / 2;
                int cy = y + h / 2;
                Polygon p = new Polygon(
                    new int[]{cx, x + w, cx, x},
                    new int[]{y, cy, y + h, cy},
                    4
                );
                g2d.fillPolygon(p);
                g2d.setColor(border);
                g2d.drawPolygon(p);
                break;
            }
            default:
                g2d.fillRoundRect(x, y, w, h, 12, 12);
                g2d.setColor(border);
                g2d.drawRoundRect(x, y, w, h, 12, 12);
        }
        
        // Node text
        Color textColor = parseHexColor(node.getTextColorHex(), TEXT_COLOR);
        g2d.setColor(textColor);
        // Use per-node font if available
        String family = (node.getTextFontFamily() != null && !node.getTextFontFamily().trim().isEmpty()) ? node.getTextFontFamily() : Font.MONOSPACED;
        int size = node.getTextFontSize() > 0 ? node.getTextFontSize() : 12;
        int style = node.getTextFontStyle();
        Font nodeFont;
        try {
            nodeFont = new Font(family, style, size);
            if (nodeFont == null) nodeFont = MONO_FONT;
        } catch (Exception ex) {
            nodeFont = MONO_FONT;
        }
        g2d.setFont(nodeFont);
        
        String text = isEditing ? editingText : node.getText();
        FontMetrics fm = g2d.getFontMetrics();
        
        // Determine bounds for centering text based on shape actually drawn
        int boundsX = x;
        int boundsY = y;
        int boundsW = w;
        int boundsH = h;
        switch (shape) {
            case SQUARE: {
                int side = Math.min(w, h);
                boundsW = side;
                boundsH = side;
                break;
            }
            case CIRCLE: {
                int diameter = Math.min(w, h);
                boundsW = diameter;
                boundsH = diameter;
                boundsX = x + (w - diameter) / 2;
                boundsY = y + (h - diameter) / 2;
                break;
            }
            default:
                // RECTANGLE, OVAL, DIAMOND already use w x h
                break;
        }
        
        int textX = boundsX + (boundsW - fm.stringWidth(text)) / 2;
        int textY = boundsY + (boundsH + fm.getAscent() - fm.getDescent()) / 2;
        g2d.drawString(text, textX, textY);
        
        // Subflow indicator
        if (node.hasSubFlow()) {
            int indicatorSize = 8;
            int indicatorX = x + w - indicatorSize - 6;
            int indicatorY = y + h - indicatorSize - 6;
            g2d.setColor(SUBFLOW_INDICATOR_COLOR);
            g2d.fillOval(indicatorX, indicatorY, indicatorSize, indicatorSize);
        }
    }

    // ---------- Connection editing helpers ----------
    private void openConnectionDialog(FlowConnection connection) {
        Window window = SwingUtilities.getWindowAncestor(this);
        Frame owner = (window instanceof Frame) ? (Frame) window : null;
        ConnectionDialog dialog = new ConnectionDialog(owner, connection);
        dialog.setLocationRelativeTo(owner);
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
        JMenuItem edit = new JMenuItem(I18n.t("canvas.popup.connection.edit"));
        edit.addActionListener(ev -> openConnectionDialog(connection));
        popup.add(edit);

        JMenuItem reverse = new JMenuItem(I18n.t("canvas.popup.connection.reverse"));
        reverse.addActionListener(ev -> reverseDirection(connection));
        popup.add(reverse);

        popup.addSeparator();
        JMenuItem dirFromTo = new JMenuItem(I18n.t("canvas.popup.connection.direction.from"));
        dirFromTo.addActionListener(ev -> {
            connection.setDirectionStyle(FlowConnection.DirectionStyle.FROM_TO);
            if (flowDiagram != null) {
                List<FlowConnection> updated = new ArrayList<>(flowDiagram.getConnections());
                flowDiagram.setConnections(updated);
            }
            repaint();
        });
        popup.add(dirFromTo);

        JMenuItem dirToFrom = new JMenuItem(I18n.t("canvas.popup.connection.direction.to"));
        dirToFrom.addActionListener(ev -> {
            connection.setDirectionStyle(FlowConnection.DirectionStyle.TO_FROM);
            if (flowDiagram != null) {
                List<FlowConnection> updated = new ArrayList<>(flowDiagram.getConnections());
                flowDiagram.setConnections(updated);
            }
            repaint();
        });
        popup.add(dirToFrom);

        JMenuItem dirBi = new JMenuItem(I18n.t("canvas.popup.connection.direction.bi"));
        dirBi.addActionListener(ev -> {
            connection.setDirectionStyle(FlowConnection.DirectionStyle.BIDIRECTIONAL);
            if (flowDiagram != null) {
                List<FlowConnection> updated = new ArrayList<>(flowDiagram.getConnections());
                flowDiagram.setConnections(updated);
            }
            repaint();
        });
        popup.add(dirBi);

        JMenuItem dirNone = new JMenuItem(I18n.t("canvas.popup.connection.direction.none"));
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
        JMenuItem lineColorItem = new JMenuItem(I18n.t("canvas.popup.connection.linecolor"));
        lineColorItem.addActionListener(ev -> {
            Color initial = parseHexColor(connection.getLineColorHex(), CONNECTION_COLOR);
            Color chosen = chooseColor(I18n.t("canvas.color.selectline"), initial);
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

        JMenuItem arrowColorItem = new JMenuItem(I18n.t("canvas.popup.connection.arrowcolor"));
        arrowColorItem.addActionListener(ev -> {
            Color initial = parseHexColor(connection.getArrowColorHex(), CONNECTION_COLOR);
            Color chosen = chooseColor(I18n.t("canvas.color.selectarrow"), initial);
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
        if (e.isPopupTrigger()) {
            Point2D.Double worldPos = screenToWorld(e.getPoint());
            FlowConnection conn = findConnectionAt(worldPos);
            if (conn != null) {
                selectedConnection = conn;
                if (flowDiagram != null) {
                    // Clear any node selection when selecting a connection via popup
                    flowDiagram.selectNode(null);
                }
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
    
        double cx = current.getX() + current.getWidth() / 2.0;
        double cy = current.getY() + current.getHeight() / 2.0;
    
        FlowNode best = null;
        double bestDist = Double.MAX_VALUE;
        FlowNode fallback = null;
        double fallbackDist = Double.MAX_VALUE;
    
        for (FlowNode n : nodes) {
            if (n == current) continue;
            double nx = n.getX() + n.getWidth() / 2.0;
            double ny = n.getY() + n.getHeight() / 2.0;
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

    // Connection mode menu items
    JMenuItem connectItem = new JMenuItem(I18n.t("canvas.connect.to"));
    connectItem.addActionListener(ev -> {
        connectStartNode = node;
        connectMouseWorld = null;
        requestFocusInWindow();
        repaint();
    });
    popup.add(connectItem);
    if (connectStartNode != null) {
        JMenuItem cancelConnect = new JMenuItem(I18n.t("canvas.cancel.connect"));
        cancelConnect.addActionListener(ev -> {
            connectStartNode = null;
            connectMouseWorld = null;
            repaint();
        });
        popup.add(cancelConnect);
    }

    popup.addSeparator();

    JMenuItem fillColorItem = new JMenuItem(I18n.t("canvas.change.fill"));
    fillColorItem.addActionListener(ev -> {
        Color initial = parseHexColor(node.getFillColorHex(), NODE_COLOR);
        Color chosen = chooseColor(I18n.t("canvas.color.selectfill"), initial);
        if (chosen != null) {
            node.setFillColorHex(colorToHex(chosen));
            repaint();
        }
    });
    popup.add(fillColorItem);

    JMenuItem borderColorItem = new JMenuItem(I18n.t("canvas.change.border"));
    borderColorItem.addActionListener(ev -> {
        Color initial = parseHexColor(node.getBorderColorHex(), CONNECTION_COLOR);
        Color chosen = chooseColor(I18n.t("canvas.color.selectborder"), initial);
        if (chosen != null) {
            node.setBorderColorHex(colorToHex(chosen));
            repaint();
        }
    });
    popup.add(borderColorItem);

    JMenuItem textColorItem = new JMenuItem(I18n.t("canvas.text.color.font"));
    textColorItem.addActionListener(ev -> {
        Window window = SwingUtilities.getWindowAncestor(this);
        Frame owner = (window instanceof Frame) ? (Frame) window : null;
        Color initialColor = parseHexColor(node.getTextColorHex(), TEXT_COLOR);
        String initialFamily = (node.getTextFontFamily() != null && !node.getTextFontFamily().trim().isEmpty()) ? node.getTextFontFamily() : Font.MONOSPACED;
        int initialSize = node.getTextFontSize() > 0 ? node.getTextFontSize() : 12;
        int initialStyle = node.getTextFontStyle();
        TextStyleDialog dlg = new TextStyleDialog(owner, initialColor, initialFamily, initialSize, initialStyle);
        dlg.setLocationRelativeTo(owner);
        dlg.setVisible(true);
        if (dlg.isConfirmed()) {
            Color chosen = dlg.getSelectedColor();
            if (chosen != null) {
                node.setTextColorHex(colorToHex(chosen));
            }
            node.setTextFontFamily(dlg.getSelectedFamily());
            node.setTextFontSize(dlg.getSelectedSize());
            node.setTextFontStyle(dlg.getSelectedStyle());
            repaint();
        }
    });
    popup.add(textColorItem);

    popup.addSeparator();
    JMenu shapeMenu = new JMenu(I18n.t("canvas.shape"));
    for (FlowNode.NodeShape s : FlowNode.NodeShape.values()) {
        JMenuItem item = new JMenuItem(s.name());
        item.addActionListener(ev -> {
            node.setShape(s);
            repaint();
        });
        shapeMenu.add(item);
    }
    popup.add(shapeMenu);

    // Resize submenu
    JMenu sizeMenu = new JMenu(I18n.t("canvas.size"));

    JMenuItem incW = new JMenuItem(I18n.t("canvas.size.incW"));
    incW.addActionListener(ev -> { node.setWidth(node.getWidth() + 10); repaint(); });
    sizeMenu.add(incW);

    JMenuItem decW = new JMenuItem(I18n.t("canvas.size.decW"));
    decW.addActionListener(ev -> { node.setWidth(node.getWidth() - 10); repaint(); });
    sizeMenu.add(decW);

    JMenuItem incH = new JMenuItem(I18n.t("canvas.size.incH"));
    incH.addActionListener(ev -> { node.setHeight(node.getHeight() + 10); repaint(); });
    sizeMenu.add(incH);

    JMenuItem decH = new JMenuItem(I18n.t("canvas.size.decH"));
    decH.addActionListener(ev -> { node.setHeight(node.getHeight() - 10); repaint(); });
    sizeMenu.add(decH);

    sizeMenu.addSeparator();

    JMenuItem setSize = new JMenuItem(I18n.t("canvas.size.set"));
    setSize.addActionListener(ev -> {
        String wStr = JOptionPane.showInputDialog(this, I18n.t("canvas.node.width"), node.getWidth());
        if (wStr == null) return;
        String hStr = JOptionPane.showInputDialog(this, I18n.t("canvas.node.height"), node.getHeight());
        if (hStr == null) return;
        try {
            int wv = Integer.parseInt(wStr.trim());
            int hv = Integer.parseInt(hStr.trim());
            node.setWidth(wv);
            node.setHeight(hv);
            repaint();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, I18n.t("canvas.invalid.values"), I18n.t("canvas.error"), JOptionPane.ERROR_MESSAGE);
        }
    });
    sizeMenu.add(setSize);

    popup.add(sizeMenu);

    JMenuItem resetColors = new JMenuItem(I18n.t("canvas.reset.colors"));
    resetColors.addActionListener(ev -> {
        node.setFillColorHex("#3a3a3a");
        node.setBorderColorHex("#666666");
        node.setTextColorHex("#cccccc");
        repaint();
    });
    popup.add(resetColors);

    popup.show(this, x, y);
}

private boolean maybeShowNodePopup(MouseEvent e) {
    if (e.isPopupTrigger()) {
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
    Window owner = SwingUtilities.getWindowAncestor(this);
    return JColorChooser.showDialog(owner != null ? owner : this, title, initial);
}

    private void promptRenameTimelineEvent(TimelineEvent ev) {
        // Mantém compatibilidade chamando editor completo
        promptEditTimelineEvent(ev);
    }

    private void promptEditTimelineEvent(TimelineEvent ev) {
        if (ev == null || flowDiagram == null) return;
        String current = ev.getLabel();
        Date currentTs = ev.getTimestamp() != null ? (Date) ev.getTimestamp().clone() : new Date();

        JTextField labelField = new JTextField(current != null ? current : "", 24);
        SpinnerDateModel dateModel = new SpinnerDateModel(currentTs, null, null, java.util.Calendar.MINUTE);
        JSpinner dateSpinner = new JSpinner(dateModel);
        dateSpinner.setEditor(new JSpinner.DateEditor(dateSpinner, "dd/MM/yyyy HH:mm"));

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4,4,4,4);
        gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.EAST;
        panel.add(new JLabel(I18n.t("canvas.timeline.edit.desc")), gbc);
        gbc.gridx = 1; gbc.gridy = 0; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        panel.add(labelField, gbc);
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0; gbc.fill = GridBagConstraints.NONE; gbc.anchor = GridBagConstraints.EAST;
        panel.add(new JLabel(I18n.t("canvas.timeline.edit.datetime")), gbc);
        gbc.gridx = 1; gbc.gridy = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        panel.add(dateSpinner, gbc);

        int res = JOptionPane.showConfirmDialog(this, panel, I18n.t("canvas.timeline.edit.title"), JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res == JOptionPane.OK_OPTION) {
            String newLabel = labelField.getText() != null ? labelField.getText().trim() : "";
            Date newTs = (Date) dateSpinner.getValue();
            if (newTs == null) newTs = new Date();
            flowDiagram.updateTimelineEvent(ev, newLabel.isEmpty() ? null : newLabel, null, newTs, true);
            repaint();
        }
    }

    private Date computeTimestampForPosition(double pos, TimelineEvent exclude) {
        if (flowDiagram == null) return new Date();
        List<TimelineEvent> events = new ArrayList<>(flowDiagram.getTimelineEvents());
        if (exclude != null) {
            events.removeIf(ev -> ev == exclude);
        }
        if (events.isEmpty()) {
            return new Date();
        }
        // Encontrar vizinhos pela posição atual
        events.sort((a, b) -> Double.compare(a.getPosition(), b.getPosition()));
        TimelineEvent prev = null;
        TimelineEvent next = null;
        for (int i = 0; i < events.size(); i++) {
            TimelineEvent ev = events.get(i);
            if (pos <= ev.getPosition()) {
                next = ev;
                prev = (i - 1 >= 0) ? events.get(i - 1) : null;
                break;
            }
        }
        if (next == null) {
            prev = events.get(events.size() - 1);
        }
        Date prevTs = prev != null ? prev.getTimestamp() : null;
        Date nextTs = next != null ? next.getTimestamp() : null;
        return midpointTimestamp(prevTs, nextTs);
    }

    private Date midpointTimestamp(Date prev, Date next) {
        if (prev != null && next != null) {
            long a = prev.getTime();
            long b = next.getTime();
            if (b <= a) {
                // Garante ordem estrita
                return new Date(a + 1);
            }
            long mid = a + (b - a) / 2L;
            if (mid == a) mid = a + 1; // assegura incremento
            return new Date(mid);
        } else if (prev == null && next != null) {
            return new Date(next.getTime() - 1L);
        } else if (prev != null) { // next == null
            return new Date(prev.getTime() + 1L);
        } else {
            return new Date();
        }
    }

    // Exposed for offscreen export rendering
    public void setViewOffset(double x, double y) {
        this.viewOffset.x = x;
        this.viewOffset.y = y;
        repaint();
    }

    public void setZoomLevel(double zoom) {
        // clamp to a sensible range
        this.zoomLevel = Math.max(0.1, Math.min(4.0, zoom));
        repaint();
    }

    /**
     * Returns the preferred total height to render the timeline area,
     * including paddings, matching the on-screen layout.
     */
    public int getTimelinePreferredHeight() {
        return TIMELINE_HEIGHT + 2 * TIMELINE_PADDING;
    }
}