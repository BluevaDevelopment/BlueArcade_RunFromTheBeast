package net.blueva.arcade.modules.runfromthebeast.support;

import net.blueva.arcade.api.game.GameContext;
import net.blueva.arcade.modules.runfromthebeast.state.RunFromTheBeastArenaState;
import com.hypixel.hytale.math.vector.Location;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.meta.BlockState;

import java.util.ArrayList;
import java.util.List;

public class RunFromTheBeastCageService {

    private final RunFromTheBeastConfigHelper configHelper;

    public RunFromTheBeastCageService(RunFromTheBeastConfigHelper configHelper) {
        this.configHelper = configHelper;
    }

    public List<String> loadCageBlocks(GameContext<Player, Location, World, String, ItemStack, String, BlockState, Entity> context) {
        List<?> raw = context.getDataAccess().getGameData("game.beast_cage.blocks", List.class);
        if (raw == null) {
            return new ArrayList<>();
        }

        List<String> result = new ArrayList<>();
        for (Object element : raw) {
            result.add(String.valueOf(element));
        }
        return result;
    }

    public void clearCage(GameContext<Player, Location, World, String, ItemStack, String, BlockState, Entity> context,
                          RunFromTheBeastArenaState state) {
        if (state.getCageBlocks().isEmpty()) {
            Location min = context.getDataAccess().getGameLocation("game.beast_cage.bounds.min");
            Location max = context.getDataAccess().getGameLocation("game.beast_cage.bounds.max");
            if (min != null && max != null) {
                context.getBlocksAPI().clearRegion(min, max);
            }
            return;
        }

        clearSavedCageBlocks(context, state.getCageBlocks());
    }

    public void restoreCage(GameContext<Player, Location, World, String, ItemStack, String, BlockState, Entity> context,
                            RunFromTheBeastArenaState state) {
        if (state == null || state.getCageBlocks().isEmpty()) {
            return;
        }
        applySavedCageBlocks(context, state.getCageBlocks());
    }

    private void clearSavedCageBlocks(GameContext<Player, Location, World, String, ItemStack, String, BlockState, Entity> context,
                                      List<String> blockData) {
        for (String entry : blockData) {
            String[] parts = entry.split(",");
            if (parts.length != 5 && parts.length != 8) {
                continue;
            }

            int startX = configHelper.parseIntSafe(parts[1], Integer.MIN_VALUE);
            int startY = configHelper.parseIntSafe(parts[2], Integer.MIN_VALUE);
            int startZ = configHelper.parseIntSafe(parts[3], Integer.MIN_VALUE);

            if (startX == Integer.MIN_VALUE || startY == Integer.MIN_VALUE || startZ == Integer.MIN_VALUE) {
                continue;
            }

            int endX = startX;
            int endY = startY;
            int endZ = startZ;

            if (parts.length == 8) {
                endX = configHelper.parseIntSafe(parts[4], startX);
                endY = configHelper.parseIntSafe(parts[5], startY);
                endZ = configHelper.parseIntSafe(parts[6], startZ);
            }

            for (int x = startX; x <= endX; x++) {
                for (int y = startY; y <= endY; y++) {
                    for (int z = startZ; z <= endZ; z++) {
                        Location location = new Location(parts[0], x, y, z);
                        context.getBlocksAPI().setBlock(location, "Empty");
                    }
                }
            }
        }
    }

    private void applySavedCageBlocks(GameContext<Player, Location, World, String, ItemStack, String, BlockState, Entity> context,
                                      List<String> blockData) {
        for (String entry : blockData) {
            String[] parts = entry.split(",");
            if (parts.length != 8) {
                continue;
            }

            int startX = configHelper.parseIntSafe(parts[1], Integer.MIN_VALUE);
            int startY = configHelper.parseIntSafe(parts[2], Integer.MIN_VALUE);
            int startZ = configHelper.parseIntSafe(parts[3], Integer.MIN_VALUE);
            int endX = configHelper.parseIntSafe(parts[4], Integer.MIN_VALUE);
            int endY = configHelper.parseIntSafe(parts[5], Integer.MIN_VALUE);
            int endZ = configHelper.parseIntSafe(parts[6], Integer.MIN_VALUE);
            String material = configHelper.getItemIdSafe(parts[7], "Empty");

            if (startX == Integer.MIN_VALUE || startY == Integer.MIN_VALUE || startZ == Integer.MIN_VALUE ||
                    endX == Integer.MIN_VALUE || endY == Integer.MIN_VALUE || endZ == Integer.MIN_VALUE) {
                continue;
            }

            for (int x = startX; x <= endX; x++) {
                for (int y = startY; y <= endY; y++) {
                    for (int z = startZ; z <= endZ; z++) {
                        Location location = new Location(parts[0], x, y, z);
                        context.getBlocksAPI().setBlock(location, material);
                    }
                }
            }
        }
    }
}
