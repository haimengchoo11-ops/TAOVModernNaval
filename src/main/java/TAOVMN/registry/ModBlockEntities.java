package TAOVMN.registry;

import TAOVMN.TAOVModernNaval;
import TAOVMN.block.entity.RakLauncherBlockEntity;
import TAOVMN.block.entity.RadarBlockEntity;
import TAOVMN.block.entity.AK630BlockEntity;    
import TAOVMN.block.entity.AK230BlockEntity;
import TAOVMN.block.entity.P1DirectorBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, TAOVModernNaval.MODID);

    // Utility blocks
    public static final RegistryObject<BlockEntityType<RakLauncherBlockEntity>> RAK_LAUNCHER = BLOCK_ENTITY_TYPES.register("rak_launcher",
            () -> BlockEntityType.Builder.of(RakLauncherBlockEntity::new, ModBlocks.RAK_LAUNCHER.get()).build(null));

    public static final RegistryObject<BlockEntityType<TAOVMN.block.entity.LaunchTubeBlockEntity>> LAUNCH_TUBE = BLOCK_ENTITY_TYPES.register("launch_tube",
            () -> BlockEntityType.Builder.of(TAOVMN.block.entity.LaunchTubeBlockEntity::new, ModBlocks.LAUNCH_TUBE.get()).build(null));

    public static final RegistryObject<BlockEntityType<RadarBlockEntity>> RADAR_BLOCK = BLOCK_ENTITY_TYPES.register("radar_block",
        () -> BlockEntityType.Builder.of(RadarBlockEntity::new, ModBlocks.RADAR_BLOCK.get()).build(null));

    public static final RegistryObject<BlockEntityType<TAOVMN.block.entity.TotalControlBlockEntity>> TOTAL_CONTROL_BLOCK = BLOCK_ENTITY_TYPES.register("total_control_block",
            () -> BlockEntityType.Builder.of(TAOVMN.block.entity.TotalControlBlockEntity::new, ModBlocks.TOTAL_CONTROL_BLOCK.get()).build(null));

    // Fire control
    public static final RegistryObject<BlockEntityType<P1DirectorBlockEntity>> P1_DIRECTOR = BLOCK_ENTITY_TYPES.register("p1_director",
            () -> BlockEntityType.Builder.of(P1DirectorBlockEntity::new, ModBlocks.P1_DIRECTOR_BLOCK.get()).build(null));
    public static final RegistryObject<BlockEntityType<AK230BlockEntity>> AK230_BLOCK = BLOCK_ENTITY_TYPES.register("ak230_block",
            () -> BlockEntityType.Builder.of(AK230BlockEntity::new, ModBlocks.AK230_BLOCK.get()).build(null));
    public static final RegistryObject<BlockEntityType<AK630BlockEntity>> AK630_BLOCK = BLOCK_ENTITY_TYPES.register("ak630_block",
            () -> BlockEntityType.Builder.of(AK630BlockEntity::new, ModBlocks.AK630_BLOCK.get()).build(null));            
    private ModBlockEntities() {
    }

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITY_TYPES.register(eventBus);
    }
}