# DeskBack

DeskBack remembers your open apps so you can reopen them after restarting or shutting down your PC. It works locally on Windows.

## How to use

- After you sign in, a **Restore** button appears for 10 seconds.
- Click **Restore** to reopen your saved apps. Apps already running are skipped.
- Hold **Ctrl** while clicking to choose which apps to reopen.
- If you miss the button, open it again from the DeskBack tray icon. Choose **Exit** to close DeskBack.

DeskBack updates the saved app list every 30 seconds while running in the background. It reopens apps, but does not restore unsaved work or their full state. Browser tabs depend on your browser's session restore settings.

## Installation

Requires Windows and JDK 17+. From the project folder, run:

```powershell
.\mvnw.cmd package
javaw -jar target\deskback-0.1.0.jar
```

Autostart is configured on the first launch. Keep the JAR and `lib` folder in `target` in place.

## Technologies

- **Java 17** — application logic.
- **Swing / AWT** — popup, app selection and tray icon.
- **JNA** — access to Windows APIs for detecting apps and handling shutdown.
- **Maven Wrapper** — build and dependencies; no separate Maven installation needed.
