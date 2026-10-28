package TAOVMN.core;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class NavalCoreBlock extends Block {
    public NavalCoreBlock() {
        super(BlockBehaviour.Properties.of()
                .strength(4.0F, 18.0F)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops());
    }
}
