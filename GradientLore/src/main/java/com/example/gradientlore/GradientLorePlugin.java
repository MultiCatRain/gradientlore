package com.example.gradientlore;

import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.plugin.java.JavaPlugin;

public final class GradientLorePlugin extends JavaPlugin {

    private GradientManager gradients;
    private TextParser parser;
    private Limits limits;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        limits = new Limits(this);
        gradients = new GradientManager(this);
        gradients.load();
        parser = new TextParser(gradients);

        register("gradient", new GradientCommand(this));
        register("name", new NameCommand(this));
        register("lore", new LoreCommand(this));
        getServer().getPluginManager().registerEvents(new AnvilListener(this), this);
    }

    @Override
    public void onDisable() {
        if (gradients != null) {
            gradients.save();
        }
    }

    private void register(String name, TabExecutor executor) {
        PluginCommand command = getCommand(name);
        if (command == null) {
            getLogger().severe("Command /" + name + " is missing from plugin.yml");
            return;
        }
        command.setExecutor(executor);
        command.setTabCompleter(executor);
    }

    GradientManager gradients() {
        return gradients;
    }

    TextParser parser() {
        return parser;
    }

    Limits limits() {
        return limits;
    }
}
