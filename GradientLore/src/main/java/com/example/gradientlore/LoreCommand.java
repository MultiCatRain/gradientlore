package com.example.gradientlore;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** /lore add|set|insert|remove|clear|view - free to use. Use | to write several lines at once. */
final class LoreCommand implements TabExecutor {

    private static final List<String> SUBS = List.of("add", "set", "insert", "remove", "clear", "view");

    private final GradientLorePlugin plugin;

    LoreCommand(GradientLorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Messages.error("Only players can use this command."));
            return true;
        }
        if (args.length == 0) {
            usage(player);
            return true;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType().isAir()) {
            player.sendMessage(Messages.error("Hold the item you want to edit in your main hand."));
            return true;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            player.sendMessage(Messages.error("That item can't have lore."));
            return true;
        }

        List<Component> lore = currentLore(meta);
        boolean changed = false;

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "add" -> changed = add(player, args, lore);
            case "set" -> changed = set(player, args, lore);
            case "insert" -> changed = insert(player, args, lore);
            case "remove" -> changed = remove(player, args, lore);
            case "clear" -> {
                if (lore.isEmpty()) {
                    player.sendMessage(Messages.info("This item has no lore."));
                } else {
                    lore.clear();
                    player.sendMessage(Messages.ok("Lore cleared."));
                    changed = true;
                }
            }
            case "view" -> {
                view(player, lore);
                return true;
            }
            default -> {
                usage(player);
                return true;
            }
        }

        if (!changed) {
            return true;
        }
        meta.lore(lore.isEmpty() ? null : lore);
        item.setItemMeta(meta);
        player.getInventory().setItemInMainHand(item);
        return true;
    }

    private static List<Component> currentLore(ItemMeta meta) {
        List<Component> existing = meta.lore();
        return existing == null ? new ArrayList<>() : new ArrayList<>(existing);
    }

    private void usage(Player p) {
        p.sendMessage(Messages.info("/lore add <text>   - add line(s); separate several lines with |"));
        p.sendMessage(Messages.info("/lore set <line> <text>   - replace a line"));
        p.sendMessage(Messages.info("/lore insert <line> <text>   - insert line(s) at a position"));
        p.sendMessage(Messages.info("/lore remove <line>   - remove one line"));
        p.sendMessage(Messages.info("/lore clear   - remove all lore"));
        p.sendMessage(Messages.info("/lore view   - show the current lore with line numbers"));
        p.sendMessage(Messages.info("Codes: &g (active gradient), &g<name> (saved), &r reset, &c &l etc. Free to use."));
    }

    private boolean add(Player p, String[] args, List<Component> lore) {
        List<Component> lines = parseLines(p, args, 1);
        if (lines == null) {
            return false;
        }
        int max = plugin.limits().maxLoreLines();
        if (Limits.exceeds(lore.size() + lines.size(), max)) {
            p.sendMessage(Messages.error("Too many lore lines (max " + max + ")."));
            return false;
        }
        lore.addAll(lines);
        p.sendMessage(Messages.ok("Added " + lines.size() + (lines.size() == 1 ? " line." : " lines.")));
        return true;
    }

    private boolean set(Player p, String[] args, List<Component> lore) {
        if (args.length < 3) {
            p.sendMessage(Messages.error("Usage: /lore set <line> <text>"));
            return false;
        }
        int index = parseIndex(args[1]);
        if (index < 1 || index > lore.size()) {
            p.sendMessage(Messages.error("Line must be between 1 and " + lore.size() + ". Use /lore view."));
            return false;
        }
        Component line = parsePiece(p, String.join(" ", Arrays.copyOfRange(args, 2, args.length)));
        if (line == null) {
            return false;
        }
        lore.set(index - 1, line);
        p.sendMessage(Messages.ok("Line " + index + " updated."));
        return true;
    }

    private boolean insert(Player p, String[] args, List<Component> lore) {
        if (args.length < 3) {
            p.sendMessage(Messages.error("Usage: /lore insert <line> <text>"));
            return false;
        }
        int index = parseIndex(args[1]);
        if (index < 1 || index > lore.size() + 1) {
            p.sendMessage(Messages.error("Line must be between 1 and " + (lore.size() + 1) + "."));
            return false;
        }
        List<Component> lines = parseLines(p, args, 2);
        if (lines == null) {
            return false;
        }
        int max = plugin.limits().maxLoreLines();
        if (Limits.exceeds(lore.size() + lines.size(), max)) {
            p.sendMessage(Messages.error("Too many lore lines (max " + max + ")."));
            return false;
        }
        lore.addAll(index - 1, lines);
        p.sendMessage(Messages.ok("Inserted " + lines.size() + (lines.size() == 1 ? " line." : " lines.")));
        return true;
    }

    private boolean remove(Player p, String[] args, List<Component> lore) {
        if (args.length < 2) {
            p.sendMessage(Messages.error("Usage: /lore remove <line>"));
            return false;
        }
        int index = parseIndex(args[1]);
        if (index < 1 || index > lore.size()) {
            p.sendMessage(Messages.error("Line must be between 1 and " + lore.size() + ". Use /lore view."));
            return false;
        }
        lore.remove(index - 1);
        p.sendMessage(Messages.ok("Removed line " + index + "."));
        return true;
    }

    private void view(Player p, List<Component> lore) {
        if (lore.isEmpty()) {
            p.sendMessage(Messages.info("This item has no lore."));
            return;
        }
        for (int i = 0; i < lore.size(); i++) {
            p.sendMessage(Component.text()
                    .append(Component.text((i + 1) + ". ", NamedTextColor.DARK_GRAY))
                    .append(lore.get(i))
                    .build());
        }
    }

    private static int parseIndex(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /** Splits the text on | into lines and parses each. Returns null (after telling the player) on any problem. */
    private List<Component> parseLines(Player p, String[] args, int from) {
        String text = String.join(" ", Arrays.copyOfRange(args, from, args.length));
        List<Component> out = new ArrayList<>();
        for (String piece : text.split("\\|")) {
            String trimmed = piece.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            Component line = parsePiece(p, trimmed);
            if (line == null) {
                return null;
            }
            out.add(line);
        }
        if (out.isEmpty()) {
            p.sendMessage(Messages.error("Write some text for the lore line."));
            return null;
        }
        return out;
    }

    private Component parsePiece(Player p, String text) {
        TextParser.Result parsed = plugin.parser().parse(p.getUniqueId(), text);
        Limits limits = plugin.limits();
        if (parsed.plain().isBlank()) {
            p.sendMessage(Messages.error("A lore line can't be empty."));
            return null;
        }
        if (Limits.exceeds(parsed.plain().length(), limits.maxLoreLineLength())) {
            p.sendMessage(Messages.error("A lore line can have at most " + limits.maxLoreLineLength()
                    + " characters (codes not counted)."));
            return null;
        }
        if (limits.findBlockedWord(parsed.plain()) != null) {
            p.sendMessage(Messages.error("That lore contains a blocked word."));
            return null;
        }
        return parsed.component();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player)) {
            return List.of();
        }
        if (args.length == 1) {
            return Completions.filter(SUBS, args[0]);
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (args.length == 2 && (sub.equals("set") || sub.equals("insert") || sub.equals("remove"))) {
            ItemMeta meta = player.getInventory().getItemInMainHand().getItemMeta();
            int size = meta == null || meta.lore() == null ? 0 : meta.lore().size();
            int top = sub.equals("insert") ? size + 1 : size;
            List<String> numbers = new ArrayList<>();
            for (int i = 1; i <= top; i++) {
                numbers.add(String.valueOf(i));
            }
            return Completions.filter(numbers, args[1]);
        }
        boolean textArg = sub.equals("add") || ((sub.equals("set") || sub.equals("insert")) && args.length >= 3);
        if (textArg) {
            return Completions.codes(plugin, player, args[args.length - 1]);
        }
        return List.of();
    }
}
