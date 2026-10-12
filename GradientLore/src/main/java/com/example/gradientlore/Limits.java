package com.example.gradientlore;

import java.util.Locale;

/** Reads the configurable limits. A value of 0 means "no limit". */
final class Limits {

    private final GradientLorePlugin plugin;

    Limits(GradientLorePlugin plugin) {
        this.plugin = plugin;
    }

    int maxNameLength() {
        return plugin.getConfig().getInt("max-name-length", 50);
    }

    int maxLoreLines() {
        return plugin.getConfig().getInt("max-lore-lines", 15);
    }

    int maxLoreLineLength() {
        return plugin.getConfig().getInt("max-lore-line-length", 60);
    }

    int maxSavedGradients() {
        return plugin.getConfig().getInt("max-saved-gradients", 25);
    }

    int maxGradientColors() {
        return plugin.getConfig().getInt("max-gradient-colors", 30);
    }

    static boolean exceeds(int value, int limit) {
        return limit > 0 && value > limit;
    }

    /** Returns the first blocked word found in the (code-free) text, or null. */
    String findBlockedWord(String plain) {
        String lower = plain.toLowerCase(Locale.ROOT);
        for (String word : plugin.getConfig().getStringList("blocked-words")) {
            if (word == null || word.isBlank()) {
                continue;
            }
            if (lower.contains(word.toLowerCase(Locale.ROOT))) {
                return word;
            }
        }
        return null;
    }
}
