package TAOVMN.client.render;

import TAOVMN.item.AK230BlockItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class AK230ItemRenderer extends GeoItemRenderer<AK230BlockItem> {
    public AK230ItemRenderer() {
        super(new AK230ItemGeoModel());
        // 修正 GeckoLib 物品在背包/手持 GUI 中默认光照偏暗/偏平的问题
        this.useAlternateGuiLighting();
    }
}
