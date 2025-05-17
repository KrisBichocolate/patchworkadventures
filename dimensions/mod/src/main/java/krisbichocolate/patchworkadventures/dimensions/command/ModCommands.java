package krisbichocolate.patchworkadventures.dimensions.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import krisbichocolate.patchworkadventures.dimensions.PatchworkAdventures;
import krisbichocolate.patchworkadventures.dimensions.component.ModComponents;
import krisbichocolate.patchworkadventures.dimensions.TickScheduler;
import krisbichocolate.patchworkadventures.dimensions.InstanceData;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

import static net.minecraft.server.command.CommandManager.literal;

public class ModCommands {
    private static final String CONFIG_NAMESPACE = "pwa_dimensions";
    private static final String TEMPLATES_KEY = "templates";

    /**
     * Gets the registry key for an instance world
     * @param instanceName The name of the instance
     * @return The registry key for the instance world
     */
    public static RegistryKey<World> getInstanceWorldKey(String instanceName) {
        String worldId = "pwa_dimensions:instance/" + instanceName;
        return RegistryKey.of(RegistryKeys.WORLD, Identifier.of(worldId));
    }

    /**
     * Gets the registry key for a template world
     * @param templateName The name of the template
     * @return The registry key for the template world
     */
    public static RegistryKey<World> getTemplateWorldKey(String templateName) {
        String worldId = "pwa_dimensions:template/" + templateName;
        return RegistryKey.of(RegistryKeys.WORLD, Identifier.of(worldId));
    }

    /**
     * Gets the template storage from the server
     * @param server The Minecraft server
     * @return The template storage compound
     */
    private static NbtCompound getTemplateStorage(MinecraftServer server) {
        Identifier configId = Identifier.of(CONFIG_NAMESPACE, TEMPLATES_KEY);
        return server.getDataCommandStorage().get(configId);
    }

    public static void registerCommands() {
        CommandRegistrationCallback.EVENT.register(ModCommands::register);
    }

    private static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        // Register resource world commands
        ResourceWorldCommands.registerCommands(dispatcher, registryAccess, environment);

        // Register player anchor commands
        PlayerAnchorCommands.registerCommands(dispatcher, registryAccess, environment);

        // Main dungeon command
        var dungeonCommand = literal("dungeon");

        // Template subcommands (admin level)
        var templateCommand = literal("template")
            .requires(source -> source.hasPermissionLevel(4));

        templateCommand.then(
            literal("create")
                .then(CommandManager.argument("name", StringArgumentType.word())
                    .executes(ModCommands::executeInstanceTemplateCreateCommand))
        );

        templateCommand.then(
            literal("edit")
                .then(CommandManager.argument("name", StringArgumentType.word())
                    .suggests((context, builder) -> {
                        getTemplateNames(context.getSource().getServer())
                            .forEach(builder::suggest);
                        return builder.buildFuture();
                    })
                    .executes(ModCommands::executeInstanceTemplateEditCommand))
        );

        templateCommand.then(
            literal("save_and_finish")
                .then(CommandManager.argument("name", StringArgumentType.word())
                    .executes(ModCommands::executeInstanceTemplateSaveCommand))
        );

        templateCommand.then(
            literal("delete")
                .then(CommandManager.argument("name", StringArgumentType.word())
                    .suggests((context, builder) -> {
                        getTemplateNames(context.getSource().getServer())
                            .forEach(builder::suggest);
                        return builder.buildFuture();
                    })
                    .executes(ModCommands::executeInstanceTemplateDeleteCommand))
        );

        var instanceCommand = literal("instance");

        // Instance management subcommands
        instanceCommand.then(
            literal("create")
                .requires(source -> source.hasPermissionLevel(2))
                .then(CommandManager.argument("template", StringArgumentType.word())
                    .suggests((context, builder) -> {
                        getTemplateNames(context.getSource().getServer())
                            .forEach(builder::suggest);
                        return builder.buildFuture();
                    })
                    .executes(ModCommands::executeInstanceCreateCommand))
        );

        instanceCommand.then(
            literal("delete")
                .requires(source -> source.hasPermissionLevel(4))
                .then(CommandManager.argument("name", StringArgumentType.word())
                    .suggests((context, builder) -> {
                        getLoadedInstanceNames(context.getSource().getServer())
                            .forEach(builder::suggest);
                        return builder.buildFuture();
                    })
                    .executes(ModCommands::executeInstanceDeleteCommand))
        );

        // Keepalive player management
        var keepaliveCommand = literal("keepalive")
            .requires(source -> source.hasPermissionLevel(2));

        keepaliveCommand.then(
            literal("add")
                .then(CommandManager.argument("players", EntityArgumentType.players())
                    .executes(ModCommands::executeInstanceKeepalivePlayerAddCommand))
        );

        keepaliveCommand.then(
            literal("remove")
                .then(CommandManager.argument("players", EntityArgumentType.players())
                    .executes(ModCommands::executeInstanceKeepalivePlayerRemoveCommand))
        );

        // Add all subcommands
        instanceCommand.then(keepaliveCommand);
        dungeonCommand.then(templateCommand);
        dungeonCommand.then(instanceCommand);

        // Register the main command
        dispatcher.register(dungeonCommand);
    }


    /**
     * Creates a new instance template world
     * Command: /dungeon template create <name>
     */
    private static int executeInstanceTemplateCreateCommand(CommandContext<ServerCommandSource> context) {
        ServerCommandSource playerSource = context.getSource();
        ServerCommandSource source = playerSource.getServer().getCommandSource();
        String templateName = StringArgumentType.getString(context, "name");
        ServerPlayerEntity player = playerSource.getPlayer();

        if (player == null) {
            source.sendError(Text.literal("This command can only be executed by a player"));
            return 0;
        }

        try {
            // Create a unique edit ID
            int editId = (int)(System.currentTimeMillis() % 10000);
            String editTemplateName = templateName + "_edit" + editId;
            RegistryKey<World> worldKey = getTemplateWorldKey(editTemplateName);
            String worldId = worldKey.getValue().toString();

            // Create the world
            CommandManager cmd = source.getServer().getCommandManager();
            cmd.executeWithPrefix(source, "mw create " + worldId + " minecraft:flat");

            // Create an empty template config for the edit world
            NbtCompound templateStorage = getTemplateStorage(source.getServer());
            templateStorage.put(editTemplateName, new NbtCompound());

            UUID playerId = player.getUuid();
            TickScheduler.schedule(1, () -> {
                // Get the world and teleport the player there
                ServerWorld world = source.getServer().getWorld(worldKey);
                if (world == null) {
                    source.sendError(Text.literal("Failed to create template world"));
                    return;
                }

                ServerPlayerEntity deferredPlayer = source.getServer().getPlayerManager().getPlayer(playerId);
                if (deferredPlayer == null) {
                    return;
                }
                deferredPlayer.teleport(world, 0, 100, 0, 0f, 0f);
                source.sendFeedback(() -> Text.literal("Created new template world: " + worldId), true);
            });

            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            source.sendError(Text.literal("Failed to create template: " + e.getMessage()));
            return 0;
        }
    }

    /**
     * Edits an existing instance template
     * Command: /dungeon template edit <name>
     */
    private static int executeInstanceTemplateEditCommand(CommandContext<ServerCommandSource> context) {
        ServerCommandSource playerSource = context.getSource();
        ServerCommandSource source = playerSource.getServer().getCommandSource();
        String templateName = StringArgumentType.getString(context, "name");
        ServerPlayerEntity player = playerSource.getPlayer();

        if (player == null) {
            source.sendError(Text.literal("This command can only be executed by a player"));
            return 0;
        }

        try {
            // Create a unique edit ID
            int editId = (int)(System.currentTimeMillis() % 10000);
            String editTemplateName = templateName + "_edit" + editId;
            RegistryKey<World> sourceWorldKey = getTemplateWorldKey(templateName);
            RegistryKey<World> targetWorldKey = getTemplateWorldKey(editTemplateName);
            String sourceWorldId = sourceWorldKey.getValue().toString();
            String targetWorldId = targetWorldKey.getValue().toString();

            // Clone the world
            CommandManager cmd = source.getServer().getCommandManager();
            cmd.executeWithPrefix(source, "mw clone " + sourceWorldId + " " + targetWorldId);

            // Copy the template config for editing
            NbtCompound templateStorage = getTemplateStorage(source.getServer());
            if (templateStorage.contains(templateName)) {
                NbtCompound originalConfig = templateStorage.getCompound(templateName);
                templateStorage.put(editTemplateName, originalConfig.copy());
            } else {
                // If no config exists for the original template, create an empty one
                templateStorage.put(editTemplateName, new NbtCompound());
            }

            UUID playerId = player.getUuid();
            TickScheduler.schedule(1, () -> {
                // Get the world and teleport the player there
                ServerWorld world = source.getServer().getWorld(targetWorldKey);
                if (world == null) {
                    source.sendError(Text.literal("Failed to clone template world"));
                    return;
                }

                ServerPlayerEntity deferredPlayer = source.getServer().getPlayerManager().getPlayer(playerId);
                if (deferredPlayer == null) {
                    return;
                }
                CommandManager cmd2 = source.getServer().getCommandManager();
                cmd2.executeWithPrefix(source.withEntity(deferredPlayer), "mw tp " + targetWorldId);

                source.sendFeedback(() -> Text.literal("Created editable copy of template: " + targetWorldId), true);
            });

            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            source.sendError(Text.literal("Failed to edit template: " + e.getMessage()));
            return 0;
        }
    }

    /**
     * Saves an edited template to a permanent name
     * Command: /dungeon template save_and_finish <name>
     */
    private static int executeInstanceTemplateSaveCommand(CommandContext<ServerCommandSource> context) {
        ServerCommandSource playerSource = context.getSource();
        ServerCommandSource source = playerSource.getServer().getCommandSource();
        String templateName = StringArgumentType.getString(context, "name");
        ServerPlayerEntity player = playerSource.getPlayer();

        if (player == null) {
            source.sendError(Text.literal("This command can only be executed by a player"));
            return 0;
        }

        try {
            // Find the edit world the player is in
            ServerWorld currentWorld = player.getServerWorld();
            String currentWorldId = currentWorld.getRegistryKey().getValue().toString();
            String currentWorldName = currentWorld.getRegistryKey().getValue().getPath();

            if (!currentWorldId.contains("_edit")) {
                source.sendError(Text.literal("You must be in an editable template world to save it"));
                return 0;
            }

            // Extract the edit template name from the world ID
            String editTemplateName = currentWorldName.substring(currentWorldName.lastIndexOf('/') + 1);

            Collection<ServerPlayerEntity> playersInWorld = new ArrayList<>(currentWorld.getPlayers());
            for (ServerPlayerEntity plr : playersInWorld) {
                ResourceWorldCommands.teleportPlayerToOverworld(plr);
            }

            // Save the world to the permanent name
            String targetWorldId = "pwa_dimensions:template/" + templateName;

            CommandManager cmd = source.getServer().getCommandManager();
            cmd.executeWithPrefix(source, "mw clone " + currentWorldId + " " + targetWorldId);
            cmd.executeWithPrefix(source, "mw delete " + currentWorldId);

            // Save the template config
            NbtCompound templateStorage = getTemplateStorage(source.getServer());
            if (templateStorage.contains(editTemplateName)) {
                // Copy the edit config to the permanent name
                NbtCompound editConfig = templateStorage.getCompound(editTemplateName);
                templateStorage.put(templateName, editConfig.copy());
                // Remove the edit config
                templateStorage.remove(editTemplateName);
            } else {
                // If no config exists for the edit template, create an empty one for the permanent template
                templateStorage.put(templateName, new NbtCompound());
            }

            source.sendFeedback(() -> Text.literal("Saved template"), true);
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            source.sendError(Text.literal("Failed to save template: " + e.getMessage()));
            return 0;
        }
    }

    /**
     * Deletes an instance template
     * Command: /dungeon template delete <name>
     */
    private static int executeInstanceTemplateDeleteCommand(CommandContext<ServerCommandSource> context) {
        ServerCommandSource playerSource = context.getSource();
        ServerCommandSource source = playerSource.getServer().getCommandSource();
        String templateName = StringArgumentType.getString(context, "name");

        try {
            RegistryKey<World> worldKey = getTemplateWorldKey(templateName);
            ServerWorld world = source.getServer().getWorld(worldKey);
            String worldId = worldKey.getValue().toString();

            if (world == null) {
                source.sendError(Text.literal("Template world not found: " + worldId));
                return 0;
            }

            // Teleport all players out
            Collection<ServerPlayerEntity> playersInWorld = new ArrayList<>(world.getPlayers());
            for (ServerPlayerEntity plr : playersInWorld) {
                ResourceWorldCommands.teleportPlayerToOverworld(plr);
            }

            // Delete the world
            CommandManager cmd = source.getServer().getCommandManager();
            cmd.executeWithPrefix(source, "mw delete " + worldId);

            // Remove the template config
            NbtCompound templateStorage = getTemplateStorage(source.getServer());
            if (templateStorage.contains(templateName)) {
                templateStorage.remove(templateName);
            }

            source.sendFeedback(() -> Text.literal("Template deleted: " + worldId), true);
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            source.sendError(Text.literal("Failed to delete template: " + e.getMessage()));
            return 0;
        }
    }

    /**
     * Deletes an instance
     * Command: /dungeon instance delete <name>
     */
    private static int executeInstanceDeleteCommand(CommandContext<ServerCommandSource> context) {
        ServerCommandSource playerSource = context.getSource();
        ServerCommandSource source = playerSource.getServer().getCommandSource();
        String instanceName = StringArgumentType.getString(context, "name");

        try {
            RegistryKey<World> worldKey = getInstanceWorldKey(instanceName);
            ServerWorld world = source.getServer().getWorld(worldKey);
            String worldId = worldKey.getValue().toString();

            if (world == null) {
                source.sendError(Text.literal("Instance world not found: " + worldId));
                return 0;
            }

            // Teleport all players out
            Collection<ServerPlayerEntity> playersInWorld = new ArrayList<>(world.getPlayers());
            for (ServerPlayerEntity plr : playersInWorld) {
                ResourceWorldCommands.teleportPlayerToOverworld(plr);
            }

            // Delete the world
            CommandManager cmd = source.getServer().getCommandManager();
            cmd.executeWithPrefix(source, "mw delete " + worldId);

            // Remove from instance storage
            InstanceData.removeInstance(source.getServer(), instanceName);

            source.sendFeedback(() -> Text.literal("Instance deleted: " + worldId), true);
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            source.sendError(Text.literal("Failed to delete instance: " + e.getMessage()));
            return 0;
        }
    }

    /**
     * Creates a new instance from a template with an automatically generated name
     * Command: /dungeon instance create <template>
     */
    private static int executeInstanceCreateCommand(CommandContext<ServerCommandSource> context) {
        ServerCommandSource playerSource = context.getSource();
        ServerCommandSource source = playerSource.getServer().getCommandSource();
        String templateName = StringArgumentType.getString(context, "template");
        ServerPlayerEntity player = playerSource.getPlayer();

        try {
            // Generate a unique instance name based on template name and timestamp
            String instanceName = templateName + "_" + System.currentTimeMillis() % 1000000;

            // Check if this instance name already exists and generate a new one if needed
            String baseInstanceName = instanceName;
            int counter = 1;
            while (instanceExists(source.getServer(), instanceName)) {
                instanceName = baseInstanceName + "_" + counter++;
            }
            final String finalInstanceName = instanceName;

            String sourceWorldId = getTemplateWorldKey(templateName).getValue().toString();
            String targetWorldId = getInstanceWorldKey(instanceName).getValue().toString();

            // Clone the template to create the instance
            CommandManager cmd = source.getServer().getCommandManager();
            cmd.executeWithPrefix(source, "mw clone " + sourceWorldId + " " + targetWorldId);

            // Store the template name in the instance data
            NbtCompound instanceData = new NbtCompound();
            instanceData.putString("template", templateName);
            InstanceData.addInstanceData(source.getServer(), instanceName, instanceData);

            // Store the new instance world ID in data storage for datapacks to access
            NbtCompound returnValue = new NbtCompound();
            returnValue.putString("new_world", targetWorldId);
            returnValue.putString("new_instance", instanceName);
            returnValue.putString("template", templateName);
            Identifier returnStorageId = Identifier.of("pwa_dimensions", "return_value");
            source.getServer().getDataCommandStorage().set(returnStorageId, returnValue);

            // Copy template config to the return value for datapacks to access
            NbtCompound templateStorage = getTemplateStorage(source.getServer());
            if (templateStorage.contains(templateName)) {
                NbtCompound templateConfig = templateStorage.getCompound(templateName);
                returnValue.put("template_config", templateConfig.copy());
                source.getServer().getDataCommandStorage().set(returnStorageId, returnValue);

                // Schedule instantiation function execution.
                // It's surprising, but a single tick delay is not enough to ensure
                // forceload blocks and entities are available (weirdly, if this command is called by a function!)
                TickScheduler.schedule(2, () -> {
                    handleOnInstantiationEvents(source.getServer(), templateConfig, finalInstanceName);
                });
            }

            source.sendFeedback(() -> Text.literal("Created new instance: " + targetWorldId), true);
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            source.sendError(Text.literal("Failed to instantiate template: " + e.getMessage()));
            return 0;
        }
    }

    /**
     * Adds multiple players to the keepalive list for the current instance
     * Command: /dungeon instance keepalive add <players>
     */
    private static int executeInstanceKeepalivePlayerAddCommand(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();

        try {
            // Get the current world
            ServerWorld currentWorld = source.getWorld();
            if (currentWorld == null) {
                source.sendError(Text.literal("Could not determine the current world"));
                return 0;
            }

            String worldId = currentWorld.getRegistryKey().getValue().toString();

            // Check if this is an instance world
            String prefix = "pwa_dimensions:instance/";
            if (!worldId.startsWith(prefix)) {
                source.sendError(Text.literal("This command can only be used in an instance world"));
                return 0;
            }

            // Extract the instance name from the world ID
            String instanceName = worldId.substring(prefix.length());
            Collection<ServerPlayerEntity> players = EntityArgumentType.getPlayers(context, "players");

            for (ServerPlayerEntity player : players) {
                InstanceData.addKeepalivePlayer(source.getServer(), instanceName, player.getUuid());
            }

            source.sendFeedback(() -> Text.literal("Added " + players.size() + " player(s) to instance keepalive"), true);
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            source.sendError(Text.literal("Failed to add players to keepalive: " + e.getMessage()));
            return 0;
        }
    }

    /**
     * Checks if an instance with the given name already exists
     * @param server The Minecraft server
     * @param instanceName The instance name to check
     * @return true if the instance exists, false otherwise
     */
    private static boolean instanceExists(MinecraftServer server, String instanceName) {
        // Check both the world registry and our persistent storage
        RegistryKey<World> worldKey = getInstanceWorldKey(instanceName);
        return server.getWorld(worldKey) != null ||
               InstanceData.hasKeepalivePlayers(server, instanceName);
    }

    /**
     * Gets all currently loaded instance names
     * @param server The Minecraft server
     * @return A collection of instance names
     */
    private static Collection<String> getLoadedInstanceNames(MinecraftServer server) {
        Collection<String> instances = new ArrayList<>();
        String prefix = "pwa_dimensions:instance/";

        // Check all loaded worlds
        for (ServerWorld world : server.getWorlds()) {
            String worldId = world.getRegistryKey().getValue().toString();
            if (worldId.startsWith(prefix)) {
                // Extract the instance name from the world ID
                String instanceName = worldId.substring(prefix.length());
                instances.add(instanceName);
            }
        }

        return instances;
    }

    /**
     * Gets all template names from the template storage
     * @param server The Minecraft server
     * @return A collection of template names
     */
    private static Collection<String> getTemplateNames(MinecraftServer server) {
        Collection<String> templates = new ArrayList<>();
        NbtCompound templateStorage = getTemplateStorage(server);

        // Get all template names from storage
        templates.addAll(templateStorage.getKeys());

        // Also check for template worlds that might not be in storage yet
        String prefix = "pwa_dimensions:template/";
        for (ServerWorld world : server.getWorlds()) {
            String worldId = world.getRegistryKey().getValue().toString();
            if (worldId.startsWith(prefix)) {
                String templateName = worldId.substring(prefix.length());
                if (!templates.contains(templateName)) {
                    templates.add(templateName);
                }
            }
        }

        return templates;
    }

    /**
     * Teleports a player to their saved exit location
     *
     * @param player The player to teleport
     * @return true if teleported successfully, false otherwise
     */
    public static boolean teleportPlayerToExit(ServerPlayerEntity player) {
        return PlayerAnchorCommands.teleportPlayerToAnchor(player, ModComponents.INSTANCE_EXIT_NAME);
    }

    /**
     * Removes a player from the keepalive list for an instance and deletes the instance if empty
     *
     * @param server The Minecraft server
     * @param instanceName The name of the instance
     * @param playerUuid The UUID of the player to remove
     * @return true if the instance was deleted, false otherwise
     */
    public static boolean removePlayerFromInstanceAndDeleteIfEmpty(MinecraftServer server, String instanceName, UUID playerUuid) {
        // Remove player from keepalive and check if instance is now empty
        boolean isEmpty = InstanceData.removeKeepalivePlayer(server, instanceName, playerUuid);

        // If instance is now empty, delete it
        if (isEmpty) {
            RegistryKey<World> instanceWorldKey = getInstanceWorldKey(instanceName);
            ServerWorld instanceWorld = server.getWorld(instanceWorldKey);
            String instanceWorldId = instanceWorldKey.getValue().toString();

            if (instanceWorld != null) {
                // Teleport any remaining players out
                Collection<ServerPlayerEntity> playersInWorld = new ArrayList<>(instanceWorld.getPlayers());
                for (ServerPlayerEntity worldPlayer : playersInWorld) {
                    ResourceWorldCommands.teleportPlayerToOverworld(worldPlayer);
                }

                // Delete the world
                CommandManager cmd = server.getCommandManager();
                cmd.executeWithPrefix(server.getCommandSource(), "mw delete " + instanceWorldId);

                PatchworkAdventures.LOGGER.info("Instance deleted as it's now empty: " + instanceWorldId);
                return true;
            }
        }

        return false;
    }

    /**
     * Handles instantiation function defined in the template config
     * @param server The Minecraft server
     * @param templateConfig The template configuration
     * @param instanceName The name of the new instance
     */
    private static void handleOnInstantiationEvents(MinecraftServer server, NbtCompound templateConfig, String instanceName) {
        // Return early if no instantiation_function configuration exists
        if (!templateConfig.contains("instantiation_function")) {
            return;
        }

        String function = templateConfig.getString("instantiation_function");
        if (function.isEmpty()) {
            return;
        }

        RegistryKey<World> instanceWorldKey = getInstanceWorldKey(instanceName);
        ServerWorld instanceWorld = server.getWorld(instanceWorldKey);

        if (instanceWorld == null) {
            PatchworkAdventures.LOGGER.error("Failed to handle instantiation function: instance world not found");
            return;
        }

        // Tick to ensure entities are available
        instanceWorld.tick(() -> true);

        // Execute the function in the instance world
        ServerCommandSource cmdSource = server.getCommandSource()
            .withWorld(instanceWorld)
            .withPosition(new Vec3d(0, 0, 0))
            .withSilent();

        server.getCommandManager().executeWithPrefix(cmdSource, "function " + function);
        PatchworkAdventures.LOGGER.info("Executed function {} in instance {}",
                                      function, instanceName);
    }

    /**
     * Removes multiple players from the keepalive list for the current instance
     * Command: /dungeon instance keepalive remove <players>
     */
    private static int executeInstanceKeepalivePlayerRemoveCommand(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();

        try {
            // Get the current world
            ServerWorld currentWorld = source.getWorld();
            if (currentWorld == null) {
                source.sendError(Text.literal("Could not determine the current world"));
                return 0;
            }

            String worldId = currentWorld.getRegistryKey().getValue().toString();

            // Check if this is an instance world
            String prefix = "pwa_dimensions:instance/";
            if (!worldId.startsWith(prefix)) {
                source.sendError(Text.literal("This command can only be used in an instance world"));
                return 0;
            }

            // Extract the instance name from the world ID
            String instanceName = worldId.substring(prefix.length());
            Collection<ServerPlayerEntity> players = EntityArgumentType.getPlayers(context, "players");

            if (players.isEmpty()) {
                source.sendError(Text.literal("No players specified"));
                return 0;
            }

            boolean instanceDeleted = false;

            for (ServerPlayerEntity player : players) {
                // Remove player from keepalive and check if instance was deleted
                UUID playerUuid = player.getUuid();
                boolean wasDeleted = removePlayerFromInstanceAndDeleteIfEmpty(
                    source.getServer(),
                    instanceName,
                    playerUuid
                );

                if (wasDeleted) {
                    instanceDeleted = true;
                    break;
                }
            }

            if (instanceDeleted) {
                source.sendFeedback(() -> Text.literal("Instance deleted as it's now empty"), true);
            }

            source.sendFeedback(() -> Text.literal("Removed " + players.size() + " player(s) from instance keepalive"), true);
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            source.sendError(Text.literal("Failed to remove players from keepalive: " + e.getMessage()));
            return 0;
        }
    }
}
