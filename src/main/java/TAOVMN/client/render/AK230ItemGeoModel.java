package TAOVMN.client.render;

import TAOVMN.TAOVModernNaval;
import TAOVMN.item.AK230BlockItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

// 物品渲染复用与方块完全相同的 geo/贴图/动画资源
@SuppressWarnings("removal")
public class AK230ItemGeoModel extends GeoModel<AK230BlockItem> {
    @Override
    public ResourceLocation getModelResource(AK230BlockItem animatable) {
        return new ResourceLocation(TAOVModernNaval.MODID, "geo/ak230.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(AK230BlockItem animatable) {
        return new ResourceLocation(TAOVModernNaval.MODID, "textures/block/ak230.png");
    }

    @Override
    public ResourceLocation getAnimationResource(AK230BlockItem animatable) {
        return new ResourceLocation(TAOVModernNaval.MODID, "animations/ak230.animation.json");
    }
}
