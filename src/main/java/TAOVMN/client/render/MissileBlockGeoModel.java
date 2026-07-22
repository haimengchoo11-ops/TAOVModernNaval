package TAOVMN.client.render;

import TAOVMN.TAOVModernNaval;
import TAOVMN.block.entity.MissileBlockEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import software.bernie.geckolib.model.GeoModel;

public class MissileBlockGeoModel extends GeoModel<MissileBlockEntity> {
    @Override
    public ResourceLocation getModelResource(MissileBlockEntity animatable) {
        return new ResourceLocation(TAOVModernNaval.MODID, "geo/p1_missile.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(MissileBlockEntity animatable) {
        ResourceLocation key = ForgeRegistries.BLOCKS.getKey(animatable.getBlockState().getBlock());
        String textureName = key != null ? key.getPath() : "p1_missile";
        return new ResourceLocation(TAOVModernNaval.MODID, "textures/item/" + textureName + ".png");
    }

    @Override
    public ResourceLocation getAnimationResource(MissileBlockEntity animatable) {
        return new ResourceLocation(TAOVModernNaval.MODID, "animations/p1_missile.animation.json");
    }
}
