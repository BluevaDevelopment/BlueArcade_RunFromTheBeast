package net.blueva.arcade.modules.runfromthebeast.state;

import net.blueva.arcade.api.game.GameContext;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RunFromTheBeastStateRegistry {

    private final Map<Integer, GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity>> activeGames = new ConcurrentHashMap<>();
    private final Map<Player, Integer> playerArenas = new ConcurrentHashMap<>();
    private final Map<Integer, RunFromTheBeastArenaState> gameStates = new ConcurrentHashMap<>();

    public void registerGame(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                             RunFromTheBeastArenaState state) {
        int arenaId = context.getArenaId();
        activeGames.put(arenaId, context);
        gameStates.put(arenaId, state);
        for (Player player : context.getPlayers()) {
            playerArenas.put(player, arenaId);
        }
    }

    public GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> getGameContext(Player player) {
        Integer arenaId = playerArenas.get(player);
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

    public Collection<GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity>> getActiveGames() {
        return new ArrayList<>(activeGames.values());
    }

    public void clear() {
        activeGames.clear();
        playerArenas.clear();
        gameStates.clear();
    }
}
