package krisbichocolate.patchworkadventures.dimensions.mixin;

import krisbichocolate.patchworkadventures.dimensions.PatchworkAdventures;
import krisbichocolate.patchworkadventures.dimensions.NetherPortalUtil;
import net.minecraft.block.AbstractFireBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Optional;

@Mixin(AbstractFireBlock.class)
public class AbstractFireBlockMixin {

    @Redirect(
            method = {
                    "onBlockAdded",
                    "shouldLightPortalAt"
            },
            at = @At(
                    value   = "INVOKE",
                    target  = "Lnet/minecraft/block/AbstractFireBlock;" +
                            "isOverworldOrNether(Lnet/minecraft/world/World;)Z"
            )
    )
    private static boolean pwa_dimensions$customPortalDimensionCheck(World world) {
        // Only proceed for server worlds
        if (!(world instanceof ServerWorld serverWorld)) {
            return pwa_dimensions$isOverworldOrNether(world);
        }

        // Check if we have a custom configuration for this world
        Optional<Boolean> canCreatePortal = NetherPortalUtil.canCreateNetherPortal(
                serverWorld.getServer(),
                world.getRegistryKey()
        );

        if (canCreatePortal.isPresent()) {
            return canCreatePortal.get();
        } else {
            return pwa_dimensions$isOverworldOrNether(world);
        }
    }

    // Vanilla impl
    private static boolean pwa_dimensions$isOverworldOrNether(World world) {
        return world.getRegistryKey() == World.OVERWORLD || world.getRegistryKey() == World.NETHER;
    }
}
