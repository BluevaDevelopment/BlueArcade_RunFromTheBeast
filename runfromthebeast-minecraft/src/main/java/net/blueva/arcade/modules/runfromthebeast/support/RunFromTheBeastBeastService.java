package net.blueva.arcade.modules.runfromthebeast.support;

import net.blueva.arcade.api.config.ModuleConfigAPI;
import net.blueva.arcade.api.game.GameContext;
import net.blueva.arcade.api.store.StoreAPI;
import net.blueva.arcade.modules.runfromthebeast.state.RunFromTheBeastArenaState;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class RunFromTheBeastBeastService {

    private final ModuleConfigAPI moduleConfig;
    private final StoreAPI storeAPI;

    public RunFromTheBeastBeastService(ModuleConfigAPI moduleConfig, StoreAPI storeAPI) {
        this.moduleConfig = moduleConfig;
        this.storeAPI = storeAPI;
    }

    public void selectBeast(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                            RunFromTheBeastArenaState state) {
        List<Player> candidates = new ArrayList<>(context.getPlayers());
        if (candidates.isEmpty()) {
            return;
        }

        Map<Player, Integer> weighted = buildBeastWeights(candidates);
        int totalWeight = weighted.values().stream().mapToInt(Integer::intValue).sum();
        int roll = ThreadLocalRandom.current().nextInt(Math.max(totalWeight, 1)) + 1;
        int cumulative = 0;
        Player chosen = candidates.get(0);

        for (Map.Entry<Player, Integer> entry : weighted.entrySet()) {
            cumulative += entry.getValue();
            if (roll <= cumulative) {
                chosen = entry.getKey();
                break;
            }
        }

        state.setBeastId(chosen.getUniqueId());
        teleportBeastToCage(context, chosen);
    }

    public Player getBeast(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                           RunFromTheBeastArenaState state) {
        if (state.getBeastId() == null) {
            return null;
        }
        for (Player player : context.getPlayers()) {
            if (player.getUniqueId().equals(state.getBeastId())) {
                return player;
            }
        }
        return null;
    }

    public void applyBeastGlow(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                               RunFromTheBeastArenaState state,
                               Player beast) {
        if (beast == null) {
            return;
        }
        int durationSeconds = Math.max(state.getTimeLeftSeconds(), 0) + 30;
        beast.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, durationSeconds * 20, 0, false, false));

        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        if (scoreboard == null) {
            return;
        }
        String teamName = getBeastTeamName(context.getArenaId());
        Team team = scoreboard.getTeam(teamName);
        if (team == null) {
            team = scoreboard.registerNewTeam(teamName);
        }
        team.setColor(ChatColor.RED);
        if (!team.hasEntry(beast.getName())) {
            team.addEntry(beast.getName());
        }
    }

    public void removeBeastGlow(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                                RunFromTheBeastArenaState state) {
        if (context == null) {
            return;
        }
        Player beast = state != null ? getBeast(context, state) : null;
        if (beast != null) {
            beast.removePotionEffect(PotionEffectType.GLOWING);
        }
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        if (scoreboard == null) {
            return;
        }
        String teamName = getBeastTeamName(context.getArenaId());
        Team team = scoreboard.getTeam(teamName);
        if (team != null) {
            for (String entry : new ArrayList<>(team.getEntries())) {
                team.removeEntry(entry);
            }
            if (team.getEntries().isEmpty()) {
                team.unregister();
            }
        }
    }

    private Map<Player, Integer> buildBeastWeights(List<Player> candidates) {
        Map<Player, Integer> weights = new HashMap<>();
        int defaultWeight = moduleConfig.getIntFrom("store.yml", "weights.beast_pass.default", 1);
        int bonusWeight = moduleConfig.getIntFrom("store.yml", "weights.beast_pass.bonus", 3);
        String beastPassCategory = moduleConfig.getStringFrom("store.yml", "category_settings.beast_pass.id", "rftb_beast_pass");
        for (Player player : candidates) {
            int weight = defaultWeight;
            boolean ownsPass = storeAPI != null && storeAPI.isUnlocked(player, beastPassCategory, "pass");
            if (ownsPass) {
                weight += bonusWeight;
            }
            weights.put(player, weight);
        }
        return weights;
    }

    private void teleportBeastToCage(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                                     Player beast) {
        Location beastSpawn = context.getDataAccess().getGameLocation("game.beast.spawn");
        if (beastSpawn != null) {
            context.getSchedulerAPI().runLater("teleport_beast_" + context.getArenaId(), () ->
                    beast.teleport(beastSpawn), 0L);
        }
    }

    private String getBeastTeamName(int arenaId) {
        String base = "rftb_beast_" + arenaId;
        return base.substring(0, Math.min(16, base.length()));
    }
}
