package com.example.gradientlore;

import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class Completions {

    private Completions() {
    }

    static List<String> filter(List<String> options, String current) {
        String lower = current.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase(Locale.ROOT).startsWith(lower)) {
                out.add(option);
            }
        }
        return out;
    }

    /** Suggests color codes while the current word is empty or just starts with "&". */
    static List<String> codes(GradientLorePlugin plugin, Player player, String current) {
        if (!current.isEmpty() && !current.startsWith("&")) {
            return List.of();
        }
        List<String> all = new ArrayList<>(List.of("&g", "&r", "&l", "&#ff8800"));
        for (String name : plugin.gradients().savedNames(player.getUniqueId())) {
            all.add("&g<" + name + ">");
        }
        return filter(all, current);
    }
}
