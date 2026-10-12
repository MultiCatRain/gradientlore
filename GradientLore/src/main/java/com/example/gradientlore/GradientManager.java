package com.example.gradientlore;

import net.kyori.adventure.text.format.TextColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.regex.Pattern;

/** Per-player active + saved gradients, stored in plugins/GradientLore/gradients.yml. */
final class GradientManager {

    private static final Pattern NAME = Pattern.compile("[a-z0-9_-]{1,24}");

    private static final class PlayerData {
        List<TextColor> active = new ArrayList<>();
        final Map<String, List<TextColor>> saved = new LinkedHashMap<>();
    }

    private final GradientLorePlugin plugin;
    private final File file;
    private final Map<UUID, PlayerData> data = new HashMap<>();

    GradientManager(GradientLorePlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "gradients.yml");
    }

    /** Lowercases and validates a gradient name. Returns null if it is not a valid name. */
    static String normalizeName(String raw) {
        if (raw == null) {
            return null;
        }
        String s = raw.replace("\"", "").trim().toLowerCase(Locale.ROOT);
        return NAME.matcher(s).matches() ? s : null;
    }

    private PlayerData of(UUID id) {
        return data.computeIfAbsent(id, k -> new PlayerData());
    }

    List<TextColor> getActive(UUID id) {
        PlayerData pd = data.get(id);
        return pd == null ? List.of() : pd.active;
    }

    void setActive(UUID id, List<TextColor> colors) {
        of(id).active = new ArrayList<>(colors);
        save();
    }

    /** Returns the saved gradient, or null if the player has none with that name. */
    List<TextColor> getSaved(UUID id, String name) {
        PlayerData pd = data.get(id);
        return pd == null ? null : pd.saved.get(name);
    }

    Set<String> savedNames(UUID id) {
        PlayerData pd = data.get(id);
        return pd == null ? Set.of() : new LinkedHashSet<>(pd.saved.keySet());
    }

    void putSaved(UUID id, String name, List<TextColor> colors) {
        of(id).saved.put(name, new ArrayList<>(colors));
        save();
    }

    boolean removeSaved(UUID id, String name) {
        PlayerData pd = data.get(id);
        if (pd == null || pd.saved.remove(name) == null) {
            return false;
        }
        save();
        return true;
    }

    void load() {
        if (!file.exists()) {
            return;
        }
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection players = yml.getConfigurationSection("players");
        if (players == null) {
            return;
        }
        for (String key : players.getKeys(false)) {
            UUID id;
            try {
                id = UUID.fromString(key);
            } catch (IllegalArgumentException e) {
                continue;
            }
            ConfigurationSection section = players.getConfigurationSection(key);
            if (section == null) {
                continue;
            }
            PlayerData pd = new PlayerData();
            pd.active = fromHex(section.getStringList("active"));
            ConfigurationSection saved = section.getConfigurationSection("saved");
            if (saved != null) {
                for (String name : saved.getKeys(false)) {
                    List<TextColor> colors = fromHex(saved.getStringList(name));
                    if (colors.size() >= 2) {
                        pd.saved.put(name, colors);
                    }
                }
            }
            data.put(id, pd);
        }
    }

    void save() {
        YamlConfiguration yml = new YamlConfiguration();
        for (Map.Entry<UUID, PlayerData> entry : data.entrySet()) {
            PlayerData pd = entry.getValue();
            if (pd.active.isEmpty() && pd.saved.isEmpty()) {
                continue;
            }
            String base = "players." + entry.getKey();
            yml.set(base + ".active", toHex(pd.active));
            for (Map.Entry<String, List<TextColor>> s : pd.saved.entrySet()) {
                yml.set(base + ".saved." + s.getKey(), toHex(s.getValue()));
            }
        }
        try {
            plugin.getDataFolder().mkdirs();
            yml.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save gradients.yml", e);
        }
    }

    private static List<String> toHex(List<TextColor> colors) {
        List<String> out = new ArrayList<>();
        for (TextColor c : colors) {
            out.add(c.asHexString());
        }
        return out;
    }

    private static List<TextColor> fromHex(List<String> raw) {
        List<TextColor> out = new ArrayList<>();
        for (String s : raw) {
            TextColor c = GradientRenderer.parseHex(s);
            if (c != null) {
                out.add(c);
            }
        }
        return out;
    }
}
