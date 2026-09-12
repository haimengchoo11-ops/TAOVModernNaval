package TAOVMN.client.render;

import TAOVMN.item.MissileBlockItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class MissileItemRenderer extends GeoItemRenderer<MissileBlockItem> {
    public MissileItemRenderer() {
        super(new MissileItemGeoModel());
        this.useAlternateGuiLighting();
    }
}