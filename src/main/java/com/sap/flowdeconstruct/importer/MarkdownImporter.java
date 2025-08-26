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

        for (String rawLine : lines) {
            String trimmed = rawLine.trim();
            if (trimmed.isEmpty()) continue;

            // Remove possíveis marcadores de lista antes de analisar ("- ", "* ", "1. ", etc.)
            String normalized = stripListMarker(trimmed);

            if (normalized.startsWith("# ")) {
                currentFlow.setName(normalized.substring(2).trim());
                inConnectionsSection = false;
            } else if (isConnectionsHeading(normalized)) {
                inConnectionsSection = true;
            } else if (!inConnectionsSection) {
                // Parse node
                if (normalized.startsWith("[")) {
                    int idEnd = normalized.indexOf("]");
                    if (idEnd != -1) {
                        String id = normalized.substring(1, idEnd).trim();
                        String text = normalized.substring(idEnd + 1).trim();
                        // Accept any valid ID format, not just UUID
                        if (id.length() > 0 && !id.contains("[") && !id.contains("]")) {
                            String unescapedText = unescapeMarkdown(text);
                            FlowNode flowNode = new FlowNode(unescapedText);
                            flowNode.setId(id);
                            currentFlow.addNode(flowNode);
                            currentNode = flowNode;
                        }
                    }
                } else if ((normalized.startsWith("Position:")) && currentNode != null) {
                    // Parse position line: Position: x, y
                    try {
                        String coords = normalized.substring("Position:".length()).trim();
                        String[] parts = coords.split(",");
                        if (parts.length >= 2) {
                            int x = Integer.parseInt(parts[0].trim());
                            int y = Integer.parseInt(parts[1].trim());
                            currentNode.setPosition(x, y);
                        }
                    } catch (Exception ignore) {
                        // If parsing fails, keep default position (0,0)
                    }
                } else if ((normalized.startsWith("Size:")) && currentNode != null) {
                    // Parse size line: Size: w, h
                    try {
                        String dims = normalized.substring("Size:".length()).trim();
                        String[] parts = dims.split(",");
                        if (parts.length >= 2) {
                            int w = Integer.parseInt(parts[0].trim());
                            int h = Integer.parseInt(parts[1].trim());
                            currentNode.setWidth(w);
                            currentNode.setHeight(h);
                        }
                    } catch (Exception ignore) {
                        // keep defaults
                    }
                } else if ((normalized.startsWith("Shape:")) && currentNode != null) {
                    try {
                        String shapeStr = normalized.substring("Shape:".length()).trim();
                        // aceitar sinônimo comum
                        if ("ELLIPSE".equalsIgnoreCase(shapeStr)) {
                            shapeStr = "OVAL";
                        }
                        currentNode.setShape(FlowNode.NodeShape.valueOf(shapeStr));
                    } catch (Exception ignore) {
                        // keep default RECTANGLE
                    }
                } else if ((normalized.startsWith("FillColor:")) && currentNode != null) {
                    String color = normalized.substring("FillColor:".length()).trim();
                    currentNode.setFillColorHex(color);
                } else if ((normalized.startsWith("BorderColor:")) && currentNode != null) {
                    String color = normalized.substring("BorderColor:".length()).trim();
                    currentNode.setBorderColorHex(color);
                } else if ((normalized.startsWith("TextColor:")) && currentNode != null) {
                    String color = normalized.substring("TextColor:".length()).trim();
                    currentNode.setTextColorHex(color);
                } else if ((normalized.startsWith("TextFontFamily:")) && currentNode != null) {
                    String fam = normalized.substring("TextFontFamily:".length()).trim();
                    currentNode.setTextFontFamily(fam);
                } else if ((normalized.startsWith("TextFontSize:")) && currentNode != null) {
                    String szStr = normalized.substring("TextFontSize:".length()).trim();
                    try { currentNode.setTextFontSize(Integer.parseInt(szStr)); } catch (Exception ignore) {}
                } else if ((normalized.startsWith("TextFontBold:")) && currentNode != null) {
                    String val = normalized.substring("TextFontBold:".length()).trim();
                    boolean bold = Boolean.parseBoolean(val);
                    int style = currentNode.getTextFontStyle();
                    if (bold) style |= java.awt.Font.BOLD; else style &= ~java.awt.Font.BOLD;
                    currentNode.setTextFontStyle(style);
                } else if ((normalized.startsWith("TextFontItalic:")) && currentNode != null) {
                    String val = normalized.substring("TextFontItalic:".length()).trim();
                    boolean italic = Boolean.parseBoolean(val);
                    int style = currentNode.getTextFontStyle();
                    if (italic) style |= java.awt.Font.ITALIC; else style &= ~java.awt.Font.ITALIC;
                    currentNode.setTextFontStyle(style);
                } else if ((normalized.startsWith("*Notes:") || normalized.startsWith("Notes:")) && currentNode != null) {
                    // Parse note line
                    String noteText = normalized;
                    if (noteText.startsWith("*Notes:")) {
                        noteText = noteText.substring(7).trim();
                        if (noteText.endsWith("*")) {
                            noteText = noteText.substring(0, noteText.length() - 1);
                        }
                    } else if (noteText.startsWith("Notes:")) {
                        noteText = noteText.substring(6).trim();
                    }
                    // Unescape markdown to match exporter
                    String unescaped = unescapeMarkdown(noteText);
                    currentNode.setNotes(unescaped.trim());
                }
            } else {
                // Parse connection
                String connLine = normalized;
                if (connLine.contains("From:") && connLine.contains("To:")) {
                    try {
                        String from = null;
                        String to = null;
                        String type = "NORMAL";
                        String direction = null;
                        String protocol = null;
                        String lineColor = null;
                        String arrowColor = null;

                        // Tokenize by spaces but keep simple parsing for labeled fields
                        String[] parts = connLine.split(" ");
                        for (int i = 0; i < parts.length; i++) {
                            String p = parts[i];
                            if ("From:".equals(p) && i + 1 < parts.length) {
                                from = parts[++i];
                            } else if ("To:".equals(p) && i + 1 < parts.length) {
                                to = parts[++i];
                            } else if (p.startsWith("(") && p.endsWith(")")) {
                                type = p.substring(1, p.length() - 1);
                            } else if ("Direction:".equals(p) && i + 1 < parts.length) {
                                direction = parts[++i];
                            } else if ("LineColor:".equals(p) && i + 1 < parts.length) {
                                lineColor = parts[++i];
                            } else if ("ArrowColor:".equals(p) && i + 1 < parts.length) {
                                arrowColor = parts[++i];
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
                                    // Direction style (tolerar sinônimos comuns)
                                    if (direction != null) {
                                        try {
                                            String dirUp = direction.toUpperCase();
                                            if ("FORWARD".equals(dirUp)) {
                                                dirUp = "FROM_TO";
                                            } else if ("BACKWARD".equals(dirUp) || "REVERSE".equals(dirUp)) {
                                                dirUp = "TO_FROM";
                                            }
                                            conn.setDirectionStyle(FlowConnection.DirectionStyle.valueOf(dirUp));
                                        } catch (IllegalArgumentException e) {
                                            // default already set in model
                                        }
                                    }
                                    if (lineColor != null) {
                                        conn.setLineColorHex(lineColor);
                                    }
                                    if (arrowColor != null) {
                                        conn.setArrowColorHex(arrowColor);
                                    }
                                    // Protocol
                                    if (protocol != null) {
                                        // Unescape markdown special characters similar to exporter
                                        String unescaped = unescapeMarkdown(protocol);
                                        conn.setProtocol(unescaped);
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("Error parsing connection: " + connLine);
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
            } else if (node instanceof OrderedList) {
                Node item = node.getFirstChild();
                while (item != null) {
                    String itemText = getTextContent(item);
                    for (String l : itemText.split("\n")) {
                        String s = l.trim();
                        if (!s.isEmpty()) lines.add(s);
                    }
                    item = item.getNext();
                }
            } else if (node instanceof FencedCodeBlock) {
                FencedCodeBlock cb = (FencedCodeBlock) node;
                String literal = cb.getLiteral();
                for (String l : literal.split("\n")) {
                    String s = l.trim();
                    if (!s.isEmpty()) lines.add(s);
                }
            } else if (node instanceof IndentedCodeBlock) {
                IndentedCodeBlock cb = (IndentedCodeBlock) node;
                String literal = cb.getLiteral();
                for (String l : literal.split("\n")) {
                    String s = l.trim();
                    if (!s.isEmpty()) lines.add(s);
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

    private String stripListMarker(String s) {
        // Remove marcadores comuns de lista/enumeração no início da linha
        return s.replaceFirst("^(?:[-*]\\s+|\\d+\\.\\s+)", "");
    }

    private boolean isConnectionsHeading(String line) {
        String l = line.trim();
        // Aceitar qualquer nível de heading começando com '#'
        if (!l.startsWith("#")) return false;
        // Remover todos os '#', espaços iniciais e dois-pontos opcionais ao final
        l = l.replaceFirst("^#+\\s*", "").trim();
        if (l.endsWith(":")) {
            l = l.substring(0, l.length() - 1).trim();
        }
        // Aceitar EN e PT, com ou sem acentuação
        if (l.equalsIgnoreCase("connections")) return true;
        if (l.equalsIgnoreCase("conexoes")) return true;
        if (l.equalsIgnoreCase("conexões")) return true;
        return false;
    }

    // Centraliza a lógica de unescape para alinhar com o escape do exporter
    private String unescapeMarkdown(String s) {
        if (s == null) return null;
        return s.replace("<br>", "\n").replace("\\*", "*").replace("\\_", "_");
    }
}