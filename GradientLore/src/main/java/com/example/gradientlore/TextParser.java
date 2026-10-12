package com.example.gradientlore;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Turns text with &amp; codes into a Component.
 *
 * Supported codes:
 *   &amp;0-&amp;f      vanilla colors (also resets styles and any gradient, like vanilla)
 *   &amp;#rrggbb      hex color
 *   &amp;l &amp;o &amp;n &amp;m &amp;k   bold, italic, underline, strikethrough, obfuscated
 *   &amp;g           the player's active gradient
 *   &amp;g&lt;name&gt;     one of the player's saved gradients
 *   &amp;r           reset everything
 *   &amp;&amp;           a literal &amp;
 *
 * Only these codes exist, so players can never create click or hover events.
 */
final class TextParser {

    record Result(Component component, String plain) {
    }

    private static final Pattern HEX = Pattern.compile("#[0-9a-fA-F]{6}");
    private static final String LEGACY_CODES = "0123456789abcdef";
    private static final TextColor[] LEGACY = {
            NamedTextColor.BLACK, NamedTextColor.DARK_BLUE, NamedTextColor.DARK_GREEN, NamedTextColor.DARK_AQUA,
            NamedTextColor.DARK_RED, NamedTextColor.DARK_PURPLE, NamedTextColor.GOLD, NamedTextColor.GRAY,
            NamedTextColor.DARK_GRAY, NamedTextColor.BLUE, NamedTextColor.GREEN, NamedTextColor.AQUA,
            NamedTextColor.RED, NamedTextColor.LIGHT_PURPLE, NamedTextColor.YELLOW, NamedTextColor.WHITE
    };

    private static final class State {
        TextColor color;
        List<TextColor> gradient;
        boolean bold;
        boolean italic;
        boolean underlined;
        boolean strikethrough;
        boolean obfuscated;

        void reset() {
            color = null;
            gradient = null;
            clearStyles();
        }

        void clearStyles() {
            bold = false;
            italic = false;
            underlined = false;
            strikethrough = false;
            obfuscated = false;
        }

        Style decorations() {
            Style.Builder b = Style.style();
            if (bold) {
                b.decoration(TextDecoration.BOLD, true);
            }
            if (italic) {
                b.decoration(TextDecoration.ITALIC, true);
            }
            if (underlined) {
                b.decoration(TextDecoration.UNDERLINED, true);
            }
            if (strikethrough) {
                b.decoration(TextDecoration.STRIKETHROUGH, true);
            }
            if (obfuscated) {
                b.decoration(TextDecoration.OBFUSCATED, true);
            }
            return b.build();
        }
    }

    private final GradientManager gradients;

    TextParser(GradientManager gradients) {
        this.gradients = gradients;
    }

    /** Parses the text for the given player (their gradients are used for &amp;g). */
    Result parse(UUID player, String input) {
        List<Component> parts = new ArrayList<>();
        StringBuilder buf = new StringBuilder();
        StringBuilder plain = new StringBuilder();
        State st = new State();

        int n = input.length();
        int i = 0;
        while (i < n) {
            char c = input.charAt(i);
            if (c != '&' || i + 1 >= n) {
                buf.append(c);
                plain.append(c);
                i++;
                continue;
            }

            char code = Character.toLowerCase(input.charAt(i + 1));

            if (code == '&') {
                buf.append('&');
                plain.append('&');
                i += 2;
                continue;
            }

            if (code == 'g') {
                flush(parts, buf, st);
                i += 2;
                List<TextColor> chosen = null;
                boolean named = false;
                if (i < n && input.charAt(i) == '<') {
                    int end = input.indexOf('>', i);
                    if (end > i) {
                        String name = GradientManager.normalizeName(input.substring(i + 1, end));
                        if (name != null) {
                            chosen = gradients.getSaved(player, name);
                            i = end + 1;
                            named = true;
                        }
                    }
                }
                if (!named) {
                    chosen = gradients.getActive(player);
                }
                st.gradient = (chosen == null || chosen.size() < 2) ? null : chosen;
                continue;
            }

            if (code == 'r') {
                flush(parts, buf, st);
                st.reset();
                i += 2;
                continue;
            }

            if (code == '#') {
                if (i + 8 <= n) {
                    String hex = input.substring(i + 1, i + 8);
                    if (HEX.matcher(hex).matches()) {
                        flush(parts, buf, st);
                        st.color = TextColor.fromHexString(hex);
                        st.gradient = null;
                        st.clearStyles();
                        i += 8;
                        continue;
                    }
                }
                buf.append('&');
                plain.append('&');
                i++;
                continue;
            }

            int legacy = LEGACY_CODES.indexOf(code);
            if (legacy >= 0) {
                flush(parts, buf, st);
                st.color = LEGACY[legacy];
                st.gradient = null;
                st.clearStyles();
                i += 2;
                continue;
            }

            if (code == 'l' || code == 'o' || code == 'n' || code == 'm' || code == 'k') {
                flush(parts, buf, st);
                switch (code) {
                    case 'l' -> st.bold = true;
                    case 'o' -> st.italic = true;
                    case 'n' -> st.underlined = true;
                    case 'm' -> st.strikethrough = true;
                    default -> st.obfuscated = true;
                }
                i += 2;
                continue;
            }

            // Unknown code: keep it as plain text.
            buf.append('&');
            plain.append('&');
            i++;
        }
        flush(parts, buf, st);

        // Names and lore are italic by default in Minecraft; turn that off unless the player used &o.
        TextComponent.Builder root = Component.text().decoration(TextDecoration.ITALIC, false);
        for (Component part : parts) {
            root.append(part);
        }
        return new Result(root.build(), plain.toString());
    }

    private static void flush(List<Component> parts, StringBuilder buf, State st) {
        if (buf.isEmpty()) {
            return;
        }
        String text = buf.toString();
        buf.setLength(0);

        Style decorations = st.decorations();
        int count = text.codePointCount(0, text.length());

        // A gradient is only applied when the text has at least one character per color.
        if (st.gradient != null && count >= st.gradient.size()) {
            parts.add(GradientRenderer.render(text, st.gradient, decorations));
        } else {
            Style style = st.color == null ? decorations : decorations.color(st.color);
            parts.add(Component.text(text, style));
        }
    }
}
