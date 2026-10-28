package TAOVMN.client.render;

import TAOVMN.TAOVModernNaval;
import TAOVMN.item.AK630BlockItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

// 物品渲染复用与方块完全相同的geo/贴图/动画资源，确保手持/背包/地面掉落与放置后的模型一致
@SuppressWarnings("removal")
public class AK630ItemGeoModel extends GeoModel<AK630BlockItem> {
    @Override
    public ResourceLocation getModelResource(AK630BlockItem animatable) {
        return new ResourceLocation(TAOVModernNaval.MODID, "geo/ak630.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(AK630BlockItem animatable) {
        return new ResourceLocation(TAOVModernNaval.MODID, "textures/block/ak630.png");
    }

    @Override
    public ResourceLocation getAnimationResource(AK630BlockItem animatable) {
        return new ResourceLocation(TAOVModernNaval.MODID, "animations/ak630.animation.json");
    }
}
