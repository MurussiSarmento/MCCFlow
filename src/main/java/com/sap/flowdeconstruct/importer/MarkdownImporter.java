package com.sap.flowdeconstruct.importer;

import com.sap.flowdeconstruct.model.FlowDiagram;
import com.sap.flowdeconstruct.model.FlowNode;
import com.sap.flowdeconstruct.model.FlowConnection;
import org.commonmark.node.*;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.text.TextContentRenderer;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class MarkdownImporter {

    public FlowDiagram importFlow(String filePath) throws IOException {
        String content = new String(Files.readAllBytes(Paths.get(filePath)));
        Parser parser = Parser.builder().build();
        Node document = parser.parse(content);
        return parseDocument(document);
    }

    private FlowDiagram parseDocument(Node document) {
        FlowDiagram currentFlow = new FlowDiagram();
        List<String> lines = getAllLines(document);
        boolean inConnectionsSection = false;
        FlowNode currentNode = null;
    
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;
    
            if (trimmed.startsWith("# ")) {
                currentFlow.setName(trimmed.substring(2).trim());
                inConnectionsSection = false;
            } else if (trimmed.startsWith("## Connections")) {
                inConnectionsSection = true;
            } else if (!inConnectionsSection) {
                // Parse node
                if (trimmed.startsWith("[")) {
                    int idEnd = trimmed.indexOf("]");
                    if (idEnd != -1) {
                        String id = trimmed.substring(1, idEnd).trim();
                        String text = trimmed.substring(idEnd + 1).trim();
                        // Accept any valid ID format, not just UUID
                        if (id.length() > 0 && !id.contains("[") && !id.contains("]")) {
                            FlowNode flowNode = new FlowNode(text);
                            flowNode.setId(id);
                            currentFlow.addNode(flowNode);
                            currentNode = flowNode;
                        }
                    }
                } else if (trimmed.startsWith("*Notes:") && currentNode != null) {
                    // Parse note line: "*Notes: some note text*"
                    String noteText = trimmed.substring(7).trim(); // Remove "*Notes:"
                    if (noteText.endsWith("*")) {
                        noteText = noteText.substring(0, noteText.length() - 1); // Remove trailing "*"
                    }
                    currentNode.setNotes(noteText.trim());
                }
            } else {
                // Parse connection
                if (trimmed.contains("From:") && trimmed.contains("To:")) {
                    try {
                        String[] parts = trimmed.split(" ");
                        String from = null;
                        String to = null;
                        String type = "NORMAL";
                        for (int i = 0; i < parts.length; i++) {
                            if (parts[i].equals("From:")) from = parts[i+1];
                            else if (parts[i].equals("To:")) to = parts[i+1];
                            else if (parts[i].startsWith("(") && parts[i].endsWith(")")) type = parts[i].substring(1, parts[i].length()-1);
                        }
                        if (from != null && to != null) {
                            FlowNode fromNode = currentFlow.findNodeById(from);
                            FlowNode toNode = currentFlow.findNodeById(to);
                            if (fromNode != null && toNode != null) {
                                FlowConnection conn = currentFlow.addConnection(fromNode, toNode);
                                if (conn != null) {
                                    try {
                                        conn.setType(FlowConnection.ConnectionType.valueOf(type.toUpperCase()));
                                    } catch (IllegalArgumentException e) {
                                        conn.setType(FlowConnection.ConnectionType.NORMAL);
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("Error parsing connection: " + trimmed);
                    }
                }
            }
        }
        return currentFlow;
    }

    private List<String> getAllLines(Node document) {
        List<String> lines = new ArrayList<>();
        Node node = document.getFirstChild();
        while (node != null) {
            if (node instanceof Heading) {
                Heading h = (Heading) node;
                StringBuilder hashes = new StringBuilder();
                for (int i = 0; i < h.getLevel(); i++) hashes.append('#');
                String text = getTextContent(node);
                lines.add(hashes.toString() + " " + text);
            } else if (node instanceof Paragraph) {
                String para = getTextContent(node);
                for (String l : para.split("\n")) {
                    String s = l.trim();
                    if (!s.isEmpty()) lines.add(s);
                }
            } else if (node instanceof BulletList) {
                Node item = node.getFirstChild();
                while (item != null) {
                    String itemText = getTextContent(item);
                    for (String l : itemText.split("\n")) {
                        String s = l.trim();
                        if (!s.isEmpty()) lines.add(s);
                    }
                    item = item.getNext();
                }
            }
            node = node.getNext();
        }
        return lines;
    }

    private String getTextContent(Node node) {
        TextContentRenderer renderer = TextContentRenderer.builder().build();
        return renderer.render(node).trim();
    }
}