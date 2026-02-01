package net.blueva.arcade.modules.runfromthebeast.listener;

import net.blueva.arcade.api.game.GameContext;
import net.blueva.arcade.api.game.GamePhase;
import net.blueva.arcade.modules.runfromthebeast.RunFromTheBeastModule;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;

public class RunFromTheBeastListener implements Listener {

    private final RunFromTheBeastModule module;

    public RunFromTheBeastListener(RunFromTheBeastModule module) {
        this.module = module;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location from = event.getFrom();
        Location to = event.getTo();

        GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context = module.getGameContext(player);
        if (context == null || to == null) {
            return;
        }

        if (!context.isPlayerPlaying(player)) {
            return;
        }

        if (from.getBlockX() == to.getBlockX() &&
                from.getBlockY() == to.getBlockY() &&
                from.getBlockZ() == to.getBlockZ()) {
            return;
        }

        if (context.getPhase() == GamePhase.COUNTDOWN) {
            player.teleport(from);
            return;
        }

        if (!context.isInsideBounds(to)) {
            module.handleOutOfBounds(context, player, false);
            return;
        }

        Material deathBlock = getDeathBlock(context);
        Material below = to.clone().subtract(0, 1, 0).getBlock().getType();
        if (below == deathBlock) {
            module.handleOutOfBounds(context, player, true);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context = module.getGameContext(player);
        if (context == null) {
            return;
        }

        if (!context.isPlayerPlaying(player)) {
            return;
        }

        if (event.getItem() != null) {
            Material type = event.getItem().getType();

            if (type == module.getCheckpointSetMaterial()) {
                event.setCancelled(true);
                module.handleCheckpointSet(context, player);
                return;
            }

            if (type == module.getCheckpointReturnMaterial()) {
                event.setCancelled(true);
                module.handleCheckpointReturn(context, player);
                return;
            }
        }

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null) {
            org.bukkit.block.BlockState state = event.getClickedBlock().getState();
            if (state instanceof org.bukkit.block.Container container) {
                event.setCancelled(true);
                module.openArmory(player, context, container);
            }
        }
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context = module.getGameContext(player);
        if (context == null || !context.isPlayerPlaying(player)) {
            return;
        }

        if (event instanceof EntityDamageByEntityEvent damageByEntityEvent) {
            Player damagerPlayer = resolveDamager(damageByEntityEvent);
            if (damagerPlayer != null && context.isPlayerPlaying(damagerPlayer)) {
                boolean victimIsBeast = module.isBeast(context, player);
                boolean damagerIsBeast = module.isBeast(context, damagerPlayer);
                if (!victimIsBeast && !damagerIsBeast) {
                    event.setCancelled(true);
                    return;
                }
            }
        }

        if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            event.setCancelled(true);
            return;
        }

        if (event.getCause() == EntityDamageEvent.DamageCause.VOID) {
            event.setCancelled(true);
            module.handleOutOfBounds(context, player, false);
            return;
        }

        if (event.getFinalDamage() >= player.getHealth()) {
            event.setCancelled(true);
            Player killer = null;
            if (event instanceof EntityDamageByEntityEvent damageByEntityEvent) {
                if (damageByEntityEvent.getDamager() instanceof Player) {
                    killer = (Player) damageByEntityEvent.getDamager();
                } else if (damageByEntityEvent.getDamager() instanceof Projectile projectile
                        && projectile.getShooter() instanceof Player shooter) {
                    killer = shooter;
                }
            }
            module.handleDamage(context, player, killer);
        }
    }

    private Material getDeathBlock(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context) {
        try {
            String deathBlockName = context.getDataAccess().getGameData("basic.death_block", String.class);
            if (deathBlockName != null) {
                return Material.valueOf(deathBlockName.toUpperCase());
            }
        } catch (Exception e) {
            // ignore
        }
        return Material.BARRIER;
    }

    private Player resolveDamager(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) {
            return player;
        }
        if (event.getDamager() instanceof Projectile projectile && projectile.getShooter() instanceof Player shooter) {
            return shooter;
        }
        return null;
    }
}
