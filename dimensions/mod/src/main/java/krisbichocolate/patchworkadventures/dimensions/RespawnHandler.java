package krisbichocolate.patchworkadventures.dimensions;

import krisbichocolate.patchworkadventures.dimensions.PatchworkAdventures;
import krisbichocolate.patchworkadventures.dimensions.command.ModCommands;
import krisbichocolate.patchworkadventures.dimensions.command.ResourceWorldCommands;
import krisbichocolate.patchworkadventures.dimensions.command.PlayerAnchorCommands;
import krisbichocolate.patchworkadventures.dimensions.component.ModComponents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

import java.util.UUID;

/**
 * Handles player respawn logic based on their previous location
 */
public class RespawnHandler {

    /**
     * Initializes the respawn handler and registers event listeners
     */
    public static void init() {
        PatchworkAdventures.LOGGER.info("Initializing respawn handler");

        // Register the copy from event to handle player respawning
        ServerPlayerEvents.COPY_FROM.register(RespawnHandler::onPlayerCopyFrom);
    }

    /**
     * Handles the player copy from event which occurs during respawn
     * Determines where the player should respawn based on their previous location
     *
     * @param oldPlayer The player entity before death
     * @param newPlayer The new player entity after respawn
     * @param alive Whether the player is alive (false for death, true for dimension change)
     */
    private static void onPlayerCopyFrom(ServerPlayerEntity oldPlayer, ServerPlayerEntity newPlayer, boolean alive) {
        // If this is a dimension change, not a death, we don't need to modify respawn
        if (alive) {
            return;
        }

        MinecraftServer server = newPlayer.getServer();
        if (server == null) {
            return;
        }

        // Get the world registry keys
        RegistryKey<World> overworldKey = ResourceWorldCommands.getOverworldKey(server);
        RegistryKey<World> resourceWorldKey = ResourceWorldCommands.getResourceWorldKey(server);
        RegistryKey<World> resourceWorldNetherKey = ResourceWorldCommands.getResourceWorldNetherKey(server);

        // Get the world the player died in
        RegistryKey<World> deathWorldKey = oldPlayer.getWorld().getRegistryKey();

        // Schedule the respawn handling for the next tick to ensure it happens after vanilla respawn
        UUID playerId = newPlayer.getUuid();
        server.execute(() -> {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerId);
            if (player == null) {
                return;
            }

            String deathWorldName = deathWorldKey.getValue().toString();
            String instancePrefix = "pwa_dimensions:instance/";
            if (deathWorldName.startsWith(instancePrefix)) {
                // If player died in an instance world, delegate to datapack
                ServerWorld deathInstance = server.getWorld(deathWorldKey);
                if (deathInstance == null) {
                    return;
                }

                ServerCommandSource source = server.getCommandSource().withEntity(player).withWorld(deathInstance);
                server.getCommandManager().executeWithPrefix(source, "function pwa_dimensions:player_death_in_instance");
            } else if (deathWorldKey.equals(resourceWorldKey) || deathWorldKey.equals(resourceWorldNetherKey)) {
                // Deaths in the resource worlds respawn in the overworld
                ResourceWorldCommands.teleportPlayerToOverworld(player, overworldKey);
            }
        });
    }
}
