package TAOVMN.registry;

import TAOVMN.TAOVModernNaval;
import TAOVMN.block.ArmorBlock;
import TAOVMN.block.RadarBlock;
import TAOVMN.block.RakLauncherBlock;
import TAOVMN.block.P1DirectorBlock;
import TAOVMN.block.AK230Block;
import TAOVMN.block.AK630Block;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, TAOVModernNaval.MODID);

    // Core blocks
    public static final RegistryObject<Block> RAK_LAUNCHER = BLOCKS.register("rak_launcher",
            () -> new RakLauncherBlock(BlockBehaviour.Properties.of().strength(3.5F).sound(SoundType.METAL).requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> LAUNCH_TUBE = BLOCKS.register("launch_tube",
            () -> new TAOVMN.block.LaunchTubeBlock(BlockBehaviour.Properties.of().strength(3.5F).sound(SoundType.METAL).requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> RADAR_BLOCK = BLOCKS.register("radar_block",
            () -> new RadarBlock(BlockBehaviour.Properties.of().strength(3.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> TOTAL_CONTROL_BLOCK = BLOCKS.register("total_control_block",
            () -> new TAOVMN.block.TotalControlBlock(BlockBehaviour.Properties.of().strength(4.5F).sound(SoundType.METAL).requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> ARMOR_BLOCK = BLOCKS.register("armor_block",
            () -> new ArmorBlock(BlockBehaviour.Properties.of().strength(30.0F, 128.0F).sound(SoundType.NETHERITE_BLOCK).requiresCorrectToolForDrops()));

    // Fire control
    public static final RegistryObject<Block> P1_DIRECTOR_BLOCK = BLOCKS.register("p1_director",
            () -> new P1DirectorBlock(BlockBehaviour.Properties.of().strength(3.5F).sound(SoundType.METAL).requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> AK230_BLOCK = BLOCKS.register("ak230_block",
            () -> new AK230Block(BlockBehaviour.Properties.of().strength(3.5F).sound(SoundType.METAL).requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> AK630_BLOCK = BLOCKS.register("ak630_block",
            () -> new AK630Block(BlockBehaviour.Properties.of().strength(3.5F).sound(SoundType.METAL).requiresCorrectToolForDrops()));
    private ModBlocks() {
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}