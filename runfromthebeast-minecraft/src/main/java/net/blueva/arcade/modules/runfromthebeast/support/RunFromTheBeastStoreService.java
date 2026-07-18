package net.blueva.arcade.modules.runfromthebeast.support;

import net.blueva.arcade.api.config.ModuleConfigAPI;
import net.blueva.arcade.api.module.ModuleInfo;
import net.blueva.arcade.api.store.StoreAPI;
import net.blueva.arcade.api.store.StoreCategoryDefinition;
import net.blueva.arcade.api.store.StoreCategoryType;
import net.blueva.arcade.api.store.StoreItemDefinition;
import net.blueva.arcade.api.store.StoreScope;
import net.blueva.arcade.modules.runfromthebeast.state.RunFromTheBeastArenaState;
import net.blueva.foundation.items.Items;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RunFromTheBeastStoreService {

    private static final String ITEM_MARKER_PREFIX = "bluearcade:rftb/";

    private final ModuleConfigAPI moduleConfig;
    private final StoreAPI storeAPI;
    private final ModuleInfo moduleInfo;
    private final RunFromTheBeastConfigHelper configHelper;

    public RunFromTheBeastStoreService(ModuleConfigAPI moduleConfig, StoreAPI storeAPI, ModuleInfo moduleInfo,
                                       RunFromTheBeastConfigHelper configHelper) {
        this.moduleConfig = moduleConfig;
        this.storeAPI = storeAPI;
        this.moduleInfo = moduleInfo;
        this.configHelper = configHelper;
    }

    public void registerStoreItems() {
        if (storeAPI == null) {
            return;
        }

        List<String> categories = moduleConfig.getStringListFrom("store.yml", "categories");
        for (String key : categories == null ? List.<String>of() : categories) {
            registerCategoryFromStore(key);
        }
    }

    public String applyBeastSelections(Player beast) {
        if (beast == null || storeAPI == null) {
            return null;
        }

        applySelectedZapper(beast);
        giveSelectedKit(beast);
        return resolveSelected(beast, "beast_skins");
    }

    public boolean isShredderPlate(ItemStack item) {
        return hasMarker(item, "shredder");
    }

    public boolean isResetWand(ItemStack item) {
        return hasMarker(item, "reset_wand");
    }

    public void registerShredderTrap(RunFromTheBeastArenaState state, Block block, Material replacedMaterial) {
        if (state == null || block == null) {
            return;
        }
        Location location = block.getLocation();
        state.getShredderTraps().put(blockKey(location),
                new RunFromTheBeastArenaState.ShredderTrap(location, replacedMaterial));
    }

    public boolean triggerShredderTrap(RunFromTheBeastArenaState state, Player player, Location location) {
        if (state == null || player == null || location == null) {
            return false;
        }

        RunFromTheBeastArenaState.ShredderTrap trap = findTrap(state, location);
        if (trap == null || !trap.isArmed()) {
            return false;
        }

        trap.setArmed(false);
        int duration = Math.max(1, moduleConfig.getIntFrom(
                "store.yml", "gameplay.shredder.slow_duration_ticks", 80));
        int amplifier = Math.max(0, moduleConfig.getIntFrom(
                "store.yml", "gameplay.shredder.slow_amplifier", 1));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, duration, amplifier, false, true));
        return true;
    }

    public int resetNearbyTraps(RunFromTheBeastArenaState state, Location center) {
        if (state == null || center == null || center.getWorld() == null) {
            return 0;
        }

        double radius = Math.max(1.0, moduleConfig.getDoubleFrom(
                "store.yml", "gameplay.reset_wand.radius", 12.0));
        double radiusSquared = radius * radius;
        int reset = 0;
        for (RunFromTheBeastArenaState.ShredderTrap trap : state.getShredderTraps().values()) {
            Location trapLocation = trap.getLocation();
            if (trapLocation.getWorld() == null || !trapLocation.getWorld().equals(center.getWorld())) {
                continue;
            }
            if (!trap.isArmed() && trapLocation.distanceSquared(center) <= radiusSquared) {
                trap.setArmed(true);
                reset++;
            }
        }
        return reset;
    }

    public void restoreShredderTraps(RunFromTheBeastArenaState state) {
        if (state == null) {
            return;
        }
        for (RunFromTheBeastArenaState.ShredderTrap trap : state.getShredderTraps().values()) {
            Location location = trap.getLocation();
            if (location.getWorld() != null) {
                location.getBlock().setType(trap.getReplacedMaterial(), false);
            }
        }
        state.getShredderTraps().clear();
    }

    private void registerCategoryFromStore(String key) {
        if (storeAPI == null) {
            return;
        }

        String base = "category_settings." + key;

        String id = moduleConfig.getStringFrom("store.yml", base + ".id");
        String name = moduleConfig.getStringFrom("store.yml", base + ".name");
        Material icon = configHelper.getMaterialSafe(moduleConfig.getStringFrom("store.yml", base + ".icon"), Material.CHEST);
        List<String> description = moduleConfig.getStringListFrom("store.yml", base + ".description");
        boolean enabled = moduleConfig.getBooleanFrom("store.yml", base + ".enabled", true);
        boolean selectionEnabled = moduleConfig.getBooleanFrom("store.yml", base + ".selection_enabled", true);
        int sortOrder = moduleConfig.getIntFrom("store.yml", base + ".sort_order", 0);
        String parentId = moduleConfig.getStringFrom("store.yml", base + ".parent_id", null);
        StoreCategoryType type = parseCategoryType(moduleConfig.getStringFrom("store.yml", base + ".type", "SELECTION"));
        boolean randomSelectionEnabled = moduleConfig.getBooleanFrom("store.yml", base + ".random_selection_enabled", false);
        String randomDisplayName = moduleConfig.getStringFrom("store.yml", base + ".random_item.display_name", "Random");
        Material randomIcon = configHelper.getMaterialSafe(moduleConfig.getStringFrom("store.yml", base + ".random_item.icon"), Material.ENDER_CHEST);
        List<String> randomDescription = moduleConfig.getStringListFrom("store.yml", base + ".random_item.description");

        List<String> itemsOrder = moduleConfig.getStringListFrom("store.yml", base + ".items.order");
        List<StoreItemDefinition<Material>> items = new ArrayList<>();
        for (String itemId : itemsOrder == null ? List.<String>of() : itemsOrder) {
            String itemPath = base + ".items." + itemId;
            String displayName = moduleConfig.getStringFrom("store.yml", itemPath + ".name", itemId);
            Material itemIcon = configHelper.getMaterialSafe(moduleConfig.getStringFrom("store.yml", itemPath + ".icon"), icon);
            int price = moduleConfig.getIntFrom("store.yml", itemPath + ".price", 0);
            boolean itemEnabled = moduleConfig.getBooleanFrom("store.yml", itemPath + ".enabled", true);
            boolean defaultUnlocked = moduleConfig.getBooleanFrom("store.yml", itemPath + ".default_unlocked", false);
            List<String> lore = moduleConfig.getStringListFrom("store.yml", itemPath + ".description");

            items.add(new StoreItemDefinition<>(itemId, displayName, itemIcon, lore != null ? lore : List.of(),
                    price, itemEnabled, defaultUnlocked));
        }

        StoreCategoryDefinition<Material> category = new StoreCategoryDefinition<>(
                id != null ? id : key,
                name != null ? name : key,
                icon,
                description != null ? description : List.of(),
                StoreScope.MODULE,
                parentId,
                type,
                moduleInfo.getId(),
                enabled,
                sortOrder,
                selectionEnabled,
                randomSelectionEnabled,
                randomDisplayName,
                randomIcon,
                randomDescription != null ? randomDescription : List.of()
        );

        storeAPI.registerCategory(category, items);
    }

    private StoreCategoryType parseCategoryType(String raw) {
        try {
            return StoreCategoryType.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            return StoreCategoryType.SELECTION;
        }
    }

    private void applySelectedZapper(Player beast) {
        String selected = resolveSelected(beast, "zappers");
        if (selected == null) {
            return;
        }

        Material fallback = switch (selected.toLowerCase(Locale.ROOT)) {
            case "stone" -> Material.STONE_HOE;
            case "iron" -> Material.IRON_HOE;
            case "diamond" -> Material.DIAMOND_HOE;
            default -> Material.WOODEN_HOE;
        };
        ItemStack zapper = new ItemStack(getStoreItemMaterial("zappers", selected, "gameplay.material", fallback));
        int defaultSharpness = switch (selected.toLowerCase(Locale.ROOT)) {
            case "stone" -> 1;
            case "iron" -> 2;
            case "diamond" -> 4;
            default -> 0;
        };
        int defaultUnbreaking = switch (selected.toLowerCase(Locale.ROOT)) {
            case "stone" -> 1;
            case "iron" -> 2;
            case "diamond" -> 3;
            default -> 0;
        };
        String base = itemPath("zappers", selected) + ".gameplay";
        Items.enchant(zapper, "SHARPNESS",
                moduleConfig.getIntFrom("store.yml", base + ".sharpness", defaultSharpness));
        Items.enchant(zapper, "UNBREAKING",
                moduleConfig.getIntFrom("store.yml", base + ".unbreaking", defaultUnbreaking));
        beast.getInventory().setItemInMainHand(zapper);
    }

    private void giveSelectedKit(Player beast) {
        String selected = resolveSelected(beast, "kits");
        if (selected == null) {
            return;
        }

        switch (selected.toLowerCase(Locale.ROOT)) {
            case "shredder" -> beast.getInventory().addItem(createMarkedItem(
                    Material.LIGHT_WEIGHTED_PRESSURE_PLATE,
                    Math.max(1, moduleConfig.getIntFrom("store.yml", "gameplay.shredder.amount", 4)),
                    "shredder",
                    getItemDisplayName("kits", selected)));
            case "swiftness" -> beast.getInventory().addItem(createSwiftnessPotion(
                    getItemDisplayName("kits", selected)));
            case "reset_wand" -> beast.getInventory().addItem(createMarkedItem(
                    Material.REDSTONE_TORCH, 1, "reset_wand", getItemDisplayName("kits", selected)));
            default -> {
            }
        }
    }

    private ItemStack createSwiftnessPotion(String displayName) {
        ItemStack potion = new ItemStack(Material.POTION);
        if (potion.getItemMeta() instanceof PotionMeta meta) {
            int duration = Math.max(1, moduleConfig.getIntFrom(
                    "store.yml", "gameplay.swiftness.duration_ticks", 200));
            int amplifier = Math.max(0, moduleConfig.getIntFrom(
                    "store.yml", "gameplay.swiftness.amplifier", 1));
            meta.addCustomEffect(new PotionEffect(PotionEffectType.SPEED, duration, amplifier), true);
            if (displayName != null && !displayName.isBlank()) {
                meta.setDisplayName(displayName);
            }
            potion.setItemMeta(meta);
        }
        return potion;
    }

    private ItemStack createMarkedItem(Material material, int amount, String marker, String displayName) {
        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setLocalizedName(ITEM_MARKER_PREFIX + marker);
            if (displayName != null && !displayName.isBlank()) {
                meta.setDisplayName(displayName);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private boolean hasMarker(ItemStack item, String marker) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        return meta != null && (ITEM_MARKER_PREFIX + marker).equals(meta.getLocalizedName());
    }

    private String resolveSelected(Player player, String categoryKey) {
        String categoryId = moduleConfig.getStringFrom(
                "store.yml", "category_settings." + categoryKey + ".id", "rftb_" + categoryKey);
        return storeAPI.resolveSelected(player, categoryId);
    }

    private Material getStoreItemMaterial(String categoryKey, String itemId, String suffix, Material fallback) {
        return configHelper.getMaterialSafe(moduleConfig.getStringFrom(
                "store.yml", itemPath(categoryKey, itemId) + "." + suffix), fallback);
    }

    private String itemPath(String categoryKey, String itemId) {
        return "category_settings." + categoryKey + ".items." + itemId;
    }

    private String getItemDisplayName(String categoryKey, String itemId) {
        return moduleConfig.getStringFrom("store.yml", itemPath(categoryKey, itemId) + ".name", itemId);
    }

    private RunFromTheBeastArenaState.ShredderTrap findTrap(RunFromTheBeastArenaState state, Location location) {
        RunFromTheBeastArenaState.ShredderTrap trap = state.getShredderTraps().get(blockKey(location));
        if (trap != null) {
            return trap;
        }
        return state.getShredderTraps().get(blockKey(location.clone().subtract(0, 1, 0)));
    }

    private String blockKey(Location location) {
        String world = location.getWorld() != null ? location.getWorld().getUID().toString() : "";
        return world + ':' + location.getBlockX() + ':' + location.getBlockY() + ':' + location.getBlockZ();
    }
}
