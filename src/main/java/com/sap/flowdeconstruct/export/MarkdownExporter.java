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
            sb.append(indent).append("From: ").append(conn.getFromNodeId()).append(" To: ").append(conn.getToNodeId()).append(" (").append(conn.getType()).append(")\n");
        }
        sb.append("\n");
    }

    private String escapeMarkdown(String text) {
        return text.replace("\n", "<br>").replace("*", "\\*").replace("_", "\\_");
    }
}