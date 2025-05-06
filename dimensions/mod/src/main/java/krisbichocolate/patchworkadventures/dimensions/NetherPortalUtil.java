package krisbichocolate.patchworkadventures.dimensions;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import java.util.Optional;

/**
 * Utility class for handling custom nether portal functionality
 */
public class NetherPortalUtil {
    private static final String CONFIG_NAMESPACE = "pwa_dimensions";
    private static final String CONFIG_KEY = "config";

    /**
     * Gets the target world for a nether portal in the given world if configured
     *
     * @param server The Minecraft server
     * @param worldKey The source world key
     * @return Optional containing the target world key if configured, empty otherwise
     */
    public static Optional<RegistryKey<World>> getNetherPortalTarget(MinecraftServer server, RegistryKey<World> worldKey) {
        // Get config data
        Identifier configId = Identifier.of(CONFIG_NAMESPACE, CONFIG_KEY);
        NbtCompound config = server.getDataCommandStorage().get(configId);

        // Check if we have a custom nether portal target
        NbtCompound worldConfig = config.getCompound("worlds").getCompound(worldKey.getValue().toString());
        if (worldConfig.contains("nether_portal_target_world")) {
            String targetWorldId = worldConfig.getString("nether_portal_target_world");
            RegistryKey<World> targetWorldKey = RegistryKey.of(RegistryKeys.WORLD, Identifier.of(targetWorldId));
            return Optional.of(targetWorldKey);
        }

        // No custom configuration found
        return Optional.empty();
    }

    /**
     * Checks if nether portals can be created in the given world based on custom configuration
     *
     * @param server The Minecraft server
     * @param worldKey The world key to check
     * @return Optional containing the portal creation status if configured, empty to use vanilla behavior
     */
    public static Optional<Boolean> canCreateNetherPortal(MinecraftServer server, RegistryKey<World> worldKey) {
        Optional<RegistryKey<World>> targetWorldKey = getNetherPortalTarget(server, worldKey);

        // No custom configuration found, use vanilla behavior
        if (targetWorldKey.isEmpty()) {
            return Optional.empty();
        }

        ServerWorld targetWorld = server.getWorld(targetWorldKey.get());
        return Optional.of(targetWorld != null);
    }
}
