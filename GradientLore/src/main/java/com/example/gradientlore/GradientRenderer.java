package com.example.gradientlore;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;

import java.util.List;
import java.util.regex.Pattern;

final class GradientRenderer {

    private static final Pattern HEX = Pattern.compile("#?[0-9a-fA-F]{6}");

    private GradientRenderer() {
    }

    /** Accepts "#rrggbb" or "rrggbb", with or without surrounding quotes. Returns null if invalid. */
    static TextColor parseHex(String raw) {
        if (raw == null) {
            return null;
        }
        String s = raw.replace("\"", "").trim();
        if (!HEX.matcher(s).matches()) {
            return null;
        }
        if (!s.startsWith("#")) {
            s = "#" + s;
        }
        return TextColor.fromHexString(s);
    }

    /** Colors are spaced evenly; t runs from 0.0 (first color) to 1.0 (last color). */
    static TextColor sample(List<TextColor> colors, double t) {
        int k = colors.size();
        if (k == 1) {
            return colors.get(0);
        }
        double scaled = Math.max(0.0, Math.min(1.0, t)) * (k - 1);
        int index = (int) Math.floor(scaled);
        if (index >= k - 1) {
            return colors.get(k - 1);
        }
        double f = scaled - index;
        TextColor a = colors.get(index);
        TextColor b = colors.get(index + 1);
        int r = (int) Math.round(a.red() + (b.red() - a.red()) * f);
        int g = (int) Math.round(a.green() + (b.green() - a.green()) * f);
        int bl = (int) Math.round(a.blue() + (b.blue() - a.blue()) * f);
        return TextColor.color(r, g, bl);
    }

    /** Colors every character of the text along the gradient, keeping the given decorations. */
    static Component render(String text, List<TextColor> colors, Style decorations) {
        int[] codePoints = text.codePoints().toArray();
        int n = codePoints.length;
        TextComponent.Builder builder = Component.text();
        for (int i = 0; i < n; i++) {
            double t = n <= 1 ? 0.0 : (double) i / (n - 1);
            String ch = new String(Character.toChars(codePoints[i]));
            builder.append(Component.text(ch, decorations.color(sample(colors, t))));
        }
        return builder.build();
    }
}
