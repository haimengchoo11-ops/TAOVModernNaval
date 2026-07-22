package TAOVMN.block;

import TAOVMN.block.entity.RakLauncherBlockEntity;
import TAOVMN.entity.P1MissileEntity;
import TAOVMN.registry.ModItems;
import TAOVMN.compat.cbc.CbcCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;


public class RakLauncherBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    private static final double MUZZLE_FORWARD_OFFSET = 1.25D;
    private static final double MUZZLE_UP_OFFSET = 1.05D;
    private static final double MUZZLE_SIDE_OFFSET = 0.0D;

    public RakLauncherBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(POWERED, false));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RakLauncherBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(FACING, POWERED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite()).setValue(POWERED, false);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, net.minecraft.world.level.block.Block block, BlockPos fromPos, boolean moving) {
        if (level.isClientSide) {
            return;
        }

        boolean poweredNow = level.hasNeighborSignal(pos);
        boolean wasPowered = state.getValue(POWERED);
        if (poweredNow == wasPowered) {
            return;
        }

        level.setBlock(pos, state.setValue(POWERED, poweredNow), 3);
        if (poweredNow) {
            launch((ServerLevel) level, pos, state);
        }
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide) {
            return;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof RakLauncherBlockEntity launcher)) {
            return;
        }

        if (stack.hasTag()) {
            launcher.applyPlacementTag(stack.getTag());
        }
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof RakLauncherBlockEntity launcher)) {
            return InteractionResult.PASS;
        }

        ItemStack held = player.getItemInHand(hand);

        if (ModItems.isMissile(held)) {
            if (launcher.isFull()) {
                player.displayClientMessage(Component.translatable("message.taovmodernnaval.launcher_loaded"), true);
                return InteractionResult.CONSUME;
            }

            if (!launcher.loadMissile(held)) {
                return InteractionResult.CONSUME;
            }

            if (!player.getAbilities().instabuild) {
                held.shrink(1);
            }
            player.displayClientMessage(Component.translatable("message.taovmodernnaval.launcher_load_success", launcher.getMissileCount(), RakLauncherBlockEntity.MAX_MISSILES), true);
            return InteractionResult.CONSUME;
        }

        if (isMechanicalArmItem(held)) {
            boolean setStorage = !player.isShiftKeyDown();
            boolean changed = launcher.setArmStorageTarget(setStorage);
            if (setStorage) {
                player.displayClientMessage(Component.translatable(changed
                        ? "message.taovmodernnaval.launcher_arm_storage_set"
                        : "message.taovmodernnaval.launcher_arm_storage_already"), true);
            } else {
                player.displayClientMessage(Component.translatable(changed
                        ? "message.taovmodernnaval.launcher_arm_storage_cleared"
                        : "message.taovmodernnaval.launcher_arm_storage_already_cleared"), true);
            }
            return InteractionResult.CONSUME;
        }

        if (player.isShiftKeyDown()) {
            if (launcher.hasMissile()) {
                ItemStack missile = launcher.removeMissile();
                if (!player.addItem(missile)) {
                    player.drop(missile, false);
                }
                player.displayClientMessage(Component.translatable("message.taovmodernnaval.launcher_unload_success", launcher.getMissileCount(), RakLauncherBlockEntity.MAX_MISSILES), true);
                return InteractionResult.CONSUME;
            }
        }

        if (!launcher.ensureRadarBound()) {
            player.displayClientMessage(Component.translatable("message.taovmodernnaval.launcher_radar_required"), true);
            return InteractionResult.CONSUME;
        }

        if (player.isShiftKeyDown()) {
            boolean enabled = launcher.toggleRadarIff();
            player.displayClientMessage(Component.translatable(enabled ? "message.taovmodernnaval.launcher_iff_on" : "message.taovmodernnaval.launcher_iff_off"), true);
            return InteractionResult.CONSUME;
        }

        java.util.UUID selected = launcher.cycleRadarTarget();
        Vec3 selectedPos = launcher.getSelectedRadarTargetPos();
        if (selected == null && selectedPos == null) {
            player.displayClientMessage(Component.translatable("message.taovmodernnaval.launcher_target_none"), true);
        } else {
            player.displayClientMessage(Component.translatable("message.taovmodernnaval.launcher_target_selected", launcher.getRadarTrackedCount()), true);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.getBlock() != newState.getBlock()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof RakLauncherBlockEntity launcher && launcher.hasMissile()) {
                while (launcher.hasMissile()) {
                    ItemStack missile = launcher.removeMissile();
                    if (!missile.isEmpty()) {
                        popResource(level, pos, missile);
                    }
                }
            }
        }

        super.onRemove(state, level, pos, newState, isMoving);
    }

    private static void launch(ServerLevel level, BlockPos pos, BlockState state) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof RakLauncherBlockEntity launcher) || !launcher.hasMissile()) {
            return;
        }

        ItemStack ammoStack = launcher.removeMissile();
        if (ammoStack.isEmpty()) {
            return;
        }

        Direction facing = state.getValue(FACING);
        Vec3 dir = Vec3.atLowerCornerOf(facing.getNormal()).normalize();
        Vec3 center = Vec3.atCenterOf(pos);
        Vec3 right = new Vec3(-dir.z, 0.0D, dir.x);
        Vec3 muzzlePos = center
            .add(dir.scale(MUZZLE_FORWARD_OFFSET))
            .add(0.0D, MUZZLE_UP_OFFSET, 0.0D)
            .add(right.scale(MUZZLE_SIDE_OFFSET));

        P1MissileEntity missile = new P1MissileEntity(level, muzzlePos.x, muzzlePos.y, muzzlePos.z);
        int resolvedFuze = CbcCompat.resolveFuzeTicks(ammoStack, launcher.getFuzeTicks());
        missile.setFuzeTicks(resolvedFuze);
        ItemStack cbcFuze = CbcCompat.resolveFuzeItem(ammoStack);
        if (!cbcFuze.isEmpty()) {
            missile.setFuze(cbcFuze);
        }
        missile.applyProfile(TAOVMN.util.MissileProfile.resolve(ammoStack));
        missile.setGuidanceTuning(
            TAOVMN.util.MissileProfile.resolveLockHoldTicks(ammoStack, P1MissileEntity.defaultLockHoldTicks()),
            TAOVMN.util.MissileProfile.resolveSwitchAdvantageDistance(ammoStack, P1MissileEntity.defaultSwitchAdvantageDistance()),
            TAOVMN.util.MissileProfile.resolveForceSwitchDistance(ammoStack, P1MissileEntity.defaultForceSwitchDistance())
        );
        if (launcher.ensureRadarBound()) {
            launcher.refreshRadarTargets();
            missile.setAssignedTarget(launcher.getSelectedRadarTargetUuid());
            Vec3 selectedPos = launcher.getSelectedRadarTargetPos();
            if (selectedPos != null) {
                missile.setDesignatedTargetPos(selectedPos);
            }
        }
        missile.setLauncherOrigin(center);
        missile.setDeltaMovement(dir.scale(1.0D).add(0.0D, 0.16D, 0.0D));
        missile.setItem(ammoStack.copyWithCount(1));
        level.addFreshEntity(missile);
        level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof RakLauncherBlockEntity launcher && launcher.hasMissile()) {
            return (launcher.getMissileCount() * 15) / RakLauncherBlockEntity.MAX_MISSILES;
        }
        return 0;
    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return false;
    }

    private static boolean isMechanicalArmItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        var key = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (key == null) {
            return false;
        }
        return "create".equals(key.getNamespace()) && "mechanical_arm".equals(key.getPath());
    }
}