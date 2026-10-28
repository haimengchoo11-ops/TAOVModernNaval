package TAOVMN.core;

import TAOVMN.content.NavalItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class NavalCoreCreativeTab {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, TAOVMN.TAOVModernNaval.MODID);

    public static final RegistryObject<CreativeModeTab> TAOV_NAVAL_TAB = CREATIVE_TABS.register("taov_naval_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.taovmodernnaval.taov_naval_tab"))
                    .icon(() -> new ItemStack(ModBootstrap.NAVAL_CORE_ITEM.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(ModBootstrap.NAVAL_CORE_ITEM.get());
                        output.accept(NavalItems.NAVAL_TURRET_ITEM.get());
                        output.accept(NavalItems.RADAR_ITEM.get());
                        output.accept(NavalItems.LAUNCH_TUBE_ITEM.get());
                        output.accept(NavalItems.NAVAL_MISSILE.get());
                    })
                    .build());

    private NavalCoreCreativeTab() {
    }

    public static void register(IEventBus modEventBus) {
        CREATIVE_TABS.register(modEventBus);
    }
}
