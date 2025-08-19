package com.sap.flowdeconstruct.markdown;

import com.sap.flowdeconstruct.export.MarkdownExporter;
import com.sap.flowdeconstruct.importer.MarkdownImporter;
import com.sap.flowdeconstruct.model.FlowConnection;
import com.sap.flowdeconstruct.model.FlowDiagram;
import com.sap.flowdeconstruct.model.FlowNode;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

public class MarkdownDebugTest {

    @Test
    public void debugExportImport() throws Exception {
        // Create simple diagram
        FlowDiagram original = new FlowDiagram("Test Flow");
        FlowNode a = new FlowNode("Alpha");
        FlowNode b = new FlowNode("Beta");
        original.addNode(a);
        original.addNode(b);
        FlowConnection c = original.addConnection(a, b);
        
        // Export
        Path tempFile = Files.createTempFile("debug-", ".md");
        new MarkdownExporter().export(original, tempFile.toString(), true, false);
        
        // Print exported content
        String content = new String(Files.readAllBytes(tempFile));
        System.out.println("EXPORTED CONTENT:");
        System.out.println(content);
        System.out.println("END EXPORTED CONTENT");
        
        // Import
        FlowDiagram imported = new MarkdownImporter().importFlow(tempFile.toString());
        
        // Print results
        System.out.println("Original: " + original.getNodeCount() + " nodes, " + original.getConnectionCount() + " connections");
        System.out.println("Imported: " + imported.getNodeCount() + " nodes, " + imported.getConnectionCount() + " connections");
        System.out.println("Imported name: '" + imported.getName() + "'");
        
        // Delete temp file
        Files.delete(tempFile);
    }
}