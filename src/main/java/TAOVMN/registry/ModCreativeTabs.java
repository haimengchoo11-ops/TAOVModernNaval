package TAOVMN.registry;

import TAOVMN.TAOVModernNaval;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS = 
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, TAOVModernNaval.MODID);

    public static final RegistryObject<CreativeModeTab> TAOV_MODERN_NAVAL_TAB = CREATIVE_TABS.register("taovmodernnaval_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.taovmodernnaval.taovmodernnaval_tab"))
                    .icon(() -> new ItemStack(ModItems.RAK_LAUNCHER_ITEM.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.RAK_LAUNCHER_ITEM.get());
                        output.accept(ModItems.LAUNCH_TUBE_ITEM.get());
                        output.accept(ModItems.RADAR_BLOCK_ITEM.get());
                        output.accept(ModItems.TOTAL_CONTROL_BLOCK_ITEM.get());
                        output.accept(ModItems.P1_DIRECTOR_ITEM.get());
                        output.accept(ModItems.ARMOR_BLOCK_ITEM.get());
                        output.accept(ModItems.AK230_BLOCK_ITEM.get());
                        output.accept(ModItems.AK630_BLOCK_ITEM.get());
                        output.accept(ModItems.MISSILE_FUZE.get());
                        for (RegistryObject<net.minecraft.world.item.Item> missile : ModItems.MISSILE_TYPES) {
                            output.accept(missile.get());
                        }
                    })
                    .build());

    private ModCreativeTabs() {
    }

    public static void register(IEventBus eventBus) {
        CREATIVE_TABS.register(eventBus);
    }

}
