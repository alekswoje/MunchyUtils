# Contributing to MunchyUtils

Thank you for your interest in contributing to MunchyUtils! This document provides guidelines and technical details for developers who want to contribute to the project.

## Project Structure

```fs
src/
├── client/
│   └── java/
│       └── munchyutils/
│           ├── client/
│           │   ├── CooldownManager.java      # Manages cooldown triggers and states
│           │   ├── CooldownTrigger.java      # Cooldown trigger data structure
│           │   ├── CooldownHudOverlay.java   # Cooldown HUD rendering
│           │   ├── InfoHudOverlay.java       # Info HUD (mining/fishing) and tool detection
│           │   ├── InfoHudSession.java       # Mining/Income session management
│           │   ├── FishingHudSession.java    # Fishing session management (XP, rewards, etc.)
│           │   ├── HudInputHandler.java      # Handles HUD movement and input
│           │   ├── FeatureManager.java       # Feature toggles and HUD positions
│           │   ├── FreethriftCommand.java    # Freethrift item lookup logic
│           │   ├── MunchyConfigScreen.java   # YACL-based config GUI
│           │   ├── HudEditScreen.java        # HUD edit mode
│           │   ├── Utils.java                # Utility functions
│           │   ├── ScoreboardReaderClient.java # Monitors scoreboard for balance changes
│           │   └── MunchyUtilsClient.java    # Main mod entry point
```

## Key Components

### Info HUD (Mining & Fishing)
- `InfoHudOverlay`: Renders the Info HUD, handles hotbar tool detection (only shows HUD if pickaxe or fishing rod is in hotbar)
- `InfoHudSession`: Tracks mining/income session stats, AFK, and session management
- `FishingHudSession`: Tracks fishing XP, rewards, level, time to next level, and session stats
- **AFK/Idle Detection**: 1 minute idle triggers AFK, session time pauses
- **Session Reset**: 2 minutes idle (mining), 3m30s (fishing) resets session

### Cooldown System
- `CooldownTrigger`: Defines the structure for cooldown triggers
  - Types: `HELD`, `WORN`
  - Actions: `CROUCH`, `RCLICK`, `LCLICK`, `BREAK`
- `CooldownManager`: Handles trigger storage and cooldown state
  - JSON persistence for triggers and cooldowns
  - Default trigger initialization
  - Cooldown state management
- `CooldownHudOverlay`: Renders cooldown information, color-coded, grid-snapped

### Freethrift Item Lookup
- `FreethriftCommand`: Handles `/mu freethrift` commands and item info lookup
- Community-contributed item info via JSON

### HUD System
- `HudInputHandler`, `HudEditScreen`: Handles HUD movement, edit mode, and positioning
- `FeatureManager`: Feature toggles and HUD positions
- `MunchyConfigScreen`: YACL-based config GUI (all features configurable in-game)

## Development Guidelines

### Adding New Features
1. **Feature Toggle**
   - Add new feature to `FeatureManager.ModFeature` enum
   - Implement toggle functionality in `FeatureManager`
   - Add command handling if needed
2. **HUD Components**
   - Extend `HudRenderCallback` for new overlays
   - Implement grid snapping and positioning
   - Use consistent color scheme and styling
3. **Data Persistence**
   - Use JSON for configuration storage
   - Implement save/load methods in manager classes
   - Handle migration for existing data

### Code Style
- Follow Java naming conventions
- Use meaningful variable and method names
- Add comments for complex logic
- Keep methods focused and concise
- Use proper access modifiers

### Testing
1. **Manual Testing**
   - Test all trigger types and actions
   - Verify HUD positioning and behavior
   - Check persistence across restarts
   - Validate AFK detection and session reset
   - Test hotbar-only tool detection for HUD switching
2. **Edge Cases**
   - Test with special characters in item names
   - Verify behavior with multiple active cooldowns
   - Check session timeout handling
   - Test HUD positioning at screen edges

## Building the Project

1. **Prerequisites**
   - Java Development Kit (JDK) 17 or later
   - Gradle build system
   - Minecraft development environment

2. **Build Steps**
   ```bash
   ./gradlew build
   ```
3. **Development Setup**
   ```bash
   ./gradlew genSources
   ./gradlew eclipse  # or idea for IntelliJ
   ```

## Pull Request Process
1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Test thoroughly
5. Submit a pull request with:
   - Clear description of changes
   - Testing performed
   - Screenshots if UI changes
   - Any relevant issue numbers

## Versioning

This project uses [Semantic Versioning](https://semver.org/):
- **MAJOR**: Incompatible changes
- **MINOR**: Backwards-compatible features
- **PATCH**: Backwards-compatible fixes

## License

This project is licensed under the MIT License - see the LICENSE file for details.

---

Thank you for contributing to MunchyUtils! Your help makes the mod better for everyone.
