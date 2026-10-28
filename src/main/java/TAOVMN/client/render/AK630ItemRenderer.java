package TAOVMN.client.render;

import TAOVMN.item.AK630BlockItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class AK630ItemRenderer extends GeoItemRenderer<AK630BlockItem> {
    public AK630ItemRenderer() {
        super(new AK630ItemGeoModel());
        // 修正GeckoLib物品在背包/手持GUI中默认光照偏暗/偏平的问题
        this.useAlternateGuiLighting();
    }
}
