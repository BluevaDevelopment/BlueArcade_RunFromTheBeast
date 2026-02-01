package net.blueva.arcade.modules.runfromthebeast.support;

import net.blueva.arcade.api.config.CoreConfigAPI;
import net.blueva.arcade.api.config.ModuleConfigAPI;
import net.blueva.arcade.api.game.GameContext;
import net.blueva.arcade.api.module.ModuleInfo;
import net.blueva.arcade.modules.runfromthebeast.state.RunFromTheBeastArenaState;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class RunFromTheBeastMessagingService {

    private final ModuleConfigAPI moduleConfig;
    private final CoreConfigAPI coreConfig;
    private final ModuleInfo moduleInfo;
    private final RunFromTheBeastConfigHelper configHelper;

    public RunFromTheBeastMessagingService(ModuleConfigAPI moduleConfig, CoreConfigAPI coreConfig, ModuleInfo moduleInfo,
                                           RunFromTheBeastConfigHelper configHelper) {
        this.moduleConfig = moduleConfig;
        this.coreConfig = coreConfig;
        this.moduleInfo = moduleInfo;
        this.configHelper = configHelper;
    }

    public void sendDescription(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context) {
        List<String> description = moduleConfig.getStringListFrom("language.yml", "description");

        for (Player player : context.getPlayers()) {
            for (String line : description) {
                context.getMessagesAPI().sendRaw(player, line);
            }
        }
    }

    public void sendCountdownTick(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                                  int secondsLeft) {
        for (Player player : context.getPlayers()) {
            if (!player.isOnline()) {
                continue;
            }

            context.getSoundsAPI().play(player, coreConfig.getSound("sounds.starting_game.countdown"));

            String title = coreConfig.getLanguage("titles.starting_game.title")
                    .replace("{game_display_name}", moduleInfo.getName())
                    .replace("{time}", String.valueOf(secondsLeft));

            String subtitle = coreConfig.getLanguage("titles.starting_game.subtitle")
                    .replace("{game_display_name}", moduleInfo.getName())
                    .replace("{time}", String.valueOf(secondsLeft));

            context.getTitlesAPI().sendRaw(player, title, subtitle, 0, 20, 5);
        }
    }

    public void sendCountdownFinish(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context) {
        for (Player player : context.getPlayers()) {
            if (!player.isOnline()) {
                continue;
            }

            String title = coreConfig.getLanguage("titles.game_started.title")
                    .replace("{game_display_name}", moduleInfo.getName());

            String subtitle = coreConfig.getLanguage("titles.game_started.subtitle")
                    .replace("{game_display_name}", moduleInfo.getName());

            context.getTitlesAPI().sendRaw(player, title, subtitle, 0, 20, 20);

            context.getSoundsAPI().play(player, coreConfig.getSound("sounds.starting_game.start"));
        }
    }

    public void sendActionBar(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                              RunFromTheBeastArenaState state,
                              Player player,
                              Map<String, String> placeholders) {
        if (!player.isOnline()) {
            return;
        }

        String template = coreConfig.getLanguage("action_bar.in_game.global");
        String formatted = template
                .replace("{time}", String.valueOf(state.getTimeLeftSeconds()))
                .replace("{round}", String.valueOf(context.getCurrentRound()))
                .replace("{round_max}", String.valueOf(context.getMaxRounds()));

        context.getMessagesAPI().sendActionBar(player, formatted);
        context.getScoreboardAPI().update(player, placeholders);
    }

    public Map<String, String> buildCustomPlaceholders(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                                                       RunFromTheBeastArenaState state,
                                                       Player player,
                                                       Player beast, int runnersAlive) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("beast", beast != null ? beast.getName() : "-");
        placeholders.put("runners_alive", String.valueOf(runnersAlive));
        placeholders.put("release_time", String.valueOf(Math.max(state.getReleaseTimeSeconds(), 0)));
        placeholders.put("checkpoint_uses", String.valueOf(state.getCheckpointUses().getOrDefault(player.getUniqueId(), 0)));
        return placeholders;
    }

    public void sendBeastTitle(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                               Player beast) {
        String title = moduleConfig.getStringFrom("language.yml", "titles.beast.title");
        String subtitle = moduleConfig.getStringFrom("language.yml", "titles.beast.subtitle");

        if ((title == null || title.isEmpty()) && (subtitle == null || subtitle.isEmpty())) {
            return;
        }

        context.getTitlesAPI().sendRaw(beast, title, subtitle, 0, 40, 10);
    }

    public void sendOutcomeMessage(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                                   String multilinePath,
                                   String fallbackLine,
                                   Map<String, String> placeholders) {
        List<String> lines = moduleConfig.getStringListFrom("language.yml", multilinePath);
        if (lines != null && !lines.isEmpty()) {
            for (String line : lines) {
                String processed = configHelper.applyPlaceholders(line, placeholders);
                for (Player target : context.getPlayers()) {
                    context.getMessagesAPI().sendRaw(target, processed);
                }
            }
            return;
        }

        if (fallbackLine != null) {
            String processed = configHelper.applyPlaceholders(fallbackLine, placeholders);
            for (Player target : context.getPlayers()) {
                context.getMessagesAPI().sendRaw(target, processed);
            }
        }
    }

    public void sendVictoryTitles(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                                  String basePath,
                                  Map<String, String> placeholders) {
        String rawTitle = moduleConfig.getStringFrom("language.yml", basePath + ".title");
        String rawSubtitle = moduleConfig.getStringFrom("language.yml", basePath + ".subtitle");

        if ((rawTitle == null || rawTitle.isEmpty()) && (rawSubtitle == null || rawSubtitle.isEmpty())) {
            return;
        }

        String title = configHelper.applyPlaceholders(rawTitle, placeholders);
        String subtitle = configHelper.applyPlaceholders(rawSubtitle, placeholders);

        for (Player player : context.getPlayers()) {
            context.getTitlesAPI().sendRaw(player, title, subtitle, 0, 60, 20);
        }
    }

    public void showFinalScoreboard(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                                    String path,
                                    Map<String, String> placeholders) {
        for (Player player : context.getPlayers()) {
            context.getScoreboardAPI().showModuleFinalScoreboard(player, path, placeholders);
        }
    }

    public void broadcastBeastKill(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                                   Player victim,
                                   Player beast) {
        String message = getRandomMessage("messages.deaths.killed_by_beast");
        if (message == null) {
            return;
        }

        message = message.replace("{victim}", victim.getName())
                .replace("{beast}", beast.getName());

        for (Player target : context.getPlayers()) {
            context.getMessagesAPI().sendRaw(target, message);
        }
    }

    public String getRandomMessage(String path) {
        List<String> messages = moduleConfig.getStringListFrom("language.yml", path);
        if (messages == null || messages.isEmpty()) {
            return null;
        }

        int index = ThreadLocalRandom.current().nextInt(messages.size());
        return messages.get(index);
    }
}
