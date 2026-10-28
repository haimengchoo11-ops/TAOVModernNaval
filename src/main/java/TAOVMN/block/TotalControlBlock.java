package TAOVMN.block;

import TAOVMN.block.entity.TotalControlBlockEntity;
import TAOVMN.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import org.jetbrains.annotations.Nullable;

public class TotalControlBlock extends BaseEntityBlock {
    public TotalControlBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TotalControlBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.TOTAL_CONTROL_BLOCK.get(),
                (serverLevel, pos, blockState, entity) -> entity.tick());
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            if (!player.isCrouching()) {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                        net.minecraft.client.Minecraft.getInstance().setScreen(
                                new TAOVMN.client.gui.TotalControlScreen(pos)));
            }
            return InteractionResult.SUCCESS;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof TotalControlBlockEntity controller)) {
            return InteractionResult.PASS;
        }

        if (player.isCrouching()) {
            controller.cycleMode();
            player.displayClientMessage(Component.literal("总控模式: " + controller.getModeName()), true);
        }
        return InteractionResult.CONSUME;
    }
}
