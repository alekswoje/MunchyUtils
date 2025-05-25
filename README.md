# MunchyUtils

**Made by HogMower**

Credits to WuKnows and the other amazing people who created [item-guide.com](https://item-guide.com)

A utility mod for MunchyMC that helps you track cooldowns, monitor how much you are making, and better special item hints than /thrift.

## Cloth Config Dependency

This mod requires [Cloth Config](https://modrinth.com/mod/cloth-config/versions?version=1.21.5&loader=fabric) for its configuration GUI.

**You must install Cloth Config for the `/mu config` command and config screen to work!**

Download the correct version for your Minecraft and Fabric loader from:
- [Cloth Config for 1.21.5 (Fabric) on Modrinth](https://modrinth.com/mod/cloth-config/versions?version=1.21.5&loader=fabric)

Place the downloaded Cloth Config jar in your `mods` folder alongside this mod.

## Features

### Cooldown System

- **Custom Cooldown Triggers**: Create personalized cooldown timers for any item or armor piece
- **Multiple Trigger Types**:
  - Held items (main hand)
  - Worn items (armor)
- **Various Actions**:
  - Right-click (`rclick`)
  - Left-click (`lclick`)
  - Crouch (`crouch`)
  - Block break (`break`)
- **Smart Item Detection**: Match items by name substring
- **Visual Feedback**: Clean HUD display showing all active cooldowns
- **Sound Notifications**: Audio alert when a cooldown finishes
- **Persistent Storage**: Cooldowns and triggers are saved between game sessions
- **Default Triggers**: Pre-configured for popular items (Dooku, Scrubee, Moloch's, etc.)

### Income Tracking Features

- **Real-time Income Monitoring**: Track your earnings per hour
- **Session Statistics**:
  - Current hourly rate
  - Total earnings for the session
  - Session duration
  - AFK time tracking
- **AFK Detection**: Automatically detects when you're away and pauses income tracking
- **Session Management**:
  - Automatic session timeout after 2 minutes of inactivity
  - Manual session reset option
  - Session summary with copy-to-clipboard feature

### Freethrift Item Lookup Features

- **Item Info Lookup**: Instantly view the effects and usage of rare/special items
- **Flexible Search**:
  - Hold an item and run `/mu freethrift` to look it up
  - Or use `/mu freethrift <item_tag>` to search by tag (auto-suggested)
  - Add `true` to copy the info to your clipboard: `/mu freethrift <item_tag> true`
- **Community Contributions**: If an item is missing, you can help add it ([see below](#contributing-freethrift-item-info))

### HUD Customization

- **Movable Overlays**: Drag and position both cooldown and income HUDs
- **Grid Snapping**: Easy alignment with 10-pixel grid
- **Color Coding**:
  - Green: Active/Ready
  - Yellow: AFK
  - Red: Inactive/On Cooldown
- **Independent Toggle**: Enable/disable each HUD separately

## Commands

### Cooldown Management

```sh
/mu trigger add <name> <held|worn> <crouch|rclick|lclick|break> <itemNamePart> <cooldownSeconds>
/mu trigger remove <name>
/mu trigger list
```

### Income Tracking Commands

```sh
/mu reset                    # Reset the current income tracking session
/mu summary                  # Show current session statistics
/mu summary true             # Show and copy statistics to clipboard
```

### Freethrift Item Lookup Commands

```sh
/mu freethrift               # Look up the item you're holding
/mu freethrift <item_tag>    # Look up a specific item by tag (auto-suggested)
/mu freethrift <item_tag> true # Look up and copy info to clipboard
```

- If the item is not found, you'll get instructions on how to contribute info for it.

### Feature Control

```sh
/mu feature list             # List all features and their states
/mu feature toggle <feature> # Toggle a specific feature
```

## Tips & Tricks

1. **Item Names with Spaces**: Use quotes for item names containing spaces or special characters:

   ```sh
   /mu trigger add "Luga's Net" held rclick "Luga's Net" 900
   ```

2. **HUD Positioning**:
   - Hold TAB to switch between moving cooldown and income HUDs
   - Use arrow keys for precise positioning
   - HUDs snap to a 10-pixel grid for clean alignment

3. **AFK Detection**:
   - Automatically detects when you're not moving or using controls
   - Pauses income tracking during AFK periods
   - Resumes automatically when you return

4. **Session Management**:
   - Sessions automatically reset if no income changes for 2 minutes
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

You can modify or remove these defaults using the trigger commands.

## Contributing Freethrift Item Info

If you notice a special item is missing from `/mu freethrift`, you can help!

- **Not technical?** Just DM me (`alwoj`) on Discord with the item name, description, and any notes.
- **Know GitHub?** Make a pull request to add or update items in the [`mu_freethrift_items.json`](./src/main/resources/mu_freethrift_items.json) file.

Your contributions help everyone!

---

For developers interested in contributing to this project, please see [CONTRIBUTING.md](CONTRIBUTING.md).

**Enjoy optimizing your Minecraft experience!**
