package TAOVMN.block;

import TAOVMN.block.entity.RakLauncherBlockEntity;
import TAOVMN.registry.ModBlockEntities;
import TAOVMN.registry.ModItems;
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

/**
 * RAK 导弹发射架。
 *
 * <p>右键（火控基础操作）：手持导弹=装填；潜行+空手=卸载（无弹则循环引信预设）；
 * 空手=未绑定时自动绑定最近雷达并显示状态。
 * <p>发射：<b>红石上升沿</b>触发（见 {@link RakLauncherBlockEntity#tick}）。
 */
public class RakLauncherBlock extends BaseEntityBlock {
    public RakLauncherBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RakLauncherBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.RAK_LAUNCHER.get(), RakLauncherBlockEntity::tick);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof RakLauncherBlockEntity launcher) {
            launcher.applyPlacementTag(stack.getTag());
            // 记录放置者作为归属，供 IFF 判定使用（非玩家放置时保持归属未知）
            OwnerTracked.assign(launcher, placer);
        }
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof RakLauncherBlockEntity launcher)) {
            return InteractionResult.PASS;
        }

        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        ItemStack held = player.getItemInHand(hand);

        // 手持导弹 → 装填
        if (ModItems.isMissile(held)) {
            if (launcher.isFull()) {
                player.displayClientMessage(Component.translatable("message.taovmodernnaval.launcher_loaded"), true);
            } else if (launcher.loadMissile(held)) {
                if (!player.getAbilities().instabuild) {
                    held.shrink(1);
                }
                player.displayClientMessage(Component.translatable("message.taovmodernnaval.launcher_load_success",
                        launcher.getMissileCount(), RakLauncherBlockEntity.MAX_MISSILES), true);
            }
            return InteractionResult.CONSUME;
        }

        // 潜行 + 空手 → 卸载（无弹则循环引信预设）
        if (player.isShiftKeyDown() && held.isEmpty()) {
            ItemStack removed = launcher.removeMissile();
            if (!removed.isEmpty()) {
                if (!player.addItem(removed)) {
                    player.drop(removed, false);
                }
                player.displayClientMessage(Component.translatable("message.taovmodernnaval.launcher_unload_success",
                        launcher.getMissileCount(), RakLauncherBlockEntity.MAX_MISSILES), true);
            } else {
                int fuze = launcher.cycleFuzePreset();
                player.displayClientMessage(Component.translatable("message.taovmodernnaval.launcher_fuze", fuze), true);
            }
            return InteractionResult.CONSUME;
        }

        // 空手 → 未绑定则自动绑定最近雷达，随后显示状态
        if (held.isEmpty()) {
            if (!launcher.hasBoundRadar()) {
                if (launcher.bindNearestRadar()) {
                    BlockPos radarPos = launcher.getBoundRadarPos();
                    player.displayClientMessage(Component.translatable("message.taovmodernnaval.launcher_radar_bound",
                            radarPos.getX(), radarPos.getY(), radarPos.getZ()), true);
                } else {
                    player.displayClientMessage(Component.translatable("message.taovmodernnaval.launcher_radar_not_found"), true);
                }
            }

            String radar = launcher.hasBoundRadar() ? launcher.getBoundRadarPos().toShortString() : "-";
            player.displayClientMessage(Component.translatable("message.taovmodernnaval.launcher_status",
                    launcher.getMissileCount(), RakLauncherBlockEntity.MAX_MISSILES, radar,
                    launcher.getRadarTrackedCount(), launcher.isRadarIffEnabled() ? "ON" : "OFF"), true);
            return InteractionResult.CONSUME;
        }

        return InteractionResult.PASS;
    }
}
