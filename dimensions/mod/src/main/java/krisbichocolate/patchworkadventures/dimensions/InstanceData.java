package krisbichocolate.patchworkadventures.dimensions;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import krisbichocolate.patchworkadventures.dimensions.PatchworkAdventures;

import java.util.*;

/**
 * Manages instance data storage that persists across server restarts
 * using Minecraft's DataCommandStorage system
 */
public class InstanceData {
    private static final String STORAGE_NAMESPACE = "pwa_dimensions";
    private static final String INSTANCES_KEY = "instances";
    private static final String KEEPALIVE_PLAYERS_KEY = "keepalive_players";

    /**
     * Adds a player to the keepalive list for an instance
     * @param server The Minecraft server
     * @param instanceName The instance name
     * @param playerUuid The player's UUID
     */
    public static void addKeepalivePlayer(MinecraftServer server, String instanceName, UUID playerUuid) {
        NbtCompound instances = getStorage(server);

        // Get or create instance data
        NbtCompound instanceData;
        if (instances.contains(instanceName)) {
            instanceData = instances.getCompound(instanceName);
        } else {
            instanceData = new NbtCompound();
            instances.put(instanceName, instanceData);
        }

        // Get or create keepalive players list
        NbtList keepalivePlayers;
        if (instanceData.contains(KEEPALIVE_PLAYERS_KEY)) {
            keepalivePlayers = instanceData.getList(KEEPALIVE_PLAYERS_KEY, NbtElement.STRING_TYPE);
        } else {
            keepalivePlayers = new NbtList();
            instanceData.put(KEEPALIVE_PLAYERS_KEY, keepalivePlayers);
        }

        // Add player UUID if not already in the list
        String playerUuidString = playerUuid.toString();
        boolean playerExists = false;
        for (int i = 0; i < keepalivePlayers.size(); i++) {
            if (keepalivePlayers.getString(i).equals(playerUuidString)) {
                playerExists = true;
                break;
            }
        }

        if (!playerExists) {
            keepalivePlayers.add(NbtString.of(playerUuidString));
        }
    }

    /**
     * Removes a player from the keepalive list for an instance
     * @param server The Minecraft server
     * @param instanceName The instance name
     * @param playerUuid The player's UUID
     * @return true if the instance is now empty (no keepalive players), false otherwise
     */
    public static boolean removeKeepalivePlayer(MinecraftServer server, String instanceName, UUID playerUuid) {
        NbtCompound instances = getStorage(server);

        if (!instances.contains(instanceName)) {
            return true;
        }

        NbtCompound instanceData = instances.getCompound(instanceName);
        if (!instanceData.contains(KEEPALIVE_PLAYERS_KEY)) {
            return true;
        }

        NbtList keepalivePlayers = instanceData.getList(KEEPALIVE_PLAYERS_KEY, NbtElement.STRING_TYPE);
        String playerUuidString = playerUuid.toString();

        // Remove player UUID from the list
        for (int i = keepalivePlayers.size() - 1; i >= 0; i--) {
            if (keepalivePlayers.getString(i).equals(playerUuidString)) {
                keepalivePlayers.remove(i);
                break;
            }
        }

        // Check if the instance is now empty
        boolean isEmpty = keepalivePlayers.isEmpty();

        // If empty, remove the instance data
        if (isEmpty) {
            instances.remove(instanceName);
        }

        return isEmpty;
    }

    /**
     * Checks if an instance has any keepalive players
     * @param server The Minecraft server
     * @param instanceName The instance name
     * @return true if the instance has keepalive players, false otherwise
     */
    public static boolean hasKeepalivePlayers(MinecraftServer server, String instanceName) {
        NbtCompound instances = getStorage(server);

        // Check if instance exists
        if (!instances.contains(instanceName)) {
            return false;
        }

        NbtCompound instanceData = instances.getCompound(instanceName);

        // Check if keepalive players list exists and is not empty
        if (!instanceData.contains(KEEPALIVE_PLAYERS_KEY)) {
            return false;
        }

        NbtList keepalivePlayers = instanceData.getList(KEEPALIVE_PLAYERS_KEY, NbtElement.STRING_TYPE);
        return !keepalivePlayers.isEmpty();
    }

    /**
     * Gets all keepalive players for an instance
     * @param server The Minecraft server
     * @param instanceName The instance name
     * @return A collection of player UUIDs
     */
    public static Collection<UUID> getKeepalivePlayers(MinecraftServer server, String instanceName) {
        NbtCompound instances = getStorage(server);
        Collection<UUID> players = new ArrayList<>();

        // Check if instance exists
        if (!instances.contains(instanceName)) {
            return players;
        }

        NbtCompound instanceData = instances.getCompound(instanceName);

        // Check if keepalive players list exists
        if (!instanceData.contains(KEEPALIVE_PLAYERS_KEY)) {
            return players;
        }

        NbtList keepalivePlayers = instanceData.getList(KEEPALIVE_PLAYERS_KEY, NbtElement.STRING_TYPE);

        // Convert string UUIDs to UUID objects
        for (int i = 0; i < keepalivePlayers.size(); i++) {
            try {
                players.add(UUID.fromString(keepalivePlayers.getString(i)));
            } catch (IllegalArgumentException e) {
                // Skip invalid UUIDs
            }
        }

        return players;
    }

    /**
     * Gets all instance names
     * @param server The Minecraft server
     * @return A collection of instance names
     */
    public static Collection<String> getAllInstanceNames(MinecraftServer server) {
        NbtCompound instances = getStorage(server);
        return instances.getKeys();
    }

    /**
     * Adds or updates instance data
     * @param server The Minecraft server
     * @param instanceName The instance name
     * @param data The data to add or update
     */
    public static void addInstanceData(MinecraftServer server, String instanceName, NbtCompound data) {
        NbtCompound instances = getStorage(server);

        // Get or create instance data
        NbtCompound instanceData;
        if (instances.contains(instanceName)) {
            instanceData = instances.getCompound(instanceName);
        } else {
            instanceData = new NbtCompound();
            instances.put(instanceName, instanceData);
        }

        // Merge the new data with existing data
        for (String key : data.getKeys()) {
            instanceData.put(key, data.get(key));
        }
    }

    /**
     * Gets instance data
     * @param server The Minecraft server
     * @param instanceName The instance name
     * @return The instance data or an empty compound if not found
     */
    public static NbtCompound getInstanceData(MinecraftServer server, String instanceName) {
        NbtCompound instances = getStorage(server);
        return instances.getCompound(instanceName);
    }

    /**
     * Removes an instance from storage
     * @param server The Minecraft server
     * @param instanceName The instance name
     */
    public static void removeInstance(MinecraftServer server, String instanceName) {
        NbtCompound instances = getStorage(server);

        if (instances.contains(instanceName)) {
            instances.remove(instanceName);
        }
    }

    /**
     * Gets the storage NBT compound
     * @param server The Minecraft server
     * @return The storage NBT compound
     */
    private static NbtCompound getStorage(MinecraftServer server) {
        Identifier storageId = Identifier.of(STORAGE_NAMESPACE, INSTANCES_KEY);
        NbtCompound storage = server.getDataCommandStorage().get(storageId);

        return storage;
    }
}
