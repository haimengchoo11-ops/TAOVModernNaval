package TAOVMN.registry;

import TAOVMN.TAOVModernNaval;
import TAOVMN.block.entity.MissileBlockEntity;
import TAOVMN.block.entity.RakLauncherBlockEntity;
import TAOVMN.block.entity.RadarBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, TAOVModernNaval.MODID);

    public static final RegistryObject<BlockEntityType<RakLauncherBlockEntity>> RAK_LAUNCHER = BLOCK_ENTITY_TYPES.register("rak_launcher",
            () -> BlockEntityType.Builder.of(RakLauncherBlockEntity::new, ModBlocks.RAK_LAUNCHER.get()).build(null));

        public static final RegistryObject<BlockEntityType<RadarBlockEntity>> RADAR_BLOCK = BLOCK_ENTITY_TYPES.register("radar_block",
            () -> BlockEntityType.Builder.of(RadarBlockEntity::new, ModBlocks.RADAR_BLOCK.get()).build(null));

    public static final RegistryObject<BlockEntityType<MissileBlockEntity>> MISSILE_BLOCK = BLOCK_ENTITY_TYPES.register("missile_block",
            () -> BlockEntityType.Builder.of(MissileBlockEntity::new,
                    ModBlocks.P1_MISSILE_BLOCK.get(),
                    ModBlocks.P15_MISSILE_BLOCK.get(),
                    ModBlocks.P30_MISSILE_BLOCK.get(),
                    ModBlocks.P45_MISSILE_BLOCK.get(),
                    ModBlocks.P100_MISSILE_BLOCK.get(),
                    ModBlocks.P300_MISSILE_BLOCK.get(),
                    ModBlocks.P500_MISSILE_BLOCK.get(),
                    ModBlocks.P700_MISSILE_BLOCK.get()).build(null));

    private ModBlockEntities() {
    }

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITY_TYPES.register(eventBus);
    }
}