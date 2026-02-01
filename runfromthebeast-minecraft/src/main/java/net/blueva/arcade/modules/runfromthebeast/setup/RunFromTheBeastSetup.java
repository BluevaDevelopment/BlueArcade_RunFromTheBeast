package net.blueva.arcade.modules.runfromthebeast.setup;

import net.blueva.arcade.api.setup.GameSetupHandler;
import net.blueva.arcade.api.setup.SetupContext;
import net.blueva.arcade.api.setup.SetupDataAPI;
import net.blueva.arcade.api.setup.TabCompleteContext;
import net.blueva.arcade.api.setup.TabCompleteResult;
import net.blueva.arcade.modules.runfromthebeast.RunFromTheBeastModule;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;

public class RunFromTheBeastSetup implements GameSetupHandler {

    private final RunFromTheBeastModule module;

    public RunFromTheBeastSetup(RunFromTheBeastModule module) {
        this.module = module;
    }

    @Override
    public boolean handle(SetupContext context) {
        return handleInternal(castSetupContext(context));
    }

    private boolean handleInternal(SetupContext<Player, CommandSender, Location> context) {
        if (!context.hasHandlerArgs(1)) {
            context.getMessagesAPI().send(context.getPlayer(),
                    module.moduleConfig.getStringFrom("language.yml", "setup_messages.usage"));
            return true;
        }

        String subcommand = context.getArg(context.getStartIndex() - 1).toLowerCase();

        return switch (subcommand) {
            case "beastspawn" -> handleBeastSpawn(context);
            case "beastzone" -> handleBeastZone(context);
            default -> {
                context.getMessagesAPI().send(context.getPlayer(),
                        module.coreConfig.getLanguage("admin_commands.errors.unknown_subcommand"));
                yield true;
            }
        };
    }

    @Override
    public TabCompleteResult tabComplete(TabCompleteContext context) {
        return tabCompleteInternal(castTabContext(context));
    }

    private TabCompleteResult tabCompleteInternal(TabCompleteContext<Player, CommandSender> context) {
        int relIndex = context.getRelativeArgIndex();
        String sub = context.getArg(context.getStartIndex() - 1).toLowerCase();

        if (relIndex == 0) {
            return TabCompleteResult.of("beastspawn", "beastzone");
        }

        if ("beastspawn".equals(sub) || "beastzone".equals(sub)) {
            return TabCompleteResult.of("set");
        }

        return TabCompleteResult.empty();
    }

    @Override
    public List<String> getSubcommands() {
        return Arrays.asList("beastspawn", "beastzone");
    }

    @Override
    public boolean validateConfig(SetupContext context) {
        return validateConfigInternal(castSetupContext(context));
    }

    private boolean validateConfigInternal(SetupContext<Player, CommandSender, Location> context) {
        SetupDataAPI data = context.getData();

        boolean hasBeastSpawn = data.has("game.beast.spawn.x");
        boolean hasCage = data.has("game.beast_cage.bounds.min.x");

        if (!hasBeastSpawn || !hasCage) {
            if (context.getSender() != null && context.isPlayer()) {
                context.getMessagesAPI().send(context.getPlayer(),
                        module.moduleConfig.getStringFrom("language.yml", "setup_messages.not_configured")
                                .replace("{arena_id}", String.valueOf(context.getArenaId())));
            }
        }

        return hasBeastSpawn && hasCage;
    }

    private boolean handleBeastSpawn(SetupContext<Player, CommandSender, Location> context) {
        if (!requirePlayer(context)) {
            return true;
        }

        if (!context.hasHandlerArgs(1) || !"set".equalsIgnoreCase(context.getHandlerArg(0))) {
            context.getMessagesAPI().send(context.getPlayer(),
                    module.moduleConfig.getStringFrom("language.yml", "setup_messages.usage_beast_spawn"));
            return true;
        }

        Player player = context.getPlayer();
        context.getData().setLocation("game.beast.spawn", player.getLocation());
        context.getData().save();

        context.getMessagesAPI().send(player,
                module.moduleConfig.getStringFrom("language.yml", "setup_messages.beast_spawn_set"));
        return true;
    }

    private boolean handleBeastZone(SetupContext<Player, CommandSender, Location> context) {
        if (!requirePlayer(context)) {
            return true;
        }

        if (!context.hasHandlerArgs(1) || !"set".equalsIgnoreCase(context.getHandlerArg(0))) {
            context.getMessagesAPI().send(context.getPlayer(),
                    module.moduleConfig.getStringFrom("language.yml", "setup_messages.usage_beast_zone"));
            return true;
        }

        Player player = context.getPlayer();

        if (!context.getSelection().hasCompleteSelection(player)) {
            context.getMessagesAPI().send(player,
                    module.moduleConfig.getStringFrom("language.yml", "setup_messages.must_use_stick"));
            return true;
        }

        Location pos1 = context.getSelection().getPosition1(player);
        Location pos2 = context.getSelection().getPosition2(player);

        context.getData().setRegionBounds("game.beast_cage.bounds", pos1, pos2);
        context.getData().saveRegionBlocks("game.beast_cage.blocks", pos1, pos2);
        context.getData().save();

        sendRegionMessage(context, pos1, pos2, "setup_messages.beast_zone_set");
        return true;
    }

    private boolean requirePlayer(SetupContext<Player, CommandSender, Location> context) {
        if (!context.isPlayer()) {
            context.getMessagesAPI().send(context.getPlayer(),
                    module.coreConfig.getLanguage("admin_commands.errors.must_be_player"));
            return false;
        }
        return true;
    }

    private void sendRegionMessage(SetupContext<Player, CommandSender, Location> context,
                                   Location pos1,
                                   Location pos2,
                                   String messagePath) {
        int x = (int) Math.abs(pos2.getX() - pos1.getX()) + 1;
        int y = (int) Math.abs(pos2.getY() - pos1.getY()) + 1;
        int z = (int) Math.abs(pos2.getZ() - pos1.getZ()) + 1;
        int blocks = x * y * z;

        context.getMessagesAPI().send(context.getPlayer(),
                module.moduleConfig.getStringFrom("language.yml", messagePath)
                        .replace("{blocks}", String.valueOf(blocks))
                        .replace("{x}", String.valueOf(x))
                        .replace("{y}", String.valueOf(y))
                        .replace("{z}", String.valueOf(z)));
    }


    @SuppressWarnings("unchecked")
    private SetupContext<Player, CommandSender, Location> castSetupContext(SetupContext context) {
        return (SetupContext<Player, CommandSender, Location>) context;
    }

    @SuppressWarnings("unchecked")
    private TabCompleteContext<Player, CommandSender> castTabContext(TabCompleteContext context) {
        return (TabCompleteContext<Player, CommandSender>) context;
    }
}
