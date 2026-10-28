package TAOVMN.content;

import TAOVMN.TAOVModernNaval;
import TAOVMN.content.missile.MissileItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class NavalItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, TAOVModernNaval.MODID);

    public static final RegistryObject<Item> NAVAL_TURRET_ITEM = ITEMS.register("naval_turret",
            () -> new BlockItem(NavalBlocks.NAVAL_TURRET.get(), new Item.Properties()));

    public static final RegistryObject<Item> RADAR_ITEM = ITEMS.register("radar",
            () -> new BlockItem(NavalBlocks.RADAR.get(), new Item.Properties()));

    public static final RegistryObject<Item> LAUNCH_TUBE_ITEM = ITEMS.register("naval_launch_tube",
            () -> new BlockItem(NavalBlocks.LAUNCH_TUBE.get(), new Item.Properties()));

    public static final RegistryObject<Item> NAVAL_MISSILE = ITEMS.register("naval_missile",
            () -> new MissileItem(new Item.Properties().stacksTo(16)));

    private NavalItems() {
    }

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
