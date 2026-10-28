package TAOVMN.block;

import TAOVMN.block.entity.RadarBlockEntity;
import TAOVMN.registry.ModBlockEntities;
import TAOVMN.util.OwnerTracked;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class RadarBlock extends BaseEntityBlock {
    public RadarBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RadarBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.RADAR_BLOCK.get(), RadarBlockEntity::tick);
    }

    /** 记录放置者作为归属：IFF 开启时用来排除友军。 */
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide) {
            OwnerTracked.assign(level.getBlockEntity(pos), placer);
        }
    }

    /** 潜行 + 空手右键：切换 IFF（开启=只锁定敌对生物与敌方玩家）。 */
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof RadarBlockEntity radar)) {
            return InteractionResult.PASS;
        }

        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        if (player.isShiftKeyDown() && player.getItemInHand(hand).isEmpty()) {
            boolean iff = radar.toggleIff();
            player.displayClientMessage(Component.translatable(iff
                    ? "message.taovmodernnaval.launcher_iff_on"
                    : "message.taovmodernnaval.launcher_iff_off"), true);
            return InteractionResult.CONSUME;
        }

        return InteractionResult.PASS;
    }
}