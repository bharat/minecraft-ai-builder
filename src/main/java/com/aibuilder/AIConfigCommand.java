package com.aibuilder;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class AIConfigCommand implements CommandExecutor {

    private final AIBuilderPlugin plugin;

    public AIConfigCommand(AIBuilderPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /aiconfig <key> <value>", NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("Keys: apikey, model, maxblocks, speed", NamedTextColor.GRAY));
            return true;
        }

        String key = args[0].toLowerCase();
        String value = args[1];

        switch (key) {
            case "apikey" -> {
                plugin.getConfig().set("openai-api-key", value);
                plugin.saveConfig();
                sender.sendMessage(Component.text("API key updated!", NamedTextColor.GREEN));
            }
            case "model" -> {
                plugin.getConfig().set("openai-model", value);
                plugin.saveConfig();
                sender.sendMessage(Component.text("Model set to: " + value, NamedTextColor.GREEN));
            }
            case "maxblocks" -> {
                try {
                    int max = Integer.parseInt(value);
                    plugin.getConfig().set("max-blocks", max);
                    plugin.saveConfig();
                    sender.sendMessage(Component.text("Max blocks set to: " + max, NamedTextColor.GREEN));
                } catch (NumberFormatException e) {
                    sender.sendMessage(Component.text("Invalid number: " + value, NamedTextColor.RED));
                }
            }
            case "speed" -> {
                try {
                    int speed = Integer.parseInt(value);
                    plugin.getConfig().set("blocks-per-tick", speed);
                    plugin.saveConfig();
                    sender.sendMessage(Component.text("Build speed set to: " + speed +
                            " blocks/tick", NamedTextColor.GREEN));
                } catch (NumberFormatException e) {
                    sender.sendMessage(Component.text("Invalid number: " + value, NamedTextColor.RED));
                }
            }
            default -> sender.sendMessage(Component.text("Unknown key: " + key +
                    ". Use: apikey, model, maxblocks, speed", NamedTextColor.RED));
        }

        return true;
    }
}
