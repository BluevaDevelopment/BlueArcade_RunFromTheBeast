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

import java.util.ArrayList;
import java.util.List;

public class RunFromTheBeastArmoryService {

    private final ModuleConfigAPI moduleConfig;
    private final RunFromTheBeastConfigHelper configHelper;

    public RunFromTheBeastArmoryService(ModuleConfigAPI moduleConfig, RunFromTheBeastConfigHelper configHelper) {
        this.moduleConfig = moduleConfig;
        this.configHelper = configHelper;
    }

    public void openArmory(Player player,
                           GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                           RunFromTheBeastArenaState state,
                           org.bukkit.block.Container container) {
        boolean allowBeast = moduleConfig.getBoolean("game.allow_beast_loot", true);
        if (state != null && state.getBeastId() != null && player.getUniqueId().equals(state.getBeastId()) && !allowBeast) {
            context.getMessagesAPI().sendRaw(player, moduleConfig.getStringFrom("language.yml", "messages.armory_beast_blocked"));
            return;
        }

        List<ItemStack> items = new ArrayList<>();
        if (container != null) {
            for (ItemStack content : container.getInventory().getContents()) {
                if (content != null) {
                    items.add(content.clone());
                }
            }
        }

        if (items.isEmpty()) {
            for (String spec : moduleConfig.getStringList("armory.fallback_items")) {
                ItemStack item = configHelper.parseItem(spec);
                if (item != null) {
                    items.add(item);
                }
            }
        }

        int menuSize = 54;
        if (container != null) {
            int containerSize = container.getInventory().getSize();
            if (containerSize % 9 != 0) {
                containerSize = ((containerSize / 9) + 1) * 9;
            }
            menuSize = Math.max(9, Math.min(54, containerSize));
        }

        String title = context.getItemAPI().formatInventoryTitle(
                moduleConfig.getStringFrom("language.yml", "menus.armory.title"));
        org.bukkit.inventory.Inventory menu = player.getServer().createInventory(null, menuSize, title);

        for (int i = 0; i < Math.min(menu.getSize(), items.size()); i++) {
            menu.setItem(i, items.get(i));
        }

        player.openInventory(menu);
    }
}
