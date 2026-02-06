package com.aibuilder;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.*;

/**
 * Handles the actual block placement in the world.
 * Places blocks gradually (not all at once) to avoid lag and for visual effect.
 */
public class BuildEngine {

    private final AIBuilderPlugin plugin;
    private final Map<UUID, BukkitRunnable> activeBuilds = new HashMap<>();

    public BuildEngine(AIBuilderPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Start building a structure from a list of block placements.
     * Blocks are placed gradually over time for performance and visual appeal.
     */
    public void build(Player player, Location origin, List<BlockPlacement> blocks, String description) {
        // Cancel any existing build for this player
        cancelBuild(player.getUniqueId());

        int blocksPerTick = plugin.getConfig().getInt("blocks-per-tick", 50);
        int maxBlocks = plugin.getConfig().getInt("max-blocks", 10000);

        // Sort blocks bottom-up so the structure builds naturally
        List<BlockPlacement> sorted = new ArrayList<>(blocks);
        sorted.sort(Comparator.comparingInt(BlockPlacement::y)
                .thenComparingInt(BlockPlacement::x)
                .thenComparingInt(BlockPlacement::z));

        if (sorted.size() > maxBlocks) {
            player.sendMessage(Component.text("Build too large (" + sorted.size() +
                    " blocks, max " + maxBlocks + "). Try a smaller structure.", NamedTextColor.RED));
            return;
        }

        player.sendMessage(Component.text("Building: " + description +
                " (" + sorted.size() + " blocks)", NamedTextColor.GREEN));

        BukkitRunnable task = new BukkitRunnable() {
            int index = 0;
            int errors = 0;

            @Override
            public void run() {
                int placed = 0;
                while (index < sorted.size() && placed < blocksPerTick) {
                    BlockPlacement bp = sorted.get(index);
                    try {
                        Material material = Material.matchMaterial(bp.material());
                        if (material == null || !material.isBlock()) {
                            plugin.getLogger().warning("Invalid material: " + bp.material() +
                                    " (skipping)");
                            errors++;
                            index++;
                            continue;
                        }

                        Location blockLoc = origin.clone().add(bp.x(), bp.y(), bp.z());
                        Block block = blockLoc.getBlock();
                        block.setType(material, true);
                        placed++;
                    } catch (Exception e) {
                        plugin.getLogger().warning("Failed to place block: " + bp + " - " + e.getMessage());
                        errors++;
                    }
                    index++;
                }

                if (index >= sorted.size()) {
                    // Build complete
                    player.sendMessage(Component.text("Build complete! " +
                            (sorted.size() - errors) + " blocks placed.", NamedTextColor.GREEN));
                    if (errors > 0) {
                        player.sendMessage(Component.text(errors +
                                " blocks skipped due to errors.", NamedTextColor.YELLOW));
                    }
                    activeBuilds.remove(player.getUniqueId());
                    this.cancel();
                }
            }
        };

        activeBuilds.put(player.getUniqueId(), task);
        task.runTaskTimer(plugin, 1L, 1L); // Run every tick
    }

    public void cancelBuild(UUID playerId) {
        BukkitRunnable task = activeBuilds.remove(playerId);
        if (task != null) {
            task.cancel();
        }
    }

    public void cancelAllBuilds() {
        activeBuilds.values().forEach(BukkitRunnable::cancel);
        activeBuilds.clear();
    }

    public boolean isBuilding(UUID playerId) {
        return activeBuilds.containsKey(playerId);
    }
}
