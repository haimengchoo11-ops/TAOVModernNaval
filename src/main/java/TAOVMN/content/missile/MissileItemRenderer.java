package TAOVMN.content.missile;

import software.bernie.geckolib.renderer.GeoItemRenderer;

public class MissileItemRenderer extends GeoItemRenderer<MissileItem> {
    public MissileItemRenderer() {
        super(new MissileGeoModel());
        this.useAlternateGuiLighting();
    }
}
