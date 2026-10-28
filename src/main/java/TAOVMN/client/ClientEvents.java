package TAOVMN.client;

import TAOVMN.TAOVModernNaval;
import TAOVMN.client.render.AK230BlockRenderer;
import TAOVMN.client.render.AK630BlockRenderer;
import TAOVMN.client.render.LaunchTubeGeoRenderer;
import TAOVMN.client.render.P1DirectorGeoRenderer;
import TAOVMN.client.render.RadarGeoRenderer;
import TAOVMN.client.render.RakLauncherGeoRenderer;
import TAOVMN.content.NavalEntityTypes;
import TAOVMN.content.NavalMissileRenderer;
import TAOVMN.registry.ModBlockEntities;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TAOVModernNaval.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientEvents {
    private ClientEvents() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // 新版重写骨架层的导弹实体。必须与 TAOVModernNaval.REWRITE_LAYER_ENABLED 保持一致：
        // 该层的 DeferredRegister 未被注册时，NavalEntityTypes.NAVAL_MISSILE.get() 会直接抛异常。
        if (TAOVModernNaval.REWRITE_LAYER_ENABLED) {
            event.registerEntityRenderer(NavalEntityTypes.NAVAL_MISSILE.get(), NavalMissileRenderer::new);
        }

        // 旧版功能层的 GeckoLib 方块实体渲染器
        // （并存过渡前从未注册，导致 RAK 发射架/发射管/雷达/AK630 不显示模型）
        event.registerBlockEntityRenderer(ModBlockEntities.RAK_LAUNCHER.get(), RakLauncherGeoRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.LAUNCH_TUBE.get(), LaunchTubeGeoRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.RADAR_BLOCK.get(), RadarGeoRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.P1_DIRECTOR.get(), P1DirectorGeoRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.AK230_BLOCK.get(), AK230BlockRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.AK630_BLOCK.get(), AK630BlockRenderer::new);
    }
}
