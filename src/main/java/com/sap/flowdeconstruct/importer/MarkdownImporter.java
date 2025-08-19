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
                } else if ((trimmed.startsWith("Position:")) && currentNode != null) {
                    // Parse position line: Position: x, y
                    try {
                        String coords = trimmed.substring("Position:".length()).trim();
                        String[] parts = coords.split(",");
                        if (parts.length >= 2) {
                            int x = Integer.parseInt(parts[0].trim());
                            int y = Integer.parseInt(parts[1].trim());
                            currentNode.setPosition(x, y);
                        }
                    } catch (Exception ignore) {
                        // If parsing fails, keep default position (0,0)
                    }
                } else if ((trimmed.startsWith("*Notes:") || trimmed.startsWith("Notes:")) && currentNode != null) {
                    // Parse note line
                    String noteText = trimmed;
                    if (noteText.startsWith("*Notes:")) {
                        noteText = noteText.substring(7).trim();
                        if (noteText.endsWith("*")) {
                            noteText = noteText.substring(0, noteText.length() - 1);
                        }
                    } else if (noteText.startsWith("Notes:")) {
                        noteText = noteText.substring(6).trim();
                    }
                    currentNode.setNotes(noteText.trim());
                }
            } else {
                // Parse connection
                if (trimmed.contains("From:") && trimmed.contains("To:")) {
                    try {
                        String from = null;
                        String to = null;
                        String type = "NORMAL";
                        String direction = null;
                        String protocol = null;

                        // Tokenize by spaces but keep simple parsing for labeled fields
                        String[] parts = trimmed.split(" ");
                        for (int i = 0; i < parts.length; i++) {
                            String p = parts[i];
                            if ("From:".equals(p) && i + 1 < parts.length) {
                                from = parts[i + 1];
                                i++;
                            } else if ("To:".equals(p) && i + 1 < parts.length) {
                                to = parts[i + 1];
                                i++;
                            } else if (p.startsWith("(") && p.endsWith(")")) {
                                type = p.substring(1, p.length() - 1);
                            } else if ("Direction:".equals(p) && i + 1 < parts.length) {
                                direction = parts[i + 1];
                                i++;
                            } else if ("Protocol:".equals(p) && i + 1 < parts.length) {
                                // Protocol may contain spaces; capture the rest of the line after this token
                                StringBuilder protoSb = new StringBuilder();
                                for (int j = i + 1; j < parts.length; j++) {
                                    if (protoSb.length() > 0) protoSb.append(" ");
                                    protoSb.append(parts[j]);
                                }
                                protocol = protoSb.toString().trim();
                                break; // protocol is last meaningful field
                            }
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
                                    // Direction style
                                    if (direction != null) {
                                        try {
                                            conn.setDirectionStyle(FlowConnection.DirectionStyle.valueOf(direction.toUpperCase()));
                                        } catch (IllegalArgumentException e) {
                                            // default already set in model
                                        }
                                    }
                                    // Protocol
                                    if (protocol != null) {
                                        // Unescape markdown special characters similar to exporter
                                        String unescaped = protocol.replace("<br>", "\n").replace("\\*", "*").replace("\\_", "_");
                                        conn.setProtocol(unescaped);
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