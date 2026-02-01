package net.blueva.arcade.modules.runfromthebeast.state;

import net.blueva.arcade.api.game.GameContext;
import com.hypixel.hytale.math.vector.Location;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.meta.BlockState;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class RunFromTheBeastStateRegistry {

    private final Map<Integer, GameContext<Player, Location, World, String, ItemStack, String, BlockState, Entity>> activeGames = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> playerArenas = new ConcurrentHashMap<>();
    private final Map<Integer, RunFromTheBeastArenaState> gameStates = new ConcurrentHashMap<>();

    public void registerGame(GameContext<Player, Location, World, String, ItemStack, String, BlockState, Entity> context,
                             RunFromTheBeastArenaState state) {
        int arenaId = context.getArenaId();
        activeGames.put(arenaId, context);
        gameStates.put(arenaId, state);
        for (Player player : context.getPlayers()) {
            playerArenas.put(player.getUuid(), arenaId);
        }
    }

    public GameContext<Player, Location, World, String, ItemStack, String, BlockState, Entity> getGameContext(Player player) {
        if (player == null) {
            return null;
        }
        Integer arenaId = playerArenas.get(player.getUuid());
        if (arenaId == null) {
            return null;
        }
        return activeGames.get(arenaId);
    }

    public RunFromTheBeastArenaState getState(int arenaId) {
        return gameStates.get(arenaId);
    }

    public void removeGame(int arenaId) {
        activeGames.remove(arenaId);
        gameStates.remove(arenaId);
        playerArenas.entrySet().removeIf(entry -> entry.getValue().equals(arenaId));
    }

    public Collection<GameContext<Player, Location, World, String, ItemStack, String, BlockState, Entity>> getActiveGames() {
        return new ArrayList<>(activeGames.values());
    }

    public void clear() {
        activeGames.clear();
        playerArenas.clear();
        gameStates.clear();
    }
}
