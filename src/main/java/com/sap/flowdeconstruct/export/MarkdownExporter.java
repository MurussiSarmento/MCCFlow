package com.sap.flowdeconstruct.export;

import com.sap.flowdeconstruct.model.FlowDiagram;
import com.sap.flowdeconstruct.model.FlowNode;
import com.sap.flowdeconstruct.model.FlowConnection;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class MarkdownExporter {

    public void export(FlowDiagram flow, String filePath, boolean includeNotes, boolean includeSubflows) throws IOException {
        StringBuilder sb = new StringBuilder();
        appendFlowToMarkdown(sb, flow, 0, includeNotes, includeSubflows);
        try (FileWriter writer = new FileWriter(filePath)) {
            writer.write(sb.toString());
        }
    }

    private void appendFlowToMarkdown(StringBuilder sb, FlowDiagram flow, int level, boolean includeNotes, boolean includeSubflows) {
        StringBuilder indentBuilder = new StringBuilder();
        for (int i = 0; i < level * 2; i++) {
            indentBuilder.append(" ");
        }
        String indent = indentBuilder.toString();
        sb.append(indent).append("# ").append(flow.getName()).append("\n\n");

        List<FlowNode> nodes = flow.getNodes();
        for (FlowNode node : nodes) {
            sb.append(indent).append("[").append(node.getId()).append("] ").append(escapeMarkdown(node.getText())).append("\n");
            // Write node position so it can be preserved on import
            sb.append(indent).append("  Position: ").append((int) node.getX()).append(", ").append((int) node.getY()).append("\n");
            // Write node size (width,height)
            sb.append(indent).append("  Size: ").append(node.getWidth()).append(", ").append(node.getHeight()).append("\n");
            // Write node shape
            sb.append(indent).append("  Shape: ").append(node.getShape()).append("\n");
            // Write node colors (fill, border, text)
            if (node.getFillColorHex() != null) {
                sb.append(indent).append("  FillColor: ").append(node.getFillColorHex()).append("\n");
            }
            if (node.getBorderColorHex() != null) {
                sb.append(indent).append("  BorderColor: ").append(node.getBorderColorHex()).append("\n");
            }
            if (node.getTextColorHex() != null) {
                sb.append(indent).append("  TextColor: ").append(node.getTextColorHex()).append("\n");
            }
            if (node.getTextFontFamily() != null && !node.getTextFontFamily().trim().isEmpty()) {
                sb.append(indent).append("  TextFontFamily: ").append(node.getTextFontFamily()).append("\n");
            }
            if (node.getTextFontSize() > 0) {
                sb.append(indent).append("  TextFontSize: ").append(node.getTextFontSize()).append("\n");
            }
            sb.append(indent).append("  TextFontBold: ").append((node.getTextFontStyle() & java.awt.Font.BOLD) != 0).append("\n");
            sb.append(indent).append("  TextFontItalic: ").append((node.getTextFontStyle() & java.awt.Font.ITALIC) != 0).append("\n");
            if (includeNotes && !node.getNotes().isEmpty()) {
                sb.append(indent).append("  *Notes: ").append(escapeMarkdown(node.getNotes())).append("*\n");
            }
            if (includeSubflows && node.hasSubFlow()) {
                appendFlowToMarkdown(sb, node.getSubFlow(), level + 1, includeNotes, includeSubflows);
            }
        }

        sb.append("\n## Connections\n");
        List<FlowConnection> connections = flow.getConnections();
        for (FlowConnection conn : connections) {
            sb.append(indent)
              .append("From: ").append(conn.getFromNodeId())
              .append(" To: ").append(conn.getToNodeId())
              .append(" (").append(conn.getType()).append(")")
              .append(" Direction: ").append(conn.getDirectionStyle());
            // Include connection colors
            if (conn.getLineColorHex() != null) {
                sb.append(" LineColor: ").append(conn.getLineColorHex());
            }
            if (conn.getArrowColorHex() != null) {
                sb.append(" ArrowColor: ").append(conn.getArrowColorHex());
            }
            // Protocol MUST be last to allow spaces in value during import
            String protocol = conn.getProtocol();
            if (protocol != null && !protocol.trim().isEmpty()) {
                sb.append(" Protocol: ").append(escapeMarkdown(protocol.trim()));
            }
            sb.append("\n");
        }
        sb.append("\n");
    }

    private String escapeMarkdown(String text) {
        return text.replace("\n", "<br>").replace("*", "\\*").replace("_", "\\_");
    }
}