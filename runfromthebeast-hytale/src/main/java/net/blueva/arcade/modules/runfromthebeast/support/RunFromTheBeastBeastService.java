package net.blueva.arcade.modules.runfromthebeast.support;

import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import net.blueva.arcade.api.config.ModuleConfigAPI;
import net.blueva.arcade.api.game.GameContext;
import net.blueva.arcade.api.store.StoreAPI;
import net.blueva.arcade.modules.runfromthebeast.state.RunFromTheBeastArenaState;
import com.hypixel.hytale.math.vector.Location;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.meta.BlockState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class RunFromTheBeastBeastService {

    private final ModuleConfigAPI moduleConfig;
    private final StoreAPI storeAPI;

    public RunFromTheBeastBeastService(ModuleConfigAPI moduleConfig, StoreAPI storeAPI) {
        this.moduleConfig = moduleConfig;
        this.storeAPI = storeAPI;
    }

    public void selectBeast(GameContext<Player, Location, World, String, ItemStack, String, BlockState, Entity> context,
                            RunFromTheBeastArenaState state) {
        List<Player> candidates = new ArrayList<>(context.getPlayers());
        if (candidates.isEmpty()) {
            return;
        }

        Map<Player, Integer> weighted = buildBeastWeights(candidates);
        int totalWeight = weighted.values().stream().mapToInt(Integer::intValue).sum();
        int roll = ThreadLocalRandom.current().nextInt(Math.max(totalWeight, 1)) + 1;
        int cumulative = 0;
        Player chosen = candidates.get(0);

        for (Map.Entry<Player, Integer> entry : weighted.entrySet()) {
            cumulative += entry.getValue();
            if (roll <= cumulative) {
                chosen = entry.getKey();
                break;
            }
        }

        state.setBeastId(chosen.getUuid());
        teleportBeastToCage(context, chosen);
    }

    public Player getBeast(GameContext<Player, Location, World, String, ItemStack, String, BlockState, Entity> context,
                           RunFromTheBeastArenaState state) {
        if (state.getBeastId() == null) {
            return null;
        }
        for (Player player : context.getPlayers()) {
            if (player.getUuid().equals(state.getBeastId())) {
                return player;
            }
        }
        return null;
    }

    public void applyBeastGlow(GameContext<Player, Location, World, String, ItemStack, String, BlockState, Entity> context,
                               RunFromTheBeastArenaState state,
                               Player beast) {
        // Glow effects are not yet available in Hytale.
    }

    public void removeBeastGlow(GameContext<Player, Location, World, String, ItemStack, String, BlockState, Entity> context,
                                RunFromTheBeastArenaState state) {
        // No-op until Hytale glow effects are available.
    }

    private Map<Player, Integer> buildBeastWeights(List<Player> candidates) {
        Map<Player, Integer> weights = new HashMap<>();
        int defaultWeight = moduleConfig.getIntFrom("store.yml", "weights.beast_pass.default", 1);
        int bonusWeight = moduleConfig.getIntFrom("store.yml", "weights.beast_pass.bonus", 3);
        String beastPassCategory = moduleConfig.getStringFrom("store.yml", "category_settings.beast_pass.id", "rftb_beast_pass");
        for (Player player : candidates) {
            int weight = defaultWeight;
            boolean ownsPass = storeAPI != null && storeAPI.isUnlocked(player, beastPassCategory, "pass");
            if (ownsPass) {
                weight += bonusWeight;
            }
            weights.put(player, weight);
        }
        return weights;
    }

    private void teleportBeastToCage(GameContext<Player, Location, World, String, ItemStack, String, BlockState, Entity> context,
                                     Player beast) {
        Location beastSpawn = context.getDataAccess().getGameLocation("game.beast.spawn");
        if (beastSpawn != null) {
            World world = context.getArenaAPI().getWorld();
            if (world != null) {
                world.execute(() -> {
                    if (beast.getReference() == null) {
                        return;
                    }
                    Store<EntityStore> store = beast.getReference().getStore();
                    Transform transform = new Transform(beastSpawn.getPosition(), resolveRotation(beastSpawn));
                    Teleport teleport = Teleport.createForPlayer(world, transform);
                    store.addComponent(beast.getReference(), Teleport.getComponentType(), teleport);
                });
            }
        }
    }

    private Vector3f resolveRotation(Location location) {
        if (location.getRotation() != null) {
            return location.getRotation();
        }
        return new Vector3f(0.0f, 0.0f, 0.0f);
    }
}
