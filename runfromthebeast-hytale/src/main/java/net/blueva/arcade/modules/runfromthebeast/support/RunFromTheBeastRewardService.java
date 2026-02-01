package net.blueva.arcade.modules.runfromthebeast.support;

import net.blueva.arcade.api.config.ModuleConfigAPI;
import net.blueva.arcade.api.game.GameContext;
import com.hypixel.hytale.server.core.command.system.CommandManager;
import com.hypixel.hytale.server.core.console.ConsoleSender;
import com.hypixel.hytale.math.vector.Location;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.meta.BlockState;

import java.util.List;

public class RunFromTheBeastRewardService {

    private final ModuleConfigAPI moduleConfig;

    public RunFromTheBeastRewardService(ModuleConfigAPI moduleConfig) {
        this.moduleConfig = moduleConfig;
    }

    public void distributeTeamRewards(GameContext<Player, Location, World, String, ItemStack, String, BlockState, Entity> context,
                                      boolean beastWon,
                                      Player beast,
                                      List<Player> runnerWinners) {
        boolean enabled = moduleConfig.getBooleanFrom("rewards.yml", "rewards.enabled", true);
        if (!enabled) {
            return;
        }

        String beastName = beast != null ? beast.getDisplayName() : "-";

        List<String> beastRewards = moduleConfig.getStringListFrom("rewards.yml", "rewards.beast_win");
        List<String> runnerRewards = moduleConfig.getStringListFrom("rewards.yml", "rewards.runners_win");
        List<String> participationRewards = moduleConfig.getStringListFrom("rewards.yml", "rewards.participation");

        if (beastWon && beast != null) {
            executeRewardCommands(List.of(beast), beastRewards, "Beast", beastName);
        } else if (!beastWon && runnerWinners != null && !runnerWinners.isEmpty()) {
            executeRewardCommands(runnerWinners, runnerRewards, "Runners", beastName);
        }

        executeRewardCommands(context.getPlayers(), participationRewards, "Participation", beastName);
    }

    private void executeRewardCommands(List<Player> targets, List<String> commands, String role, String beastName) {
        if (commands == null || commands.isEmpty() || targets == null || targets.isEmpty()) {
            return;
        }

        for (Player target : targets) {
            for (String command : commands) {
                String processed = command
                        .replace("{player}", target.getDisplayName())
                        .replace("{team}", role)
                        .replace("{beast}", beastName != null ? beastName : "-");
                CommandManager.get().handleCommand(ConsoleSender.INSTANCE, processed);
            }
        }
    }
}
