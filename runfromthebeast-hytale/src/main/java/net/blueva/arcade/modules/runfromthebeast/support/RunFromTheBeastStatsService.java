package net.blueva.arcade.modules.runfromthebeast.support;

import net.blueva.arcade.api.module.ModuleInfo;
import net.blueva.arcade.api.stats.StatDefinition;
import net.blueva.arcade.api.stats.StatScope;
import net.blueva.arcade.api.stats.StatsAPI;
import com.hypixel.hytale.server.core.entity.entities.Player;

public class RunFromTheBeastStatsService {

    private final StatsAPI<Player> statsAPI;
    private final ModuleInfo moduleInfo;

    public RunFromTheBeastStatsService(StatsAPI<Player> statsAPI, ModuleInfo moduleInfo) {
        this.statsAPI = statsAPI;
        this.moduleInfo = moduleInfo;
    }

    public void registerStats() {
        if (statsAPI == null) {
            return;
        }

        statsAPI.registerModuleStat(moduleInfo.getId(),
                new StatDefinition("games_played", "Games Played", "Run From The Beast games played", StatScope.MODULE));
        statsAPI.registerModuleStat(moduleInfo.getId(),
                new StatDefinition("runner_wins", "Runner wins", "Wins achieved as a runner", StatScope.MODULE));
        statsAPI.registerModuleStat(moduleInfo.getId(),
                new StatDefinition("beast_wins", "Beast wins", "Wins achieved as the beast", StatScope.MODULE));
        statsAPI.registerModuleStat(moduleInfo.getId(),
                new StatDefinition("beast_roles", "Beast selections", "Times selected as the beast", StatScope.MODULE));
        statsAPI.registerModuleStat(moduleInfo.getId(),
                new StatDefinition("beast_kills", "Beast kills", "Runners killed while playing as the beast", StatScope.MODULE));
        statsAPI.registerModuleStat(moduleInfo.getId(),
                new StatDefinition("runner_kills", "Runner kills", "Beasts slain while playing as a runner", StatScope.MODULE));
    }

    public void addGamesPlayed(Player player) {
        if (statsAPI != null) {
            statsAPI.addModuleStat(player, moduleInfo.getId(), "games_played", 1);
        }
    }

    public void addRunnerWin(Player runner) {
        if (statsAPI != null) {
            statsAPI.addModuleStat(runner, moduleInfo.getId(), "runner_wins", 1);
            statsAPI.addGlobalStat(runner, "wins", 1);
        }
    }

    public void addBeastWin(Player beast) {
        if (statsAPI != null) {
            statsAPI.addModuleStat(beast, moduleInfo.getId(), "beast_wins", 1);
            statsAPI.addGlobalStat(beast, "wins", 1);
        }
    }

    public void addBeastRole(Player beast) {
        if (statsAPI != null) {
            statsAPI.addModuleStat(beast, moduleInfo.getId(), "beast_roles", 1);
        }
    }

    public void addBeastKill(Player beast) {
        if (statsAPI != null) {
            statsAPI.addModuleStat(beast, moduleInfo.getId(), "beast_kills", 1);
        }
    }

    public void addRunnerKill(Player runner) {
        if (statsAPI != null) {
            statsAPI.addModuleStat(runner, moduleInfo.getId(), "runner_kills", 1);
        }
    }
}
