package net.blueva.arcade.modules.runfromthebeast.support;

import net.blueva.arcade.api.config.ModuleConfigAPI;
import net.blueva.arcade.api.game.GameContext;
import net.blueva.arcade.modules.runfromthebeast.state.RunFromTheBeastArenaState;
import com.hypixel.hytale.math.vector.Location;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.PageManager;
import com.hypixel.hytale.server.core.entity.entities.player.windows.ContainerWindow;
import com.hypixel.hytale.server.core.entity.entities.player.windows.Window;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.meta.BlockState;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.component.Store;
import org.bson.BsonDocument;

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
                           GameContext<Player, Location, World, String, ItemStack, String, BlockState, Entity> context,
                           RunFromTheBeastArenaState state,
                           Vector3i blockPosition) {
        boolean allowBeast = moduleConfig.getBoolean("game.allow_beast_loot", true);
        boolean blockedBeast = state != null && state.getBeastId() != null
                && player.getUuid().equals(state.getBeastId()) && !allowBeast;
        if (blockedBeast) {
            context.getMessagesAPI().sendRaw(player, moduleConfig.getStringFrom("language.yml", "messages.armory_beast_blocked"));
            return;
        }

        // Always use configured items - each player gets their own cloned copy
        List<ItemStack> items = getConfiguredItems();
        if (items.isEmpty()) {
            return;
        }

        short capacity = resolveMenuCapacity(items.size());
        ItemContainer menuContainer = new SimpleItemContainer(capacity);
        for (short i = 0; i < Math.min(items.size(), capacity); i++) {
            ItemStack stack = items.get(i);
            if (stack != null && !stack.isEmpty()) {
                menuContainer.setItemStackForSlot(i, cloneItem(stack));
            }
        }

        openContainerWindow(player, menuContainer);
    }

    private List<ItemStack> getConfiguredItems() {
        List<ItemStack> items = new ArrayList<>();
        for (String spec : moduleConfig.getStringList("armory.items")) {
            ItemStack item = configHelper.parseItem(spec);
            if (item != null && !item.isEmpty()) {
                items.add(item);
            }
        }
        return items;
    }

    private short resolveMenuCapacity(int itemCount) {
        int base = Math.max(9, itemCount);
        int rounded = ((base + 8) / 9) * 9;
        int bounded = Math.max(9, Math.min(54, rounded));
        return (short) bounded;
    }

    private void openContainerWindow(Player player, ItemContainer menuContainer) {
        if (player == null || menuContainer == null) {
            return;
        }
        PlayerRef ref = player.getPlayerRef();
        if (ref == null || ref.getReference() == null) {
            return;
        }
        PageManager pageManager = player.getPageManager();
        World world = player.getWorld();
        if (pageManager == null || world == null) {
            return;
        }
        Store<EntityStore> store = world.getEntityStore().getStore();
        ContainerWindow window = new ContainerWindow(menuContainer);

        pageManager.setPageWithWindows(ref.getReference(), store, Page.Bench, true,
                new Window[]{window});
    }

    private ItemStack cloneItem(ItemStack stack) {
        if (stack == null) {
            return null;
        }
        BsonDocument metadata = stack.getMetadata();
        BsonDocument cloned = metadata == null ? null : metadata.clone();
        return new ItemStack(stack.getItemId(), stack.getQuantity(), stack.getDurability(), stack.getMaxDurability(), cloned);
    }
}
