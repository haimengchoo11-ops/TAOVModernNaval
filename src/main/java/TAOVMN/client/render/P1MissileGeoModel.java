package TAOVMN.client.render;

import TAOVMN.TAOVModernNaval;
import TAOVMN.entity.P1MissileEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class P1MissileGeoModel extends GeoModel<P1MissileEntity> {
    @Override
    public ResourceLocation getModelResource(P1MissileEntity animatable) {
        return new ResourceLocation(TAOVModernNaval.MODID, "geo/p1_missile.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(P1MissileEntity animatable) {
        return new ResourceLocation(TAOVModernNaval.MODID, "textures/item/p1_missile.png");
    }

    @Override
    public ResourceLocation getAnimationResource(P1MissileEntity animatable) {
        return new ResourceLocation(TAOVModernNaval.MODID, "animations/p1_missile.animation.json");
    }
}
