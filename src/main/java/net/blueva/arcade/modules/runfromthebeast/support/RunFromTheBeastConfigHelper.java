package net.blueva.arcade.modules.runfromthebeast.support;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RunFromTheBeastConfigHelper {

    public Material getMaterialSafe(String name, Material fallback) {
        if (name == null || name.isBlank()) {
            return fallback;
        }
        try {
            return Material.valueOf(name.toUpperCase());
        } catch (Exception e) {
            return fallback;
        }
    }

    public int parseIntSafe(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    public ItemStack parseItem(String spec) {
        try {
            String[] parts = spec.split(":");
            Material material = Material.valueOf(parts[0].toUpperCase());
            int amount = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;
            return new ItemStack(material, amount);
        } catch (Exception e) {
            return null;
        }
    }

    public String applyPlaceholders(String text, Map<String, String> placeholders) {
        if (text == null || text.isEmpty() || placeholders == null || placeholders.isEmpty()) {
            return text;
        }
        String result = text;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }

    public String formatPlayerList(List<Player> players) {
        if (players == null || players.isEmpty()) {
            return "-";
        }
        return players.stream().map(Player::getName).collect(Collectors.joining(", "));
    }
}
