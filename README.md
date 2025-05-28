# MunchyUtils 1.0.0-beta

**Made by HogMower**

Credits to WuKnow, floki and the other amazing people who created [item-guide.com](https://item-guide.com)

A utility mod for MunchyMC that helps you track cooldowns, monitor your mining/fishing income, and get better special item hints than /thrift.

## Configuration GUI

This mod uses [YetAnotherConfigLib (YACL)](https://modrinth.com/mod/yacl?version=1.21.5&loader=fabric) for its configuration GUI.

**YACL is required for the config menu. If you do not have it installed, the game will crash when you try to open /mu config.**

Open the config screen with `/mu config` or from Mod Menu. All features and HUDs are configurable here.

## Features

### Info HUD (Mining & Fishing)

- **Income Tracking**: Real-time income per hour, session stats, and AFK time
- **Session Management**: Automatic timeout after inactivity, manual reset, and summary
- **AFK Detection**: Pauses session when you are away (1 minute idle)
- **Fishing HUD**: Tracks fishing XP, rewards, level, time to next level, and casts
- **Mining HUD**: Tracks mining income and session stats
- **Hotbar Tool Detection**: Only shows the relevant HUD if you have a pickaxe or fishing rod in your hotbar

### Cooldown HUD

- **Custom Cooldown Triggers**: For any item or armor piece
- **Multiple Trigger Types**: Held, worn, right-click, left-click, crouch, block break (chat message coming soon)
- **Visual Feedback**: Clean HUD display, color-coded, and sound notifications
- **Persistent Storage**: Cooldowns and triggers are saved between sessions
- **Default Triggers**: Pre-configured for popular items (Dooku, Scrubee, Moloch's, etc.)

### Freethrift Item Lookup

- **Item Info Lookup**: Instantly view effects and usage of rare/special items
- **Flexible Search**: Hold an item or use `/mu freethrift <item_tag>`, or hold the item and type `/mu freethrift`.
- **Community Contributions**: Add missing items via GitHub

### HUD Customization

- **Movable Overlays**: Drag and position both cooldown and info HUDs
- **Grid Snapping**: Easy alignment with 10-pixel grid
- **Color Coding**: Green (active), yellow (AFK), red (inactive/cooldown)
- **Independent Toggle**: Enable/disable each HUD separately

## Commands

### Cooldown Management (moving to gui soon)

```sh
/mu trigger add <name> <held|worn> <crouch|rclick|lclick|break> <itemNamePart> <cooldownSeconds>
/mu trigger remove <name>
/mu trigger list
```

### Info HUD & Session Commands

```sh
/mu reset                    # Reset the current income/fishing session
/mu summary                  # Show current session statistics
/mu summary true             # Show and copy statistics to clipboard
```

### Freethrift Item Lookup

```sh
/mu freethrift               # Look up the item you're holding
/mu freethrift <item_tag>    # Look up a specific item by tag (auto-suggested)
/mu freethrift <item_tag> true # Look up and copy info to clipboard
```

### Feature Control

Feature toggling is now handled in the config GUI (open with `/mu config` or from Mod Menu).

## Tips & Tricks

1. **Item Names with Spaces**: Use quotes for item names containing spaces or special characters:

   ```sh
   /mu trigger add "Luga's Net" held rclick "Luga's Net" 900
   ```

2. **HUD Positioning**:
   - Hold TAB to switch between moving cooldown and info HUDs
   - Use arrow keys for precise positioning
   - HUDs snap to a 10-pixel grid for clean alignment
3. **AFK Detection**:
   - Automatically detects when you're not moving or using controls (1 minute idle)
   - Pauses session and income tracking during AFK
   - Resumes automatically when you return
4. **Session Management**:
   - Sessions automatically reset if no income changes for 2 minutes (mining) or 3m30s (fishing)
   - Use `/mu reset` to manually start a new session
   - Get a summary anytime with `/mu summary`

## Default Triggers

The mod comes with several pre-configured triggers for popular items:

- Dooku Pickaxe (25 minutes)
- Scrubee Head (80 minutes)
- Moloch's Scythe (2 hours)
- Yoda's Pickaxe (20 minutes)
- Zadkiel's Sacred Pickaxe (1 hour)
- Luga's Net (15 minutes)
- and more...

You can modify or remove these defaults using the config GUI (open with `/mu config` or from Mod Menu).

## Contributing

If you notice a special item is missing from `/mu freethrift`, or want to help improve the mod:

- **Not technical?** DM `alwoj` on Discord with the item name, description, and any notes.
- **Know GitHub?** Make a pull request to add or update items in [`mu_freethrift_items.json`](./src/main/resources/mu_freethrift_items.json) or contribute code (see [CONTRIBUTING.md](CONTRIBUTING.md)).

Your contributions help everyone!

## Versioning

This project uses [Semantic Versioning](https://semver.org/):

- **MAJOR** version: Significant changes or new features
- **MINOR** version: Improvements and additions that are backwards-compatible
- **PATCH** version: Bug fixes and small tweaks

Example: `1.2.3`

---

For developers interested in contributing to this project, please see [CONTRIBUTING.md](CONTRIBUTING.md).
