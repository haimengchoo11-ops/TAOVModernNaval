package TAOVMN.compat.cbc;

import TAOVMN.block.LaunchTubeBlock;
import TAOVMN.block.MissileBlock;
import TAOVMN.block.RakLauncherBlock;
import TAOVMN.block.entity.LaunchTubeBlockEntity;
import TAOVMN.block.entity.P1DirectorBlockEntity;
import TAOVMN.block.entity.RadarBlockEntity;
import TAOVMN.entity.GenericMissileEntity;
import TAOVMN.entity.P1MissileEntity;
import TAOVMN.registry.ModBlockEntities;
import TAOVMN.registry.ModBlocks;
import TAOVMN.registry.ModItems;
import TAOVMN.util.MissileProfile;
import com.simibubi.create.content.contraptions.AssemblyException;
import com.simibubi.create.content.contraptions.ContraptionType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import rbasamoyai.createbigcannons.cannon_control.ControlPitchContraption;
import rbasamoyai.createbigcannons.cannon_control.cannon_types.CBCCannonContraptionTypes;
import rbasamoyai.createbigcannons.cannon_control.cannon_types.ICannonContraptionType;
import rbasamoyai.createbigcannons.cannon_control.contraption.AbstractMountedCannonContraption;
import rbasamoyai.createbigcannons.cannon_control.contraption.PitchOrientedContraptionEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MountedLaunchTubeContraption extends AbstractMountedCannonContraption {
    private final Map<BlockPos, ItemStack> loadedMissiles = new HashMap<>();
    private final List<BlockPos> tubePositions = new ArrayList<>();

    public MountedLaunchTubeContraption() {
    }

    @Override
    public boolean assemble(Level level, BlockPos startPos) throws AssemblyException {
        BlockState startState = level.getBlockState(startPos);
        if (!(startState.getBlock() instanceof LaunchTubeBlock) && !(startState.getBlock() instanceof MissileBlock)) {
            return false;
        }

        Direction initialDir = Direction.NORTH;
        if (startState.getBlock() instanceof LaunchTubeBlock) {
            initialDir = startState.getValue(LaunchTubeBlock.FACING);
        } else if (startState.getBlock() instanceof MissileBlock) {
            initialDir = startState.getValue(MissileBlock.FACING);
        }
        this.initialOrientation = initialDir;
        this.startPos = startPos;
        this.bounds = createInitialLightingBounds();

        Direction.Axis axis = initialDir.getAxis();
        BlockPos.MutableBlockPos current = startPos.mutable();

        // 向上/往前与往后延伸收集连通的发射管/导弹方块
        List<BlockPos> collected = new ArrayList<>();
        for (int i = 0; i < getMaxCannonLength(); i++) {
            BlockState state = level.getBlockState(current);
            if (state.getBlock() instanceof LaunchTubeBlock tube) {
                if (tube.getFacing(state).getAxis() == axis) {
                    collected.add(current.immutable());
                    BlockEntity be = level.getBlockEntity(current);
                    if (be instanceof LaunchTubeBlockEntity tubeBE && tubeBE.hasMissile()) {
                        loadedMissiles.put(current.immutable().subtract(startPos), tubeBE.getLoadedMissile().copy());
                    }
                    current.move(initialDir);
                    continue;
                }
            } else if (state.getBlock() instanceof MissileBlock missile) {
                if (missile.getAxis(state) == axis) {
                    collected.add(current.immutable());
                    ItemStack missileStack = new ItemStack(state.getBlock());
                    loadedMissiles.put(current.immutable().subtract(startPos), missileStack);
                    current.move(initialDir);
                    continue;
                }
            }
            break;
        }

        if (collected.isEmpty()) {
            return false;
        }

        for (BlockPos pos : collected) {
            BlockState state = level.getBlockState(pos);
            BlockPos relative = pos.subtract(startPos);
            tubePositions.add(relative);
            BlockEntity be = level.getBlockEntity(pos);
            CompoundTag nbt = be != null ? be.saveWithFullMetadata() : null;
            this.blocks.put(relative, new net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo(relative, state, nbt));
        }

        this.frontExtensionLength = collected.size();
        this.backExtensionLength = 0;

        return true;
    }

    @Override
    public void onRedstoneUpdate(ServerLevel level, PitchOrientedContraptionEntity entity, boolean firePoweredChanged, int firePower, ControlPitchContraption controller) {
        if (firePoweredChanged && firePower > 0) {
            fireShot(level, entity);
        }
    }

    @Override
    public void fireShot(ServerLevel level, PitchOrientedContraptionEntity entity) {
        if (level.isClientSide) {
            return;
        }

        BlockPos targetTubePos = null;
        ItemStack missileStack = ItemStack.EMPTY;

        for (Map.Entry<BlockPos, ItemStack> entry : loadedMissiles.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                targetTubePos = entry.getKey();
                missileStack = entry.getValue();
                break;
            }
        }

        if (targetTubePos == null || missileStack.isEmpty()) {
            return;
        }

        // 从 Contraption 移除该发导弹
        loadedMissiles.remove(targetTubePos);

        // 计算世界坐标与朝向
        Vec3 localCenter = Vec3.atCenterOf(targetTubePos);
        Vec3 initialFacingVec = Vec3.atLowerCornerOf(initialOrientation.getNormal());
        Vec3 muzzleWorld = entity.toGlobalVector(localCenter.add(initialFacingVec.scale(0.8D)), 1.0F);
        Vec3 launchDir = entity.applyRotation(initialFacingVec, 1.0F).normalize();

        // 计算导弹物理与制导 profile
        MissileProfile profile = MissileProfile.resolve(missileStack);
        int resolvedFuze = CbcCompat.resolveFuzeTicks(missileStack, 200);
        ItemStack cbcFuze = CbcCompat.resolveFuzeItem(missileStack);
        boolean fuzeInstalled = missileStack.hasTag() && missileStack.getOrCreateTag().getBoolean("FuzeInstalled");

        Vec3 launchVelocity = launchDir.scale(Math.max(0.6D, profile.maxSpeed() * 0.4F)).add(0.0D, 0.16D, 0.0D);

        // 绑定/查找附近的雷达目标
        Vec3 targetPos = null;
        UUID targetUuid = null;
        BlockPos controllerPos = entity.getAnchorVec() != null ? BlockPos.containing(entity.getAnchorVec()) : BlockPos.containing(muzzleWorld);
        
        // 查找 48 格范围内的火控 / 雷达
        for (BlockPos testPos : BlockPos.betweenClosed(controllerPos.offset(-32, -16, -32), controllerPos.offset(32, 16, 32))) {
            BlockEntity be = level.getBlockEntity(testPos);
            if (be instanceof P1DirectorBlockEntity director) {
                targetUuid = director.getSelectedTargetUuid();
                targetPos = director.getSelectedTargetPos();
                if (targetPos != null) break;
            } else if (be instanceof RadarBlockEntity radar) {
                targetUuid = radar.getSelectedTargetUuid();
                targetPos = radar.getSelectedTargetPos();
                if (targetPos != null) break;
            }
        }

        GenericMissileEntity genericMissile = RakLauncherBlock.createGenericMissile(missileStack, level, muzzleWorld.x, muzzleWorld.y, muzzleWorld.z);
        if (genericMissile != null) {
            genericMissile.setFuzeTicks(resolvedFuze);
            if (!cbcFuze.isEmpty()) {
                genericMissile.setFuze(cbcFuze);
            }
            genericMissile.applyProfile(profile);
            genericMissile.setGuidanceTuning(
                    MissileProfile.resolveLockHoldTicks(missileStack, P1MissileEntity.defaultLockHoldTicks()),
                    MissileProfile.resolveSwitchAdvantageDistance(missileStack, P1MissileEntity.defaultSwitchAdvantageDistance()),
                    MissileProfile.resolveForceSwitchDistance(missileStack, P1MissileEntity.defaultForceSwitchDistance())
            );
            if (targetUuid != null) {
                genericMissile.setAssignedTarget(targetUuid);
            }
            if (targetPos != null) {
                genericMissile.setDesignatedTargetPos(targetPos);
            }
            genericMissile.setLauncherOrigin(muzzleWorld);
            genericMissile.setDeltaMovement(launchVelocity);
            genericMissile.setItem(missileStack.copyWithCount(1));
            genericMissile.setFuzeInstalled(fuzeInstalled);
            level.addFreshEntity(genericMissile);
        } else {
            P1MissileEntity missile = new P1MissileEntity(level, muzzleWorld.x, muzzleWorld.y, muzzleWorld.z);
            missile.setFuzeTicks(resolvedFuze);
            if (!cbcFuze.isEmpty()) {
                missile.setFuze(cbcFuze);
            }
            missile.applyProfile(profile);
            missile.setGuidanceTuning(
                    MissileProfile.resolveLockHoldTicks(missileStack, P1MissileEntity.defaultLockHoldTicks()),
                    MissileProfile.resolveSwitchAdvantageDistance(missileStack, P1MissileEntity.defaultSwitchAdvantageDistance()),
                    MissileProfile.resolveForceSwitchDistance(missileStack, P1MissileEntity.defaultForceSwitchDistance())
            );
            if (targetUuid != null) {
                missile.setAssignedTarget(targetUuid);
            }
            if (targetPos != null) {
                missile.setDesignatedTargetPos(targetPos);
            }
            missile.setLauncherOrigin(muzzleWorld);
            missile.setDeltaMovement(launchVelocity);
            missile.setItem(missileStack.copyWithCount(1));
            missile.setFuzeInstalled(fuzeInstalled);
            level.addFreshEntity(missile);
        }

        level.playSound(null, BlockPos.containing(muzzleWorld), SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    public boolean handlePlayerInteraction(Player player, BlockPos localPos, Direction side, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (ModItems.isMissile(held)) {
            if (loadedMissiles.containsKey(localPos) && !loadedMissiles.get(localPos).isEmpty()) {
                player.displayClientMessage(Component.translatable("message.taovmodernnaval.launcher_loaded"), true);
                return true;
            }
            ItemStack loaded = held.copyWithCount(1);
            loadedMissiles.put(localPos, loaded);
            if (!player.getAbilities().instabuild) {
                held.shrink(1);
            }
            player.displayClientMessage(Component.translatable("message.taovmodernnaval.launcher_load_success", 1, 1), true);
            return true;
        } else if (player.isShiftKeyDown()) {
            if (loadedMissiles.containsKey(localPos) && !loadedMissiles.get(localPos).isEmpty()) {
                ItemStack removed = loadedMissiles.remove(localPos);
                if (!player.addItem(removed)) {
                    player.drop(removed, false);
                }
                player.displayClientMessage(Component.translatable("message.taovmodernnaval.launcher_unload_success", 0, 1), true);
                return true;
            }
        }
        return false;
    }

    @Override
    public float getWeightForStress() {
        return 2.0F;
    }

    @Override
    public Vec3 getInteractionVec(PitchOrientedContraptionEntity entity) {
        return entity.position();
    }

    @Override
    public ICannonContraptionType getCannonType() {
        return CBCCannonContraptionTypes.BIG_CANNON;
    }

    @Override
    public ContraptionType getType() {
        return rbasamoyai.createbigcannons.index.CBCContraptionTypes.MOUNTED_CANNON;
    }

    @Override
    public CompoundTag writeNBT(boolean spawnPacket) {
        CompoundTag tag = super.writeNBT(spawnPacket);
        ListTag missilesList = new ListTag();
        for (Map.Entry<BlockPos, ItemStack> entry : loadedMissiles.entrySet()) {
            CompoundTag entryTag = new CompoundTag();
            entryTag.put("Pos", NbtUtils.writeBlockPos(entry.getKey()));
            entryTag.put("Item", entry.getValue().save(new CompoundTag()));
            missilesList.add(entryTag);
        }
        tag.put("LoadedMissiles", missilesList);
        return tag;
    }

    @Override
    public void readNBT(Level level, CompoundTag tag, boolean spawnPacket) {
        super.readNBT(level, tag, spawnPacket);
        loadedMissiles.clear();
        if (tag.contains("LoadedMissiles", 9)) {
            ListTag missilesList = tag.getList("LoadedMissiles", 10);
            for (int i = 0; i < missilesList.size(); i++) {
                CompoundTag entryTag = missilesList.getCompound(i);
                BlockPos pos = NbtUtils.readBlockPos(entryTag.getCompound("Pos"));
                ItemStack item = ItemStack.of(entryTag.getCompound("Item"));
                if (!item.isEmpty()) {
                    loadedMissiles.put(pos, item);
                }
            }
        }
    }
}
