package net.blueva.arcade.modules.runfromthebeast.support;

import net.blueva.arcade.api.config.ModuleConfigAPI;
import net.blueva.arcade.api.module.ModuleInfo;
import net.blueva.arcade.api.store.StoreAPI;
import net.blueva.arcade.api.store.StoreCategoryDefinition;
import net.blueva.arcade.api.store.StoreCategoryType;
import net.blueva.arcade.api.store.StoreItemDefinition;
import net.blueva.arcade.api.store.StoreScope;

import java.util.ArrayList;
import java.util.List;

public class RunFromTheBeastStoreService {

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
        for (String key : categories) {
            registerCategoryFromStore(key);
        }
    }

    private void registerCategoryFromStore(String key) {
        if (storeAPI == null) {
            return;
        }

        String base = "category_settings." + key;

        String id = moduleConfig.getStringFrom("store.yml", base + ".id");
        String name = moduleConfig.getStringFrom("store.yml", base + ".name");
        String icon = configHelper.getItemIdSafe(moduleConfig.getStringFrom("store.yml", base + ".icon"), "Container_Chest");
        List<String> description = moduleConfig.getStringListFrom("store.yml", base + ".description");
        boolean enabled = moduleConfig.getBooleanFrom("store.yml", base + ".enabled", true);
        boolean selectionEnabled = moduleConfig.getBooleanFrom("store.yml", base + ".selection_enabled", true);
        int sortOrder = moduleConfig.getIntFrom("store.yml", base + ".sort_order", 0);
        String parentId = moduleConfig.getStringFrom("store.yml", base + ".parent_id", null);
        StoreCategoryType type = parseCategoryType(moduleConfig.getStringFrom("store.yml", base + ".type", "SELECTION"));
        boolean randomSelectionEnabled = moduleConfig.getBooleanFrom("store.yml", base + ".random_selection_enabled", false);
        String randomDisplayName = moduleConfig.getStringFrom("store.yml", base + ".random_item.display_name", "Random");
        String randomIcon = configHelper.getItemIdSafe(moduleConfig.getStringFrom("store.yml", base + ".random_item.icon"), "Container_Chest");
        List<String> randomDescription = moduleConfig.getStringListFrom("store.yml", base + ".random_item.description");

        List<String> itemsOrder = moduleConfig.getStringListFrom("store.yml", base + ".items.order");
        List<StoreItemDefinition<String>> items = new ArrayList<>();
        for (String itemId : itemsOrder == null ? List.<String>of() : itemsOrder) {
            String itemPath = base + ".items." + itemId;
            String displayName = moduleConfig.getStringFrom("store.yml", itemPath + ".name", itemId);
            String itemIcon = configHelper.getItemIdSafe(moduleConfig.getStringFrom("store.yml", itemPath + ".icon"), icon);
            int price = moduleConfig.getIntFrom("store.yml", itemPath + ".price", 0);
            boolean itemEnabled = moduleConfig.getBooleanFrom("store.yml", itemPath + ".enabled", true);
            boolean defaultUnlocked = moduleConfig.getBooleanFrom("store.yml", itemPath + ".default_unlocked", false);
            List<String> lore = moduleConfig.getStringListFrom("store.yml", itemPath + ".description");

            items.add(new StoreItemDefinition<>(itemId, displayName, itemIcon, lore != null ? lore : List.of(),
                    price, itemEnabled, defaultUnlocked));
        }

        StoreCategoryDefinition<String> category = new StoreCategoryDefinition<>(
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
            return StoreCategoryType.valueOf(raw.toUpperCase());
        } catch (Exception e) {
            return StoreCategoryType.SELECTION;
        }
    }
}
