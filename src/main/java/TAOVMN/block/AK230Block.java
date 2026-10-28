package TAOVMN.block;

import TAOVMN.block.entity.AbstractCiwsBlockEntity;
import TAOVMN.block.entity.AK230BlockEntity;
import TAOVMN.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * AK-230 舰炮方块。其它逻辑（朝向、渲染形状）见 {@link AbstractCiwsBlock}。
 */
public class AK230Block extends AbstractCiwsBlock {

    public AK230Block(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AK230BlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.AK230_BLOCK.get(), AbstractCiwsBlockEntity::tick);
    }
}
