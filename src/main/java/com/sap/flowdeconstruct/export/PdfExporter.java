package com.sap.flowdeconstruct.export;

import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.AreaBreak;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.sap.flowdeconstruct.i18n.I18n;
import com.sap.flowdeconstruct.model.FlowDiagram;
import com.sap.flowdeconstruct.model.FlowNode;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class PdfExporter {

    private final ExportRenderer renderer = new ExportRenderer();

    public void export(FlowDiagram flow, String filePath, boolean includeNotes, boolean includeSubflows) throws IOException {
        export(flow, filePath, includeNotes, includeSubflows, true, true);
    }

    public void export(FlowDiagram flow, String filePath, boolean includeNotes, boolean includeSubflows, boolean includeFlow, boolean includeTimeline) throws IOException {
        if (flow == null) throw new IOException("No flow to export");

        try (PdfWriter writer = new PdfWriter(new FileOutputStream(filePath));
             PdfDocument pdf = new PdfDocument(writer);
             Document doc = new Document(pdf, PageSize.A4)) {

            // Root flow section
            exportDiagram(doc, flow, safeTitle(flow.getName()), includeNotes, includeSubflows, includeFlow, includeTimeline, new HashSet<>());
        }
    }

    private void exportDiagram(Document doc, FlowDiagram diagram, String title,
                               boolean includeNotes, boolean includeSubflows,
                               boolean includeFlow, boolean includeTimeline,
                               Set<FlowDiagram> visited) throws IOException {
        if (diagram == null || visited.contains(diagram)) return;
        visited.add(diagram);

        if (title != null && !title.trim().isEmpty()) {
            doc.add(new Paragraph(title).setBold());
        } else {
            doc.add(new Paragraph(I18n.t("export.dialog.header")).setBold());
        }

        // Flow image
        if (includeFlow) {
            BufferedImage flowImg = renderer.renderFlowImage(diagram, 1600, 1000);
            if (flowImg != null) {
                Image flowImageEl = new Image(ImageDataFactory.create(toPNG(flowImg)));
                flowImageEl.setAutoScale(true);
                doc.add(flowImageEl);
            }
        }

        // Timeline image
        if (includeTimeline) {
            BufferedImage timelineImg = renderer.renderTimelineImage(diagram, 1600);
            if (timelineImg != null && timelineImg.getWidth() > 1 && timelineImg.getHeight() > 1) {
                doc.add(new AreaBreak());
                doc.add(new Paragraph(I18n.t("export.dialog.include.timeline")).setBold());
                Image timelineImageEl = new Image(ImageDataFactory.create(toPNG(timelineImg)));
                timelineImageEl.setAutoScale(true);
                doc.add(timelineImageEl);
            }
        }

        // Notes section
        if (includeNotes) {
            StringBuilder sb = new StringBuilder();
            for (FlowNode n : diagram.getNodes()) {
                String notes = n.getNotes();
                if (notes != null && !notes.trim().isEmpty()) {
                    sb.append("\u2022 ").append(n.getText() == null ? "" : n.getText()).append(": ")
                      .append(notes.trim()).append("\n");
                }
            }
            if (sb.length() > 0) {
                doc.add(new AreaBreak());
                doc.add(new Paragraph(I18n.t("export.dialog.include.notes")).setBold());
                doc.add(new Paragraph(sb.toString()));
            }
        }

        // Subflows
        if (includeSubflows) {
            for (FlowNode n : diagram.getNodes()) {
                if (n.hasSubFlow()) {
                    doc.add(new AreaBreak());
                    String childTitle = (title == null || title.isEmpty()) ? n.getText() : (title + " > " + n.getText());
                    exportDiagram(doc, n.getSubFlow(), childTitle, includeNotes, true, includeFlow, includeTimeline, visited);
                }
            }
        }
    }

    private byte[] toPNG(BufferedImage img) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);
        return baos.toByteArray();
    }

    private String safeTitle(String s) {
        return (s == null || s.trim().isEmpty()) ? I18n.t("export.dialog.header") : s.trim();
    }
}