package TAOVMN.client.render;

import TAOVMN.TAOVModernNaval;
import TAOVMN.item.RadarBlockItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

// 物品渲染复用与方块完全相同的geo/贴图/动画资源，确保手持/背包/地面掉落与放置后的模型一致
@SuppressWarnings("removal")
public class RadarItemGeoModel extends GeoModel<RadarBlockItem> {
    @Override
    public ResourceLocation getModelResource(RadarBlockItem animatable) {
        return new ResourceLocation(TAOVModernNaval.MODID, "geo/mr800.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(RadarBlockItem animatable) {
        return new ResourceLocation(TAOVModernNaval.MODID, "textures/block/mr800.png");
    }

    @Override
    public ResourceLocation getAnimationResource(RadarBlockItem animatable) {
        return new ResourceLocation(TAOVModernNaval.MODID, "animations/mr800.animation.json");
    }
}
