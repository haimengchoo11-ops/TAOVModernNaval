package TAOVMN.client.render;

import TAOVMN.TAOVModernNaval;
import TAOVMN.block.entity.AK630BlockEntity;
import net.minecraft.resources.ResourceLocation;

/**
 * AK-630 近防炮模型：炮塔朝向逻辑见 {@link AbstractCiwsGeoModel}。
 */
@SuppressWarnings("removal")
public class AK630BlockModel extends AbstractCiwsGeoModel<AK630BlockEntity> {

    @Override
    public ResourceLocation getModelResource(AK630BlockEntity animatable) {
        return new ResourceLocation(TAOVModernNaval.MODID, "geo/ak630.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(AK630BlockEntity animatable) {
        return new ResourceLocation(TAOVModernNaval.MODID, "textures/block/ak630.png");
    }

    @Override
    public ResourceLocation getAnimationResource(AK630BlockEntity animatable) {
        return new ResourceLocation(TAOVModernNaval.MODID, "animations/ak630.animation.json");
    }
}
