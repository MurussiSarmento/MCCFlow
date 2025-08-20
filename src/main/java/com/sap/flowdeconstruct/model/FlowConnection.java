package com.sap.flowdeconstruct.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.UUID;

public class FlowConnection {
    public enum ConnectionType { NORMAL, CONDITIONAL, ERROR }
    public enum DirectionStyle { FROM_TO, TO_FROM, BIDIRECTIONAL, NONE }

    private String id;
    private String fromNodeId;
    private String toNodeId;
    private ConnectionType type = ConnectionType.NORMAL;
    private DirectionStyle directionStyle = DirectionStyle.FROM_TO;
    private String protocol;

    // Customization (persisted)
    private String lineColorHex; // e.g. "#888888"
    private String arrowColorHex; // e.g. "#f0f0f0"

    // UI state (not persisted)
    @JsonIgnore
    private boolean selected;

    public FlowConnection() {
        this.id = UUID.randomUUID().toString();
        // defaults aligned with FlowCanvas constants
        this.lineColorHex = "#666666"; // match CONNECTION_COLOR default
        this.arrowColorHex = "#666666"; // arrow defaults to same as line
    }

    public FlowConnection(String fromNodeId, String toNodeId) {
        this();
        this.fromNodeId = fromNodeId;
        this.toNodeId = toNodeId;
    }

    // Overloaded constructor to keep compatibility with existing code
    public FlowConnection(FlowNode fromNode, FlowNode toNode) {
        this(fromNode != null ? fromNode.getId() : null, toNode != null ? toNode.getId() : null);
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getFromNodeId() { return fromNodeId; }
    public void setFromNodeId(String fromNodeId) { this.fromNodeId = fromNodeId; }

    public String getToNodeId() { return toNodeId; }
    public void setToNodeId(String toNodeId) { this.toNodeId = toNodeId; }

    public ConnectionType getType() { return type; }
    public void setType(ConnectionType type) { this.type = type; }

    public DirectionStyle getDirectionStyle() { return directionStyle; }
    public void setDirectionStyle(DirectionStyle directionStyle) { this.directionStyle = directionStyle; }

    public String getProtocol() { return protocol; }
    public void setProtocol(String protocol) { this.protocol = protocol; }

    public boolean isSelected() { return selected; }
    public void setSelected(boolean selected) { this.selected = selected; }

    public String getLineColorHex() { return lineColorHex; }
    public void setLineColorHex(String lineColorHex) { this.lineColorHex = lineColorHex; }

    public String getArrowColorHex() { return arrowColorHex; }
    public void setArrowColorHex(String arrowColorHex) { this.arrowColorHex = arrowColorHex; }

    /**
     * Returns true if this connection involves the given node (as source or target).
     */
    public boolean connectsNode(FlowNode node) {
        if (node == null) return false;
        String nodeId = node.getId();
        if (nodeId == null) return false;
        return nodeId.equals(fromNodeId) || nodeId.equals(toNodeId);
    }
}