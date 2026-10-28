package TAOVMN.client.render;

import TAOVMN.block.entity.P1DirectorBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class P1DirectorGeoRenderer extends GeoBlockRenderer<P1DirectorBlockEntity> {
    public P1DirectorGeoRenderer(BlockEntityRendererProvider.Context context) {
        super(new P1DirectorGeoModel());
    }
}