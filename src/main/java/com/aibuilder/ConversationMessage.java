package com.aibuilder;

/**
 * A single message in a player's conversation with the AI.
 */
public record ConversationMessage(String role, String content) {

    public static ConversationMessage user(String content) {
        return new ConversationMessage("user", content);
    }

    public static ConversationMessage assistant(String content) {
        return new ConversationMessage("assistant", content);
    }
}
