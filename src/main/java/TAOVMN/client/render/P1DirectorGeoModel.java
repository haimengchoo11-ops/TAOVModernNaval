package TAOVMN.client.render;

import TAOVMN.TAOVModernNaval;
import TAOVMN.block.entity.P1DirectorBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

// P-1 火控指挥仪：使用自己专用的几何体与贴图（原先借用 MR800 雷达的资源，已解耦）
// 动画暂时仍指向 mr800 的（它目前是空壳，等指挥仪真的做动画时再换成 p1_director.animation.json）
@SuppressWarnings("removal")
public class P1DirectorGeoModel extends GeoModel<P1DirectorBlockEntity> {
    @Override
    public ResourceLocation getModelResource(P1DirectorBlockEntity animatable) {
        return new ResourceLocation(TAOVModernNaval.MODID, "geo/p1_director.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(P1DirectorBlockEntity animatable) {
        return new ResourceLocation(TAOVModernNaval.MODID, "textures/block/p1_director.png");
    }

    @Override
    public ResourceLocation getAnimationResource(P1DirectorBlockEntity animatable) {
        return new ResourceLocation(TAOVModernNaval.MODID, "animations/mr800.animation.json");
    }
}
