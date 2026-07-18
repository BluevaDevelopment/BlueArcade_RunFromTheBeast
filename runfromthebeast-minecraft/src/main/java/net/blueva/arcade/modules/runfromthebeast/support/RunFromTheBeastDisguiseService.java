package net.blueva.arcade.modules.runfromthebeast.support;

import net.blueva.arcade.api.game.GameContext;
import net.blueva.arcade.modules.runfromthebeast.state.RunFromTheBeastArenaState;
import net.blueva.foundation.players.Players;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sheep;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class RunFromTheBeastDisguiseService {

    private final Plugin corePlugin;

    public RunFromTheBeastDisguiseService() {
        this.corePlugin = Bukkit.getPluginManager().getPlugin("BlueArcade3");
        if (corePlugin == null) {
            throw new IllegalStateException("BlueArcade3 plugin is not available for Beast disguises");
        }
    }

    public void apply(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                      RunFromTheBeastArenaState state,
                      Player beast,
                      String skinId) {
        remove(context, state, beast);
        EntityType type = resolveEntityType(skinId);
        if (type == null || beast == null || beast.getWorld() == null) {
            return;
        }

        Entity spawned = beast.getWorld().spawnEntity(beast.getLocation(), type);
        if (!(spawned instanceof LivingEntity disguise)) {
            spawned.remove();
            return;
        }

        configure(disguise, skinId);
        state.setBeastDisguiseId(disguise.getUniqueId());
        updateVisibility(context, state, beast, disguise);

        String taskId = taskId(context.getArenaId());
        context.getSchedulerAPI().runTimer(taskId, () -> {
            if (state.isEnded() || !beast.isOnline()) {
                remove(context, state, beast);
                context.getSchedulerAPI().cancelTask(taskId);
                return;
            }

            Entity current = getDisguise(state);
            if (!(current instanceof LivingEntity living) || !current.isValid()) {
                remove(context, state, beast);
                return;
            }

            living.setFireTicks(0);
            living.teleport(beast.getLocation());
            updateVisibility(context, state, beast, living);
        }, 0L, 1L);
    }

    public void remove(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                       RunFromTheBeastArenaState state,
                       Player beast) {
        if (state == null) {
            return;
        }

        if (context != null) {
            context.getSchedulerAPI().cancelTask(taskId(context.getArenaId()));
        }

        Entity disguise = getDisguise(state);
        if (disguise != null) {
            disguise.remove();
        }

        if (beast != null) {
            for (UUID viewerId : new HashSet<>(state.getBeastDisguiseViewers())) {
                Player viewer = Bukkit.getPlayer(viewerId);
                if (viewer != null) {
                    Players.showPlayer(viewer, corePlugin, beast);
                }
            }
        }

        state.getBeastDisguiseViewers().clear();
        state.setBeastDisguiseId(null);
    }

    public boolean isDisguise(RunFromTheBeastArenaState state, Entity entity) {
        return state != null && entity != null && entity.getUniqueId().equals(state.getBeastDisguiseId());
    }

    private void updateVisibility(
            GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
            RunFromTheBeastArenaState state,
            Player beast,
            Entity disguise) {
        Set<UUID> arenaPlayers = new HashSet<>();
        for (Player viewer : context.getPlayers()) {
            arenaPlayers.add(viewer.getUniqueId());
            if (viewer.getUniqueId().equals(beast.getUniqueId())) {
                viewer.hideEntity(corePlugin, disguise);
                continue;
            }
            if (!state.getBeastDisguiseViewers().contains(viewer.getUniqueId())) {
                Players.hidePlayer(viewer, corePlugin, beast);
                state.getBeastDisguiseViewers().add(viewer.getUniqueId());
            }
            viewer.showEntity(corePlugin, disguise);
        }

        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (!arenaPlayers.contains(viewer.getUniqueId())) {
                viewer.hideEntity(corePlugin, disguise);
            }
        }
    }

    private void configure(LivingEntity disguise, String skinId) {
        disguise.setAI(false);
        disguise.setSilent(true);
        disguise.setGravity(false);
        disguise.setCollidable(false);
        disguise.setPersistent(false);
        disguise.setRemoveWhenFarAway(false);
        if (disguise instanceof Ageable ageable) {
            ageable.setAdult();
            ageable.setAgeLock(true);
        }
        if (disguise instanceof Sheep sheep && "jeb".equalsIgnoreCase(skinId)) {
            sheep.setCustomName("jeb_");
            sheep.setCustomNameVisible(false);
        }
    }

    private Entity getDisguise(RunFromTheBeastArenaState state) {
        return state == null || state.getBeastDisguiseId() == null
                ? null
                : Bukkit.getEntity(state.getBeastDisguiseId());
    }

    private EntityType resolveEntityType(String skinId) {
        if (skinId == null) {
            return null;
        }
        return switch (skinId.toLowerCase(Locale.ROOT)) {
            case "creeper" -> EntityType.CREEPER;
            case "villager" -> EntityType.VILLAGER;
            case "jeb" -> EntityType.SHEEP;
            default -> null;
        };
    }

    private String taskId(int arenaId) {
        return "arena_" + arenaId + "_rftb_disguise";
    }
}
