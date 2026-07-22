package TAOVMN.client;

import TAOVMN.TAOVModernNaval;
import TAOVMN.client.render.MissileBlockGeoRenderer;
import TAOVMN.client.render.P1MissileGeoRenderer;
import TAOVMN.client.render.RakLauncherGeoRenderer;
import TAOVMN.registry.ModBlockEntities;
import TAOVMN.registry.ModEntities;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = TAOVModernNaval.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientEvents {
    private ClientEvents() {
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.P1_MISSILE.get(), P1MissileGeoRenderer::new);
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            BlockEntityRenderers.register(ModBlockEntities.RAK_LAUNCHER.get(), RakLauncherGeoRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.MISSILE_BLOCK.get(), MissileBlockGeoRenderer::new);
        });
    }
}