package krisbichocolate.patchworkadventures.dimensions.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import krisbichocolate.patchworkadventures.dimensions.TickScheduler;
import krisbichocolate.patchworkadventures.dimensions.component.ModComponents;
import krisbichocolate.patchworkadventures.dimensions.component.PlayerAnchorComponent;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Collection;

import static net.minecraft.server.command.CommandManager.literal;

public class ResourceWorldCommands {

    /**
     * Registers all resource world related commands
     */
    public static void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher,
                                        CommandRegistryAccess registryAccess,
                                        CommandManager.RegistrationEnvironment environment) {
        // Player facing commands
        dispatcher.register(
            literal("rw")
                .executes(ResourceWorldCommands::executeRwCommand)
        );

        dispatcher.register(
            literal("overworld")
                .executes(ResourceWorldCommands::executeOverworldCommand)
        );

        // Admin commands
        dispatcher.register(
            literal("reset_resource_world")
                .requires(source -> source.hasPermissionLevel(2))
                .executes(ResourceWorldCommands::executeResetResourceWorldCommand)
        );
    }

    /**
     * Gets the registry key for the overworld dimension
     * @param server The Minecraft server
     * @return The registry key for the overworld
     */
    public static RegistryKey<World> getOverworldKey(MinecraftServer server) {
        Identifier configId = Identifier.of("pwa_dimensions", "config");
        NbtCompound config = server.getDataCommandStorage().get(configId);
        String overworldId = config.contains("overworld") ? config.getString("overworld") : "minecraft:overworld";
        return RegistryKey.of(RegistryKeys.WORLD, Identifier.of(overworldId));
    }

    /**
     * Gets the registry key for the resource world dimension
     * @param server The Minecraft server
     * @return The registry key for the resource world
     */
    public static RegistryKey<World> getResourceWorldKey(MinecraftServer server) {
        Identifier configId = Identifier.of("pwa_dimensions", "config");
        NbtCompound config = server.getDataCommandStorage().get(configId);
        String resourceWorldId = config.contains("resourceworld") ? config.getString("resourceworld") : "pwa_dimensions:rw";
        return RegistryKey.of(RegistryKeys.WORLD, Identifier.of(resourceWorldId));
    }

    /**
     * Gets the registry key for the resource world nether dimension
     * @param server The Minecraft server
     * @return The registry key for the resource world nether
     */
    public static RegistryKey<World> getResourceWorldNetherKey(MinecraftServer server) {
        Identifier configId = Identifier.of("pwa_dimensions", "config");
        NbtCompound config = server.getDataCommandStorage().get(configId);
        String resourceWorldNetherId = config.contains("resourceworld_nether") ? config.getString("resourceworld_nether") : "pwa_dimensions:rw_nether";
        return RegistryKey.of(RegistryKeys.WORLD, Identifier.of(resourceWorldNetherId));
    }

    /**
     * Teleports a player from the resource world to the overworld
     * @param player The player to teleport
     */
    public static void teleportPlayerToOverworld(ServerPlayerEntity player) {
        RegistryKey<World> overworldKey = getOverworldKey(player.getServer());
        teleportPlayerToOverworld(player, overworldKey);
    }

    /**
     * Teleports a player from the resource world to the overworld
     * @param player The player to teleport
     * @param overworldKey The registry key for the overworld
     */
    public static void teleportPlayerToOverworld(ServerPlayerEntity player, RegistryKey<World> overworldKey) {
        // First try to teleport using the anchor system
        if (PlayerAnchorCommands.teleportPlayerToAnchor(player, ModComponents.OVERWORLD_ANCHOR_NAME)) {
            player.sendMessage(Text.literal("Teleported back to last overworld position"), false);
            return;
        }

        // If no anchor exists, teleport to overworld spawn
        ServerWorld overworld = player.getServer().getWorld(overworldKey);
        if (overworld == null) {
            player.sendMessage(Text.literal("Could not find the overworld"));
            return;
        }

        player.teleport(overworld, overworld.getSpawnPos().getX(), overworld.getSpawnPos().getY(),
                overworld.getSpawnPos().getZ(), overworld.getSpawnAngle(), 0.0f);
        player.sendMessage(Text.literal("Teleported to overworld spawn"), false);
    }

    private static int executeRwCommand(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        if (player == null) {
            source.sendError(Text.literal("This command can only be executed by a player"));
            return 0;
        }

        // Get dimension registry keys
        RegistryKey<World> overworldKey = getOverworldKey(source.getServer());
        RegistryKey<World> resourceWorldKey = getResourceWorldKey(source.getServer());
        RegistryKey<World> resourceWorldNetherKey = getResourceWorldNetherKey(source.getServer());

        // Get the current world registry key
        RegistryKey<World> currentWorldKey = player.getWorld().getRegistryKey();

        // Check if player is in either the overworld, resource world, or resource world nether
        if (currentWorldKey != overworldKey &&
            currentWorldKey != resourceWorldKey &&
            currentWorldKey != resourceWorldNetherKey) {
            source.sendError(Text.literal("You can only use this command from the overworld or resource world"));
            return 0;
        }

        // Only save the location if the player is in the overworld
        if (currentWorldKey == overworldKey) {
            // Save the player's current location to the anchor component
            PlayerAnchorComponent anchors = ModComponents.ANCHORS.get(player);
            anchors.setAnchor(ModComponents.OVERWORLD_ANCHOR_NAME,
                             player.getX(), player.getY(), player.getZ(),
                             player.getYaw(), overworldKey);
        }

        // Get the resource world dimension
        ServerWorld rwWorld = source.getServer().getWorld(resourceWorldKey);

        if (rwWorld == null) {
            source.sendError(Text.literal("The resource world dimension could not be found"));
            return 0;
        }

        try {
            // First teleport to the dimension
            player.teleport(rwWorld, 0, 100, 0, player.getYaw(), player.getPitch());

            // Then use spreadplayers logic to find a safe spot on the ground
            ServerCommandSource rwSource = source.withWorld(rwWorld).withLevel(4);
            player.getServer().getCommandManager().executeWithPrefix(rwSource, "spreadplayers ~ ~ 25 500 false @s");

            source.sendFeedback(() -> Text.literal("Teleported to a random location in resource world"), false);

            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            source.sendError(Text.literal("Failed to teleport: " + e.getMessage()));
            return 0;
        }
    }

    private static int executeOverworldCommand(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        if (player == null) {
            source.sendError(Text.literal("This command can only be executed by a player"));
            return 0;
        }

        // Get dimension registry keys
        RegistryKey<World> overworldKey = getOverworldKey(source.getServer());
        RegistryKey<World> resourceWorldKey = getResourceWorldKey(source.getServer());
        RegistryKey<World> resourceWorldNetherKey = getResourceWorldNetherKey(source.getServer());

        // Get the current world registry key
        RegistryKey<World> currentWorldKey = player.getWorld().getRegistryKey();

        // Check if player is in either the overworld, resource world, or resource world nether
        if (currentWorldKey != overworldKey &&
            currentWorldKey != resourceWorldKey &&
            currentWorldKey != resourceWorldNetherKey) {
            source.sendError(Text.literal("You can only use this command from the overworld or resource world"));
            return 0;
        }

        teleportPlayerToOverworld(player, overworldKey);
        return Command.SINGLE_SUCCESS;
    }

    public static int executeResetResourceWorldCommand(CommandContext<ServerCommandSource> context) {
        ServerCommandSource playerSource = context.getSource();
        ServerCommandSource source = playerSource.getServer().getCommandSource();

        // Get resource world registry key
        RegistryKey<World> rwDimension = getResourceWorldKey(source.getServer());
        ServerWorld rwWorld = source.getServer().getWorld(rwDimension);

        if (rwWorld == null) {
            source.sendError(Text.literal("The resource world dimension could not be found"));
            return 0;
        }

        Collection<ServerPlayerEntity> playersInRw = new ArrayList<>(rwWorld.getPlayers());
        for (ServerPlayerEntity player : playersInRw) {
            ResourceWorldCommands.teleportPlayerToOverworld(player);
        }

        try {
            String resourceWorldId = rwDimension.getValue().toString();
            String resourceWorldNetherId = getResourceWorldNetherKey(source.getServer()).getValue().toString();
            CommandManager cmd = source.getServer().getCommandManager();

            // Delete both the resource world and its nether
            cmd.executeWithPrefix(source, "mw delete " + resourceWorldId);
            cmd.executeWithPrefix(source, "mw delete " + resourceWorldNetherId);

            TickScheduler.schedule(20, () -> {
                try {
                    // Recreate both the resource world and its nether
                    cmd.executeWithPrefix(source, "mw create " + resourceWorldId + " pwa_dimensions:resource_world_preset minecraft:overworld");
                    cmd.executeWithPrefix(source, "mw create " + resourceWorldNetherId + " pwa_dimensions:resource_world_preset minecraft:the_nether");
                    source.sendFeedback(() -> Text.literal("Resource world and its nether have been reset with a new seed"), true);
                } catch (Exception e) {
                    source.sendError(Text.literal("Failed to recreate resource world dimensions: " + e.getMessage()));
                }
            });
            source.sendFeedback(() -> Text.literal("Resource world dimensions are being reset..."), true);

            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            source.sendError(Text.literal("Failed to reset resource world dimension: " + e.getMessage()));
            return 0;
        }
    }
}
