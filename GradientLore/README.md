# GradientLore (Paper 26.3)

Per-player gradients for item names and lore. No LuckPerms needed: every permission defaults to true.

## Commands

| Command | What it does |
|---|---|
| `/gradient set "#ff0000" "#ff8800" "#0000ff" ...` | Set your active gradient (2+ hex colors, evenly spaced) |
| `/gradient save "name"` | Save your active gradient (max 25) |
| `/gradient load "name"` | Make a saved gradient active |
| `/gradient delete "name"` | Delete a saved gradient |
| `/gradient list` | List your saved gradients, drawn in their own colors |
| `/name <text>` | Rename the held item. Costs 1 XP level (creative too) |
| `/lore add <text>` | Add lore. Separate several lines with `|` |
| `/lore set <line> <text>` | Replace a line |
| `/lore insert <line> <text>` | Insert line(s) at a position |
| `/lore remove <line>` / `/lore clear` | Remove one line / all lore |
| `/lore view` | Show lore with line numbers |

## Codes (anvil, /name, /lore)

- `&g` active gradient, `&g<name>` a saved gradient, `&r` reset
- `&0`-`&f` vanilla colors, `&l &o &n &m &k` styles, `&#rrggbb` hex, `&&` literal &
- Example in the anvil: `&l&gMy Sword` or `&3&lHi`
- A gradient needs at least one character per color, otherwise the text stays plain.
- Anvil renaming keeps its normal vanilla XP cost.

## Build

You need a JDK 25 and Gradle 9.1 or newer (Paper 26.3 targets Java 25).

    gradle build

The jar is `build/libs/GradientLore-1.0.0.jar` -> copy it to `plugins/` and restart.

Or open this folder in IntelliJ IDEA, let it import the Gradle project, and run the `build` task.

If the Paper API version in `build.gradle.kts` can't be resolved, check the available
26.3 builds at https://repo.papermc.io/repository/maven-public/io/papermc/paper/paper-api/
and change the version string.

## Config (plugins/GradientLore/config.yml)

Limits for name length, lore lines, line length, saved gradients, colors per gradient, and a
blocked-words list. Set a number to 0 for no limit. Restart to apply changes.
