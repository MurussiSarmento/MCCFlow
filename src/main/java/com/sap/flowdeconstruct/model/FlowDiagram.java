package com.sap.flowdeconstruct.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents a complete flow diagram containing nodes and connections
 * Can be a main flow or a sub-flow within another node
 */
public class FlowDiagram {
    
    private String id;
    private String name;
    private List<FlowNode> nodes;
    private List<FlowConnection> connections;
    private Date createdAt;
    private Date modifiedAt;
    
    // UI state (not persisted)
    @JsonIgnore
    private FlowNode selectedNode;
    @JsonIgnore
    private List<DiagramStateListener> listeners;
    @JsonIgnore
    private final FlowNode.NodeStateListener nodeStateListener;
    
    public FlowDiagram() {
        this.id = UUID.randomUUID().toString();
        this.name = "Main Flow";
        this.nodes = new ArrayList<>();
        this.connections = new ArrayList<>();
        this.createdAt = new Date();
        this.modifiedAt = new Date();
        this.listeners = new ArrayList<>();
        this.nodeStateListener = (n, property, oldValue, newValue) -> {
            if ("text".equals(property) || "notes".equals(property) || "position".equals(property)) {
                updateModifiedTime();
                notifyListeners("nodeModified", n, property);
            }
        };
    }
    
    public FlowDiagram(String name) {
        this();
        this.name = name;
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
        updateModifiedTime();
    }
    
    public List<FlowNode> getNodes() {
        return new ArrayList<>(nodes); // Return copy to prevent external modification
    }
    
    public void setNodes(List<FlowNode> nodes) {
        // Clear existing listeners to prevent duplicates
        for (FlowNode oldNode : this.nodes) {
            oldNode.removeStateListener(this.nodeStateListener);
        }

        this.nodes = new ArrayList<>(nodes);
        
        // Add listeners to new nodes
        for (FlowNode node : this.nodes) {
            if (node != null) {
                node.addStateListener(this.nodeStateListener);
            }
        }
        
        updateModifiedTime();
        notifyListeners("nodes", null, this.nodes);
    }
    
    public List<FlowConnection> getConnections() {
        return new ArrayList<>(connections); // Return copy to prevent external modification
    }
    
    public void setConnections(List<FlowConnection> connections) {
        this.connections = new ArrayList<>(connections);
        updateModifiedTime();
        notifyListeners("connections", null, this.connections);
    }
    
    public Date getCreatedAt() {
        return new Date(createdAt.getTime());
    }
    
    public void setCreatedAt(Date createdAt) {
        this.createdAt = new Date(createdAt.getTime());
    }
    
    public Date getModifiedAt() {
        return new Date(modifiedAt.getTime());
    }
    
    public void setModifiedAt(Date modifiedAt) {
        this.modifiedAt = new Date(modifiedAt.getTime());
    }
    
    public FlowNode getSelectedNode() {
        return selectedNode;
    }
    
    public void setSelectedNode(FlowNode selectedNode) {
        FlowNode oldSelected = this.selectedNode;
        
        // Deselect previous node
        if (oldSelected != null) {
            oldSelected.setSelected(false);
        }
        
        this.selectedNode = selectedNode;
        
        // Select new node
        if (selectedNode != null) {
            selectedNode.setSelected(true);
        }
        
        notifyListeners("selectedNode", oldSelected, selectedNode);
    }
    
    public void selectNode(FlowNode node) {
        setSelectedNode(node);
    }
    
    // Node management
    public FlowNode addNode(String text) {
        FlowNode node = new FlowNode(text);
        addNode(node);
        return node;
    }
    
    public FlowNode addNode(String text, int x, int y) {
        FlowNode node = new FlowNode(text, x, y);
        addNode(node);
        return node;
    }
    
    public void addNode(FlowNode node) {
        if (node != null && !nodes.contains(node)) {
            nodes.add(node);
            
            // Add listener to track node changes
            node.addStateListener(this.nodeStateListener);
            
            updateModifiedTime();
            notifyListeners("nodeAdded", null, node);
        }
    }
    
    public boolean removeNode(FlowNode node) {
        if (node == null) return false;
        
        // Remove all connections involving this node
        connections.removeIf(conn -> conn.connectsNode(node));
        
        // Remove the node
        boolean removed = nodes.remove(node);
        
        if (removed) {
            // Remove listener
            node.removeStateListener(this.nodeStateListener);

            // Clear selection if this was the selected node
            if (selectedNode == node) {
                setSelectedNode(null);
            }
            

            
            updateModifiedTime();
            notifyListeners("nodeRemoved", node, null);
        }
        
        return removed;
    }
    
    public FlowNode findNodeById(String nodeId) {
        return nodes.stream()
                .filter(node -> node.getId().equals(nodeId))
                .findFirst()
                .orElse(null);
    }
    
    // Connection management
    public FlowConnection addConnection(FlowNode fromNode, FlowNode toNode) {
        if (fromNode == null || toNode == null || fromNode == toNode) {
            return null;
        }
        
        // Check if connection already exists
        boolean exists = connections.stream()
                .anyMatch(conn -> conn.getFromNodeId().equals(fromNode.getId()) && 
                                conn.getToNodeId().equals(toNode.getId()));
        
        if (exists) {
            return null; // Connection already exists
        }
        
        FlowConnection connection = new FlowConnection(fromNode, toNode);
        connections.add(connection);
        updateModifiedTime();
        notifyListeners("connectionAdded", null, connection);
        
        return connection;
    }
    
    public boolean removeConnection(FlowConnection connection) {
        boolean removed = connections.remove(connection);
        if (removed) {
            updateModifiedTime();
            notifyListeners("connectionRemoved", connection, null);
        }
        return removed;
    }
    
    public List<FlowConnection> getConnectionsForNode(FlowNode node) {
        return connections.stream()
                .filter(conn -> conn.connectsNode(node))
                .collect(Collectors.toList());
    }
    
    public List<FlowConnection> getOutgoingConnections(FlowNode node) {
        return connections.stream()
                .filter(conn -> conn.getFromNodeId().equals(node.getId()))
                .collect(Collectors.toList());
    }
    
    public List<FlowConnection> getIncomingConnections(FlowNode node) {
        return connections.stream()
                .filter(conn -> conn.getToNodeId().equals(node.getId()))
                .collect(Collectors.toList());
    }
    
    // Navigation helpers
    public FlowNode getNextNode(FlowNode currentNode) {
        if (currentNode == null || nodes.isEmpty()) return null;
        
        int currentIndex = nodes.indexOf(currentNode);
        if (currentIndex == -1) return null;
        
        int nextIndex = (currentIndex + 1) % nodes.size();
        return nodes.get(nextIndex);
    }
    
    public FlowNode getPreviousNode(FlowNode currentNode) {
        if (currentNode == null || nodes.isEmpty()) return null;
        
        int currentIndex = nodes.indexOf(currentNode);
        if (currentIndex == -1) return null;
        
        int prevIndex = (currentIndex - 1 + nodes.size()) % nodes.size();
        return nodes.get(prevIndex);
    }
    
    // Utility methods
    public boolean isEmpty() {
        return nodes.isEmpty();
    }
    
    public int getNodeCount() {
        return nodes.size();
    }
    
    public int getConnectionCount() {
        return connections.size();
    }
    
    public void clear() {
        nodes.clear();
        connections.clear();
        setSelectedNode(null);
        updateModifiedTime();
        notifyListeners("cleared", null, null);
    }
    
    private void updateModifiedTime() {
        this.modifiedAt = new Date();
    }
    
    // Listener management
    public void addStateListener(DiagramStateListener listener) {
        if (listeners == null) {
            listeners = new ArrayList<>();
        }
        listeners.add(listener);
    }
    
    public void removeStateListener(DiagramStateListener listener) {
        if (listeners != null) {
            listeners.remove(listener);
        }
    }
    
    private void notifyListeners(String event, Object oldValue, Object newValue) {
        if (listeners != null) {
            for (DiagramStateListener listener : listeners) {
                listener.onDiagramStateChanged(this, event, oldValue, newValue);
            }
        }
    }
    
    @Override
    public String toString() {
        return "FlowDiagram{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", nodeCount=" + nodes.size() +
                ", connectionCount=" + connections.size() +
                '}';
    }
    
    /**
     * Interface for listening to diagram state changes
     */
    public interface DiagramStateListener {
        void onDiagramStateChanged(FlowDiagram diagram, String event, Object oldValue, Object newValue);
    }
}