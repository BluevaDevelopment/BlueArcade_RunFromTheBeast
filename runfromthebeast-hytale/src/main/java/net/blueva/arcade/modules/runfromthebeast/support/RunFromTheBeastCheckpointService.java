package net.blueva.arcade.modules.runfromthebeast.support;

import net.blueva.arcade.api.config.ModuleConfigAPI;
import net.blueva.arcade.api.game.GameContext;
import net.blueva.arcade.modules.runfromthebeast.state.RunFromTheBeastArenaState;
import com.hypixel.hytale.math.vector.Location;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.meta.BlockState;

import java.util.List;

public class RunFromTheBeastCheckpointService {

    private final ModuleConfigAPI moduleConfig;
    private final RunFromTheBeastConfigHelper configHelper;

    public RunFromTheBeastCheckpointService(ModuleConfigAPI moduleConfig, RunFromTheBeastConfigHelper configHelper) {
        this.moduleConfig = moduleConfig;
        this.configHelper = configHelper;
    }

    public void giveCheckpointItems(GameContext<Player, Location, World, String, ItemStack, String, BlockState, Entity> context,
                                    Player player,
                                    RunFromTheBeastArenaState state) {
        int defaultUses = moduleConfig.getInt("checkpoints.default_uses", 1);
        state.getCheckpointUses().put(player.getUuid(), defaultUses);

        String setter = moduleConfig.getString("checkpoints.items.set");
        String back = moduleConfig.getString("checkpoints.items.return");

        String setItemId = configHelper.getItemIdSafe(setter, "Ability_Checkpoint_Set");
        String returnItemId = configHelper.getItemIdSafe(back, "Ability_Checkpoint_Return");

        addItem(player, decorateCheckpointItem(context, setItemId,
                moduleConfig.getStringFrom("language.yml", "messages.checkpoint_items.set_name"),
                moduleConfig.getStringListFrom("language.yml", "messages.checkpoint_items.set_lore")));
        addItem(player, decorateCheckpointItem(context, returnItemId,
                moduleConfig.getStringFrom("language.yml", "messages.checkpoint_items.return_name"),
                moduleConfig.getStringListFrom("language.yml", "messages.checkpoint_items.return_lore")));
    }

    public String getCheckpointSetItemId() {
        return configHelper.getItemIdSafe(moduleConfig.getString("checkpoints.items.set"), "Ability_Checkpoint_Set");
    }

    public String getCheckpointReturnItemId() {
        return configHelper.getItemIdSafe(moduleConfig.getString("checkpoints.items.return"), "Ability_Checkpoint_Return");
    }

    public void handleCheckpointSet(GameContext<Player, Location, World, String, ItemStack, String, BlockState, Entity> context,
                                    Player player,
                                    RunFromTheBeastArenaState state) {
        int uses = state.getCheckpointUses().getOrDefault(player.getUuid(), 0);
        if (uses <= 0) {
            context.getMessagesAPI().sendRaw(player, moduleConfig.getStringFrom("language.yml", "messages.checkpoint.no_uses"));
            return;
        }

        Location location = resolvePlayerLocation(player);
        if (location == null) {
            context.getMessagesAPI().sendRaw(player, moduleConfig.getStringFrom("language.yml", "messages.checkpoint.none"));
            return;
        }

        state.getCheckpoints().put(player.getUuid(), location);
        state.getCheckpointUses().put(player.getUuid(), uses - 1);
        context.getMessagesAPI().sendRaw(player, moduleConfig.getStringFrom("language.yml", "messages.checkpoint.set")
                .replace("{uses}", String.valueOf(uses - 1)));
    }

    public void handleCheckpointReturn(GameContext<Player, Location, World, String, ItemStack, String, BlockState, Entity> context,
                                       Player player,
                                       RunFromTheBeastArenaState state) {
        Location checkpoint = state.getCheckpoints().get(player.getUuid());
        if (checkpoint == null) {
            context.getMessagesAPI().sendRaw(player, moduleConfig.getStringFrom("language.yml", "messages.checkpoint.none"));
            return;
        }

        context.getSchedulerAPI().runLater("checkpoint_return_" + player.getUuid(),
                () -> {
                    if (player.getTransformComponent() != null) {
                        player.getTransformComponent().teleportPosition(checkpoint.getPosition());
                        player.getTransformComponent().teleportRotation(checkpoint.getRotation());
                    }
                }, 0L);
        context.getMessagesAPI().sendRaw(player, moduleConfig.getStringFrom("language.yml", "messages.checkpoint.teleported"));
    }

    private ItemStack decorateCheckpointItem(GameContext<Player, Location, World, String, ItemStack, String, BlockState, Entity> context,
                                             String itemId,
                                             String name,
                                             List<String> lore) {
        ItemStack item = new ItemStack(itemId, 1);
        return context.getItemAPI().decorate(item, name, lore);
    }

    private void addItem(Player player, ItemStack item) {
        if (player == null || item == null || player.getInventory() == null) {
            return;
        }
        Inventory inventory = player.getInventory();
        ItemContainer storage = inventory.getStorage();
        if (storage != null) {
            storage.addItemStack(item);
        }
    }

    private Location resolvePlayerLocation(Player player) {
        if (player == null || player.getWorld() == null || player.getTransformComponent() == null) {
            return null;
        }
        Vector3d position = player.getTransformComponent().getPosition();
        Vector3f rotation = player.getTransformComponent().getRotation();
        return new Location(player.getWorld().getName(), position, rotation);
    }
}
