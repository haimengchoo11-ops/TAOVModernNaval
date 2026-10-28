package TAOVMN.client.render;

import TAOVMN.TAOVModernNaval;
import TAOVMN.block.entity.AK230BlockEntity;
import net.minecraft.resources.ResourceLocation;

/**
 * AK-230 舰炮模型：炮塔朝向逻辑见 {@link AbstractCiwsGeoModel}。
 *
 * <p>ak230.geo.json 与 AK-630 共用骨骼命名，骨骼名由 {@code AK230BlockEntity.CiwsSpec} 提供。
 */
@SuppressWarnings("removal")
public class AK230BlockModel extends AbstractCiwsGeoModel<AK230BlockEntity> {

    @Override
    public ResourceLocation getModelResource(AK230BlockEntity animatable) {
        return new ResourceLocation(TAOVModernNaval.MODID, "geo/ak230.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(AK230BlockEntity animatable) {
        return new ResourceLocation(TAOVModernNaval.MODID, "textures/block/ak230.png");
    }

    @Override
    public ResourceLocation getAnimationResource(AK230BlockEntity animatable) {
        return new ResourceLocation(TAOVModernNaval.MODID, "animations/ak230.animation.json");
    }
}
