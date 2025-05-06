package krisbichocolate.patchworkadventures.dimensions.mixin;

import krisbichocolate.patchworkadventures.dimensions.NetherPortalUtil;
import net.minecraft.block.NetherPortalBlock;
import net.minecraft.entity.Entity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.TeleportTarget;
import net.minecraft.world.World;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(NetherPortalBlock.class)
public abstract class NetherPortalBlockMixin {

    @Shadow
    public abstract TeleportTarget getOrCreateExitPortalTarget(ServerWorld destination, Entity entity, BlockPos portalPos, BlockPos destPos, boolean destIsNether, WorldBorder worldBorder);

    @Inject(method = "createTeleportTarget", at = @At("HEAD"), cancellable = true)
    private void onCreateTeleportTarget(ServerWorld world, Entity entity, BlockPos pos, CallbackInfoReturnable<TeleportTarget> cir) {
        MinecraftServer server = world.getServer();
        RegistryKey<World> worldKey = world.getRegistryKey();

        // Get the target world for this portal if configured
        Optional<RegistryKey<World>> targetWorldKeyOpt = NetherPortalUtil.getNetherPortalTarget(server, worldKey);

        // If no custom target is configured, let vanilla handle it
        if (targetWorldKeyOpt.isEmpty()) {
            return;
        }

        RegistryKey<World> targetWorldKey = targetWorldKeyOpt.get();

        // Get the target world
        ServerWorld targetWorld = server.getWorld(targetWorldKey);
        if (targetWorld == null) {
            cir.setReturnValue(null);
            return;
        }

        // Target world exists, create teleport target similar to vanilla
        WorldBorder worldBorder = targetWorld.getWorldBorder();
        double scaleFactor = DimensionType.getCoordinateScaleFactor(world.getDimension(), targetWorld.getDimension());

        // Determine if destination is nether-like based on scale factor
        // If scale factor is close to 1/8 (0.125), it's nether-like
        boolean destIsNether = Math.abs(scaleFactor - 0.125) < 0.01;

        BlockPos destPos = worldBorder.clamp(entity.getX() * scaleFactor, entity.getY(), entity.getZ() * scaleFactor);

        // Use the shadowed method to get or create the exit portal
        TeleportTarget target = this.getOrCreateExitPortalTarget(
            targetWorld, entity, pos, destPos, destIsNether, worldBorder);

        cir.setReturnValue(target);
    }
}
