package TAOVMN.content;

import TAOVMN.TAOVModernNaval;
import TAOVMN.content.radar.RadarBlock;
import TAOVMN.content.weapon.NavalLaunchBlock;
import TAOVMN.content.weapon.NavalTurretBlock;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class NavalBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, TAOVModernNaval.MODID);

    public static final RegistryObject<Block> NAVAL_TURRET = BLOCKS.register("naval_turret", NavalTurretBlock::new);
    public static final RegistryObject<Block> RADAR = BLOCKS.register("radar", RadarBlock::new);
    // 注意：旧版 registry.ModBlocks 已注册 "launch_tube"，为避免并存过渡期间同名冲突，
    // 新版骨架层改用 "naval_launch_tube"。
    public static final RegistryObject<Block> LAUNCH_TUBE = BLOCKS.register("naval_launch_tube", NavalLaunchBlock::new);

    private NavalBlocks() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
    }
}
