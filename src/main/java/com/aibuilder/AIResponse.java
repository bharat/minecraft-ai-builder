package com.aibuilder;

import java.util.List;

/**
 * Represents a parsed response from the AI service.
 */
public record AIResponse(Type type, String message, List<BlockPlacement> blocks) {

    public enum Type {
        /** AI needs more info - asking a clarifying question */
        CLARIFY,
        /** AI has a build plan ready to execute */
        BUILD,
        /** Something went wrong */
        ERROR
    }
}
