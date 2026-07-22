package TAOVMN.block.entity;

import TAOVMN.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;
import net.minecraft.world.level.block.entity.BlockEntity;

public class MissileBlockEntity extends BlockEntity implements GeoBlockEntity {
    private final AnimatableInstanceCache geckoCache = GeckoLibUtil.createInstanceCache(this);

    public MissileBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MISSILE_BLOCK.get(), pos, state);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geckoCache;
    }
}