package com.example.gradientlore;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

/** /name <text> - renames the held item. Costs 1 XP level (also in creative). */
final class NameCommand implements TabExecutor {

    private final GradientLorePlugin plugin;

    NameCommand(GradientLorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Messages.error("Only players can use this command."));
            return true;
        }
        if (args.length == 0) {
            player.sendMessage(Messages.error("Usage: /" + label + " <text>   (supports &g, &g<name>, &r and color codes)"));
            return true;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType().isAir()) {
            player.sendMessage(Messages.error("Hold the item you want to rename in your main hand."));
            return true;
        }

        TextParser.Result parsed = plugin.parser().parse(player.getUniqueId(), String.join(" ", args));
        Limits limits = plugin.limits();

        if (parsed.plain().isBlank()) {
            player.sendMessage(Messages.error("The name can't be empty."));
            return true;
        }
        if (Limits.exceeds(parsed.plain().length(), limits.maxNameLength())) {
            player.sendMessage(Messages.error("That name is too long (max " + limits.maxNameLength() + " characters, codes not counted)."));
            return true;
        }
        if (limits.findBlockedWord(parsed.plain()) != null) {
            player.sendMessage(Messages.error("That name contains a blocked word."));
            return true;
        }
        if (player.getLevel() < 1) {
            player.sendMessage(Messages.error("You need at least 1 XP level to rename an item."));
            return true;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            player.sendMessage(Messages.error("That item can't be renamed."));
            return true;
        }
        meta.displayName(parsed.component());
        item.setItemMeta(meta);
        player.getInventory().setItemInMainHand(item);
        player.giveExpLevels(-1);

        player.sendMessage(Component.text()
                .append(Component.text("Renamed to ", NamedTextColor.GREEN))
                .append(parsed.component())
                .append(Component.text("  (-1 level)", NamedTextColor.GRAY))
                .build());
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player) || args.length == 0) {
            return List.of();
        }
        return Completions.codes(plugin, player, args[args.length - 1]);
    }
}
