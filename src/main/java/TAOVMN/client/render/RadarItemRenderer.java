package TAOVMN.client.render;

import TAOVMN.item.RadarBlockItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class RadarItemRenderer extends GeoItemRenderer<RadarBlockItem> {
    public RadarItemRenderer() {
        super(new RadarItemGeoModel());
        // 修正GeckoLib物品在背包/手持GUI中默认光照偏暗/偏平的问题
        this.useAlternateGuiLighting();
    }
}
