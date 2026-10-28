package TAOVMN.client.render;

import TAOVMN.block.entity.RadarBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class RadarGeoRenderer extends GeoBlockRenderer<RadarBlockEntity> {
    public RadarGeoRenderer(BlockEntityRendererProvider.Context context) {
        super(new RadarGeoModel());
    }
}