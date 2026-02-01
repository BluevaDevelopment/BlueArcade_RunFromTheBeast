package net.blueva.arcade.modules.runfromthebeast.support;

import net.blueva.arcade.api.config.ModuleConfigAPI;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

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
                    Material material = Material.valueOf(parts[0].toUpperCase());
                    int amount = Integer.parseInt(parts[1]);
                    int slot = parts.length >= 3 ? Integer.parseInt(parts[2]) : -1;

                    ItemStack item = new ItemStack(material, amount);

                    if (slot >= 0 && slot < 36) {
                        player.getInventory().setItem(slot, item);
                    } else {
                        player.getInventory().addItem(item);
                    }
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

        for (String effectString : effects) {
            try {
                String[] parts = effectString.split(":");
                if (parts.length >= 3) {
                    PotionEffectType effectType = PotionEffectType.getByName(parts[0].toUpperCase());
                    int duration = Integer.parseInt(parts[1]);
                    int amplifier = Integer.parseInt(parts[2]);

                    if (effectType != null) {
                        player.addPotionEffect(new PotionEffect(effectType, duration, amplifier, false, false));
                    }
                }
            } catch (Exception e) {
                // Ignore malformed effects
            }
        }
    }

    public void applyBeastEquipment(Player beast) {
        if (beast == null) {
            return;
        }

        boolean enabled = moduleConfig.getBoolean("game.beast_equipment.enabled", true);
        if (!enabled) {
            return;
        }

        applyEquipmentPiece(beast, EquipmentSlot.HEAD, moduleConfig.getString("game.beast_equipment.helmet"));
        applyEquipmentPiece(beast, EquipmentSlot.CHEST, moduleConfig.getString("game.beast_equipment.chestplate"));
        applyEquipmentPiece(beast, EquipmentSlot.LEGS, moduleConfig.getString("game.beast_equipment.leggings"));
        applyEquipmentPiece(beast, EquipmentSlot.FEET, moduleConfig.getString("game.beast_equipment.boots"));
        applyEquipmentPiece(beast, EquipmentSlot.HAND, moduleConfig.getString("game.beast_equipment.main_hand"));
    }

    private void applyEquipmentPiece(Player beast, EquipmentSlot slot, String materialName) {
        Material material = configHelper.getMaterialSafe(materialName, Material.AIR);
        if (material == Material.AIR) {
            return;
        }

        ItemStack item = new ItemStack(material);
        switch (slot) {
            case HEAD -> beast.getInventory().setHelmet(item);
            case CHEST -> beast.getInventory().setChestplate(item);
            case LEGS -> beast.getInventory().setLeggings(item);
            case FEET -> beast.getInventory().setBoots(item);
            case HAND -> beast.getInventory().setItemInMainHand(item);
            case OFF_HAND -> beast.getInventory().setItemInOffHand(item);
        }
    }
}
