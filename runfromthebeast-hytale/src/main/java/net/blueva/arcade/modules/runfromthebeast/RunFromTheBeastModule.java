package net.blueva.arcade.modules.runfromthebeast;

import net.blueva.arcade.api.ModuleAPI;
import net.blueva.arcade.api.achievements.AchievementsAPI;
import net.blueva.arcade.api.config.CoreConfigAPI;
import net.blueva.arcade.api.config.ModuleConfigAPI;
import net.blueva.arcade.api.events.CustomEventRegistry;
import net.blueva.arcade.api.events.EventSubscription;
import net.blueva.arcade.api.game.GameContext;
import net.blueva.arcade.api.game.GameModule;
import net.blueva.arcade.api.game.GameResult;
import net.blueva.arcade.api.module.ModuleInfo;
import net.blueva.arcade.api.stats.StatsAPI;
import net.blueva.arcade.api.store.StoreAPI;
import net.blueva.arcade.api.ui.VoteMenuAPI;
import net.blueva.arcade.modules.runfromthebeast.game.RunFromTheBeastGameManager;
import net.blueva.arcade.modules.runfromthebeast.listener.RunFromTheBeastDamageListener;
import net.blueva.arcade.modules.runfromthebeast.listener.RunFromTheBeastItemDropListener;
import net.blueva.arcade.modules.runfromthebeast.listener.RunFromTheBeastDeathSystem;
import net.blueva.arcade.modules.runfromthebeast.listener.RunFromTheBeastListener;
import net.blueva.arcade.modules.runfromthebeast.setup.RunFromTheBeastSetup;
import net.blueva.arcade.modules.runfromthebeast.system.RunFromTheBeastChestSystem;
import net.blueva.arcade.modules.runfromthebeast.state.RunFromTheBeastArenaState;
import net.blueva.arcade.modules.runfromthebeast.state.RunFromTheBeastStateRegistry;
import net.blueva.arcade.modules.runfromthebeast.support.RunFromTheBeastArmoryService;
import net.blueva.arcade.modules.runfromthebeast.support.RunFromTheBeastBeastService;
import net.blueva.arcade.modules.runfromthebeast.support.RunFromTheBeastCageService;
import net.blueva.arcade.modules.runfromthebeast.support.RunFromTheBeastCheckpointService;
import net.blueva.arcade.modules.runfromthebeast.support.RunFromTheBeastConfigHelper;
import net.blueva.arcade.modules.runfromthebeast.support.RunFromTheBeastDistanceService;
import net.blueva.arcade.modules.runfromthebeast.support.RunFromTheBeastLoadoutService;
import net.blueva.arcade.modules.runfromthebeast.support.RunFromTheBeastMessagingService;
import net.blueva.arcade.modules.runfromthebeast.support.RunFromTheBeastRewardService;
import net.blueva.arcade.modules.runfromthebeast.support.RunFromTheBeastStatsService;
import net.blueva.arcade.modules.runfromthebeast.support.RunFromTheBeastStoreService;
import com.hypixel.hytale.math.vector.Location;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.component.Holder;

import java.util.HashMap;
import java.util.Map;

public class RunFromTheBeastModule implements GameModule<Player, Location, World, String, ItemStack, String, Holder, Entity, EventSubscription<?>, Short> {

    private static final String MODULE_ID = "run_from_the_beast";

    public ModuleConfigAPI moduleConfig;
    public CoreConfigAPI coreConfig;
    private ModuleInfo moduleInfo;

    private RunFromTheBeastStateRegistry stateRegistry;
    private RunFromTheBeastGameManager gameManager;
    private RunFromTheBeastMessagingService messagingService;
    private RunFromTheBeastCheckpointService checkpointService;
    private boolean systemsRegistered;

    @Override
    public void onLoad() {
        moduleInfo = ModuleAPI.getModuleInfo(MODULE_ID);

        if (moduleInfo == null) {
            throw new IllegalStateException("ModuleInfo not available for Run From The Beast module");
        }

        moduleConfig = ModuleAPI.getModuleConfig(moduleInfo.getId());
        coreConfig = ModuleAPI.getCoreConfig();
        StatsAPI<Player> statsAPI = ModuleAPI.getStatsAPI();
        StoreAPI storeAPI = ModuleAPI.getStoreAPI();
        VoteMenuAPI<String> voteMenu = ModuleAPI.getVoteMenuAPI();
        AchievementsAPI achievementsAPI = ModuleAPI.getAchievementsAPI();

        RunFromTheBeastConfigHelper configHelper = new RunFromTheBeastConfigHelper();
        RunFromTheBeastStatsService statsService = new RunFromTheBeastStatsService(statsAPI, moduleInfo);
        RunFromTheBeastStoreService storeService = new RunFromTheBeastStoreService(moduleConfig, storeAPI, moduleInfo, configHelper);
        RunFromTheBeastCageService cageService = new RunFromTheBeastCageService(configHelper);
        RunFromTheBeastBeastService beastService = new RunFromTheBeastBeastService(moduleConfig, storeAPI);
        RunFromTheBeastLoadoutService loadoutService = new RunFromTheBeastLoadoutService(moduleConfig, configHelper);
        RunFromTheBeastDistanceService distanceService = new RunFromTheBeastDistanceService(moduleConfig);
        RunFromTheBeastRewardService rewardService = new RunFromTheBeastRewardService(moduleConfig);
        RunFromTheBeastArmoryService armoryService = new RunFromTheBeastArmoryService(moduleConfig, configHelper);

        messagingService = new RunFromTheBeastMessagingService(moduleConfig, coreConfig, moduleInfo, configHelper);
        checkpointService = new RunFromTheBeastCheckpointService(moduleConfig, configHelper);
        stateRegistry = new RunFromTheBeastStateRegistry();
        gameManager = new RunFromTheBeastGameManager(
                moduleConfig,
                coreConfig,
                moduleInfo,
                stateRegistry,
                cageService,
                messagingService,
                loadoutService,
                checkpointService,
                beastService,
                distanceService,
                rewardService,
                statsService,
                armoryService,
                configHelper
        );

        statsService.registerStats();

        moduleConfig.register("language.yml", 2);
        moduleConfig.register("settings.yml", 2);
        moduleConfig.register("achievements.yml", 1);
        moduleConfig.register("store.yml", 1);
        moduleConfig.register("rewards.yml", 1);

        if (achievementsAPI != null) {
            achievementsAPI.registerModuleAchievements(moduleInfo.getId(), "achievements.yml");
        }

        storeService.registerStoreItems();

        ModuleAPI.getSetupAPI().registerHandler(moduleInfo.getId(), new RunFromTheBeastSetup(this));

        if (moduleConfig != null && voteMenu != null) {
            String voteItem = moduleConfig.getString("menus.vote.item");
            voteMenu.registerGame(
                    moduleInfo.getId(),
                    voteItem,
                    moduleConfig.getStringFrom("language.yml", "vote_menu.name"),
                    moduleConfig.getStringListFrom("language.yml", "vote_menu.lore")
            );
        }
    }

    @Override
    public void onStart(GameContext<Player, Location, World, String, ItemStack, String, Holder, Entity> context) {
        gameManager.onStart(context);
    }

    @Override
    public void onCountdownTick(GameContext<Player, Location, World, String, ItemStack, String, Holder, Entity> context,
                                int secondsLeft) {
        messagingService.sendCountdownTick(context, secondsLeft);
    }

    @Override
    public void onCountdownFinish(GameContext<Player, Location, World, String, ItemStack, String, Holder, Entity> context) {
        messagingService.sendCountdownFinish(context);
    }

    @Override
    public boolean freezePlayersOnCountdown() {
        return true;
    }

    @Override
    public void onGameStart(GameContext<Player, Location, World, String, ItemStack, String, Holder, Entity> context) {
        gameManager.onGameStart(context);
    }

    @Override
    public void onEnd(GameContext<Player, Location, World, String, ItemStack, String, Holder, Entity> context,
                      GameResult<Player> result) {
        gameManager.onEnd(context, result);
    }

    @Override
    public void onDisable() {
        gameManager.onDisable();
    }

    @Override
    public void registerEvents(CustomEventRegistry<EventSubscription<?>, Short> registry) {
        if (systemsRegistered) {
            return;
        }

        new RunFromTheBeastListener(this).register(registry);

        if (registry instanceof net.blueva.arcade.api.events.hytale.HytaleEventRegistry hytaleRegistry) {
            hytaleRegistry.registerSystem(new RunFromTheBeastDamageListener(gameManager));
            hytaleRegistry.registerSystem(new RunFromTheBeastItemDropListener(gameManager));
            hytaleRegistry.registerSystem(new RunFromTheBeastChestSystem(this));
            hytaleRegistry.registerSystem(new RunFromTheBeastDeathSystem(gameManager));
            systemsRegistered = true;
        }
    }

    @Override
    public Map<String, String> getCustomPlaceholders(Player player) {
        Map<String, String> placeholders = new HashMap<>();
        GameContext<Player, Location, World, String, ItemStack, String, Holder, Entity> context = getGameContext(player);
        if (context == null) {
            return placeholders;
        }

        RunFromTheBeastArenaState state = stateRegistry.getState(context.getArenaId());
        if (state == null) {
            return placeholders;
        }

        Player beast = gameManager.getBeast(context, state);
        int runnersAlive = gameManager.getAliveRunners(context, state).size();
        placeholders.putAll(messagingService.buildCustomPlaceholders(context, state, player, beast, runnersAlive));

        return placeholders;
    }

    public GameContext<Player, Location, World, String, ItemStack, String, Holder, Entity> getGameContext(Player player) {
        return stateRegistry.getGameContext(player);
    }

    public RunFromTheBeastArenaState getArenaState(GameContext<Player, Location, World, String, ItemStack, String, Holder, Entity> context) {
        if (context == null) {
            return null;
        }
        return stateRegistry.getState(context.getArenaId());
    }

    public boolean isBeast(GameContext<Player, Location, World, String, ItemStack, String, Holder, Entity> context, Player player) {
        RunFromTheBeastArenaState state = getArenaState(context);
        return state != null && gameManager.isBeast(context, state, player);
    }

    public void handleCheckpointSet(GameContext<Player, Location, World, String, ItemStack, String, Holder, Entity> context,
                                    Player player) {
        RunFromTheBeastArenaState state = getArenaState(context);
        if (state != null) {
            gameManager.handleCheckpointSet(context, player, state);
        }
    }

    public void handleCheckpointReturn(GameContext<Player, Location, World, String, ItemStack, String, Holder, Entity> context,
                                       Player player) {
        RunFromTheBeastArenaState state = getArenaState(context);
        if (state != null) {
            gameManager.handleCheckpointReturn(context, player, state);
        }
    }

    public void handleOutOfBounds(GameContext<Player, Location, World, String, ItemStack, String, Holder, Entity> context,
                                  Player player,
                                  boolean deathBlock) {
        gameManager.handleOutOfBounds(context, player, deathBlock);
    }

    public void handleDamage(GameContext<Player, Location, World, String, ItemStack, String, Holder, Entity> context,
                             Player victim,
                             Player killer) {
        RunFromTheBeastArenaState state = getArenaState(context);
        gameManager.handleDamage(context, state, victim, killer);
    }

    public void openArmory(Player player,
                           GameContext<Player, Location, World, String, ItemStack, String, Holder, Entity> context,
                           Vector3i blockPosition) {
        RunFromTheBeastArenaState state = getArenaState(context);
        gameManager.openArmory(player, context, state, blockPosition);
    }

    public String getCheckpointSetItemId() {
        return checkpointService.getCheckpointSetItemId();
    }

    public String getCheckpointReturnItemId() {
        return checkpointService.getCheckpointReturnItemId();
    }
}
