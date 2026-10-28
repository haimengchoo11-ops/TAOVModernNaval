package TAOVMN.client.render;

import TAOVMN.item.LauncherBlockItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class RakLauncherItemRenderer extends GeoItemRenderer<LauncherBlockItem> {
    public RakLauncherItemRenderer() {
        super(new RakLauncherItemGeoModel());
        // 修正GeckoLib物品在背包/手持GUI中默认光照偏暗/偏平的问题
        this.useAlternateGuiLighting();
    }
}
