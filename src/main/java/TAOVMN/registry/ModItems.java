package TAOVMN.registry;

import TAOVMN.TAOVModernNaval;
import TAOVMN.compat.cbc.CbcCompat;
import TAOVMN.item.LauncherBlockItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, TAOVModernNaval.MODID);

    public static final RegistryObject<Item> RAK_LAUNCHER_ITEM = ITEMS.register("rak_launcher",
            () -> new LauncherBlockItem(ModBlocks.RAK_LAUNCHER.get(), new Item.Properties()));

        public static final RegistryObject<Item> RADAR_BLOCK_ITEM = ITEMS.register("radar_block",
            () -> new BlockItem(ModBlocks.RADAR_BLOCK.get(), new Item.Properties()));

        public static final RegistryObject<Item> ARMOR_BLOCK_ITEM = ITEMS.register("armor_block",
            () -> new BlockItem(ModBlocks.ARMOR_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<Item> P1_MISSILE = ITEMS.register("p1_missile",
                () -> new BlockItem(ModBlocks.P1_MISSILE_BLOCK.get(), new Item.Properties().stacksTo(64)));

        public static final RegistryObject<Item> P15_MISSILE = ITEMS.register("p15_missile",
                () -> new BlockItem(ModBlocks.P15_MISSILE_BLOCK.get(), new Item.Properties().stacksTo(64)));

        public static final RegistryObject<Item> P30_MISSILE = ITEMS.register("p30_missile",
                () -> new BlockItem(ModBlocks.P30_MISSILE_BLOCK.get(), new Item.Properties().stacksTo(64)));

        public static final RegistryObject<Item> P45_MISSILE = ITEMS.register("p45_missile",
                () -> new BlockItem(ModBlocks.P45_MISSILE_BLOCK.get(), new Item.Properties().stacksTo(64)));

        public static final RegistryObject<Item> P100_MISSILE = ITEMS.register("p100_missile",
                () -> new BlockItem(ModBlocks.P100_MISSILE_BLOCK.get(), new Item.Properties().stacksTo(64)));

        public static final RegistryObject<Item> P300_MISSILE = ITEMS.register("p300_missile",
                () -> new BlockItem(ModBlocks.P300_MISSILE_BLOCK.get(), new Item.Properties().stacksTo(64)));

        public static final RegistryObject<Item> P500_MISSILE = ITEMS.register("p500_missile",
                () -> new BlockItem(ModBlocks.P500_MISSILE_BLOCK.get(), new Item.Properties().stacksTo(64)));

        public static final RegistryObject<Item> P700_MISSILE = ITEMS.register("p700_missile",
                () -> new BlockItem(ModBlocks.P700_MISSILE_BLOCK.get(), new Item.Properties().stacksTo(64)));

        public static final List<RegistryObject<Item>> MISSILE_TYPES = List.of(
            P1_MISSILE,
            P15_MISSILE,
            P30_MISSILE,
            P45_MISSILE,
            P100_MISSILE,
            P300_MISSILE,
            P500_MISSILE,
            P700_MISSILE);

        public static boolean isMissile(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        Item item = stack.getItem();
            return MISSILE_TYPES.stream().anyMatch(reg -> reg.get() == item) || CbcCompat.isSupportedAmmo(stack);
        }

    private ModItems() {
    }

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}