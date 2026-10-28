package TAOVMN.content.weapon;

import TAOVMN.content.NavalBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * 新版舰载发射管：红石脉冲或右键触发，向 32 格内最近目标发射一枚导弹，冷却 40 tick。
 */
public class NavalLaunchBlock extends Block implements EntityBlock {
    public NavalLaunchBlock() {
        super(BlockBehaviour.Properties.of()
                .strength(4.0F, 18.0F)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new NavalLaunchBlockEntity(pos, state);
    }

    /** 仅服务器端推进冷却计时。 */
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        return type == NavalBlockEntities.NAVAL_LAUNCH.get()
                ? (lvl, pos, st, blockEntity) -> ((NavalLaunchBlockEntity) blockEntity).tickServer()
                : null;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        if (!(level.getBlockEntity(pos) instanceof NavalLaunchBlockEntity launcher)) {
            return InteractionResult.PASS;
        }

        boolean fired = launcher.tryLaunch();
        player.displayClientMessage(Component.translatable(fired
                ? "message.taovmodernnaval.launch_fired"
                : "message.taovmodernnaval.launch_no_target"), true);
        return InteractionResult.CONSUME;
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
        if (!level.isClientSide && level.hasNeighborSignal(pos)
                && level.getBlockEntity(pos) instanceof NavalLaunchBlockEntity launcher) {
            launcher.tryLaunch();
        }
    }
}
