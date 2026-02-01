package net.blueva.arcade.modules.runfromthebeast.listener;

import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.math.vector.Location;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.meta.BlockState;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import net.blueva.arcade.api.game.GameContext;
import net.blueva.arcade.api.game.GamePhase;
import net.blueva.arcade.modules.runfromthebeast.game.RunFromTheBeastGameManager;
import net.blueva.arcade.modules.runfromthebeast.state.RunFromTheBeastArenaState;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class RunFromTheBeastDamageListener extends EntityEventSystem<EntityStore, Damage> {

    private final RunFromTheBeastGameManager gameManager;

    public RunFromTheBeastDamageListener(RunFromTheBeastGameManager gameManager) {
        super(Damage.class);
        this.gameManager = gameManager;
    }

    @Override
    public void handle(int index,
                       @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
                       @Nonnull Store<EntityStore> store,
                       @Nonnull CommandBuffer<EntityStore> commandBuffer,
                       @Nonnull Damage damage) {
        Ref<EntityStore> victimRef = archetypeChunk.getReferenceTo(index);
        if (victimRef == null) {
            return;
        }

        PlayerRef victimRefComponent = store.getComponent(victimRef, PlayerRef.getComponentType());
        Player victimPlayer = store.getComponent(victimRef, Player.getComponentType());
        if (victimRefComponent == null || victimPlayer == null) {
            return;
        }

        GameContext<Player, Location, World, String, ItemStack, String, BlockState, Entity> context =
                gameManager.getGameContext(victimPlayer);
        if (context == null || !context.isPlayerPlaying(victimPlayer)) {
            return;
        }

        if (context.getPhase() != GamePhase.PLAYING) {
            damage.setCancelled(true);
            return;
        }

        Player attacker = resolveAttacker(damage);
        if (attacker == null) {
            return;
        }

        if (!context.isPlayerPlaying(attacker)) {
            damage.setCancelled(true);
            return;
        }

        RunFromTheBeastArenaState state = gameManager.getArenaState(context);
        boolean victimIsBeast = gameManager.isBeast(context, state, victimPlayer);
        boolean attackerIsBeast = gameManager.isBeast(context, state, attacker);
        if (victimIsBeast == attackerIsBeast) {
            damage.setCancelled(true);
            return;
        }
        damage.setCancelled(false);
    }

    @Nullable
    private Player resolveAttacker(@Nonnull Damage damage) {
        Damage.Source source = damage.getSource();
        if (!(source instanceof Damage.EntitySource entitySource)) {
            return null;
        }

        Ref<EntityStore> attackerRef = entitySource.getRef();
        if (attackerRef == null || !attackerRef.isValid()) {
            return null;
        }

        Store<EntityStore> attackerStore = attackerRef.getStore();
        if (attackerStore == null) {
            return null;
        }

        return attackerStore.getComponent(attackerRef, Player.getComponentType());
    }

    @Nullable
    @Override
    public Query<EntityStore> getQuery() {
        return Archetype.empty();
    }
}
