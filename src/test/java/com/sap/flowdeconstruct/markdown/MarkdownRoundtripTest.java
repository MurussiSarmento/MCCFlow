package com.sap.flowdeconstruct.markdown;

import com.sap.flowdeconstruct.export.MarkdownExporter;
import com.sap.flowdeconstruct.importer.MarkdownImporter;
import com.sap.flowdeconstruct.model.FlowConnection;
import com.sap.flowdeconstruct.model.FlowDiagram;
import com.sap.flowdeconstruct.model.FlowNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

public class MarkdownRoundtripTest {

    @Test
    public void exportThenImport_ShouldPreserveBasicStructure() throws Exception {
        // Arrange: build a simple diagram
        FlowDiagram original = new FlowDiagram("Test Flow");
        FlowNode a = new FlowNode("Alpha");
        a.setNotes("First note");
        FlowNode b = new FlowNode("Beta");
        b.setNotes("Second note");
        original.addNode(a);
        original.addNode(b);
        FlowConnection c = original.addConnection(a, b);
        Assertions.assertNotNull(c, "Connection should be created");

        // Export to a temp markdown file
        Path tempFile = Files.createTempFile("flowdeconstruct-roundtrip-", ".md");
        tempFile.toFile().deleteOnExit();
        new MarkdownExporter().export(original, tempFile.toString(), true, false);

        // Act: import back
        FlowDiagram imported = new MarkdownImporter().importFlow(tempFile.toString());

        // Assert: basic structure
        Assertions.assertEquals("Test Flow", imported.getName());
        Assertions.assertEquals(2, imported.getNodeCount());
        Assertions.assertEquals(1, imported.getConnectionCount());

        // Check that both node IDs from original exist in the imported diagram
        boolean hasA = imported.findNodeById(a.getId()) != null;
        boolean hasB = imported.findNodeById(b.getId()) != null;
        Assertions.assertTrue(hasA && hasB, "Imported diagram should contain both original node IDs");

        // Optional: validate connection endpoints exist
        FlowConnection importedConn = imported.getConnections().get(0);
        Assertions.assertNotNull(imported.findNodeById(importedConn.getFromNodeId()));
        Assertions.assertNotNull(imported.findNodeById(importedConn.getToNodeId()));

        // Cleanup
        new File(tempFile.toString()).delete();
    }
}