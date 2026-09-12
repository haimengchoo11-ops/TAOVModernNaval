package TAOVMN.client.render;

import TAOVMN.TAOVModernNaval;
import TAOVMN.item.MissileBlockItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraftforge.registries.ForgeRegistries;
import software.bernie.geckolib.model.GeoModel;

public class MissileItemGeoModel extends GeoModel<MissileBlockItem> {
    private String blockName(MissileBlockItem item) {
        ResourceLocation key = ForgeRegistries.BLOCKS.getKey(((BlockItem) item).getBlock());
        return key != null ? key.getPath() : "p1_missile";
    }

    @Override
    public ResourceLocation getModelResource(MissileBlockItem item) {
        return new ResourceLocation(TAOVModernNaval.MODID, "geo/" + blockName(item) + ".geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(MissileBlockItem item) {
        String name = blockName(item);
        if ("p700_missile".equals(name)) {
            name = "p800_missile";
        }
        return new ResourceLocation(TAOVModernNaval.MODID, "textures/item/" + name + ".png");
    }

    @Override
    public ResourceLocation getAnimationResource(MissileBlockItem item) {
        return new ResourceLocation(TAOVModernNaval.MODID, "animations/" + blockName(item) + ".animation.json");
    }
}