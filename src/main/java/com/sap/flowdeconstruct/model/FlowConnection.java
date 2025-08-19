package com.sap.flowdeconstruct.model;

import java.util.UUID;

public class FlowConnection {
    public enum ConnectionType {
        NORMAL, CONDITIONAL, ERROR
    }

    public enum DirectionStyle {
        FROM_TO, TO_FROM, BIDIRECTIONAL, NONE
    }

    private String id;
    private String fromNodeId;
    private String toNodeId;
    private ConnectionType type;

    // New fields
    private DirectionStyle directionStyle = DirectionStyle.FROM_TO;
    private String protocol = "";

    public FlowConnection(String id, String fromNodeId, String toNodeId, ConnectionType type) {
        this.id = id;
        this.fromNodeId = fromNodeId;
        this.toNodeId = toNodeId;
        this.type = type;
    }

    // Constructor for compatibility with existing FlowDiagram code
    public FlowConnection(FlowNode fromNode, FlowNode toNode) {
        this.id = UUID.randomUUID().toString();
        this.fromNodeId = fromNode.getId();
        this.toNodeId = toNode.getId();
        this.type = ConnectionType.NORMAL;
    }

    public String getId() {
        return id;
    }

    public String getFromNodeId() {
        return fromNodeId;
    }

    public String getToNodeId() {
        return toNodeId;
    }

    public ConnectionType getType() {
        return type;
    }

    public void setType(ConnectionType type) {
        this.type = type;
    }

    public DirectionStyle getDirectionStyle() {
        return directionStyle;
    }

    public void setDirectionStyle(DirectionStyle directionStyle) {
        this.directionStyle = directionStyle != null ? directionStyle : DirectionStyle.FROM_TO;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol != null ? protocol : "";
    }

    // Utility methods for compatibility
    public boolean connectsNode(String nodeId) {
        return fromNodeId.equals(nodeId) || toNodeId.equals(nodeId);
    }

    public boolean connectsNode(FlowNode node) {
        return connectsNode(node.getId());
    }
}