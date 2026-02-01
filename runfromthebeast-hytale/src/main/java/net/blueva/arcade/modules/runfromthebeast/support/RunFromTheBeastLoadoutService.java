package net.blueva.arcade.modules.runfromthebeast.support;

import net.blueva.arcade.api.config.ModuleConfigAPI;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;

import java.util.List;

public class RunFromTheBeastLoadoutService {

    private final ModuleConfigAPI moduleConfig;
    private final RunFromTheBeastConfigHelper configHelper;

    public RunFromTheBeastLoadoutService(ModuleConfigAPI moduleConfig, RunFromTheBeastConfigHelper configHelper) {
        this.moduleConfig = moduleConfig;
        this.configHelper = configHelper;
    }

    public void giveItems(Player player, String path) {
        List<String> startingItems = moduleConfig.getStringList(path);

        if (startingItems == null || startingItems.isEmpty()) {
            return;
        }

        for (String itemString : startingItems) {
            try {
                String[] parts = itemString.split(":");
                if (parts.length >= 2) {
                    String itemId = parts[0];
                    int amount = Integer.parseInt(parts[1]);
                    int slot = parts.length >= 3 ? Integer.parseInt(parts[2]) : -1;

                    ItemStack item = new ItemStack(itemId, amount);
                    addItem(player, item, slot);
                }
            } catch (Exception e) {
                // Ignore malformed items
            }
        }
    }

    public void applyStartingEffects(Player player, String path) {
        List<String> effects = moduleConfig.getStringList(path);

        if (effects == null || effects.isEmpty()) {
            return;
        }
        // Status effects are not yet implemented for Hytale.
    }

    public void applyBeastEquipment(Player beast) {
        if (beast == null) {
            return;
        }

        boolean enabled = moduleConfig.getBoolean("game.beast_equipment.enabled", true);
        if (!enabled) {
            return;
        }

        applyArmorPiece(beast, moduleConfig.getString("game.beast_equipment.helmet"));
        applyArmorPiece(beast, moduleConfig.getString("game.beast_equipment.chestplate"));
        applyArmorPiece(beast, moduleConfig.getString("game.beast_equipment.leggings"));
        applyArmorPiece(beast, moduleConfig.getString("game.beast_equipment.boots"));
        applyMainHandItem(beast, moduleConfig.getString("game.beast_equipment.main_hand"));
    }

    public void clearInventory(Player player) {
        if (player == null || player.getInventory() == null) {
            return;
        }
        Inventory inventory = player.getInventory();
        clearContainer(inventory.getArmor());
        clearContainer(inventory.getHotbar());
        clearContainer(inventory.getStorage());
        clearContainer(inventory.getUtility());
        clearContainer(inventory.getTools());
        clearContainer(inventory.getBackpack());
    }

    private void applyArmorPiece(Player beast, String itemId) {
        String resolved = configHelper.getItemIdSafe(itemId, "");
        if (resolved.isBlank()) {
            return;
        }
        Inventory inventory = beast.getInventory();
        if (inventory == null) {
            return;
        }
        ItemContainer armor = inventory.getArmor();
        if (armor == null) {
            return;
        }
        armor.addItemStack(createArmorStack(resolved));
    }

    private void applyMainHandItem(Player beast, String itemId) {
        String resolved = configHelper.getItemIdSafe(itemId, "");
        if (resolved.isBlank()) {
            return;
        }
        addItem(beast, new ItemStack(resolved, 1), 0);
    }

    private ItemStack createArmorStack(String itemId) {
        return new ItemStack(itemId, 1, 100.0, 100.0, null);
    }

    private void addItem(Player player, ItemStack item, int slot) {
        if (player == null || item == null || player.getInventory() == null) {
            return;
        }

        Inventory inventory = player.getInventory();
        if (slot >= 0) {
            ItemContainer hotbar = inventory.getHotbar();
            if (hotbar != null && slot < hotbar.getCapacity()) {
                hotbar.addItemStackToSlot((short) slot, item);
                return;
            }
        }

        ItemContainer storage = inventory.getStorage();
        if (storage != null) {
            storage.addItemStack(item);
        }
    }

    private void clearContainer(ItemContainer container) {
        if (container != null) {
            container.clear();
        }
    }
}
