package com.sap.flowdeconstruct;

import com.sap.flowdeconstruct.ui.MainWindow;
import com.sap.flowdeconstruct.ui.SystemTrayManager;
import com.sap.flowdeconstruct.core.ProjectManager;
import com.sap.flowdeconstruct.i18n.I18n;

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
        
        // Initialize i18n before creating UI
        I18n.initFromPreferences();
        
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
                I18n.t("systemtray.unavailable.message"), 
                I18n.t("systemtray.unavailable.title"), 
                JOptionPane.INFORMATION_MESSAGE);
        }
        
        // Initialize core components
        projectManager = new ProjectManager();
        
        // Load last project or create new one BEFORE creating the main window,
        // so MainWindow can immediately bind a non-null FlowDiagram
        projectManager.loadLastProject();
        
        // Initialize UI components
        mainWindow = new MainWindow(projectManager);
        if (traySupported) {
            trayManager = new SystemTrayManager(mainWindow);
        } else {
            trayManager = null; // Explicit for clarity
        }
        
        // Setup application behavior
        setupApplicationBehavior();
        
        // Always show main window on startup to avoid confusion when tray is supported
        showMainWindow();
        
        // Extra: ensure the window really comes to front on startup (single-shot)
        javax.swing.Timer startupBringToFrontTimer = new javax.swing.Timer(450, e -> {
            bringToFrontReliably();
        });
        startupBringToFrontTimer.setRepeats(false);
        startupBringToFrontTimer.start();
        
        if (traySupported && trayManager != null && trayManager.isInitialized()) {
            // Provide a gentle hint that app lives in the tray after being closed
            trayManager.showNotification(I18n.t("app.name"), I18n.t("tray.tooltip"));
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
                // Do not intercept while a Dialog is active (e.g., ExportDialog)
                Window active = KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
                if (active instanceof Dialog) {
                    return false; // let dialog handle it
                }
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
    
    // Ensures the window is brought to the very front reliably on Windows
    private void bringToFrontReliably() {
        if (mainWindow == null) return;
        SwingUtilities.invokeLater(() -> {
            try {
                mainWindow.setVisible(true);
                mainWindow.setState(JFrame.NORMAL);
                mainWindow.toFront();
                mainWindow.requestFocus();
                // Temporarily set always-on-top to guarantee frontmost, then revert (single-shot revert)
                boolean previousAlwaysOnTop = mainWindow.isAlwaysOnTop();
                mainWindow.setAlwaysOnTop(true);
                javax.swing.Timer revertAlwaysOnTopTimer = new javax.swing.Timer(700, new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        mainWindow.setAlwaysOnTop(previousAlwaysOnTop);
                    }
                });
                revertAlwaysOnTopTimer.setRepeats(false);
                revertAlwaysOnTopTimer.start();
            } catch (Exception ignored) {}
        });
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