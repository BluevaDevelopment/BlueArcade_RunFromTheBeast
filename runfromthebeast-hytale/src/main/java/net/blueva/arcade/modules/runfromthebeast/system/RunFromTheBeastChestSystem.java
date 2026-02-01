package net.blueva.arcade.modules.runfromthebeast.system;

import net.blueva.arcade.api.game.GameContext;
import net.blueva.arcade.modules.runfromthebeast.RunFromTheBeastModule;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.RootDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.math.vector.Location;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.UseBlockEvent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.meta.BlockState;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;

/**
 * System that intercepts chest interactions in Run From The Beast.
 * Uses UseBlockEvent.Pre to cancel the native chest opening and handle it ourselves.
 */
public class RunFromTheBeastChestSystem extends EntityEventSystem<EntityStore, UseBlockEvent.Pre> {

    private final RunFromTheBeastModule module;

    public RunFromTheBeastChestSystem(RunFromTheBeastModule module) {
        super(UseBlockEvent.Pre.class);
        this.module = module;
    }

    @Override
    protected boolean shouldProcessEvent(@Nonnull UseBlockEvent.Pre event) {
        return true;
    }

    @Override
    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
                       @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer,
                       @Nonnull UseBlockEvent.Pre event) {

        Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);
        Player player = store.getComponent(ref, Player.getComponentType());
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());

        if (player == null || playerRef == null) {
            return;
        }

        GameContext<Player, Location, World, String, ItemStack, String, BlockState, Entity> context =
                module.getGameContext(player);
        if (context == null || !context.isPlayerPlaying(player)) {
            return;
        }

        Vector3i targetBlock = event.getTargetBlock();
        if (targetBlock == null) {
            return;
        }

        World world = player.getWorld();
        if (world == null) {
            return;
        }

        // Check if this is a chest
        String blockId = world.getBlockType(targetBlock.getX(), targetBlock.getY(), targetBlock.getZ()).getId();
        if (blockId == null || blockId.isBlank()) {
            return;
        }

        String normalized = blockId.toLowerCase(Locale.ROOT);
        if (!normalized.contains("chest")) {
            return;
        }

        // Cancel the native chest interaction
        event.setCancelled(true);

        // Open our custom armory window
        module.openArmory(player, context, targetBlock);
    }

    @Nullable
    @Override
    public Query<EntityStore> getQuery() {
        return PlayerRef.getComponentType();
    }

    @Override
    @Nonnull
    public Set<Dependency<EntityStore>> getDependencies() {
        // High priority to run before other systems
        return Collections.singleton(new RootDependency<>(Integer.MIN_VALUE));
    }
}
