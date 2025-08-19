package com.sap.flowdeconstruct;

import com.sap.flowdeconstruct.ui.MainWindow;
import com.sap.flowdeconstruct.ui.SystemTrayManager;
import com.sap.flowdeconstruct.core.ProjectManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * FlowDeconstruct - Ultra-fast hierarchical flow mapping tool
 * Main application entry point
 * 
 * @author SAP Mission Critical Center
 * @version 1.0.0
 */
public class FlowDeconstructApp {
    
    private static final String APP_NAME = "FlowDeconstruct";
    private static final String APP_VERSION = "1.0.0";
    
    private MainWindow mainWindow;
    private SystemTrayManager trayManager;
    private ProjectManager projectManager;
    
    public static void main(String[] args) {
        // Set system properties for better performance and appearance
        System.setProperty("sun.java2d.opengl", "true");
        System.setProperty("swing.aatext", "true");
        System.setProperty("awt.useSystemAAFontSettings", "on");
        
        // Use system look and feel but with dark theme
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // Fallback to default
        }
        
        // Ensure we're on the Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            new FlowDeconstructApp().initialize();
        });
    }
    
    private void initialize() {
        // Check if system tray is supported (do NOT exit app if unsupported)
        boolean traySupported = SystemTray.isSupported();
        if (!traySupported) {
            JOptionPane.showMessageDialog(null, 
                "System tray is not supported on this platform.\n" +
                "FlowDeconstruct will start with the main window visible.", 
                "System Tray Unavailable", 
                JOptionPane.INFORMATION_MESSAGE);
        }
        
        // Initialize core components
        projectManager = new ProjectManager();
        
        // Initialize UI components
        mainWindow = new MainWindow(projectManager);
        if (traySupported) {
            trayManager = new SystemTrayManager(mainWindow);
        } else {
            trayManager = null; // Explicit for clarity
        }
        
        // Setup application behavior
        setupApplicationBehavior();
        
        // Load last project or create new one
        projectManager.loadLastProject();
        
        // Always show main window on startup to avoid confusion when tray is supported
        showMainWindow();
        if (traySupported && trayManager != null && trayManager.isInitialized()) {
            // Provide a gentle hint that app lives in the tray after being closed
            trayManager.showNotification(APP_NAME, "Running in system tray. Close window to minimize.");
        }
        
        System.out.println(APP_NAME + " v" + APP_VERSION + " initialized successfully.");
    }
    
    private void setupApplicationBehavior() {
        // Configure main window close behavior
        mainWindow.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        mainWindow.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                // Minimize to tray instead of closing
                mainWindow.setVisible(false);
            }
        });
        
        // Setup hotkey (note: this is app-scoped, not OS-global)
        setupGlobalHotkey();
    }
    
    private void setupGlobalHotkey() {
        // Register hotkey Ctrl+Shift+F to show/focus window while app has focus
        KeyboardFocusManager.getCurrentKeyboardFocusManager()
            .addKeyEventDispatcher(e -> {
                if (e.getID() == java.awt.event.KeyEvent.KEY_PRESSED &&
                    e.isControlDown() && e.isShiftDown() && 
                    e.getKeyCode() == java.awt.event.KeyEvent.VK_F) {
                    
                    SwingUtilities.invokeLater(() -> {
                        if (mainWindow.isVisible()) {
                            mainWindow.toFront();
                            mainWindow.requestFocus();
                        } else {
                            showMainWindow();
                        }
                    });
                    return true;
                }
                return false;
            });
    }
    
    public void showMainWindow() {
        mainWindow.setVisible(true);
        mainWindow.setState(JFrame.NORMAL);
        mainWindow.toFront();
        mainWindow.requestFocus();
    }
    
    public void exitApplication() {
        // Save current project
        projectManager.saveCurrentProject();
        
        // Clean up system tray
        if (trayManager != null) {
            trayManager.cleanup();
        }
        
        // Exit application
        System.exit(0);
    }
    
    public String getAppName() {
        return APP_NAME;
    }
    
    public String getAppVersion() {
        return APP_VERSION;
    }
}