package TAOVMN.content;

import TAOVMN.TAOVModernNaval;
import TAOVMN.content.radar.RadarBlockEntity;
import TAOVMN.content.weapon.NavalLaunchBlockEntity;
import TAOVMN.content.weapon.NavalTurretBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class NavalBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, TAOVModernNaval.MODID);

    public static final RegistryObject<BlockEntityType<NavalTurretBlockEntity>> NAVAL_TURRET = BLOCK_ENTITIES.register("naval_turret",
            () -> BlockEntityType.Builder.of(NavalTurretBlockEntity::new, NavalBlocks.NAVAL_TURRET.get()).build(null));

    public static final RegistryObject<BlockEntityType<RadarBlockEntity>> RADAR = BLOCK_ENTITIES.register("radar",
            () -> BlockEntityType.Builder.of(RadarBlockEntity::new, NavalBlocks.RADAR.get()).build(null));

    public static final RegistryObject<BlockEntityType<NavalLaunchBlockEntity>> NAVAL_LAUNCH = BLOCK_ENTITIES.register("naval_launch_tube",
            () -> BlockEntityType.Builder.of(NavalLaunchBlockEntity::new, NavalBlocks.LAUNCH_TUBE.get()).build(null));

    private NavalBlockEntities() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCK_ENTITIES.register(modEventBus);
    }
}
