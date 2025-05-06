package krisbichocolate.patchworkadventures.dimensions.component;

import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistryV3;
import org.ladysnake.cca.api.v3.entity.EntityComponentFactoryRegistry;
import org.ladysnake.cca.api.v3.entity.EntityComponentInitializer;
import org.ladysnake.cca.api.v3.entity.RespawnCopyStrategy;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import krisbichocolate.patchworkadventures.dimensions.PatchworkAdventures;

public class ModComponents implements EntityComponentInitializer {
    // Unified anchor component
    public static final ComponentKey<PlayerAnchorComponent> ANCHORS =
            ComponentRegistryV3.INSTANCE.getOrCreate(
                    Identifier.of("pwa_dimensions", "anchors"),
                    PlayerAnchorComponent.class);

    // Constants for standard anchor names
    public static final String OVERWORLD_ANCHOR_NAME = "overworld";
    public static final String INSTANCE_EXIT_NAME = "instance_exit";

    @Override
    public void registerEntityComponentFactories(EntityComponentFactoryRegistry registry) {
        // Register anchor component
        registry.registerForPlayers(ANCHORS,
                PlayerAnchorComponent::new,
                RespawnCopyStrategy.CHARACTER);
    }
}
