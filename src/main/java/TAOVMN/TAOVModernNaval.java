package TAOVMN;

import TAOVMN.registry.ModBlocks;
import TAOVMN.registry.ModBlockEntities;
import TAOVMN.registry.ModCreativeTabs;
import TAOVMN.registry.ModEntities;
import TAOVMN.registry.ModItems;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.eventbus.api.IEventBus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.bernie.geckolib.GeckoLib;

@Mod("taovmodernnaval")
public class TAOVModernNaval {
    private static final Logger LOGGER = LogManager.getLogger();
    public static final String MODID = "taovmodernnaval";

    public TAOVModernNaval() {
        GeckoLib.initialize();

        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::setup);

        ModBlocks.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModEntities.register(modEventBus);
        ModItems.register(modEventBus);
        ModCreativeTabs.register(modEventBus);

        MinecraftForge.EVENT_BUS.register(this);
    }

    private void setup(final FMLCommonSetupEvent event) {
        LOGGER.info("Hello from Modern Forge!");
    }
}