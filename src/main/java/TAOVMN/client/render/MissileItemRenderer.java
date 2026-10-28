package TAOVMN.client.render;

import TAOVMN.item.MissileItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class MissileItemRenderer extends GeoItemRenderer<MissileItem> {
    public MissileItemRenderer() {
        super(new MissileItemGeoModel());
        this.useAlternateGuiLighting();
    }
}