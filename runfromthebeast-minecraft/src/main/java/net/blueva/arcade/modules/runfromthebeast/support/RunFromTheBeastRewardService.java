package net.blueva.arcade.modules.runfromthebeast.support;

import net.blueva.arcade.api.config.ModuleConfigAPI;
import net.blueva.arcade.api.game.GameContext;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class RunFromTheBeastRewardService {

    private final ModuleConfigAPI moduleConfig;

    public RunFromTheBeastRewardService(ModuleConfigAPI moduleConfig) {
        this.moduleConfig = moduleConfig;
    }

    public void distributeTeamRewards(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                                      boolean beastWon,
                                      Player beast,
                                      List<Player> runnerWinners) {
        boolean enabled = moduleConfig.getBooleanFrom("rewards.yml", "rewards.enabled", true);
        if (!enabled) {
            return;
        }

        String beastName = beast != null ? beast.getName() : "-";

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
                        .replace("{player}", target.getName())
                        .replace("{team}", role)
                        .replace("{beast}", beastName != null ? beastName : "-");
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), processed);
            }
        }
    }
}
