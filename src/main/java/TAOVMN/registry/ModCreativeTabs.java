package TAOVMN.registry;

import TAOVMN.TAOVModernNaval;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModCreativeTabs {
    // 【核心修改】将之前的 createRegistryKey 改为 1.20.1 真正的原版方法 ResourceKey.create
    private static final ResourceKey<Registry<CreativeModeTab>> CREATIVE_MODE_TAB_KEY = 
            ResourceKey.create(ResourceKey.createRegistryKey(new ResourceLocation("minecraft", "root")), new ResourceLocation("minecraft", "creative_mode_tab"));

    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS = 
            DeferredRegister.create(CREATIVE_MODE_TAB_KEY, TAOVModernNaval.MODID);

    public static final RegistryObject<CreativeModeTab> TAOV_MODERN_NAVAL_TAB = CREATIVE_TABS.register("taovmodernnaval_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.taovmodernnaval.taovmodernnaval_tab"))
                    .icon(() -> new ItemStack(ModItems.RAK_LAUNCHER_ITEM.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.RAK_LAUNCHER_ITEM.get());
                        output.accept(ModItems.RADAR_BLOCK_ITEM.get());
                        output.accept(ModItems.ARMOR_BLOCK_ITEM.get());
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
