package com.aibuilder;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * Listens for chat messages starting with "AI," to trigger the AI builder
 * without needing the /ai command.
 */
public class ChatListener implements Listener {

    private final AIBuilderPlugin plugin;

    public ChatListener(AIBuilderPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onChat(AsyncChatEvent event) {
        if (!plugin.getConfig().getBoolean("chat-trigger-enabled", true)) {
            return;
        }

        Player player = event.getPlayer();
        if (!player.hasPermission("aibuilder.use")) {
            return;
        }

        String message = PlainTextComponentSerializer.plainText().serialize(event.message());
        String prefix = plugin.getConfig().getString("chat-trigger-prefix", "AI,");

        if (message.toLowerCase().startsWith(prefix.toLowerCase())) {
            // Strip the prefix and pass the rest to the AI
            String aiMessage = message.substring(prefix.length()).trim();

            if (aiMessage.isEmpty()) {
                return;
            }

            // Cancel the chat message so it doesn't appear in chat for everyone
            event.setCancelled(true);

            // Handle on the main thread
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                AICommand aiCommand = new AICommand(plugin);
                aiCommand.handleAIRequest(player, aiMessage);
            });
        }
    }
}
