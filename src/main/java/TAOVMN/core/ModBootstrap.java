package TAOVMN.core;

import TAOVMN.TAOVModernNaval;
import TAOVMN.content.NavalBlockEntities;
import TAOVMN.content.NavalBlocks;
import TAOVMN.content.NavalEntityTypes;
import TAOVMN.content.NavalItems;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBootstrap {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, TAOVModernNaval.MODID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, TAOVModernNaval.MODID);

    public static final RegistryObject<Block> NAVAL_CORE_BLOCK = BLOCKS.register("naval_core_block", NavalCoreBlock::new);
    public static final RegistryObject<Item> NAVAL_CORE_ITEM = ITEMS.register("naval_core_block", () -> new BlockItem(NAVAL_CORE_BLOCK.get(), new Item.Properties()));

    private ModBootstrap() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        NavalBlocks.register(modEventBus);
        NavalItems.register(modEventBus);
        NavalBlockEntities.register(modEventBus);
        NavalEntityTypes.register(modEventBus);
    }
}
