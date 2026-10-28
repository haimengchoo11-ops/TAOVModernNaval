package TAOVMN.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import TAOVMN.util.OwnerTracked;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import org.jetbrains.annotations.Nullable;

/**
 * 近防炮 / 舰炮方块的公共部分：水平朝向属性、放置时面向玩家、GeckoLib 渲染形状。
 *
 * <p>AK-630 与 AK-230 原本是两份逐字复制的方块类，只有 {@code newBlockEntity} 与
 * ticker 里绑定的方块实体类型不同，因此把其余部分提到这里。
 */
public abstract class AbstractCiwsBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    protected AbstractCiwsBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    /** 放置时让炮塔面向玩家。 */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    /** 必须返回 ENTITYBLOCK_ANIMATED 才能让 GeckoLib 动态模型渲染。 */
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    /** 记录放置者作为归属，供 IFF 判定使用（非玩家放置时保持归属未知）。 */
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide) {
            OwnerTracked.assign(level.getBlockEntity(pos), placer);
        }
    }
}
