package com.example.gradientlore;

import org.bukkit.entity.HumanEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.view.AnvilView;

/** Applies color codes and gradients to anvil renames. The vanilla XP cost is left alone. */
final class AnvilListener implements Listener {

    private final GradientLorePlugin plugin;

    AnvilListener(GradientLorePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        ItemStack result = event.getResult();
        if (result == null || result.getType().isAir()) {
            return;
        }

        AnvilView view = event.getView();
        String text = view.getRenameText();
        if (text == null || text.isBlank()) {
            return;
        }

        HumanEntity viewer = view.getPlayer();
        if (!viewer.hasPermission("gradientlore.anvil")) {
            return;
        }

        Limits limits = plugin.limits();
        TextParser.Result parsed = plugin.parser().parse(viewer.getUniqueId(), text);

        if (limits.findBlockedWord(parsed.plain()) != null) {
            event.setResult(null);
            return;
        }

        // No codes typed: leave vanilla behavior completely untouched.
        if (text.indexOf('&') < 0) {
            return;
        }

        if (parsed.plain().isBlank() || Limits.exceeds(parsed.plain().length(), limits.maxNameLength())) {
            event.setResult(null);
            return;
        }

        ItemStack out = result.clone();
        ItemMeta meta = out.getItemMeta();
        if (meta == null) {
            return;
        }
        meta.displayName(parsed.component());
        out.setItemMeta(meta);
        event.setResult(out);
    }
}
