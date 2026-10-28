package TAOVMN.client.render;

import TAOVMN.block.entity.AK630BlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class AK630BlockRenderer extends GeoBlockRenderer<AK630BlockEntity> {

    public AK630BlockRenderer(BlockEntityRendererProvider.Context context) {
        super(new AK630BlockModel());
    }
}
