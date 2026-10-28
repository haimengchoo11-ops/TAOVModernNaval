package TAOVMN.registry;

import java.util.List;

import TAOVMN.TAOVModernNaval;
import TAOVMN.compat.cbc.CbcCompat;
import TAOVMN.item.AK230BlockItem;
import TAOVMN.item.AK630BlockItem;
import TAOVMN.item.LauncherBlockItem;
import TAOVMN.item.MissileItem;
import TAOVMN.item.MissileFuzeItem;
import TAOVMN.item.RadarBlockItem;
import TAOVMN.util.MissileProfile;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, TAOVModernNaval.MODID);

    // Core block items
    public static final RegistryObject<Item> RAK_LAUNCHER_ITEM = ITEMS.register("rak_launcher",
            () -> new LauncherBlockItem(ModBlocks.RAK_LAUNCHER.get(), new Item.Properties()));

    public static final RegistryObject<Item> LAUNCH_TUBE_ITEM = ITEMS.register("launch_tube",
            () -> new TAOVMN.item.LaunchTubeBlockItem(ModBlocks.LAUNCH_TUBE.get(), new Item.Properties()));

    public static final RegistryObject<Item> RADAR_BLOCK_ITEM = ITEMS.register("radar_block",
            () -> new RadarBlockItem(ModBlocks.RADAR_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<Item> TOTAL_CONTROL_BLOCK_ITEM = ITEMS.register("total_control_block",
            () -> new BlockItem(ModBlocks.TOTAL_CONTROL_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<Item> P1_DIRECTOR_ITEM = ITEMS.register("p1_director",
            () -> new BlockItem(ModBlocks.P1_DIRECTOR_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<Item> ARMOR_BLOCK_ITEM = ITEMS.register("armor_block",
            () -> new BlockItem(ModBlocks.ARMOR_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<Item> AK230_BLOCK_ITEM = ITEMS.register("ak230",
            () -> new AK230BlockItem(ModBlocks.AK230_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<Item> AK630_BLOCK_ITEM = ITEMS.register("ak630",
            () -> new AK630BlockItem(ModBlocks.AK630_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<Item> MISSILE_FUZE = ITEMS.register("missile_fuze",
            () -> new MissileFuzeItem(new Item.Properties().stacksTo(64)));

    // Missile ammunition items
    public static final RegistryObject<Item> P1_MISSILE = ITEMS.register("p1_missile",
            () -> new MissileItem(new Item.Properties().stacksTo(64), MissileProfile.p1Default()));

    public static final RegistryObject<Item> P15_MISSILE = ITEMS.register("p15_missile",
            () -> new MissileItem(new Item.Properties().stacksTo(64), MissileProfile.p15()));

    public static final RegistryObject<Item> P30_MISSILE = ITEMS.register("p30_missile",
            () -> new MissileItem(new Item.Properties().stacksTo(64), MissileProfile.p30()));

    public static final RegistryObject<Item> P45_MISSILE = ITEMS.register("p45_missile",
            () -> new MissileItem(new Item.Properties().stacksTo(64), MissileProfile.p45()));

    public static final RegistryObject<Item> P100_MISSILE = ITEMS.register("p100_missile",
            () -> new MissileItem(new Item.Properties().stacksTo(64), MissileProfile.p100()));

    public static final RegistryObject<Item> P300_MISSILE = ITEMS.register("p300_missile",
            () -> new MissileItem(new Item.Properties().stacksTo(64), MissileProfile.p300()));

    public static final RegistryObject<Item> P500_MISSILE = ITEMS.register("p500_missile",
            () -> new MissileItem(new Item.Properties().stacksTo(64), MissileProfile.p500()));

    public static final RegistryObject<Item> P700_MISSILE = ITEMS.register("p700_missile",
            () -> new MissileItem(new Item.Properties().stacksTo(64), MissileProfile.p700()));

    public static final RegistryObject<Item> P120_MISSILE = ITEMS.register("p120_missile",
            () -> new MissileItem(new Item.Properties().stacksTo(64), MissileProfile.p120()));

    public static final RegistryObject<Item> P270_MISSILE = ITEMS.register("p270_missile",
            () -> new MissileItem(new Item.Properties().stacksTo(64), MissileProfile.p270()));

    public static final RegistryObject<Item> P800_MISSILE = ITEMS.register("p800_missile",
            () -> new MissileItem(new Item.Properties().stacksTo(64), MissileProfile.p800()));

    // ---- 防空弹（目标域 = ANTI_AIR）：注册名与素材库 ANTI AIR 目录一致 ----
    public static final RegistryObject<Item> S300_MISSILE = ITEMS.register("s300_missile",
            () -> new MissileItem(new Item.Properties().stacksTo(64), MissileProfile.s300()));

    public static final RegistryObject<Item> K33_MISSILE = ITEMS.register("9k33_missile",
            () -> new MissileItem(new Item.Properties().stacksTo(64), MissileProfile.nineK33()));

    public static final RegistryObject<Item> K60_MISSILE = ITEMS.register("9k60_missile",
            () -> new MissileItem(new Item.Properties().stacksTo(64), MissileProfile.nineK60()));

    public static final RegistryObject<Item> M38_MISSILE = ITEMS.register("9m38_missile",
            () -> new MissileItem(new Item.Properties().stacksTo(64), MissileProfile.nineM38()));

    public static final List<RegistryObject<Item>> MISSILE_TYPES = List.of(
            P1_MISSILE,
            P15_MISSILE,
            P30_MISSILE,
            P45_MISSILE,
            P100_MISSILE,
            P120_MISSILE,
            P270_MISSILE,
            P300_MISSILE,
            P500_MISSILE,
            P700_MISSILE,
            P800_MISSILE,
            S300_MISSILE,
            K33_MISSILE,
            K60_MISSILE,
            M38_MISSILE);

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