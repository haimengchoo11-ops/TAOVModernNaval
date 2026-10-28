package TAOVMN.content.missile;

import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.item.Item;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.function.Consumer;

public class MissileItem extends Item implements GeoItem {
    private final float maxDamage;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public MissileItem(Properties properties) {
        this(properties, 1.0F);
    }

    public MissileItem(Properties properties, float maxDamage) {
        super(properties);
        this.maxDamage = maxDamage;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private BlockEntityWithoutLevelRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    renderer = new MissileItemRenderer();
                }
                return renderer;
            }
        });
    }
}
