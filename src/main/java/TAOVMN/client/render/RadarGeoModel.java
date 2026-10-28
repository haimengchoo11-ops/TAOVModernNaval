package TAOVMN.client.render;

import TAOVMN.TAOVModernNaval;
import TAOVMN.block.entity.RadarBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

@SuppressWarnings("removal")
public class RadarGeoModel extends GeoModel<RadarBlockEntity> {
    @Override
    public ResourceLocation getModelResource(RadarBlockEntity animatable) {
        return new ResourceLocation(TAOVModernNaval.MODID, "geo/mr800.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(RadarBlockEntity animatable) {
        return new ResourceLocation(TAOVModernNaval.MODID, "textures/block/mr800.png");
    }

    @Override
    public ResourceLocation getAnimationResource(RadarBlockEntity animatable) {
        return new ResourceLocation(TAOVModernNaval.MODID, "animations/mr800.animation.json");
    }
}