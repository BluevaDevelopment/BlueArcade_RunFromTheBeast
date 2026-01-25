package net.blueva.arcade.modules.runfromthebeast.support;

import net.blueva.arcade.api.config.ModuleConfigAPI;
import net.blueva.arcade.api.game.GameContext;
import net.blueva.arcade.modules.runfromthebeast.state.RunFromTheBeastArenaState;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class RunFromTheBeastDistanceService {

    private final ModuleConfigAPI moduleConfig;

    public RunFromTheBeastDistanceService(ModuleConfigAPI moduleConfig) {
        this.moduleConfig = moduleConfig;
    }

    public void updateDistanceBars(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                                   RunFromTheBeastArenaState state,
                                   Player beast) {
        if (beast == null || !beast.isOnline()) {
            resetDistanceBars(context);
            return;
        }

        double minDistance = moduleConfig.getDouble("distance_bar.min_distance", 3.0);
        double maxDistance = moduleConfig.getDouble("distance_bar.max_distance", 60.0);

        for (Player player : context.getAlivePlayers()) {
            if (!player.isOnline()) {
                continue;
            }

            if (player.getUniqueId().equals(beast.getUniqueId())) {
                resetDistanceBar(player);
                continue;
            }

            if (!player.getWorld().equals(beast.getWorld())) {
                resetDistanceBar(player);
                continue;
            }

            double distance = player.getLocation().distance(beast.getLocation());
            float progress = calculateDistanceProgress(distance, minDistance, maxDistance);
            player.setExp(progress);
        }
    }

    public void resetDistanceBars(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context) {
        for (Player player : context.getPlayers()) {
            resetDistanceBar(player);
        }
    }

    public void resetDistanceBar(Player player) {
        player.setExp(0f);
    }

    private float calculateDistanceProgress(double distance, double minDistance, double maxDistance) {
        if (maxDistance <= minDistance) {
            return 1.0f;
        }
        double clamped = Math.min(Math.max(distance, minDistance), maxDistance);
        double ratio = (clamped - minDistance) / (maxDistance - minDistance);
        return (float) Math.max(0.0, Math.min(1.0, 1.0 - ratio));
    }
}
