package TAOVMN.client.render;

import TAOVMN.TAOVModernNaval;
import TAOVMN.item.MissileItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import software.bernie.geckolib.model.GeoModel;

@SuppressWarnings("removal")
public class MissileItemGeoModel extends GeoModel<MissileItem> {
    private static final String DEFAULT_BLOCK = "p1_missile";

    private static String assetBase(MissileItem item) {
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(item);
        return MissileAssetNames.assetBase(key != null ? key.getPath() : DEFAULT_BLOCK);
    }

    @Override
    public ResourceLocation getModelResource(MissileItem item) {
        return new ResourceLocation(TAOVModernNaval.MODID, "geo/" + assetBase(item) + ".geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(MissileItem item) {
        return new ResourceLocation(TAOVModernNaval.MODID, "textures/item/" + assetBase(item) + ".png");
    }

    @Override
    public ResourceLocation getAnimationResource(MissileItem item) {
        return new ResourceLocation(TAOVModernNaval.MODID, "animations/" + assetBase(item) + ".animation.json");
    }
}