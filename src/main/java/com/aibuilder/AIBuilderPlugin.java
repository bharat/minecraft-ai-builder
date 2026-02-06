package com.aibuilder;

import org.bukkit.plugin.java.JavaPlugin;

public class AIBuilderPlugin extends JavaPlugin {

    private AIService aiService;
    private BuildEngine buildEngine;
    private ConversationManager conversationManager;

    @Override
    public void onEnable() {
        // Save default config
        saveDefaultConfig();

        String envKey = System.getenv("OPENAI_API_KEY");
        String configKey = getConfig().getString("openai-api-key", "");
        if ((envKey == null || envKey.isEmpty()) &&
                (configKey.isEmpty() || configKey.equals("YOUR_API_KEY_HERE"))) {
            getLogger().warning("==============================================");
            getLogger().warning("  AIBuilder: No OpenAI API key configured!");
            getLogger().warning("  Set OPENAI_API_KEY environment variable");
            getLogger().warning("  or use: /aiconfig apikey <your-key>");
            getLogger().warning("==============================================");
        } else {
            getLogger().info("API key loaded" +
                    (envKey != null && !envKey.isEmpty() ? " from environment variable." : " from config.yml."));
        }

        // Initialize services
        this.aiService = new AIService(this);
        this.buildEngine = new BuildEngine(this);
        this.conversationManager = new ConversationManager(this);

        // Register commands
        AICommand aiCommand = new AICommand(this);
        getCommand("ai").setExecutor(aiCommand);
        getCommand("ai").setTabCompleter(aiCommand);

        AIConfigCommand configCommand = new AIConfigCommand(this);
        getCommand("aiconfig").setExecutor(configCommand);

        // Register chat listener
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);

        getLogger().info("AIBuilder enabled! Use /ai <message> or say 'AI, build me a house'");
    }

    @Override
    public void onDisable() {
        if (buildEngine != null) {
            buildEngine.cancelAllBuilds();
        }
        getLogger().info("AIBuilder disabled.");
    }

    public AIService getAIService() { return aiService; }
    public BuildEngine getBuildEngine() { return buildEngine; }
    public ConversationManager getConversationManager() { return conversationManager; }
}
