package com.aibuilder;

import java.util.*;

/**
 * Manages per-player conversation history so the AI can have multi-turn
 * conversations (e.g., asking clarifying questions before building).
 */
public class ConversationManager {

    private final AIBuilderPlugin plugin;
    private final Map<UUID, List<ConversationMessage>> conversations = new HashMap<>();

    // Conversations expire after 5 minutes of inactivity
    private final Map<UUID, Long> lastActivity = new HashMap<>();
    private static final long TIMEOUT_MS = 5 * 60 * 1000;

    public ConversationManager(AIBuilderPlugin plugin) {
        this.plugin = plugin;

        // Clean up stale conversations every minute
        plugin.getServer().getScheduler().runTaskTimer(plugin, (Runnable) this::cleanup, 1200L, 1200L);
    }

    public List<ConversationMessage> getHistory(UUID playerId) {
        cleanup(playerId);
        return conversations.getOrDefault(playerId, new ArrayList<>());
    }

    public void addMessage(UUID playerId, ConversationMessage message) {
        conversations.computeIfAbsent(playerId, k -> new ArrayList<>()).add(message);
        lastActivity.put(playerId, System.currentTimeMillis());
    }

    public void clearConversation(UUID playerId) {
        conversations.remove(playerId);
        lastActivity.remove(playerId);
    }

    public boolean hasActiveConversation(UUID playerId) {
        cleanup(playerId);
        return conversations.containsKey(playerId) && !conversations.get(playerId).isEmpty();
    }

    private void cleanup(UUID playerId) {
        Long last = lastActivity.get(playerId);
        if (last != null && System.currentTimeMillis() - last > TIMEOUT_MS) {
            clearConversation(playerId);
        }
    }

    private void cleanup() {
        List<UUID> expired = new ArrayList<>();
        long now = System.currentTimeMillis();
        for (Map.Entry<UUID, Long> entry : lastActivity.entrySet()) {
            if (now - entry.getValue() > TIMEOUT_MS) {
                expired.add(entry.getKey());
            }
        }
        expired.forEach(this::clearConversation);
    }
}
