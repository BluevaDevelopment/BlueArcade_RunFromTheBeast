package net.blueva.arcade.modules.runfromthebeast.game;

import net.blueva.arcade.api.config.CoreConfigAPI;
import net.blueva.arcade.api.config.ModuleConfigAPI;
import net.blueva.arcade.api.game.GameContext;
import net.blueva.arcade.api.game.GameResult;
import net.blueva.arcade.api.module.ModuleInfo;
import net.blueva.arcade.api.ModuleAPI;
import net.blueva.arcade.api.visuals.VisualEffectsAPI;
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
import org.bukkit.Material;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RunFromTheBeastGameManager {

    private final ModuleConfigAPI moduleConfig;
    private final CoreConfigAPI coreConfig;
    private final ModuleInfo moduleInfo;
    private final RunFromTheBeastStateRegistry stateRegistry;
    private final RunFromTheBeastCageService cageService;
    private final RunFromTheBeastMessagingService messagingService;
    private final RunFromTheBeastLoadoutService loadoutService;
    private final RunFromTheBeastCheckpointService checkpointService;
    private final RunFromTheBeastBeastService beastService;
    private final RunFromTheBeastDistanceService distanceService;
    private final RunFromTheBeastRewardService rewardService;
    private final RunFromTheBeastStatsService statsService;
    private final RunFromTheBeastArmoryService armoryService;
    private final RunFromTheBeastConfigHelper configHelper;

    public RunFromTheBeastGameManager(ModuleConfigAPI moduleConfig, CoreConfigAPI coreConfig, ModuleInfo moduleInfo,
                                      RunFromTheBeastStateRegistry stateRegistry,
                                      RunFromTheBeastCageService cageService,
                                      RunFromTheBeastMessagingService messagingService,
                                      RunFromTheBeastLoadoutService loadoutService,
                                      RunFromTheBeastCheckpointService checkpointService,
                                      RunFromTheBeastBeastService beastService,
                                      RunFromTheBeastDistanceService distanceService,
                                      RunFromTheBeastRewardService rewardService,
                                      RunFromTheBeastStatsService statsService,
                                      RunFromTheBeastArmoryService armoryService,
                                      RunFromTheBeastConfigHelper configHelper) {
        this.moduleConfig = moduleConfig;
        this.coreConfig = coreConfig;
        this.moduleInfo = moduleInfo;
        this.stateRegistry = stateRegistry;
        this.cageService = cageService;
        this.messagingService = messagingService;
        this.loadoutService = loadoutService;
        this.checkpointService = checkpointService;
        this.beastService = beastService;
        this.distanceService = distanceService;
        this.rewardService = rewardService;
        this.statsService = statsService;
        this.armoryService = armoryService;
        this.configHelper = configHelper;
    }

    public void onStart(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context) {
        int arenaId = context.getArenaId();

        context.getSummarySettings().setGameSummaryEnabled(false);
        context.getSummarySettings().setFinalSummaryEnabled(false);
        context.getSummarySettings().setRewardsEnabled(false);

        context.getSchedulerAPI().cancelArenaTasks(arenaId);

        RunFromTheBeastArenaState state = new RunFromTheBeastArenaState();
        state.setEnded(false);
        state.setTimeLeftSeconds(getGameTime(context));
        state.setReleaseTimeSeconds(moduleConfig.getInt("game.release_delay_seconds", 15));
        state.setCageBlocks(cageService.loadCageBlocks(context));
        stateRegistry.registerGame(context, state);

        messagingService.sendDescription(context);
    }

    public void onGameStart(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context) {
        int arenaId = context.getArenaId();
        RunFromTheBeastArenaState state = stateRegistry.getState(arenaId);
        if (state == null) {
            state = new RunFromTheBeastArenaState();
            stateRegistry.registerGame(context, state);
        }

        beastService.selectBeast(context, state);
        preparePlayers(context, state);
        startReleaseCountdown(context, state);
        startGameTimer(context, state);
        startDistanceTracking(context, state);
    }

    public void onEnd(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                      GameResult<Player> result) {
        int arenaId = context.getArenaId();
        RunFromTheBeastArenaState state = stateRegistry.getState(arenaId);

        context.getSchedulerAPI().cancelArenaTasks(arenaId);

        cageService.restoreCage(context, state);
        beastService.removeBeastGlow(context, state);
        distanceService.resetDistanceBars(context);
        stateRegistry.removeGame(arenaId);

        for (Player player : context.getPlayers()) {
            statsService.addGamesPlayed(player);
        }
    }

    public void onDisable() {
        for (GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context : new ArrayList<>(stateRegistry.getActiveGames())) {
            context.getSchedulerAPI().cancelModuleTasks(moduleInfo.getId());
            RunFromTheBeastArenaState state = stateRegistry.getState(context.getArenaId());
            cageService.restoreCage(context, state);
            beastService.removeBeastGlow(context, state);
            distanceService.resetDistanceBars(context);
            for (Player player : context.getPlayers()) {
                context.getScoreboardAPI().clearFinalScoreboard(player);
            }
        }

        stateRegistry.clear();
    }

    public Player getBeast(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                           RunFromTheBeastArenaState state) {
        return beastService.getBeast(context, state);
    }

    public List<Player> getAliveRunners(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                                        RunFromTheBeastArenaState state) {
        List<Player> alive = new ArrayList<>(context.getAlivePlayers());
        Player beast = getBeast(context, state);
        if (beast != null) {
            alive.remove(beast);
        }
        return alive;
    }

    public void openArmory(Player player,
                           GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                           RunFromTheBeastArenaState state,
                           org.bukkit.block.Container container) {
        armoryService.openArmory(player, context, state, container);
    }

    public void handleCheckpointSet(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                                    Player player,
                                    RunFromTheBeastArenaState state) {
        checkpointService.handleCheckpointSet(context, player, state);
    }

    public void handleCheckpointReturn(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                                       Player player,
                                       RunFromTheBeastArenaState state) {
        checkpointService.handleCheckpointReturn(context, player, state);
    }

    public void handleOutOfBounds(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                                  Player player,
                                  boolean deathBlock) {
        Location deathLocation = player.getLocation();
        playVisualEffects(player, null, deathLocation);
        context.respawnPlayer(player);
        loadoutService.applyStartingEffects(player, "effects.respawn_effects");

        String path = deathBlock ? "messages.deaths.death_block" : "messages.deaths.void";
        String message = messagingService.getRandomMessage(path);
        if (message != null) {
            message = message.replace("{player}", player.getName());
            for (Player target : context.getPlayers()) {
                context.getMessagesAPI().sendRaw(target, message);
            }
        }
    }

    public void handleDamage(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                             RunFromTheBeastArenaState state,
                             Player victim,
                             Player killer) {
        if (state == null || state.isEnded()) {
            return;
        }

        Location deathLocation = victim.getLocation();
        playVisualEffects(victim, killer, deathLocation);

        if (isBeast(context, state, victim)) {
            if (killer != null) {
                statsService.addRunnerKill(killer);
            }
            victim.setGameMode(GameMode.SPECTATOR);
            victim.getInventory().clear();
            distanceService.resetDistanceBar(victim);
            state.setEnded(true);
            handleRunnerVictory(context, state, getAliveRunners(context, state));
            return;
        }

        context.eliminatePlayer(victim, moduleConfig.getStringFrom("language.yml", "messages.eliminated"));
        victim.getInventory().clear();
        victim.setGameMode(GameMode.SPECTATOR);
        distanceService.resetDistanceBar(victim);

        if (killer != null && isBeast(context, state, killer)) {
            statsService.addBeastKill(killer);
            messagingService.broadcastBeastKill(context, victim, killer);
        }

        if (getAliveRunners(context, state).isEmpty()) {
            Player beast = getBeast(context, state);
            if (beast != null) {
                state.setEnded(true);
                handleBeastVictory(context, state, beast);
            }
        }
    }

    private void playVisualEffects(Player victim, Player killer, Location deathLocation) {
        VisualEffectsAPI visualEffectsAPI = ModuleAPI.getVisualEffectsAPI();
        if (visualEffectsAPI == null) {
            return;
        }
        if (deathLocation != null) {
            visualEffectsAPI.playDeathEffect(victim, deathLocation);
        } else {
            visualEffectsAPI.playDeathEffect(victim);
        }
        if (killer != null) {
            visualEffectsAPI.playKillEffect(killer);
        }
    }

    public boolean isBeast(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                           RunFromTheBeastArenaState state,
                           Player player) {
        return state != null && state.getBeastId() != null && state.getBeastId().equals(player.getUniqueId());
    }

    private void preparePlayers(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                                RunFromTheBeastArenaState state) {
        for (Player player : context.getPlayers()) {
            player.setGameMode(GameMode.SURVIVAL);
            player.setHealth(player.getMaxHealth());
            player.setFoodLevel(20);
            player.getInventory().clear();
            distanceService.resetDistanceBar(player);
            boolean isBeast = state.getBeastId() != null && state.getBeastId().equals(player.getUniqueId());
            loadoutService.applyStartingEffects(player, "effects.starting_effects");
            if (!isBeast) {
                loadoutService.giveItems(player, "items.starting_items");
                checkpointService.giveCheckpointItems(context, player, state);
            }
            context.getScoreboardAPI().clearFinalScoreboard(player);
            context.getScoreboardAPI().showModuleScoreboard(player);
        }

        Player beast = getBeast(context, state);
        if (beast != null) {
            loadoutService.giveItems(beast, "items.beast_items");
            loadoutService.applyStartingEffects(beast, "effects.beast_effects");
            beastService.applyBeastGlow(context, state, beast);
            loadoutService.applyBeastEquipment(beast);
            messagingService.sendBeastTitle(context, beast);
            context.getMessagesAPI().sendRaw(beast, moduleConfig.getStringFrom("language.yml", "messages.roles.beast"));
            statsService.addBeastRole(beast);
        }

        for (Player player : context.getPlayers()) {
            if (player.getUniqueId().equals(state.getBeastId())) {
                continue;
            }
            context.getMessagesAPI().sendRaw(player, moduleConfig.getStringFrom("language.yml", "messages.roles.runner"));
        }
    }

    private void startReleaseCountdown(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                                       RunFromTheBeastArenaState state) {
        int arenaId = context.getArenaId();
        String taskId = "arena_" + arenaId + "_rftb_release";

        context.getSchedulerAPI().runTimer(taskId, () -> {
            if (state.isEnded()) {
                context.getSchedulerAPI().cancelTask(taskId);
                return;
            }

            if (state.getReleaseTimeSeconds() <= 0) {
                releaseBeast(context, state);
                context.getSchedulerAPI().cancelTask(taskId);
                return;
            }

            if (shouldNotifyRelease(state.getReleaseTimeSeconds())) {
                for (Player player : context.getPlayers()) {
                    context.getMessagesAPI().sendRaw(player,
                            moduleConfig.getStringFrom("language.yml", "messages.beast_release_countdown")
                                    .replace("{time}", String.valueOf(state.getReleaseTimeSeconds())));
                }
            }

            state.setReleaseTimeSeconds(state.getReleaseTimeSeconds() - 1);
        }, 0L, 20L);
    }

    private void releaseBeast(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                              RunFromTheBeastArenaState state) {
        Player beast = getBeast(context, state);
        if (beast != null) {
            context.getMessagesAPI().sendRaw(beast, moduleConfig.getStringFrom("language.yml", "messages.beast_released_beast"));
        }

        for (Player player : context.getPlayers()) {
            if (beast != null && !player.getUniqueId().equals(beast.getUniqueId())) {
                context.getMessagesAPI().sendRaw(player, moduleConfig.getStringFrom("language.yml", "messages.beast_released_runners"));
            }
        }

        cageService.clearCage(context, state);
    }

    private void startGameTimer(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                                RunFromTheBeastArenaState state) {
        int arenaId = context.getArenaId();

        String taskId = "arena_" + arenaId + "_rftb_timer";

        context.getSchedulerAPI().runTimer(taskId, () -> {
            if (state.isEnded()) {
                context.getSchedulerAPI().cancelTask(taskId);
                return;
            }

            if (state.getTimeLeftSeconds() <= 0) {
                state.setEnded(true);
                context.endGame();
                return;
            }

            List<Player> alivePlayers = context.getAlivePlayers();
            if (alivePlayers.size() < 2) {
                state.setEnded(true);
                context.endGame();
                return;
            }

            Player beast = getBeast(context, state);
            boolean beastAlive = beast != null && alivePlayers.contains(beast);

            List<Player> runners = getAliveRunners(context, state);
            if (!beastAlive) {
                state.setEnded(true);
                handleRunnerVictory(context, state, runners);
                return;
            }

            if (runners.isEmpty()) {
                state.setEnded(true);
                handleBeastVictory(context, state, beast);
                return;
            }

            for (Player player : context.getPlayers()) {
                Map<String, String> placeholders = messagingService.buildCustomPlaceholders(
                        context, state, player, beast, runners.size());
                messagingService.sendActionBar(context, state, player, placeholders);
            }

            state.setTimeLeftSeconds(state.getTimeLeftSeconds() - 1);
        }, 0L, 20L);
    }

    private void startDistanceTracking(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                                       RunFromTheBeastArenaState state) {
        boolean enabled = moduleConfig.getBoolean("distance_bar.enabled", true);
        if (!enabled) {
            return;
        }

        int interval = Math.max(1, moduleConfig.getInt("distance_bar.update_interval_ticks", 10));
        String taskId = "arena_" + context.getArenaId() + "_rftb_distance";

        context.getSchedulerAPI().runTimer(taskId, () -> {
            if (state.isEnded()) {
                context.getSchedulerAPI().cancelTask(taskId);
                return;
            }
            Player beast = getBeast(context, state);
            distanceService.updateDistanceBars(context, state, beast);
        }, 0L, interval);
    }

    private void handleRunnerVictory(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                                     RunFromTheBeastArenaState state,
                                     List<Player> runners) {
        for (Player runner : runners) {
            statsService.addRunnerWin(runner);
        }

        Player beast = getBeast(context, state);
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("beast", beast != null ? beast.getName() : "-");
        placeholders.put("runner_count", String.valueOf(runners.size()));
        placeholders.put("runners", configHelper.formatPlayerList(runners));

        messagingService.sendOutcomeMessage(context, "messages.runners_win_lines",
                moduleConfig.getStringFrom("language.yml", "messages.runners_win"),
                placeholders);
        messagingService.sendVictoryTitles(context, "titles.victory.runners_won", placeholders);
        messagingService.showFinalScoreboard(context, "scoreboard.final.runners_won", placeholders);
        rewardService.distributeTeamRewards(context, false, beast, runners);

        try {
            context.markSharedFirstPlace(runners);
        } catch (NoSuchMethodError missing) {
            for (Player runner : runners) {
                context.setWinner(runner);
            }
        }

        context.endGame();
    }

    private void handleBeastVictory(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                                    RunFromTheBeastArenaState state,
                                    Player beast) {
        if (beast != null) {
            statsService.addBeastWin(beast);
        }

        List<Player> runners = getAliveRunners(context, state);
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("beast", beast != null ? beast.getName() : "-");
        placeholders.put("runner_count", String.valueOf(runners.size()));
        placeholders.put("runners", configHelper.formatPlayerList(runners));

        messagingService.sendOutcomeMessage(context, "messages.beast_win_lines",
                moduleConfig.getStringFrom("language.yml", "messages.beast_wins")
                        .replace("{beast}", beast != null ? beast.getName() : "-"),
                placeholders);
        messagingService.sendVictoryTitles(context, "titles.victory.beast_won", placeholders);
        messagingService.showFinalScoreboard(context, "scoreboard.final.beast_won", placeholders);
        rewardService.distributeTeamRewards(context, true, beast, runners);

        if (beast != null) {
            context.setWinner(beast);
        }
        context.endGame();
    }

    private boolean shouldNotifyRelease(int seconds) {
        List<Integer> notifications = new ArrayList<>();
        for (String raw : coreConfig.getSettingsStringList("game.global.countdown_notifications")) {
            try {
                notifications.add(Integer.parseInt(raw));
            } catch (NumberFormatException ignored) {
            }
        }
        if (notifications.isEmpty()) {
            notifications.addAll(List.of(15, 10, 5, 4, 3, 2, 1));
        }
        return notifications.contains(seconds);
    }

    private int getGameTime(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context) {
        Integer gameTime = context.getDataAccess().getGameData("basic.time", Integer.class);
        return (gameTime != null && gameTime > 0) ? gameTime : 240;
    }
}
