# FlowDeconstruct - Ultra-Fast Flow Mapping Tool

**Ultra-fast hierarchical flow mapping for technical process analysis.** A keyboard-first application designed for SAP technical specialists to quickly map and analyze hierarchical processes in real-time during critical system incidents.

## 🚀 Quick Start

### Prerequisites

- **SAP JVM 8** (installed at `C:\Program Files\sapjvm\sapjvm_8`)
- **Windows 10/11** (primary target platform)
- **Administrative privileges** (for setup)

### Installation & Setup

1. **Automated Setup** (Recommended):
```powershell
# Run as Administrator
.\setup-environment.ps1
```

2. **Build and Run**:
```powershell
# Build the application
.\build-jar.ps1

# Run the application
.\FlowDeconstruct.bat
```

The application will start minimized in the Windows system tray (next to the clock).

## ⚡ Core Features

### 🏃 Speed-First Design
- **Sub-500ms startup** - Ready when you need it
- **Keyboard-driven workflow** - Navigate without touching the mouse
- **Real-time auto-layout** - Automatic node positioning and connection routing
- **Instant save** - Never lose your work

### 🔄 Hierarchical Flow Mapping
- **Drill-down capability** - Navigate into subflows with `Ctrl+Enter`
- **Breadcrumb navigation** - Always know where you are
- **Infinite canvas** - No limits on complexity
- **Visual flow indicators** - Clear parent-child relationships

### 🎨 Focused Interface
- **Dark theme** - Reduce eye strain during long sessions
- **Minimal UI** - Focus on content, not interface
- **System tray integration** - Always accessible, never in the way
- **Global hotkeys** - Access from anywhere

### 📤 Export Capabilities
- **PDF export** - Professional reports with hyperlinked navigation
- **Markdown export** - Structured text for wikis and documentation
- **Hierarchical preservation** - Maintain flow relationships in exports

## ⌨️ Keyboard Shortcuts

| Shortcut | Action |
|----------|--------|
| `Tab` | Create connected node |
| `Enter` | Edit selected node |
| `Type text` | Auto-edit selected node |
| `Ctrl+Enter` | Drill down to subflow |
| `Ctrl+N` | Add note to selected node |
| `Ctrl+E` | Export flow |
| `Arrow Keys` | Navigate between nodes |
| `Esc` | Go back / Cancel |
| `?` | Show help overlay |
| `Ctrl+Shift+F` | Show/focus main window (global) |

## 🏗️ Project Architecture

```
FlowDeconstruct/
├── src/main/java/com/sap/flowdeconstruct/
│   ├── FlowDeconstructApp.java                     # Main application entry point
│   ├── core/
│   │   └── ProjectManager.java                     # Project persistence and management
│   ├── model/
│   │   ├── FlowDiagram.java                        # Flow diagram data model
│   │   ├── FlowNode.java                           # Individual node model
│   │   └── FlowConnection.java                     # Connection between nodes
│   └── ui/
│       ├── MainWindow.java                         # Main application window
│       ├── SystemTrayManager.java                  # System tray integration
│       ├── components/
│       │   └── FlowCanvas.java                     # Canvas for rendering flows
│       └── dialogs/
│           ├── NoteDialog.java                     # Note editing dialog
│           └── ExportDialog.java                   # Export configuration dialog
├── build-jar.ps1                                   # Build script
├── run-jar.ps1                                     # Execution script
├── FlowDeconstruct.bat                             # Simple execution batch file
├── setup-environment.ps1                          # Environment setup script
└── target/
    └── FlowDeconstruct.jar                         # Executable JAR file
```

## 🛠️ Technical Stack

### Runtime Environment
- **SAP JVM 8** - Enterprise-optimized Java runtime
- **Java Swing/AWT** - Native Windows GUI framework
- **Maven 3.9+** - Build and dependency management

### Dependencies
- **Jackson 2.15.2** - JSON serialization for project persistence
- **iText7 7.2.5** - PDF export capabilities
- **JUnit 5.9.3** - Testing framework

### SAP JVM 8 Integration
This application is specifically configured for **SAP JVM 8**, providing:
- **Enterprise optimization** - JVM optimized for SAP workloads
- **Performance benefits** - Superior performance in corporate environments
- **Full Java 8 compatibility** - Standard Java features with SAP enhancements
- **Official SAP support** - Enterprise-grade support and maintenance

## 💾 Data Storage

FlowDeconstruct uses a **local-first approach**:

- **Projects**: Stored as JSON files in `%USERPROFILE%/.flowdeconstruct/projects/`
- **User preferences**: Windows Registry via `java.util.prefs.Preferences`
- **No cloud dependencies** - Works completely offline
- **Open format** - JSON files can be backed up and transferred

## 🎯 Design Philosophy

### Speed Above All
Every interaction is optimized for minimal latency. The application feels instantaneous, enabling you to work at the speed of thought during critical incidents.

### Keyboard-Centric Workflow
Mouse usage is optional. All core functionality is accessible through keyboard shortcuts, allowing for uninterrupted flow creation during phone calls.

### Clarity in Chaos
The dark theme with high contrast reduces visual fatigue while maintaining clear hierarchical relationships between system components.

### Focused Utility
Deliberately minimal feature set. No color palettes, shape libraries, or complex formatting options. The tool does one thing exceptionally well.

## 📋 Use Cases

### Primary: Technical Incident Triage
**Scenario**: SAP Mission Critical Center specialist during a P1 incident
- Map system flow: `ECC → CIG → SCT`
- Drill down into problematic component
- Add notes about error conditions
- Export findings for engineering team

### Secondary: Process Documentation
- Document complex system architectures
- Create hierarchical process flows
- Generate reports for stakeholders
- Maintain technical knowledge base

### Tertiary: Training and Analysis
- Visual system architecture training
- Process improvement analysis
- Root cause analysis documentation
- Knowledge transfer sessions

## 🚀 Getting Started Guide

### 1. Environment Setup
Run the automated setup script as Administrator:
```powershell
.\setup-environment.ps1
```

This will:
- Verify SAP JVM 8 installation
- Install Apache Maven via Chocolatey
- Configure environment variables
- Verify the installation

### 2. Build the Application
```powershell
.\build-jar.ps1
```

This script:
- Compiles the source code
- Downloads dependencies
- Creates executable JAR with all dependencies
- Tests the JAR execution

### 3. Run the Application
Choose your preferred method:

**Option A: Simple Batch File**
```batch
FlowDeconstruct.bat
```

**Option B: PowerShell Script**
```powershell
.\run-jar.ps1
```

**Option C: Direct Execution**
```cmd
java -jar target\FlowDeconstruct.jar
```

### 4. First Use
1. Application starts minimized in system tray
2. Use `Ctrl+Shift+F` to show the main window
3. Press `Tab` to create your first node
4. Press `?` to see all keyboard shortcuts

## 📝 Workflow Example

### Real-World Scenario: SAP System Issue

1. **Incident Alert** - Critical data flow failure in production
2. **Quick Access** - `Ctrl+Shift+F` to open FlowDeconstruct
3. **Map Main Flow** - `Tab` to create nodes: ECC → CIG → SCT
4. **Identify Problem Area** - Navigate to CIG node with arrow keys
5. **Add Context** - `Ctrl+N` to add note: "Data transformation error"
6. **Drill Down** - `Ctrl+Enter` to map CIG internal process
7. **Document Details** - Map: API Ingestion → Data Transform → Output
8. **Root Cause** - Add note: "Mapping rule Z_RULE failing on null values"
9. **Export Results** - `Ctrl+E` to create PDF report
10. **Share Findings** - Send structured report to development team

Total time: **2-3 minutes** during live incident call.

## 🛠️ Build and Development

### Manual Development Setup

If you prefer manual setup or the automated script fails:

1. **Install SAP JVM 8**:
   - Verify installation at `C:\Program Files\sapjvm\sapjvm_8`
   - Set `JAVA_HOME=C:\Program Files\sapjvm\sapjvm_8`

2. **Install Maven**:
   - Download from https://maven.apache.org/download.cgi
   - Extract to `C:\Program Files\Apache\Maven`
   - Add `bin` directory to PATH

3. **Verify Installation**:
```powershell
java -version     # Should show SAP JVM
mvn -version      # Should show Maven 3.x.x
```

### Development Commands

```powershell
# Clean build
mvn clean compile

# Run in development mode
mvn exec:java -Dexec.mainClass="com.sap.flowdeconstruct.FlowDeconstructApp"

# Create distributable JAR
mvn clean package

# Run tests
mvn test

# Generate project reports
mvn site
```

## 📁 File Structure

### Core Application Files
- `FlowDeconstruct.jar` - Main executable (in target/ after build)
- `FlowDeconstructApp.java` - Application entry point
- `SystemTrayManager.java` - Windows system tray integration
- `MainWindow.java` - Primary user interface
- `ProjectManager.java` - Data persistence management

### Build and Execution Scripts
- `build-jar.ps1` - Automated build script
- `run-jar.ps1` - Execution script with error handling
- `FlowDeconstruct.bat` - Simple batch execution
- `setup-environment.ps1` - Environment configuration

### Configuration Files
- `pom.xml` - Maven project configuration
- `design.md` - Visual design specifications
- `prd.md` - Product requirements document

## 🔧 Troubleshooting

### Common Issues

**Q: Application doesn't start**
A: Check if SAP JVM 8 is installed and JAVA_HOME is set correctly:
```powershell
echo $env:JAVA_HOME     # Should be: C:\Program Files\sapjvm\sapjvm_8
java -version           # Should show SAP JVM
```

**Q: System tray icon doesn't appear**
A: Ensure Windows system tray is enabled and not hidden. Check Windows notification area settings.

**Q: Global hotkey (Ctrl+Shift+F) doesn't work**
A: Verify no other application is using this hotkey combination. Try restarting the application.

**Q: Build fails with Maven errors**
A: Run `.\setup-environment.ps1` as Administrator to install Maven and configure environment.

**Q: JAR file not found**
A: Run `.\build-jar.ps1` to compile and create the JAR file.

### Performance Issues

**Slow startup**: Verify you're running the JAR directly, not through Maven exec plugin.

**High memory usage**: The application is designed to be lightweight. If experiencing issues, restart the application.

**UI responsiveness**: Ensure you're using SAP JVM 8 for optimal performance.

## 📊 Project Status

### ✅ Completed Features
- ✅ Core flow mapping functionality
- ✅ Hierarchical drill-down navigation
- ✅ System tray integration
- ✅ Global keyboard shortcuts
- ✅ JSON project persistence
- ✅ PDF export with hyperlinks
- ✅ Markdown export
- ✅ Dark theme interface
- ✅ Keyboard-first navigation
- ✅ Auto-layout algorithms
- ✅ Build automation scripts
- ✅ SAP JVM 8 integration
- ✅ Node editing improvements (Enter to edit, auto-edit on type)
- ✅ Text persistence fixes (names are now saved correctly)
- ✅ Keyboard focus improvements
- ✅ Editing system stability enhancements

### 🔄 In Progress
- 🔄 Enhanced documentation
- 🔄 Performance optimizations
- 🔄 UI refinements

### 🐛 Recent Bug Fixes (v1.0.2)
- Fixed node name persistence issues
- Resolved text duplication during editing
- Improved keyboard event handling
- Enhanced project save/load stability

### 🎯 Future Enhancements
- 🔄 Windows installer (.msi)
- 🔄 Configurable hotkeys
- 🔄 Theme customization
- 🔄 Usage analytics
- 🔄 Auto-update mechanism

## 📄 License

Internal SAP tool - see company licensing guidelines.

## 🤝 Contributing

This project follows the specifications in:
- `prd.md` - Product Requirements Document
- `design.md` - Visual Design Specification
- `TODO-LIST.md` - Development Task List

For questions or contributions, contact the SAP Mission Critical Center team.

## 🙏 Acknowledgments

Developed by the **SAP Mission Critical Center** team for technical specialists who need to work at the speed of critical incidents.

Special recognition to the design philosophy of speed-first tools that inspired this project:
- **Terminal emulators** - For keyboard-centric workflows
- **Whimsical** - For rapid ideation interfaces
- **draw.io** - For local-first data storage approach

---

**FlowDeconstruct v1.0.2** - Built for the SAP Mission Critical Center  
*"Analysis at the speed of thought"*