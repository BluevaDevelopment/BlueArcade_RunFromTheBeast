package net.blueva.arcade.modules.runfromthebeast.support;

import net.blueva.arcade.api.config.ModuleConfigAPI;
import net.blueva.arcade.api.game.GameContext;
import net.blueva.arcade.modules.runfromthebeast.state.RunFromTheBeastArenaState;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class RunFromTheBeastCheckpointService {

    private final ModuleConfigAPI moduleConfig;
    private final RunFromTheBeastConfigHelper configHelper;

    public RunFromTheBeastCheckpointService(ModuleConfigAPI moduleConfig, RunFromTheBeastConfigHelper configHelper) {
        this.moduleConfig = moduleConfig;
        this.configHelper = configHelper;
    }

    public void giveCheckpointItems(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                                    Player player,
                                    RunFromTheBeastArenaState state) {
        int defaultUses = moduleConfig.getInt("checkpoints.default_uses", 1);
        state.getCheckpointUses().put(player.getUniqueId(), defaultUses);

        String setter = moduleConfig.getString("checkpoints.items.set");
        String back = moduleConfig.getString("checkpoints.items.return");

        Material setMaterial = configHelper.getMaterialSafe(setter, Material.NETHER_STAR);
        Material returnMaterial = configHelper.getMaterialSafe(back, Material.ENDER_EYE);

        player.getInventory().addItem(decorateCheckpointItem(context, setMaterial,
                moduleConfig.getStringFrom("language.yml", "messages.checkpoint_items.set_name"),
                moduleConfig.getStringListFrom("language.yml", "messages.checkpoint_items.set_lore")));
        player.getInventory().addItem(decorateCheckpointItem(context, returnMaterial,
                moduleConfig.getStringFrom("language.yml", "messages.checkpoint_items.return_name"),
                moduleConfig.getStringListFrom("language.yml", "messages.checkpoint_items.return_lore")));
    }

    public Material getCheckpointSetMaterial() {
        return configHelper.getMaterialSafe(moduleConfig.getString("checkpoints.items.set"), Material.NETHER_STAR);
    }

    public Material getCheckpointReturnMaterial() {
        return configHelper.getMaterialSafe(moduleConfig.getString("checkpoints.items.return"), Material.ENDER_EYE);
    }

    public void handleCheckpointSet(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                                    Player player,
                                    RunFromTheBeastArenaState state) {
        int uses = state.getCheckpointUses().getOrDefault(player.getUniqueId(), 0);
        if (uses <= 0) {
            context.getMessagesAPI().sendRaw(player, moduleConfig.getStringFrom("language.yml", "messages.checkpoint.no_uses"));
            return;
        }

        state.getCheckpoints().put(player.getUniqueId(), player.getLocation());
        state.getCheckpointUses().put(player.getUniqueId(), uses - 1);
        context.getMessagesAPI().sendRaw(player, moduleConfig.getStringFrom("language.yml", "messages.checkpoint.set")
                .replace("{uses}", String.valueOf(uses - 1)));
    }

    public void handleCheckpointReturn(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                                       Player player,
                                       RunFromTheBeastArenaState state) {
        Location checkpoint = state.getCheckpoints().get(player.getUniqueId());
        if (checkpoint == null) {
            context.getMessagesAPI().sendRaw(player, moduleConfig.getStringFrom("language.yml", "messages.checkpoint.none"));
            return;
        }

        context.getSchedulerAPI().runLater("checkpoint_return_" + player.getUniqueId(),
                () -> player.teleport(checkpoint), 0L);
        context.getMessagesAPI().sendRaw(player, moduleConfig.getStringFrom("language.yml", "messages.checkpoint.teleported"));
    }

    private ItemStack decorateCheckpointItem(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                                             Material material,
                                             String name,
                                             List<String> lore) {
        ItemStack item = new ItemStack(material, 1);
        return context.getItemAPI().decorate(item, name, lore);
    }
}
