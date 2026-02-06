package com.aibuilder;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

public class AICommand implements CommandExecutor, TabCompleter {

    private final AIBuilderPlugin plugin;

    public AICommand(AIBuilderPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(Component.text("Usage: /ai <message>", NamedTextColor.YELLOW));
            player.sendMessage(Component.text("Example: /ai build me a cozy wooden house", NamedTextColor.GRAY));
            player.sendMessage(Component.text("Use '/ai cancel' to stop a build, '/ai clear' to reset conversation.",
                    NamedTextColor.GRAY));
            return true;
        }

        String message = String.join(" ", args);

        // Handle special subcommands
        if (message.equalsIgnoreCase("cancel")) {
            if (plugin.getBuildEngine().isBuilding(player.getUniqueId())) {
                plugin.getBuildEngine().cancelBuild(player.getUniqueId());
                player.sendMessage(Component.text("Build cancelled.", NamedTextColor.YELLOW));
            } else {
                player.sendMessage(Component.text("No active build to cancel.", NamedTextColor.GRAY));
            }
            return true;
        }

        if (message.equalsIgnoreCase("clear")) {
            plugin.getConversationManager().clearConversation(player.getUniqueId());
            player.sendMessage(Component.text("Conversation cleared.", NamedTextColor.YELLOW));
            return true;
        }

        // Process the AI request
        handleAIRequest(player, message);
        return true;
    }

    void handleAIRequest(Player player, String message) {
        ConversationManager convo = plugin.getConversationManager();

        // Add user message to history
        convo.addMessage(player.getUniqueId(), ConversationMessage.user(message));

        player.sendMessage(Component.text("AI is thinking...", NamedTextColor.AQUA)
                .decorate(TextDecoration.ITALIC));

        // Call AI asynchronously
        plugin.getAIService()
                .chat(convo.getHistory(player.getUniqueId()), player.getLocation())
                .thenAccept(response -> {
                    // Switch back to main thread for Bukkit API calls
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        switch (response.type()) {
                            case CLARIFY -> {
                                convo.addMessage(player.getUniqueId(),
                                        ConversationMessage.assistant(response.message()));
                                player.sendMessage(Component.text("[AI] ", NamedTextColor.AQUA)
                                        .append(Component.text(response.message(), NamedTextColor.WHITE)));
                            }
                            case BUILD -> {
                                player.sendMessage(Component.text("[AI] ", NamedTextColor.AQUA)
                                        .append(Component.text(response.message(), NamedTextColor.GREEN)));
                                plugin.getBuildEngine().build(
                                        player, player.getLocation(),
                                        response.blocks(), response.message());
                                // Clear conversation after successful build start
                                convo.clearConversation(player.getUniqueId());
                            }
                            case ERROR -> {
                                player.sendMessage(Component.text("[AI Error] ", NamedTextColor.RED)
                                        .append(Component.text(response.message(), NamedTextColor.YELLOW)));
                            }
                        }
                    });
                });
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                  @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return Arrays.asList("build me a house", "build me a castle", "cancel", "clear")
                    .stream().filter(s -> s.startsWith(args[0].toLowerCase())).toList();
        }
        return List.of();
    }
}
