package TAOVMN.client.render;

import TAOVMN.block.entity.LaunchTubeBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class LaunchTubeGeoRenderer extends GeoBlockRenderer<LaunchTubeBlockEntity> {
    public LaunchTubeGeoRenderer(BlockEntityRendererProvider.Context context) {
        super(new LaunchTubeGeoModel());
    }
}
