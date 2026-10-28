package TAOVMN.client.render;

import TAOVMN.TAOVModernNaval;
import TAOVMN.item.LauncherBlockItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

// 物品渲染复用与方块完全相同的geo/贴图/动画资源，确保手持/背包/地面掉落与放置后的模型一致
@SuppressWarnings("removal")
public class RakLauncherItemGeoModel extends GeoModel<LauncherBlockItem> {
    @Override
    public ResourceLocation getModelResource(LauncherBlockItem animatable) {
        return new ResourceLocation(TAOVModernNaval.MODID, "geo/rak_launcher.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(LauncherBlockItem animatable) {
        return new ResourceLocation(TAOVModernNaval.MODID, "textures/block/rak_launcher.png");
    }

    @Override
    public ResourceLocation getAnimationResource(LauncherBlockItem animatable) {
        return new ResourceLocation(TAOVModernNaval.MODID, "animations/rak_launcher.animation.json");
    }
}
