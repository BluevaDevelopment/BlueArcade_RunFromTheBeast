package net.blueva.arcade.modules.runfromthebeast.support;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RunFromTheBeastConfigHelper {

    public String getItemIdSafe(String name, String fallback) {
        if (name == null || name.isBlank()) {
            return fallback;
        }
        return name;
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
            String itemId = parts[0];
            int amount = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;
            return new ItemStack(itemId, amount);
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
        return players.stream()
                .map(Player::getDisplayName)
                .collect(Collectors.joining(", "));
    }
}
