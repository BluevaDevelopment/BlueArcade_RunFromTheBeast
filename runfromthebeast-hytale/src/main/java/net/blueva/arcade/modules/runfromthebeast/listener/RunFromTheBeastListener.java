package net.blueva.arcade.modules.runfromthebeast.listener;

import net.blueva.arcade.api.events.CustomEventRegistry;
import net.blueva.arcade.api.events.EventSubscription;
import net.blueva.arcade.api.game.GameContext;
import net.blueva.arcade.modules.runfromthebeast.RunFromTheBeastModule;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.PlayerInteractEvent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.meta.BlockState;
import com.hypixel.hytale.math.vector.Location;

public class RunFromTheBeastListener {

    private final RunFromTheBeastModule module;

    public RunFromTheBeastListener(RunFromTheBeastModule module) {
        this.module = module;
    }

    public void register(CustomEventRegistry<EventSubscription<?>, Short> registry) {
        registry.register(new EventSubscription<>(PlayerInteractEvent.class, this::handlePlayerInteract));
    }

    private void handlePlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (player == null) {
            return;
        }

        GameContext<Player, Location, World, String, ItemStack, String, BlockState, Entity> context =
                module.getGameContext(player);
        if (context == null || !context.isPlayerPlaying(player)) {
            return;
        }

        if (event.getActionType() != InteractionType.Primary && event.getActionType() != InteractionType.Secondary) {
            return;
        }

        ItemStack item = event.getItemInHand();
        if (item != null && !item.isEmpty()) {
            String itemId = item.getItemId();
            if (itemId != null && !itemId.isBlank()) {
                if (itemId.equalsIgnoreCase(module.getCheckpointSetItemId())) {
                    event.setCancelled(true);
                    module.handleCheckpointSet(context, player);
                    return;
                }

                if (itemId.equalsIgnoreCase(module.getCheckpointReturnItemId())) {
                    event.setCancelled(true);
                    module.handleCheckpointReturn(context, player);
                    return;
                }
            }
        }
        // Chest handling is done by RunFromTheBeastChestSystem using UseBlockEvent.Pre
    }
}
