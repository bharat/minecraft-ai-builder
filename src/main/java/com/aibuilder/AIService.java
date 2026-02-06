package com.aibuilder;

import com.google.gson.*;
import okhttp3.*;
import org.bukkit.Location;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Handles communication with the OpenAI API.
 * Converts natural language build requests into structured block placement instructions.
 */
public class AIService {

    private final AIBuilderPlugin plugin;
    private final OkHttpClient httpClient;
    private final Gson gson = new Gson();

    private static final String OPENAI_URL = "https://api.openai.com/v1/chat/completions";

    private static final String SYSTEM_PROMPT = """
        You are an AI building assistant inside Minecraft. Players ask you to build structures.
        
        When a player asks you to build something, you have two modes:
        
        1. CLARIFY - If the request is ambiguous, ask a short clarifying question.
           Respond with JSON: {"action": "clarify", "message": "your question here"}
        
        2. BUILD - When you have enough information, respond with a build plan.
           Respond with JSON:
           {
             "action": "build",
             "description": "Brief description of what you're building",
             "blocks": [
               {"x": 0, "y": 0, "z": 0, "material": "OAK_PLANKS"},
               {"x": 1, "y": 0, "z": 0, "material": "OAK_PLANKS"}
             ]
           }
        
        IMPORTANT RULES FOR BUILDING:
        - Coordinates are RELATIVE to the player's position (0,0,0 = where they stand).
        - Build in the +X and +Z direction from the player (so they can watch).
        - Start at y=0 (ground level relative to player) and build UP.
        - Use valid Minecraft material names (e.g., OAK_PLANKS, COBBLESTONE, GLASS, OAK_DOOR, TORCH, etc.)
        - For a simple house, include: walls, floor, roof, a door, windows, torches inside.
        - Keep builds reasonable (under 5000 blocks for a house).
        - Make structures that look good and are functional (doors that open, torches for light).
        - For doors, use OAK_DOOR (the bottom half - the game handles placement).
        - ALWAYS respond with valid JSON only. No markdown, no code fences, just JSON.
        
        MATERIAL REFERENCE (use exact names):
        - Wood: OAK_PLANKS, SPRUCE_PLANKS, BIRCH_PLANKS, OAK_LOG, SPRUCE_LOG
        - Stone: COBBLESTONE, STONE, STONE_BRICKS, SMOOTH_STONE
        - Glass: GLASS, GLASS_PANE
        - Doors: OAK_DOOR, SPRUCE_DOOR, IRON_DOOR
        - Roof: OAK_STAIRS, SPRUCE_STAIRS, COBBLESTONE_STAIRS
        - Lighting: TORCH, LANTERN, GLOWSTONE
        - Decoration: CRAFTING_TABLE, FURNACE, CHEST, BOOKSHELF
        - Misc: AIR (to clear space), COBBLESTONE_WALL, OAK_FENCE
        
        When building a house:
        - Make it at least 7x7x5 (width x depth x height) for a small house
        - Include interior furnishing (crafting table, furnace, chest, bed)
        - Place torches on walls for lighting
        - Use glass panes for windows
        - Add a proper roof (use stairs for a sloped look, or use slabs)
        - Leave a 1-block gap for the door with OAK_DOOR
        """;

    public AIService(AIBuilderPlugin plugin) {
        this.plugin = plugin;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)  // AI can take a while for large builds
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    /**
     * Send a message to the AI and get a response.
     * Returns a CompletableFuture so we don't block the main thread.
     */
    public CompletableFuture<AIResponse> chat(List<ConversationMessage> history, Location playerLocation) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Environment variable takes priority, then config.yml
                String apiKey = System.getenv("OPENAI_API_KEY");
                if (apiKey == null || apiKey.isEmpty()) {
                    apiKey = plugin.getConfig().getString("openai-api-key", "");
                }
                String model = plugin.getConfig().getString("openai-model", "gpt-4o");

                if (apiKey.isEmpty() || apiKey.equals("YOUR_API_KEY_HERE")) {
                    return new AIResponse(AIResponse.Type.ERROR,
                            "No API key configured! Set OPENAI_API_KEY env var or use /aiconfig apikey <key>", null);
                }

                // Build messages array
                JsonArray messages = new JsonArray();

                // System message with context about player location
                JsonObject systemMsg = new JsonObject();
                systemMsg.addProperty("role", "system");
                systemMsg.addProperty("content", SYSTEM_PROMPT +
                        "\n\nPlayer is at coordinates: x=" + playerLocation.getBlockX() +
                        " y=" + playerLocation.getBlockY() +
                        " z=" + playerLocation.getBlockZ() +
                        " in biome: " + playerLocation.getBlock().getBiome().getKey().value() +
                        "\nRemember: your block coordinates should be RELATIVE offsets from the player position.");
                messages.add(systemMsg);

                // Conversation history
                for (ConversationMessage msg : history) {
                    JsonObject m = new JsonObject();
                    m.addProperty("role", msg.role());
                    m.addProperty("content", msg.content());
                    messages.add(m);
                }

                // Build request
                JsonObject requestBody = new JsonObject();
                requestBody.addProperty("model", model);
                requestBody.add("messages", messages);
                requestBody.addProperty("temperature", 0.7);
                requestBody.addProperty("max_tokens", 16000);

                Request request = new Request.Builder()
                        .url(OPENAI_URL)
                        .addHeader("Authorization", "Bearer " + apiKey)
                        .addHeader("Content-Type", "application/json")
                        .post(RequestBody.create(
                                gson.toJson(requestBody),
                                MediaType.parse("application/json")))
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        String errorBody = response.body() != null ? response.body().string() : "unknown";
                        plugin.getLogger().warning("OpenAI API error: " + response.code() + " - " + errorBody);
                        return new AIResponse(AIResponse.Type.ERROR,
                                "AI service error (HTTP " + response.code() + "). Check server logs.", null);
                    }

                    String responseBody = response.body().string();
                    JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
                    String content = json.getAsJsonArray("choices")
                            .get(0).getAsJsonObject()
                            .getAsJsonObject("message")
                            .get("content").getAsString()
                            .trim();

                    // Strip markdown code fences if present
                    if (content.startsWith("```")) {
                        content = content.replaceAll("^```(?:json)?\\s*", "").replaceAll("\\s*```$", "");
                    }

                    return parseAIResponse(content);
                }
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to contact AI service: " + e.getMessage());
                return new AIResponse(AIResponse.Type.ERROR,
                        "Failed to contact AI service. Check your internet connection.", null);
            } catch (Exception e) {
                plugin.getLogger().severe("AI response parsing error: " + e.getMessage());
                e.printStackTrace();
                return new AIResponse(AIResponse.Type.ERROR,
                        "Failed to understand AI response. Try again with a simpler request.", null);
            }
        });
    }

    private AIResponse parseAIResponse(String content) {
        try {
            JsonObject json = JsonParser.parseString(content).getAsJsonObject();
            String action = json.get("action").getAsString();

            if ("clarify".equals(action)) {
                String message = json.get("message").getAsString();
                return new AIResponse(AIResponse.Type.CLARIFY, message, null);
            } else if ("build".equals(action)) {
                String description = json.has("description") ?
                        json.get("description").getAsString() : "Building structure";
                JsonArray blocksJson = json.getAsJsonArray("blocks");

                List<BlockPlacement> blocks = new ArrayList<>();
                for (JsonElement elem : blocksJson) {
                    JsonObject b = elem.getAsJsonObject();
                    blocks.add(new BlockPlacement(
                            b.get("x").getAsInt(),
                            b.get("y").getAsInt(),
                            b.get("z").getAsInt(),
                            b.get("material").getAsString()
                    ));
                }

                return new AIResponse(AIResponse.Type.BUILD, description, blocks);
            } else {
                return new AIResponse(AIResponse.Type.ERROR,
                        "Unexpected AI response. Try again.", null);
            }
        } catch (JsonSyntaxException e) {
            // If AI returned plain text instead of JSON, treat it as a clarification
            return new AIResponse(AIResponse.Type.CLARIFY, content, null);
        }
    }
}
