package com.sap.flowdeconstruct.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;

/**
 * Manages system tray integration for FlowDeconstruct
 * Provides always-accessible entry point as specified in PRD
 */
public class SystemTrayManager {
    
    private final MainWindow mainWindow;
    private TrayIcon trayIcon;
    private SystemTray systemTray;
    
    public SystemTrayManager(MainWindow mainWindow) {
        this.mainWindow = mainWindow;
        initializeSystemTray();
    }
    
    private void initializeSystemTray() {
        if (!SystemTray.isSupported()) {
            System.err.println("System tray is not supported");
            return;
        }
        
        systemTray = SystemTray.getSystemTray();
        
        // Create tray icon
        Image trayImage = createTrayIcon();
        
        // Create popup menu
        PopupMenu popup = createPopupMenu();
        
        // Create tray icon
        trayIcon = new TrayIcon(trayImage, "FlowDeconstruct - Ultra-fast flow mapping", popup);
        trayIcon.setImageAutoSize(true);
        
        // Add double-click listener to show main window
        trayIcon.addActionListener(e -> showMainWindow());
        
        // Add to system tray
        try {
            systemTray.add(trayIcon);
        } catch (AWTException e) {
            System.err.println("Failed to add tray icon: " + e.getMessage());
        }
    }
    
    private Image createTrayIcon() {
        // Create a simple icon programmatically
        // In a real implementation, you would load this from resources
        int size = 16;
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        
        // Enable antialiasing
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Draw a simple flow diagram icon
        g2d.setColor(new Color(0x00, 0xAA, 0xFF)); // Blue color from design spec
        
        // Draw three connected boxes representing a flow
        int boxSize = 3;
        int spacing = 2;
        
        // First box
        g2d.fillRect(1, 6, boxSize, boxSize);
        
        // Connection line
        g2d.drawLine(4, 7, 6, 7);
        
        // Second box
        g2d.fillRect(6, 6, boxSize, boxSize);
        
        // Connection line
        g2d.drawLine(9, 7, 11, 7);
        
        // Third box
        g2d.fillRect(11, 6, boxSize, boxSize);
        
        // Arrow head on the last connection
        g2d.drawLine(11, 6, 10, 7);
        g2d.drawLine(11, 8, 10, 7);
        
        g2d.dispose();
        
        return image;
    }
    
    private PopupMenu createPopupMenu() {
        PopupMenu popup = new PopupMenu();
        
        // Open FlowDeconstruct
        MenuItem openItem = new MenuItem("Open FlowDeconstruct");
        openItem.addActionListener(e -> showMainWindow());
        popup.add(openItem);
        
        popup.addSeparator();
        
        // New Project
        MenuItem newProjectItem = new MenuItem("New Project");
        newProjectItem.addActionListener(e -> {
            showMainWindow();
            mainWindow.createNewProject();
        });
        popup.add(newProjectItem);
        
        // Recent Projects submenu would go here in a full implementation
        
        popup.addSeparator();
        
        // About
        MenuItem aboutItem = new MenuItem("About FlowDeconstruct");
        aboutItem.addActionListener(e -> showAboutDialog());
        popup.add(aboutItem);
        
        // Exit
        MenuItem exitItem = new MenuItem("Exit");
        exitItem.addActionListener(e -> exitApplication());
        popup.add(exitItem);
        
        return popup;
    }
    
    private void showMainWindow() {
        SwingUtilities.invokeLater(() -> {
            if (mainWindow != null) {
                mainWindow.setVisible(true);
                mainWindow.setState(JFrame.NORMAL);
                mainWindow.toFront();
                mainWindow.requestFocus();
            }
        });
    }
    
    private void showAboutDialog() {
        SwingUtilities.invokeLater(() -> {
            String message = "FlowDeconstruct v1.0.0\n\n" +
                           "Ultra-fast hierarchical flow mapping tool\n" +
                           "for technical triage specialists\n\n" +
                           "Developed for SAP Mission Critical Center\n\n" +
                           "Keyboard Shortcuts:\n" +
                           "• Tab - Create connected node\n" +
                           "• Ctrl+Enter - Drill down to subflow\n" +
                           "• Ctrl+N - Add note to selected node\n" +
                           "• Ctrl+E - Export flow\n" +
                           "• Arrow Keys - Navigate nodes\n" +
                           "• Esc - Go back / Cancel\n" +
                           "• ? - Show help";
            
            JOptionPane.showMessageDialog(null, message, "About FlowDeconstruct", 
                                        JOptionPane.INFORMATION_MESSAGE);
        });
    }
    
    private void exitApplication() {
        SwingUtilities.invokeLater(() -> {
            if (mainWindow != null) {
                mainWindow.exitApplication();
            }
        });
    }
    
    public void showNotification(String title, String message) {
        showNotification(title, message, TrayIcon.MessageType.INFO);
    }
    
    public void showNotification(String title, String message, TrayIcon.MessageType messageType) {
        if (trayIcon != null) {
            trayIcon.displayMessage(title, message, messageType);
        }
    }
    
    public void updateTooltip(String tooltip) {
        if (trayIcon != null) {
            trayIcon.setToolTip(tooltip);
        }
    }
    
    public void cleanup() {
        if (systemTray != null && trayIcon != null) {
            systemTray.remove(trayIcon);
        }
    }
}