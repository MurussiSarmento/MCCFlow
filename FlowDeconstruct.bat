@echo off
echo Starting FlowDeconstruct...

REM Set JAVA_HOME if not set
if not defined JAVA_HOME (
    set JAVA_HOME=C:\Program Files\sapjvm\sapjvm_8
    set PATH=%PATH%;C:\Program Files\sapjvm\sapjvm_8\bin
)

REM Check if JAR exists
if not exist "target\FlowDeconstruct.jar" (
    echo JAR file not found! Please run build-jar.ps1 first.
    pause
    exit /b 1
)

REM Run the JAR
echo Launching FlowDeconstruct...
echo The application will start minimized in the system tray.
echo Use Ctrl+Shift+F to show/hide the main window.

java -jar "target\FlowDeconstruct.jar"

echo FlowDeconstruct has exited.
pause