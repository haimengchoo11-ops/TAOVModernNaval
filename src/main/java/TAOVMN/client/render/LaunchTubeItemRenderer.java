package TAOVMN.client.render;

import TAOVMN.item.LaunchTubeBlockItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class LaunchTubeItemRenderer extends GeoItemRenderer<LaunchTubeBlockItem> {
    public LaunchTubeItemRenderer() {
        super(new LaunchTubeItemGeoModel());
        this.useAlternateGuiLighting();
    }
}
