package krisbichocolate.patchworkadventures.dimensions.component;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.ladysnake.cca.api.v3.component.ComponentV3;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Component that stores named anchor points for a player
 * Each anchor contains a position, yaw, and world key
 */
public class PlayerAnchorComponent implements ComponentV3 {
    private static final String KEY_POSITION_X = "x";
    private static final String KEY_POSITION_Y = "y";
    private static final String KEY_POSITION_Z = "z";
    private static final String KEY_YAW = "yaw";
    private static final String KEY_WORLD_ID = "world_id";

    private final Object provider;
    private final Map<String, AnchorData> anchors = new HashMap<>();

    public PlayerAnchorComponent(Object provider) {
        this.provider = provider;
    }

    /**
     * Gets the position for a named anchor
     * @param name The anchor name
     * @return The position or null if the anchor doesn't exist
     */
    public Vec3d getPosition(String name) {
        AnchorData data = anchors.get(name);
        return data != null ? new Vec3d(data.x, data.y, data.z) : null;
    }

    /**
     * Gets the world key for a named anchor
     * @param name The anchor name
     * @return The world key or null if the anchor doesn't exist
     */
    public RegistryKey<World> getWorldKey(String name) {
        AnchorData data = anchors.get(name);
        return data != null ? data.worldKey : null;
    }

    /**
     * Gets the yaw for a named anchor
     * @param name The anchor name
     * @return The yaw or 0 if the anchor doesn't exist
     */
    public float getYaw(String name) {
        AnchorData data = anchors.get(name);
        return data != null ? data.yaw : 0.0f;
    }

    /**
     * Sets a named anchor with position, yaw, and world key
     * @param name The anchor name
     * @param x The x coordinate
     * @param y The y coordinate
     * @param z The z coordinate
     * @param yaw The yaw angle
     * @param worldKey The world key
     */
    public void setAnchor(String name, double x, double y, double z, float yaw, RegistryKey<World> worldKey) {
        anchors.put(name, new AnchorData(x, y, z, yaw, worldKey));
    }

    /**
     * Checks if a named anchor exists
     * @param name The anchor name
     * @return true if the anchor exists, false otherwise
     */
    public boolean hasAnchor(String name) {
        return anchors.containsKey(name);
    }

    /**
     * Removes a named anchor
     * @param name The anchor name
     */
    public void removeAnchor(String name) {
        anchors.remove(name);
    }

    /**
     * Gets all anchor names
     * @return A set of anchor names
     */
    public Set<String> getAnchorNames() {
        return anchors.keySet();
    }

    @Override
    public void readFromNbt(NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        anchors.clear();

        for (String name : tag.getKeys()) {
            if (tag.contains(name, NbtElement.COMPOUND_TYPE)) {
                NbtCompound anchorTag = tag.getCompound(name);

                if (anchorTag.contains(KEY_POSITION_X) &&
                    anchorTag.contains(KEY_POSITION_Y) &&
                    anchorTag.contains(KEY_POSITION_Z) &&
                    anchorTag.contains(KEY_WORLD_ID)) {

                    double x = anchorTag.getDouble(KEY_POSITION_X);
                    double y = anchorTag.getDouble(KEY_POSITION_Y);
                    double z = anchorTag.getDouble(KEY_POSITION_Z);
                    float yaw = anchorTag.contains(KEY_YAW) ? anchorTag.getFloat(KEY_YAW) : 0.0f;
                    String worldId = anchorTag.getString(KEY_WORLD_ID);

                    RegistryKey<World> worldKey = RegistryKey.of(RegistryKeys.WORLD, Identifier.of(worldId));
                    anchors.put(name, new AnchorData(x, y, z, yaw, worldKey));
                }
            }
        }
    }

    @Override
    public void writeToNbt(NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        for (Map.Entry<String, AnchorData> entry : anchors.entrySet()) {
            String name = entry.getKey();
            AnchorData data = entry.getValue();

            NbtCompound anchorTag = new NbtCompound();
            anchorTag.putDouble(KEY_POSITION_X, data.x);
            anchorTag.putDouble(KEY_POSITION_Y, data.y);
            anchorTag.putDouble(KEY_POSITION_Z, data.z);
            anchorTag.putFloat(KEY_YAW, data.yaw);
            anchorTag.putString(KEY_WORLD_ID, data.worldKey.getValue().toString());

            tag.put(name, anchorTag);
        }
    }

    /**
     * Internal class to store anchor data
     */
    private static class AnchorData {
        final double x;
        final double y;
        final double z;
        final float yaw;
        final RegistryKey<World> worldKey;

        AnchorData(double x, double y, double z, float yaw, RegistryKey<World> worldKey) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.worldKey = worldKey;
        }
    }
}
