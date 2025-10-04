# MCP Android Tools (local)

A minimal MCP server that exposes safe, curated Android emulator/app tools for this project over stdio.

Tools provided:
- adb_devices: list devices
- adb_launch_app: start MainActivity
- adb_force_stop_app: force-stop the app
- adb_clear_app_data: clear app user data
- adb_logcat_tail: dump last N lines of logcat (with optional tag/level)
- gradle_installDebug: build+install Debug
- gradle_connectedAndroidTest: run connected tests
- app_open_deeplink: open a deeplink inside the app

Defaults (can be overridden via env):
- PROJECT_ROOT: repo root
- APP_ID: com.example.privytaskai
- APP_ACTIVITY: .presentation.MainActivity

## Install

```bash
cd tools/mcp-android
npm install
chmod +x index.js start.sh
```

## Use with GitHub Copilot (JetBrains/Android Studio)
1) Open Settings/Preferences > Tools > GitHub Copilot > Model Context Protocol.
2) Add External Server:
   - Name: mcp-android-tools
   - Command: node
   - Args: index.js
   - Working directory: /absolute/path/to/tools/mcp-android
   - Env:
     - PROJECT_ROOT=/absolute/path/to/repo
     - APP_ID=com.example.privytaskai
     - APP_ACTIVITY=.presentation.MainActivity
3) Apply and restart Copilot if needed.

## Use with Claude Desktop
Add this to the MCP section of your Claude Desktop config (macOS example path: `~/Library/Application Support/Claude/claude_desktop_config.json`). If the file doesn’t exist, create it.

```json
{
  "mcpServers": {
    "mcp-android-tools": {
      "command": "node",
      "args": ["index.js"],
      "cwd": "/absolute/path/to/tools/mcp-android",
      "env": {
        "PROJECT_ROOT": "/absolute/path/to/repo",
        "APP_ID": "com.example.privytaskai",
        "APP_ACTIVITY": ".presentation.MainActivity"
      }
    }
  }
}
```

Restart Claude Desktop. In a chat, ask it to call a tool, e.g., `adb_devices` or `gradle_installDebug`.

## Notes
- The server prepends `$ANDROID_HOME/platform-tools` to PATH if set; on macOS we default `$ANDROID_HOME` to `~/Library/Android/sdk` so adb should be available.
- All commands run relative to `PROJECT_ROOT` to ensure Gradle/adb interactions match this project.

