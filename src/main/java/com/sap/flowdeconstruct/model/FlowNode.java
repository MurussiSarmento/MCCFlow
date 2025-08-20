package com.sap.flowdeconstruct.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.awt.Point;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a single node in the flow diagram
 * Each node can contain text, notes, and optionally a sub-flow
 */
public class FlowNode {
    
    public enum NodeShape {
        RECTANGLE,
        SQUARE,
        CIRCLE,
        OVAL,
        DIAMOND
    }
    
    private String id;
    private String text;
    private String notes;
    private Point position;
    private FlowDiagram subFlow;

    // Customization (persisted)
    private String fillColorHex; // e.g. "#3a3a3a"
    private String borderColorHex; // e.g. "#666666"
    private NodeShape shape = NodeShape.RECTANGLE;
    
    // UI state (not persisted)
    @JsonIgnore
    private boolean selected;
    @JsonIgnore
    private boolean editing;
    @JsonIgnore
    private List<NodeStateListener> listeners;
    
    public FlowNode() {
        this.id = UUID.randomUUID().toString();
        this.text = "";
        this.notes = "";
        this.position = new Point(0, 0);
        this.selected = false;
        this.editing = false;
        this.listeners = new ArrayList<>();
        // defaults based on current UI palette
        this.fillColorHex = "#3a3a3a";
        this.borderColorHex = "#666666";
        this.shape = NodeShape.RECTANGLE;
    }
    
    public FlowNode(String text) {
        this();
        this.text = text;
    }
    
    public FlowNode(String text, int x, int y) {
        this(text);
        this.position = new Point(x, y);
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getText() {
        return text;
    }
    
    public void setText(String text) {
        String oldText = this.text;
        this.text = text;
        notifyListeners("text", oldText, text);
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        String oldNotes = this.notes;
        this.notes = notes;
        notifyListeners("notes", oldNotes, notes);
    }
    
    public Point getPosition() {
        return new Point(position); // Return copy to prevent external modification
    }
    
    public void setPosition(Point position) {
        Point oldPosition = this.position;
        this.position = new Point(position);
        notifyListeners("position", oldPosition, this.position);
    }
    
    public void setPosition(int x, int y) {
        setPosition(new Point(x, y));
    }
    
    public double getX() {
        return position.getX();
    }
    
    public double getY() {
        return position.getY();
    }
    
    public FlowDiagram getSubFlow() {
        return subFlow;
    }
    
    public void setSubFlow(FlowDiagram subFlow) {
        FlowDiagram oldSubFlow = this.subFlow;
        this.subFlow = subFlow;
        notifyListeners("subFlow", oldSubFlow, subFlow);
    }
    
    public boolean hasSubFlow() {
        return subFlow != null;
    }
    
    public boolean isSelected() {
        return selected;
    }
    
    public void setSelected(boolean selected) {
        boolean oldSelected = this.selected;
        this.selected = selected;
        notifyListeners("selected", oldSelected, selected);
    }
    
    public boolean isEditing() {
        return editing;
    }
    
    public void setEditing(boolean editing) {
        boolean oldEditing = this.editing;
        this.editing = editing;
        notifyListeners("editing", oldEditing, editing);
    }
    
    public boolean hasNotes() {
        return notes != null && !notes.trim().isEmpty();
    }

    // Customization accessors
    public String getFillColorHex() {
        return fillColorHex;
    }

    public void setFillColorHex(String fillColorHex) {
        String old = this.fillColorHex;
        this.fillColorHex = fillColorHex;
        notifyListeners("fillColorHex", old, fillColorHex);
    }

    public String getBorderColorHex() {
        return borderColorHex;
    }

    public void setBorderColorHex(String borderColorHex) {
        String old = this.borderColorHex;
        this.borderColorHex = borderColorHex;
        notifyListeners("borderColorHex", old, borderColorHex);
    }

    public NodeShape getShape() {
        return shape;
    }

    public void setShape(NodeShape shape) {
        NodeShape old = this.shape;
        this.shape = shape != null ? shape : NodeShape.RECTANGLE;
        notifyListeners("shape", old, this.shape);
    }
    
    // Listener management
    public void addStateListener(NodeStateListener listener) {
        if (listeners == null) {
            listeners = new ArrayList<>();
        }
        listeners.add(listener);
    }
    
    public void removeStateListener(NodeStateListener listener) {
        if (listeners != null) {
            listeners.remove(listener);
        }
    }
    
    private void notifyListeners(String property, Object oldValue, Object newValue) {
        if (listeners != null) {
            for (NodeStateListener listener : listeners) {
                listener.onNodeStateChanged(this, property, oldValue, newValue);
            }
        }
    }
    
    // Utility methods
    public FlowNode createSubFlow() {
        if (subFlow == null) {
            subFlow = new FlowDiagram();
            subFlow.setName(this.text + " Sub-flow");
            notifyListeners("subFlow", null, subFlow);
        }
        return this;
    }
    
    @Override
    public String toString() {
        return "FlowNode{" +
                "id='" + id + '\'' +
                ", text='" + text + '\'' +
                ", hasNotes=" + hasNotes() +
                ", hasSubFlow=" + hasSubFlow() +
                ", position=" + position +
                ", shape=" + shape +
                ", fillColorHex=" + fillColorHex +
                ", borderColorHex=" + borderColorHex +
                '}';
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        FlowNode flowNode = (FlowNode) obj;
        return id.equals(flowNode.id);
    }
    
    @Override
    public int hashCode() {
        return id.hashCode();
    }
    
    /**
     * Interface for listening to node state changes
     */
    public interface NodeStateListener {
        void onNodeStateChanged(FlowNode node, String property, Object oldValue, Object newValue);
    }
}