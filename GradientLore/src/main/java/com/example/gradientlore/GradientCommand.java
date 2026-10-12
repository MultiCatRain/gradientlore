package com.example.gradientlore;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

final class GradientCommand implements TabExecutor {

    private static final List<String> SUBS = List.of("set", "save", "load", "delete", "list", "help");

    private final GradientLorePlugin plugin;

    GradientCommand(GradientLorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Messages.error("Only players can use this command."));
            return true;
        }
        if (args.length == 0) {
            help(player);
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "set" -> set(player, args);
            case "save" -> save(player, args);
            case "load" -> load(player, args);
            case "delete", "remove" -> delete(player, args);
            case "list" -> list(player);
            default -> help(player);
        }
        return true;
    }

    private void help(Player p) {
        p.sendMessage(Component.text("Gradient commands", NamedTextColor.GOLD, TextDecoration.BOLD));
        p.sendMessage(Messages.info("/gradient set \"#ff0000\" \"#ff8800\" \"#0000ff\" ...  - set your active gradient (2+ hex colors)"));
        p.sendMessage(Messages.info("/gradient save \"name\"  - save your active gradient"));
        p.sendMessage(Messages.info("/gradient load \"name\"  - make a saved gradient active"));
        p.sendMessage(Messages.info("/gradient delete \"name\"  - delete a saved gradient"));
        p.sendMessage(Messages.info("/gradient list  - show your saved gradients"));
        p.sendMessage(Messages.info("Use &g (active) or &g<name> (saved) in the anvil, /name and /lore. &r resets."));
    }

    private void set(Player p, String[] args) {
        if (args.length < 3) {
            p.sendMessage(Messages.error("Give at least 2 hex colors, e.g. /gradient set \"#ff0000\" \"#0000ff\""));
            return;
        }
        List<TextColor> colors = new ArrayList<>();
        for (int i = 1; i < args.length; i++) {
            TextColor c = GradientRenderer.parseHex(args[i]);
            if (c == null) {
                p.sendMessage(Messages.error(args[i] + " is not a valid hex color (use the form #rrggbb)."));
                return;
            }
            colors.add(c);
        }
        int max = plugin.limits().maxGradientColors();
        if (Limits.exceeds(colors.size(), max)) {
            p.sendMessage(Messages.error("A gradient can have at most " + max + " colors."));
            return;
        }
        plugin.gradients().setActive(p.getUniqueId(), colors);
        preview(p, "Gradient set", colors);
    }

    private void save(Player p, String[] args) {
        if (args.length < 2) {
            p.sendMessage(Messages.error("Usage: /gradient save \"name\""));
            return;
        }
        String name = GradientManager.normalizeName(args[1]);
        if (name == null) {
            p.sendMessage(Messages.error("Names can use letters, numbers, _ and - (max 24 characters)."));
            return;
        }
        UUID id = p.getUniqueId();
        List<TextColor> active = plugin.gradients().getActive(id);
        if (active.size() < 2) {
            p.sendMessage(Messages.error("You have no active gradient. Use /gradient set first."));
            return;
        }
        boolean exists = plugin.gradients().getSaved(id, name) != null;
        int max = plugin.limits().maxSavedGradients();
        if (!exists && max > 0 && plugin.gradients().savedNames(id).size() >= max) {
            p.sendMessage(Messages.error("You already have " + max + " saved gradients. Delete one first."));
            return;
        }
        plugin.gradients().putSaved(id, name, active);
        p.sendMessage(Messages.ok((exists ? "Updated" : "Saved") + " gradient \"" + name + "\"."));
    }

    private void load(Player p, String[] args) {
        if (args.length < 2) {
            p.sendMessage(Messages.error("Usage: /gradient load \"name\""));
            return;
        }
        String name = GradientManager.normalizeName(args[1]);
        List<TextColor> colors = name == null ? null : plugin.gradients().getSaved(p.getUniqueId(), name);
        if (colors == null) {
            p.sendMessage(Messages.error("You have no saved gradient with that name. See /gradient list."));
            return;
        }
        plugin.gradients().setActive(p.getUniqueId(), colors);
        preview(p, "Gradient loaded: " + name, colors);
    }

    private void delete(Player p, String[] args) {
        if (args.length < 2) {
            p.sendMessage(Messages.error("Usage: /gradient delete \"name\""));
            return;
        }
        String name = GradientManager.normalizeName(args[1]);
        if (name == null || !plugin.gradients().removeSaved(p.getUniqueId(), name)) {
            p.sendMessage(Messages.error("You have no saved gradient with that name."));
            return;
        }
        p.sendMessage(Messages.ok("Deleted gradient \"" + name + "\"."));
    }

    private void list(Player p) {
        UUID id = p.getUniqueId();
        List<String> names = new ArrayList<>(plugin.gradients().savedNames(id));
        if (names.isEmpty()) {
            p.sendMessage(Messages.info("You have no saved gradients. Use /gradient set, then /gradient save \"name\"."));
            return;
        }
        int max = plugin.limits().maxSavedGradients();
        String count = max > 0 ? names.size() + "/" + max : String.valueOf(names.size());
        p.sendMessage(Component.text("Your gradients (" + count + ")", NamedTextColor.GOLD, TextDecoration.BOLD));
        for (String name : names) {
            List<TextColor> colors = plugin.gradients().getSaved(id, name);
            if (colors == null) {
                continue;
            }
            p.sendMessage(Component.text()
                    .append(Component.text("- ", NamedTextColor.DARK_GRAY))
                    .append(GradientRenderer.render(name, colors, Style.style(TextDecoration.BOLD)))
                    .append(Component.text("  (" + colors.size() + " colors)  ", NamedTextColor.DARK_GRAY))
                    .append(GradientRenderer.render("\u2588".repeat(Math.max(12, colors.size())), colors, Style.empty()))
                    .build());
        }
    }

    /** Shows the "Gradient set/loaded" message with the text itself drawn in the gradient. */
    private void preview(Player p, String label, List<TextColor> colors) {
        TextColor first = colors.get(0);
        TextColor last = colors.get(colors.size() - 1);
        p.sendMessage(Component.text()
                .append(Component.text("\u2726 ", first))
                .append(GradientRenderer.render(label, colors, Style.style(TextDecoration.BOLD)))
                .append(Component.text(" \u2726", last))
                .build());
        p.sendMessage(GradientRenderer.render("\u2588".repeat(Math.max(24, colors.size())), colors, Style.empty()));
        p.sendMessage(Messages.info("Type &g in the anvil, /name or /lore to use it."));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player)) {
            return List.of();
        }
        if (args.length == 1) {
            return Completions.filter(SUBS, args[0]);
        }
        if (args.length == 2) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if (sub.equals("load") || sub.equals("delete") || sub.equals("remove")) {
                return Completions.filter(new ArrayList<>(plugin.gradients().savedNames(player.getUniqueId())), args[1]);
            }
        }
        return List.of();
    }
}
