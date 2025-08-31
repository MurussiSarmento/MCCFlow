package com.sap.flowdeconstruct.export;

import com.sap.flowdeconstruct.model.FlowDiagram;
import com.sap.flowdeconstruct.model.FlowNode;
import com.sap.flowdeconstruct.i18n.I18n;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFPictureShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextBox;
// removed: import org.apache.poi.xslf.usermodel.XSLFSlideMaster;
// removed: import org.apache.poi.xslf.usermodel.XSLFSlideLayout;
// removed: import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.apache.poi.xslf.usermodel.XSLFTextParagraph;
import org.apache.poi.xslf.usermodel.XSLFTextRun;
// removed: import org.apache.poi.sl.usermodel.SlideLayout;
import org.apache.poi.sl.usermodel.PictureData;
import org.apache.poi.xslf.usermodel.XSLFPictureData;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.awt.Dimension;

public class PptxExporter {

    private final ExportRenderer renderer = new ExportRenderer();

    public void export(FlowDiagram flow, String filePath, boolean includeNotes, boolean includeSubflows) throws IOException {
        export(flow, filePath, includeNotes, includeSubflows, true, true);
    }

    public void export(FlowDiagram flow, String filePath, boolean includeNotes, boolean includeSubflows, boolean includeFlow, boolean includeTimeline) throws IOException {
        if (flow == null) throw new IOException("No flow to export");

        XMLSlideShow ppt = new XMLSlideShow();
        try {
            // Title slide
            XSLFSlide titleSlide = ppt.createSlide();
            addTitle(titleSlide, safeTitle(flow.getName()));
            
            // Flow image slide
            if (includeFlow) {
                BufferedImage flowImg = renderer.renderFlowImage(flow, 1920, 1080);
                if (flowImg != null) {
                    XSLFSlide slide = ppt.createSlide();
                    addTitle(slide, I18n.t("export.dialog.include.flow"));
                    addImageSlide(slide, flowImg);
                }
            }

            // Timeline slide
            if (includeTimeline) {
                BufferedImage timelineImg = renderer.renderTimelineImage(flow, 1920);
                if (timelineImg != null && timelineImg.getWidth() > 1 && timelineImg.getHeight() > 1) {
                    XSLFSlide slide = ppt.createSlide();
                    addTitle(slide, I18n.t("export.dialog.include.timeline"));
                    addImageSlide(slide, timelineImg);
                }
            }

            // Notes slide
            if (includeNotes) {
                String notes = buildNotes(flow);
                if (notes != null && !notes.trim().isEmpty()) {
                    XSLFSlide slide = ppt.createSlide();
                    addTitle(slide, I18n.t("export.dialog.include.notes"));
                    addBody(slide, notes);
                }
            }

            // Subflows
            if (includeSubflows) {
                for (FlowNode n : flow.getNodes()) {
                    if (n.hasSubFlow()) {
                        FlowDiagram child = n.getSubFlow();
                        String prefix = safeTitle(flow.getName()) + " > " + (n.getText() == null ? I18n.t("export.dialog.header") : n.getText());

                        if (includeFlow) {
                            BufferedImage img = renderer.renderFlowImage(child, 1920, 1080);
                            if (img != null) {
                                XSLFSlide slide = ppt.createSlide();
                                addTitle(slide, prefix + " - " + I18n.t("export.dialog.include.flow"));
                                addImageSlide(slide, img);
                            }
                        }
                        if (includeTimeline) {
                            BufferedImage img = renderer.renderTimelineImage(child, 1920);
                            if (img != null && img.getWidth() > 1 && img.getHeight() > 1) {
                                XSLFSlide slide = ppt.createSlide();
                                addTitle(slide, prefix + " - " + I18n.t("export.dialog.include.timeline"));
                                addImageSlide(slide, img);
                            }
                        }
                        if (includeNotes) {
                            String ntext = buildNotes(child);
                            if (ntext != null && !ntext.trim().isEmpty()) {
                                XSLFSlide slide = ppt.createSlide();
                                addTitle(slide, prefix + " - " + I18n.t("export.dialog.include.notes"));
                                addBody(slide, ntext);
                            }
                        }
                    }
                }
            }

            try (FileOutputStream out = new FileOutputStream(filePath)) {
                ppt.write(out);
            }
        } finally {
            ppt.close();
        }
    }

    private void addTitle(XSLFSlide slide, String text) {
        java.awt.Dimension pg = slide.getSlideShow().getPageSize();
        int left = 50, right = 50, top = 20, height = 60;
        int width = Math.max(0, pg.width - left - right);
        XSLFTextBox title = slide.createTextBox();
        title.setAnchor(new java.awt.Rectangle(left, top, width, height));
        XSLFTextParagraph p = title.addNewTextParagraph();
        XSLFTextRun r = p.addNewTextRun();
        r.setText(text);
        r.setBold(true);
        r.setFontSize(28.0);
    }

    private void addBody(XSLFSlide slide, String text) {
        java.awt.Dimension pg = slide.getSlideShow().getPageSize();
        int left = 50, right = 50, top = 100, bottom = 50;
        int width = Math.max(0, pg.width - left - right);
        int height = Math.max(0, pg.height - top - bottom);
        XSLFTextBox box = slide.createTextBox();
        box.setAnchor(new java.awt.Rectangle(left, top, width, height));
        XSLFTextParagraph p = box.addNewTextParagraph();
        for (String line : text.split("\n")) {
            XSLFTextRun r = p.addNewTextRun();
            r.setText(line);
            p = box.addNewTextParagraph();
        }
    }

    private void addImageSlide(XSLFSlide slide, BufferedImage img) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);
        byte[] data = baos.toByteArray();
        XSLFPictureData pd = slide.getSlideShow().addPicture(data, PictureData.PictureType.PNG);
        XSLFPictureShape pic = slide.createPicture(pd);

        // Compute available area based on real slide size and margins
        java.awt.Dimension pg = slide.getSlideShow().getPageSize();
        int left = 50, right = 50, top = 100, bottom = 50; // keep space for title at top
        int availW = Math.max(0, pg.width - left - right);
        int availH = Math.max(0, pg.height - top - bottom);

        int imgW = img.getWidth();
        int imgH = img.getHeight();
        if (imgW <= 0 || imgH <= 0 || availW <= 0 || availH <= 0) {
            // Fallback to full area if something is off
            pic.setAnchor(new java.awt.Rectangle(left, top, availW, availH));
            return;
        }
        double scale = Math.min((double) availW / imgW, (double) availH / imgH);
        int drawW = (int) Math.round(imgW * scale);
        int drawH = (int) Math.round(imgH * scale);
        int x = left + (availW - drawW) / 2;
        int y = top + (availH - drawH) / 2;

        pic.setAnchor(new java.awt.Rectangle(x, y, drawW, drawH));
    }

    private String buildNotes(FlowDiagram diagram) {
        StringBuilder sb = new StringBuilder();
        for (FlowNode n : diagram.getNodes()) {
            String notes = n.getNotes();
            if (notes != null && !notes.trim().isEmpty()) {
                sb.append("\u2022 ").append(n.getText() == null ? "" : n.getText()).append(": ")
                  .append(notes.trim()).append("\n");
            }
        }
        return sb.toString();
    }

    private String safeTitle(String s) {
        return (s == null || s.trim().isEmpty()) ? I18n.t("export.dialog.header") : s.trim();
    }
}