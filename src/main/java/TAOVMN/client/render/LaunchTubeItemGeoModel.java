package TAOVMN.client.render;

import TAOVMN.TAOVModernNaval;
import TAOVMN.item.LaunchTubeBlockItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

@SuppressWarnings("removal")
public class LaunchTubeItemGeoModel extends GeoModel<LaunchTubeBlockItem> {
    @Override
    public ResourceLocation getModelResource(LaunchTubeBlockItem animatable) {
        return new ResourceLocation(TAOVModernNaval.MODID, "geo/launch_tube.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(LaunchTubeBlockItem animatable) {
        return new ResourceLocation(TAOVModernNaval.MODID, "textures/block/launch_tube.png");
    }

    @Override
    public ResourceLocation getAnimationResource(LaunchTubeBlockItem animatable) {
        return new ResourceLocation(TAOVModernNaval.MODID, "animations/launch_tube.animation.json");
    }
}
