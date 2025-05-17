package krisbichocolate.patchworkadventures.dimensions.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import krisbichocolate.patchworkadventures.dimensions.component.ModComponents;
import krisbichocolate.patchworkadventures.dimensions.component.PlayerAnchorComponent;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class PlayerAnchorCommands {

    /**
     * Registers all player anchor related commands
     */
    public static void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher,
                                        CommandRegistryAccess registryAccess,
                                        CommandManager.RegistrationEnvironment environment) {

        // Main command
        var anchorCommand = literal("playeranchor")
            .requires(source -> source.hasPermissionLevel(2));

        // Set subcommand
        anchorCommand.then(
            literal("set")
                .then(argument("anchor_players", EntityArgumentType.players())
                    .then(argument("anchor_name", StringArgumentType.word())
                        .then(argument("position_source", EntityArgumentType.entity())
                            .executes(PlayerAnchorCommands::executeSetCommand))))
        );

        // Teleport subcommand
        anchorCommand.then(
            literal("tp")
                .then(argument("entities", EntityArgumentType.entities())
                    .then(argument("anchor_player", EntityArgumentType.player())
                        .then(argument("anchor_name", StringArgumentType.word())
                            .suggests(PlayerAnchorCommands::suggestAnchorNames)
                            .executes(PlayerAnchorCommands::executeTpCommand))))
        );

        // Unset subcommand
        anchorCommand.then(
            literal("unset")
                .then(argument("anchor_players", EntityArgumentType.players())
                    .then(argument("anchor_name", StringArgumentType.word())
                        .suggests(PlayerAnchorCommands::suggestAnchorNames)
                        .executes(PlayerAnchorCommands::executeUnsetCommand)))
        );

        // List subcommand
        anchorCommand.then(
            literal("list")
                .then(argument("anchor_players", EntityArgumentType.players())
                    .executes(PlayerAnchorCommands::executeListCommand))
        );

        // Register the command
        dispatcher.register(anchorCommand);
    }

    /**
     * Suggests anchor names for a player
     */
    private static CompletableFuture<Suggestions> suggestAnchorNames(
            CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) {
        try {
            Collection<ServerPlayerEntity> anchorPlayers = EntityArgumentType.getPlayers(context, "anchor_players");
            if (!anchorPlayers.isEmpty()) {
                // Just use the first player for suggestions
                ServerPlayerEntity firstPlayer = anchorPlayers.iterator().next();
                PlayerAnchorComponent component = ModComponents.ANCHORS.get(firstPlayer);

                for (String name : component.getAnchorNames()) {
                    builder.suggest(name);
                }
            }
        } catch (Exception e) {
            // If we can't get the players or component, just return empty suggestions
        }

        return builder.buildFuture();
    }

    /**
     * Saves an anchor for multiple players at the position of another entity
     * Command: /playeranchor set <anchor_players> <anchor_name> <position_source>
     */
    private static int executeSetCommand(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();

        try {
            Collection<ServerPlayerEntity> anchorPlayers = EntityArgumentType.getPlayers(context, "anchor_players");
            String anchorName = StringArgumentType.getString(context, "anchor_name");
            net.minecraft.entity.Entity positionSource = EntityArgumentType.getEntity(context, "position_source");

            // Get the entity's position and world
            double x = positionSource.getX();
            double y = positionSource.getY();
            double z = positionSource.getZ();
            float yaw = positionSource.getYaw();
            RegistryKey<World> worldKey = positionSource.getWorld().getRegistryKey();

            int playerCount = 0;
            for (ServerPlayerEntity anchorPlayer : anchorPlayers) {
                // Save the anchor for each player
                PlayerAnchorComponent component = ModComponents.ANCHORS.get(anchorPlayer);
                component.setAnchor(anchorName, x, y, z, yaw, worldKey);
                playerCount++;
            }

            final int finalPlayerCount = playerCount;
            source.sendFeedback(() -> Text.literal("Saved anchor '" + anchorName + "' for " +
                                                 finalPlayerCount + " player(s) at " +
                                                 String.format("%.1f, %.1f, %.1f", x, y, z) +
                                                 " in " + worldKey.getValue()), true);
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            source.sendError(Text.literal("Failed to save anchor: " + e.getMessage()));
            return 0;
        }
    }

    /**
     * Teleports entities to a player's anchor
     * Command: /playeranchor tp <entities> <anchor_player> <anchor_name>
     */
    private static int executeTpCommand(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();

        try {
            Collection<? extends net.minecraft.entity.Entity> entities =
                EntityArgumentType.getEntities(context, "entities");
            ServerPlayerEntity anchorPlayer = EntityArgumentType.getPlayer(context, "anchor_player");
            String anchorName = StringArgumentType.getString(context, "anchor_name");

            // Get the anchor
            PlayerAnchorComponent component = ModComponents.ANCHORS.get(anchorPlayer);

            if (!component.hasAnchor(anchorName)) {
                source.sendError(Text.literal("Anchor '" + anchorName + "' not found for " +
                                            anchorPlayer.getName().getString()));
                return 0;
            }

            // Get the anchor data
            Vec3d pos = component.getPosition(anchorName);
            float yaw = component.getYaw(anchorName);
            RegistryKey<World> worldKey = component.getWorldKey(anchorName);
            ServerWorld world = source.getServer().getWorld(worldKey);

            if (world == null) {
                source.sendError(Text.literal("World not found for anchor '" + anchorName + "'"));
                return 0;
            }

            // Teleport each entity
            int successCount = 0;
            for (net.minecraft.entity.Entity entity : entities) {
                entity.teleport(world, pos.x, pos.y, pos.z, PositionFlag.VALUES, yaw, 0f);
                successCount++;
            }

            if (successCount > 0) {
                final int finalSuccessCount = successCount;
                source.sendFeedback(() -> Text.literal("Teleported " + finalSuccessCount +
                                                     " entity/entities to anchor '" + anchorName + "'"), true);
            }

            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            source.sendError(Text.literal("Failed to teleport to anchor: " + e.getMessage()));
            return 0;
        }
    }

    /**
     * Removes an anchor for multiple players
     * Command: /playeranchor unset <anchor_players> <anchor_name>
     */
    private static int executeUnsetCommand(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();

        try {
            Collection<ServerPlayerEntity> anchorPlayers = EntityArgumentType.getPlayers(context, "anchor_players");
            String anchorName = StringArgumentType.getString(context, "anchor_name");

            int successCount = 0;
            int notFoundCount = 0;

            for (ServerPlayerEntity anchorPlayer : anchorPlayers) {
                // Remove the anchor for each player
                PlayerAnchorComponent component = ModComponents.ANCHORS.get(anchorPlayer);

                if (component.hasAnchor(anchorName)) {
                    component.removeAnchor(anchorName);
                    successCount++;
                } else {
                    notFoundCount++;
                }
            }

            if (successCount > 0) {
                final int finalSuccessCount = successCount;
                source.sendFeedback(() -> Text.literal("Removed anchor '" + anchorName + "' for " +
                                                     finalSuccessCount + " player(s)"), true);
            }

            if (notFoundCount > 0) {
                final int finalNotFoundCount = notFoundCount;
                source.sendFeedback(() -> Text.literal("Anchor '" + anchorName + "' not found for " +
                                                     finalNotFoundCount + " player(s)"), true);
            }

            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            source.sendError(Text.literal("Failed to remove anchor: " + e.getMessage()));
            return 0;
        }
    }

    /**
     * Lists all anchors for multiple players
     * Command: /playeranchor list <anchor_players>
     */
    private static int executeListCommand(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();

        try {
            Collection<ServerPlayerEntity> anchorPlayers = EntityArgumentType.getPlayers(context, "anchor_players");

            for (ServerPlayerEntity anchorPlayer : anchorPlayers) {
                // Get all anchors for this player
                PlayerAnchorComponent component = ModComponents.ANCHORS.get(anchorPlayer);
                Collection<String> anchorNames = component.getAnchorNames();

                if (anchorNames.isEmpty()) {
                    source.sendFeedback(() -> Text.literal(anchorPlayer.getName().getString() +
                                                         " has no anchors"), false);
                    continue;
                }

                source.sendFeedback(() -> Text.literal(anchorPlayer.getName().getString() +
                                                     " has the following anchors:"), false);

                for (String name : anchorNames) {
                    Vec3d pos = component.getPosition(name);
                    RegistryKey<World> worldKey = component.getWorldKey(name);

                    source.sendFeedback(() -> Text.literal("- " + name + ": " +
                                                         String.format("%.1f, %.1f, %.1f", pos.x, pos.y, pos.z) +
                                                         " in " + worldKey.getValue()), false);
                }
            }

            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            source.sendError(Text.literal("Failed to list anchors: " + e.getMessage()));
            return 0;
        }
    }

    /**
     * Teleports a player to a named anchor
     *
     * @param player The player to teleport
     * @param anchorName The anchor name
     * @return true if teleported successfully, false otherwise
     */
    public static boolean teleportPlayerToAnchor(ServerPlayerEntity player, String anchorName) {
        PlayerAnchorComponent component = ModComponents.ANCHORS.get(player);

        if (!component.hasAnchor(anchorName)) {
            return false;
        }

        Vec3d pos = component.getPosition(anchorName);
        float yaw = component.getYaw(anchorName);
        RegistryKey<World> worldKey = component.getWorldKey(anchorName);
        ServerWorld world = player.getServer().getWorld(worldKey);

        if (world == null) {
            return false;
        }

        player.teleport(world, pos.x, pos.y, pos.z, yaw, 0f);
        return true;
    }
}
