package net.blueva.arcade.modules.runfromthebeast.support;

import net.blueva.arcade.api.config.ModuleConfigAPI;
import net.blueva.arcade.api.game.GameContext;
import net.blueva.arcade.modules.runfromthebeast.state.RunFromTheBeastArenaState;
import com.hypixel.hytale.math.vector.Location;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.meta.BlockState;

public class RunFromTheBeastDistanceService {

    private final ModuleConfigAPI moduleConfig;

    public RunFromTheBeastDistanceService(ModuleConfigAPI moduleConfig) {
        this.moduleConfig = moduleConfig;
    }

    public void updateDistanceBars(GameContext<Player, Location, World, String, ItemStack, String, BlockState, Entity> context,
                                   RunFromTheBeastArenaState state,
                                   Player beast) {
        // Distance bars are not yet implemented in Hytale.
    }

    public void resetDistanceBars(GameContext<Player, Location, World, String, ItemStack, String, BlockState, Entity> context) {
        // Distance bars are not yet implemented in Hytale.
    }

    public void resetDistanceBar(Player player) {
        // Distance bars are not yet implemented in Hytale.
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
