package TAOVMN.registry;

import TAOVMN.TAOVModernNaval;
import TAOVMN.block.ArmorBlock;
import TAOVMN.block.MissileBlock;
import TAOVMN.block.RadarBlock;
import TAOVMN.block.RakLauncherBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, TAOVModernNaval.MODID);

    public static final RegistryObject<Block> RAK_LAUNCHER = BLOCKS.register("rak_launcher",
            () -> new RakLauncherBlock(BlockBehaviour.Properties.of().strength(3.5F).sound(SoundType.METAL).requiresCorrectToolForDrops()));

        public static final RegistryObject<Block> RADAR_BLOCK = BLOCKS.register("radar_block",
            () -> new RadarBlock(BlockBehaviour.Properties.of().strength(3.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()));

        public static final RegistryObject<Block> ARMOR_BLOCK = BLOCKS.register("armor_block",
            () -> new ArmorBlock(BlockBehaviour.Properties.of().strength(30.0F, 128.0F).sound(SoundType.NETHERITE_BLOCK).requiresCorrectToolForDrops()));

            public static final RegistryObject<Block> P1_MISSILE_BLOCK = BLOCKS.register("p1_missile",
                () -> new MissileBlock(BlockBehaviour.Properties.of().strength(1.0F).sound(SoundType.METAL).noOcclusion()));

            public static final RegistryObject<Block> P15_MISSILE_BLOCK = BLOCKS.register("p15_missile",
                () -> new MissileBlock(BlockBehaviour.Properties.of().strength(1.0F).sound(SoundType.METAL).noOcclusion()));

            public static final RegistryObject<Block> P30_MISSILE_BLOCK = BLOCKS.register("p30_missile",
                () -> new MissileBlock(BlockBehaviour.Properties.of().strength(1.0F).sound(SoundType.METAL).noOcclusion()));

            public static final RegistryObject<Block> P45_MISSILE_BLOCK = BLOCKS.register("p45_missile",
                () -> new MissileBlock(BlockBehaviour.Properties.of().strength(1.0F).sound(SoundType.METAL).noOcclusion()));

            public static final RegistryObject<Block> P100_MISSILE_BLOCK = BLOCKS.register("p100_missile",
                () -> new MissileBlock(BlockBehaviour.Properties.of().strength(1.0F).sound(SoundType.METAL).noOcclusion()));

            public static final RegistryObject<Block> P300_MISSILE_BLOCK = BLOCKS.register("p300_missile",
                () -> new MissileBlock(BlockBehaviour.Properties.of().strength(1.0F).sound(SoundType.METAL).noOcclusion()));

            public static final RegistryObject<Block> P500_MISSILE_BLOCK = BLOCKS.register("p500_missile",
                () -> new MissileBlock(BlockBehaviour.Properties.of().strength(1.0F).sound(SoundType.METAL).noOcclusion()));

            public static final RegistryObject<Block> P700_MISSILE_BLOCK = BLOCKS.register("p700_missile",
                () -> new MissileBlock(BlockBehaviour.Properties.of().strength(1.0F).sound(SoundType.METAL).noOcclusion()));

    private ModBlocks() {
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}