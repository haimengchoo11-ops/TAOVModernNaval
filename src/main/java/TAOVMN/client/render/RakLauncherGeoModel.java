package TAOVMN.client.render;

import TAOVMN.TAOVModernNaval;
import TAOVMN.block.entity.RakLauncherBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class RakLauncherGeoModel extends GeoModel<RakLauncherBlockEntity> {
    @Override
    public ResourceLocation getModelResource(RakLauncherBlockEntity animatable) {
        return new ResourceLocation(TAOVModernNaval.MODID, "geo/rak_launcher.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(RakLauncherBlockEntity animatable) {
        return new ResourceLocation(TAOVModernNaval.MODID, "textures/block/rak_launcher.png");
    }

    @Override
    public ResourceLocation getAnimationResource(RakLauncherBlockEntity animatable) {
        return new ResourceLocation(TAOVModernNaval.MODID, "animations/rak_launcher.animation.json");
    }
}
