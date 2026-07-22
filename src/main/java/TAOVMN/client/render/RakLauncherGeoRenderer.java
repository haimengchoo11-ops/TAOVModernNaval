package TAOVMN.client.render;

import TAOVMN.block.entity.RakLauncherBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class RakLauncherGeoRenderer extends GeoBlockRenderer<RakLauncherBlockEntity> {
    public RakLauncherGeoRenderer(BlockEntityRendererProvider.Context context) {
        super(new RakLauncherGeoModel());
    }
}
