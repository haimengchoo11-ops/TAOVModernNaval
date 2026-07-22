package TAOVMN.client.render;

import TAOVMN.block.entity.MissileBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class MissileBlockGeoRenderer extends GeoBlockRenderer<MissileBlockEntity> {
    public MissileBlockGeoRenderer(BlockEntityRendererProvider.Context context) {
        super(new MissileBlockGeoModel());
    }
}
