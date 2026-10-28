package TAOVMN.content.weapon;

import TAOVMN.content.NavalBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class NavalTurretBlock extends Block implements EntityBlock {
    public NavalTurretBlock() {
        super(BlockBehaviour.Properties.of()
                .strength(5.0F, 24.0F)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new NavalTurretBlockEntity(pos, state);
    }

    /** 仅服务器端驱动炮塔开火逻辑。 */
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        return type == NavalBlockEntities.NAVAL_TURRET.get()
                ? (lvl, pos, st, blockEntity) -> ((NavalTurretBlockEntity) blockEntity).tickServer()
                : null;
    }
}
