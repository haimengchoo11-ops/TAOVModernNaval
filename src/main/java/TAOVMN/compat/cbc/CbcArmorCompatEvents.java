package TAOVMN.compat.cbc;

import TAOVMN.TAOVModernNaval;
import TAOVMN.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import rbasamoyai.createbigcannons.munitions.AbstractCannonProjectile;

@Mod.EventBusSubscriber(modid = TAOVModernNaval.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CbcArmorCompatEvents {
    private static final double ARMOR_BREAK_THRESHOLD = 2.1D;

    private CbcArmorCompatEvents() {
    }

    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        if (event.getProjectile().level().isClientSide || !CbcCompat.isLoaded()) {
            return;
        }

        HitResult hitResult = event.getRayTraceResult();
        if (!(hitResult instanceof BlockHitResult blockHitResult)) {
            return;
        }

        if (!isCbcProjectile(event)) {
            return;
        }

        BlockPos hitPos = blockHitResult.getBlockPos();
        if (!event.getProjectile().level().getBlockState(hitPos).is(ModBlocks.ARMOR_BLOCK.get())) {
            return;
        }

        Level level = event.getProjectile().level();
        double impactStrength = estimateImpactStrength(event);
        if (impactStrength < ARMOR_BREAK_THRESHOLD) {
            event.setCanceled(true);
            event.getProjectile().discard();
            level.playSound(null, hitPos, SoundEvents.ANVIL_HIT, SoundSource.BLOCKS, 0.7F, 0.9F);
            return;
        }

        level.destroyBlock(hitPos, false);
        level.playSound(null, hitPos, SoundEvents.NETHERITE_BLOCK_BREAK, SoundSource.BLOCKS, 0.9F, 0.9F);
    }

    private static double estimateImpactStrength(ProjectileImpactEvent event) {
        double speed = event.getProjectile().getDeltaMovement().length();
        double mass = 1.0D;
        if (event.getProjectile() instanceof AbstractCannonProjectile cannonProjectile) {
            mass = Math.max(0.2D, cannonProjectile.getProjectileMass());
        }
        return speed * Math.sqrt(mass);
    }

    private static boolean isCbcProjectile(ProjectileImpactEvent event) {
        ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(event.getProjectile().getType());
        if (key == null) {
            return false;
        }
        String namespace = key.getNamespace();
        return "createbigcannons".equals(namespace) || "cbc".equals(namespace);
    }
}
