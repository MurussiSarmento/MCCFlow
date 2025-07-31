package com.sap.flowdeconstruct.model;

import java.util.UUID;

/**
 * Represents a connection (arrow) between two nodes in the flow diagram
 */
public class FlowConnection {
    
    private String id;
    private String fromNodeId;
    private String toNodeId;
    private ConnectionType type;
    
    public enum ConnectionType {
        NORMAL,     // Standard flow connection
        CONDITIONAL, // Conditional flow (future use)
        ERROR       // Error flow (future use)
    }
    
    public FlowConnection() {
        this.id = UUID.randomUUID().toString();
        this.type = ConnectionType.NORMAL;
    }
    
    public FlowConnection(String fromNodeId, String toNodeId) {
        this();
        this.fromNodeId = fromNodeId;
        this.toNodeId = toNodeId;
    }
    
    public FlowConnection(FlowNode fromNode, FlowNode toNode) {
        this(fromNode.getId(), toNode.getId());
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getFromNodeId() {
        return fromNodeId;
    }
    
    public void setFromNodeId(String fromNodeId) {
        this.fromNodeId = fromNodeId;
    }
    
    public String getToNodeId() {
        return toNodeId;
    }
    
    public void setToNodeId(String toNodeId) {
        this.toNodeId = toNodeId;
    }
    
    public ConnectionType getType() {
        return type;
    }
    
    public void setType(ConnectionType type) {
        this.type = type;
    }
    
    // Utility methods
    public boolean connectsNode(String nodeId) {
        return fromNodeId.equals(nodeId) || toNodeId.equals(nodeId);
    }
    
    public boolean connectsNode(FlowNode node) {
        return connectsNode(node.getId());
    }
    
    public boolean isValidConnection() {
        return fromNodeId != null && toNodeId != null && !fromNodeId.equals(toNodeId);
    }
    
    @Override
    public String toString() {
        return "FlowConnection{" +
                "id='" + id + '\'' +
                ", from='" + fromNodeId + '\'' +
                ", to='" + toNodeId + '\'' +
                ", type=" + type +
                '}';
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        FlowConnection that = (FlowConnection) obj;
        return id.equals(that.id);
    }
    
    @Override
    public int hashCode() {
        return id.hashCode();
    }
}