package com.example.gradientlore;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

final class Messages {

    private Messages() {
    }

    static Component error(String text) {
        return Component.text(text, NamedTextColor.RED);
    }

    static Component ok(String text) {
        return Component.text(text, NamedTextColor.GREEN);
    }

    static Component info(String text) {
        return Component.text(text, NamedTextColor.GRAY);
    }
}
