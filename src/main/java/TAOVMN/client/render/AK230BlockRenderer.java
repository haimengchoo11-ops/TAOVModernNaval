package TAOVMN.client.render;

import TAOVMN.block.entity.AK230BlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class AK230BlockRenderer extends GeoBlockRenderer<AK230BlockEntity> {

    public AK230BlockRenderer(BlockEntityRendererProvider.Context context) {
        super(new AK230BlockModel());
    }
}
