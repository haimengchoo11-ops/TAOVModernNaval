package TAOVMN.client.render;

import TAOVMN.TAOVModernNaval;
import TAOVMN.block.entity.LaunchTubeBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

@SuppressWarnings("removal")
public class LaunchTubeGeoModel extends GeoModel<LaunchTubeBlockEntity> {
    @Override
    public ResourceLocation getModelResource(LaunchTubeBlockEntity animatable) {
        return new ResourceLocation(TAOVModernNaval.MODID, "geo/launch_tube.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(LaunchTubeBlockEntity animatable) {
        return new ResourceLocation(TAOVModernNaval.MODID, "textures/block/launch_tube.png");
    }

    @Override
    public ResourceLocation getAnimationResource(LaunchTubeBlockEntity animatable) {
        return new ResourceLocation(TAOVModernNaval.MODID, "animations/launch_tube.animation.json");
    }
}
