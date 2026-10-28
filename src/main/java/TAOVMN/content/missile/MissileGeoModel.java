package TAOVMN.content.missile;

import TAOVMN.TAOVModernNaval;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;
import software.bernie.geckolib.model.GeoModel;

public class MissileGeoModel extends GeoModel<MissileItem> {
    private static final String DEFAULT_NAME = "naval_missile";

    private static String assetBase(Item item) {
        if (item == null) {
            return DEFAULT_NAME;
        }

        ResourceLocation key = ForgeRegistries.ITEMS.getKey(item);
        String path = key != null ? key.getPath() : DEFAULT_NAME;
        return path.equals("naval_missile") ? "naval_missile" : path;
    }

    @Override
    public ResourceLocation getModelResource(MissileItem animatable) {
        return new ResourceLocation(TAOVModernNaval.MODID, "geo/" + assetBase(animatable) + ".geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(MissileItem animatable) {
        return new ResourceLocation(TAOVModernNaval.MODID, "textures/item/" + assetBase(animatable) + ".png");
    }

    @Override
    public ResourceLocation getAnimationResource(MissileItem animatable) {
        return new ResourceLocation(TAOVModernNaval.MODID, "animations/" + assetBase(animatable) + ".animation.json");
    }
}
