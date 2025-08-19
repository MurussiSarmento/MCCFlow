package com.sap.flowdeconstruct.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.sap.flowdeconstruct.model.FlowDiagram;
import com.sap.flowdeconstruct.model.FlowNode;
import com.sap.flowdeconstruct.export.MarkdownExporter;
import com.sap.flowdeconstruct.importer.MarkdownImporter;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.prefs.Preferences;

/**
 * Manages project persistence, auto-save, and project lifecycle
 * Implements local-first storage as specified in PRD
 */
public class ProjectManager {
    
    private static final String APP_DATA_DIR = "FlowDeconstruct";
    private static final String PROJECTS_DIR = "projects";
    private static final String LAST_PROJECT_KEY = "lastProject";
    private static final String AUTO_SAVE_INTERVAL = "autoSaveInterval";
    private static final int DEFAULT_AUTO_SAVE_INTERVAL = 5000; // 5 seconds
    
    private final ObjectMapper objectMapper;
    private final Path appDataPath;
    private final Path projectsPath;
    private final Preferences preferences;
    private final Timer autoSaveTimer;
    
    private FlowDiagram currentProject;
    private String currentProjectPath;
    private boolean hasUnsavedChanges;
    private List<ProjectStateListener> listeners;
    
    public ProjectManager() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        
        this.preferences = Preferences.userNodeForPackage(ProjectManager.class);
        this.listeners = new ArrayList<>();
        
        // Initialize application data directory
        this.appDataPath = initializeAppDataDirectory();
        this.projectsPath = appDataPath.resolve(PROJECTS_DIR);
        
        try {
            Files.createDirectories(projectsPath);
        } catch (IOException e) {
            System.err.println("Failed to create projects directory: " + e.getMessage());
        }
        
        // Setup auto-save timer
        int autoSaveInterval = preferences.getInt(AUTO_SAVE_INTERVAL, DEFAULT_AUTO_SAVE_INTERVAL);
        this.autoSaveTimer = new Timer("AutoSave", true);
        this.autoSaveTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (hasUnsavedChanges && currentProject != null) {
                    saveCurrentProject();
                }
            }
        }, autoSaveInterval, autoSaveInterval);
    }
    
    private Path initializeAppDataDirectory() {
        String userHome = System.getProperty("user.home");
        String os = System.getProperty("os.name").toLowerCase();
        
        Path appData;
        if (os.contains("win")) {
            // Windows: %APPDATA%\FlowDeconstruct
            String appDataEnv = System.getenv("APPDATA");
            if (appDataEnv != null) {
                appData = Paths.get(appDataEnv, APP_DATA_DIR);
            } else {
                appData = Paths.get(userHome, "AppData", "Roaming", APP_DATA_DIR);
            }
        } else if (os.contains("mac")) {
            // macOS: ~/Library/Application Support/FlowDeconstruct
            appData = Paths.get(userHome, "Library", "Application Support", APP_DATA_DIR);
        } else {
            // Linux: ~/.config/FlowDeconstruct
            appData = Paths.get(userHome, ".config", APP_DATA_DIR);
        }
        
        try {
            Files.createDirectories(appData);
        } catch (IOException e) {
            System.err.println("Failed to create app data directory: " + e.getMessage());
            // Fallback to user home
            appData = Paths.get(userHome, "." + APP_DATA_DIR.toLowerCase());
            try {
                Files.createDirectories(appData);
            } catch (IOException ex) {
                throw new RuntimeException("Cannot create application data directory", ex);
            }
        }
        
        return appData;
    }
    
    public FlowDiagram getCurrentProject() {
        return currentProject;
    }
    
    public String getCurrentProjectPath() {
        return currentProjectPath;
    }
    
    public boolean hasUnsavedChanges() {
        return hasUnsavedChanges;
    }
    
    public FlowDiagram createNewProject() {
        return createNewProject("Untitled Project");
    }
    
    public FlowDiagram createNewProject(String name) {
        FlowDiagram newProject = new FlowDiagram(name);
        setCurrentProject(newProject, null);
        notifyListeners("projectCreated", null, newProject);
        return newProject;
    }
    
    public boolean loadProject(String projectPath) {
        try {
            Path path = Paths.get(projectPath);
            if (!Files.exists(path)) {
                System.err.println("Project file does not exist: " + projectPath);
                return false;
            }
            
            String json = new String(Files.readAllBytes(path), "UTF-8");
            FlowDiagram project = objectMapper.readValue(json, FlowDiagram.class);
            
            setCurrentProject(project, projectPath);
            preferences.put(LAST_PROJECT_KEY, projectPath);
            
            notifyListeners("projectLoaded", null, project);
            return true;
            
        } catch (IOException e) {
            System.err.println("Failed to load project: " + e.getMessage());
            return false;
        }
    }
    
    public boolean loadLastProject() {
        String lastProjectPath = preferences.get(LAST_PROJECT_KEY, null);
        if (lastProjectPath != null && !lastProjectPath.isEmpty()) {
            if (loadProject(lastProjectPath)) {
                return true;
            }
        }
        
        // If no last project or failed to load, create new project
        createNewProject();
        return false;
    }
    
    public boolean saveCurrentProject() {
        if (currentProject == null) {
            return false;
        }
        
        if (currentProjectPath == null) {
            // Generate new project path
            String fileName = sanitizeFileName(currentProject.getName()) + ".flowproj";
            currentProjectPath = projectsPath.resolve(fileName).toString();
        }
        
        return saveProject(currentProject, currentProjectPath);
    }
    
    public boolean saveProjectAs(String projectPath) {
        if (currentProject == null) {
            return false;
        }
        
        if (saveProject(currentProject, projectPath)) {
            currentProjectPath = projectPath;
            preferences.put(LAST_PROJECT_KEY, projectPath);
            return true;
        }
        
        return false;
    }
    
    private boolean saveProject(FlowDiagram project, String projectPath) {
        try {
            String json = objectMapper.writeValueAsString(project);
            Path path = Paths.get(projectPath);
            
            // Ensure parent directory exists
            Files.createDirectories(path.getParent());
            
            // Write to temporary file first, then rename (atomic operation)
            Path tempPath = path.resolveSibling(path.getFileName() + ".tmp");
            Files.write(tempPath, json.getBytes("UTF-8"));
            Files.move(tempPath, path, StandardCopyOption.REPLACE_EXISTING);
            
            hasUnsavedChanges = false;
            notifyListeners("projectSaved", null, project);
            
            return true;
            
        } catch (IOException e) {
            System.err.println("Failed to save project: " + e.getMessage());
            return false;
        }
    }
    
    public List<ProjectInfo> getRecentProjects() {
        List<ProjectInfo> recentProjects = new ArrayList<>();
        
        try {
            if (Files.exists(projectsPath)) {
                Files.walk(projectsPath)
                    .filter(path -> path.toString().endsWith(".flowproj"))
                    .sorted((p1, p2) -> {
                        try {
                            return Files.getLastModifiedTime(p2)
                                   .compareTo(Files.getLastModifiedTime(p1));
                        } catch (IOException e) {
                            return 0;
                        }
                    })
                    .limit(10) // Limit to 10 most recent
                    .forEach(path -> {
                        try {
                            ProjectInfo info = new ProjectInfo();
                            info.path = path.toString();
                            info.name = path.getFileName().toString().replace(".flowproj", "");
                            info.lastModified = Files.getLastModifiedTime(path).toInstant();
                            recentProjects.add(info);
                        } catch (IOException e) {
                            // Skip this file
                        }
                    });
            }
        } catch (IOException e) {
            System.err.println("Failed to scan recent projects: " + e.getMessage());
        }
        
        return recentProjects;
    }
    
    private void setCurrentProject(FlowDiagram project, String projectPath) {
        FlowDiagram oldProject = this.currentProject;
        
        this.currentProject = project;
        this.currentProjectPath = projectPath;
        this.hasUnsavedChanges = false;
        
        // Setup change tracking
        if (project != null) {
            project.addStateListener((diagram, event, oldValue, newValue) -> {
                hasUnsavedChanges = true;
                switch (event) {
                    case "nodeModified":
                        // A specific node was modified
                        notifyListeners("nodeModified", newValue, diagram);
                        break;
                    default:
                        // For other events, notify that the project as a whole was modified
                        notifyListeners("projectModified", null, diagram);
                        break;
                }
            });
        }
        
        notifyListeners("currentProjectChanged", oldProject, project);
    }
    
    private String sanitizeFileName(String fileName) {
        return fileName.replaceAll("[^a-zA-Z0-9.-]", "_");
    }
    
    public void markAsModified() {
        hasUnsavedChanges = true;
    }
    
    public void shutdown() {
        if (autoSaveTimer != null) {
            autoSaveTimer.cancel();
        }
        
        // Final save
        if (hasUnsavedChanges) {
            saveCurrentProject();
        }
    }
    
    // Listener management
    public void addStateListener(ProjectStateListener listener) {
        listeners.add(listener);
    }
    
    public void removeStateListener(ProjectStateListener listener) {
        listeners.remove(listener);
    }
    
    private void notifyListeners(String event, Object oldValue, Object newValue) {
        for (ProjectStateListener listener : listeners) {
            listener.onProjectStateChanged(event, oldValue, newValue);
        }
    }
    
    /**
     * Project information for recent projects list
     */
    public static class ProjectInfo {
        public String path;
        public String name;
        public java.time.Instant lastModified;
        
        @Override
        public String toString() {
            return name + " (" + lastModified + ")";
        }
    }
    
    public void saveToMarkdown(String filePath, boolean includeNotes, boolean includeSubflows) {
        if (currentProject == null) return;
        try {
            MarkdownExporter exporter = new MarkdownExporter();
            exporter.export(currentProject, filePath, includeNotes, includeSubflows);
        } catch (Exception e) {
            throw new RuntimeException("Failed to save to Markdown: " + e.getMessage(), e);
        }
    }

    public FlowDiagram loadFromMarkdown(String filePath) {
        try {
            MarkdownImporter importer = new MarkdownImporter();
            FlowDiagram flow = importer.importFlow(filePath);
            System.out.println("Loading Markdown from: " + filePath);
            setCurrentProject(flow, filePath);
            System.out.println("Loaded flow with name: " + flow.getName());
            notifyListeners("projectLoaded", null, flow);
            return flow;
        } catch (Exception e) {
            throw new RuntimeException("Failed to load from Markdown: " + e.getMessage(), e);
        }
    }
    
    /**
     * Interface for listening to project state changes
     */
    public interface ProjectStateListener {
        void onProjectStateChanged(String event, Object oldValue, Object newValue);
    }
}