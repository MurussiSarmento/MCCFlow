package com.sap.flowdeconstruct.export;

import com.sap.flowdeconstruct.model.FlowDiagram;
import com.sap.flowdeconstruct.model.FlowNode;
import com.sap.flowdeconstruct.ui.components.FlowCanvas;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/**
 * Utility renderer to produce high-quality images of the flow and the timeline
 * by leveraging the existing FlowCanvas painting logic offscreen.
 */
public class ExportRenderer {

    private static final int MARGIN = 24; // pixels around the flow content

    /**
     * Renders the flow area (nodes + connections) into a BufferedImage that fits within maxWidth x maxHeight.
     * Keeps aspect ratio and adds a small margin around content.
     */
    public BufferedImage renderFlowImage(FlowDiagram diagram, int maxWidth, int maxHeight) {
        if (diagram == null) return emptyImage(Math.max(1, maxWidth), Math.max(1, maxHeight));

        // Compute world bounds based on node positions and sizes
        Bounds b = computeNodeBounds(diagram);
        if (b.width <= 0 || b.height <= 0) {
            // No nodes to render, fallback to minimal image
            return emptyImage(Math.max(1, maxWidth), Math.max(1, maxHeight));
        }

        // Fit content within the requested size (with margins)
        double availW = Math.max(1, maxWidth - 2.0 * MARGIN);
        double availH = Math.max(1, maxHeight - 2.0 * MARGIN);
        double scale = Math.min(availW / b.width, availH / b.height);
        // Cap scale to avoid extreme zooms
        scale = Math.max(0.1, Math.min(4.0, scale));

        int outW = (int) Math.round(b.width * scale + 2 * MARGIN);
        int outH = (int) Math.round(b.height * scale + 2 * MARGIN);

        // Prepare canvas
        FlowCanvas canvas = new FlowCanvas();
        canvas.setMode(FlowCanvas.Mode.FLOW_ONLY);
        canvas.setFlowDiagram(diagram);
        canvas.setZoomLevel(scale);
        // translate so that (minX, minY) maps to (MARGIN, MARGIN)
        double tx = MARGIN - (b.minX * scale);
        double ty = MARGIN - (b.minY * scale);
        canvas.setViewOffset(tx, ty);
        canvas.setSize(new Dimension(outW, outH));

        // Render offscreen
        BufferedImage img = new BufferedImage(outW, outH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            canvas.paint(g2);
        } finally {
            g2.dispose();
        }
        return img;
    }

    /**
     * Renders only the timeline area for the provided diagram at the given width.
     * The height is determined by FlowCanvas timeline preferred height.
     */
    public BufferedImage renderTimelineImage(FlowDiagram diagram, int width) {
        if (diagram == null) return emptyImage(Math.max(1, width), 1);

        FlowCanvas canvas = new FlowCanvas();
        canvas.setMode(FlowCanvas.Mode.TIMELINE_ONLY);
        canvas.setFlowDiagram(diagram);
        int height = canvas.getTimelinePreferredHeight();
        canvas.setSize(new Dimension(width, height));

        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            canvas.paint(g2);
        } finally {
            g2.dispose();
        }
        return img;
    }

    // --- Helpers

    private BufferedImage emptyImage(int w, int h) {
        return new BufferedImage(Math.max(1, w), Math.max(1, h), BufferedImage.TYPE_INT_ARGB);
    }

    private Bounds computeNodeBounds(FlowDiagram diagram) {
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;

        for (FlowNode n : diagram.getNodes()) {
            double x1 = n.getX();
            double y1 = n.getY();
            double x2 = x1 + n.getWidth();
            double y2 = y1 + n.getHeight();
            if (x1 < minX) minX = x1;
            if (y1 < minY) minY = y1;
            if (x2 > maxX) maxX = x2;
            if (y2 > maxY) maxY = y2;
        }

        if (minX == Double.POSITIVE_INFINITY) {
            // No nodes
            return new Bounds(0, 0, 0, 0);
        }
        return new Bounds(minX, minY, Math.max(1.0, maxX - minX), Math.max(1.0, maxY - minY));
    }

    private static class Bounds {
        final double minX, minY, width, height;
        Bounds(double minX, double minY, double width, double height) {
            this.minX = minX;
            this.minY = minY;
            this.width = width;
            this.height = height;
        }
    }
}