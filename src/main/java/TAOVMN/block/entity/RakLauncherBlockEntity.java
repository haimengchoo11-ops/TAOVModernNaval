package TAOVMN.block.entity;

import TAOVMN.compat.cbc.CbcCompat;
import TAOVMN.item.LauncherBlockItem;
import TAOVMN.registry.ModBlockEntities;
import TAOVMN.item.MissileItem;
import TAOVMN.registry.ModItems;
import TAOVMN.util.MissileProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.WorldlyContainer;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.SidedInvWrapper;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

public class RakLauncherBlockEntity extends BlockEntity implements WorldlyContainer, GeoBlockEntity {
    private static final String TAG_MISSILES = "Missiles";
    private static final String TAG_FUZE_TICKS = "FuzeTicks";
    private static final String TAG_PROFILE_MAX_SPEED = "ProfileMaxSpeed";
    private static final String TAG_PROFILE_STEERING = "ProfileSteering";
    private static final String TAG_PROFILE_BLAST_POWER = "ProfileBlastPower";
    private static final String TAG_PROFILE_SEEK_RANGE = "ProfileSeekRange";
    private static final String TAG_PROFILE_PROXIMITY_FUSE = "ProfileProximityFuse";
    private static final String TAG_PROFILE_IFF_SAFE_RADIUS = "ProfileIffSafeRadius";
    private static final String TAG_BOUND_RADAR_POS = "BoundRadarPos";
    private static final String TAG_ARM_STORAGE_TARGET = "ArmStorageTarget";
    private static final int[] FUZE_PRESETS = new int[]{120, 200, 320, 480};
    private static final int[] SLOTS = new int[]{0, 1};
    private static final int RADAR_BIND_RANGE = 48;
    public static final int MAX_MISSILES = 2;

    private final List<ItemStack> missiles = new ArrayList<>();
    private final LazyOptional<? extends IItemHandler>[] sidedHandlers = SidedInvWrapper.create(this, Direction.UP, Direction.DOWN, Direction.NORTH);
    private final LazyOptional<IItemHandler> genericHandler = LazyOptional.of(() -> new SidedInvWrapper(this, null));
    private final AnimatableInstanceCache geckoCache = GeckoLibUtil.createInstanceCache(this);
    private int fuzeTicks = 200;
    private MissileProfile missileProfile = MissileProfile.p1Default();
    private BlockPos boundRadarPos;
    private boolean armStorageTarget;

    public RakLauncherBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.RAK_LAUNCHER.get(), pos, blockState);
    }

    public boolean hasMissile() {
        return !missiles.isEmpty();
    }

    public int getMissileCount() {
        return missiles.size();
    }

    public boolean isFull() {
        return missiles.size() >= MAX_MISSILES;
    }

    public boolean loadMissile(ItemStack stack) {
        if (stack.isEmpty() || isFull() || !ModItems.isMissile(stack)) {
            return false;
        }

        ItemStack loaded = stack.copyWithCount(1);
        if (loaded.getItem() instanceof MissileItem missileItem) {
            MissileProfile.ensureProfileTag(loaded, missileItem.getDefaultProfile());
        } else if (CbcCompat.isSupportedAmmo(loaded)) {
            CbcCompat.primeTaovProfileTag(loaded);
        }
        missiles.add(loaded);
        missileProfile = MissileProfile.resolve(loaded);
        setChanged();
        return true;
    }

    public ItemStack removeMissile() {
        if (missiles.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ItemStack extracted = missiles.remove(missiles.size() - 1);
        missileProfile = missiles.isEmpty() ? MissileProfile.p1Default() : MissileProfile.resolve(missiles.get(missiles.size() - 1));
        setChanged();
        return extracted;
    }

    private int lastFilledSlot() {
        for (int i = MAX_MISSILES - 1; i >= 0; i--) {
            if (i < missiles.size() && !missiles.get(i).isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    public ItemStack removeAllMissiles() {
        if (missiles.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ItemStack first = missiles.get(0).copy();
        boolean allSame = missiles.stream().allMatch(s -> ItemStack.isSameItemSameTags(first, s));

        ItemStack drop;
        if (allSame) {
            drop = first;
            drop.setCount(missiles.size());
        } else {
            drop = first;
        }

        missiles.clear();
        missileProfile = MissileProfile.p1Default();
        setChanged();
        return drop;
    }

    public int getFuzeTicks() {
        return fuzeTicks;
    }

    public MissileProfile getMissileProfile() {
        if (missiles.isEmpty()) {
            return MissileProfile.p1Default();
        }
        return MissileProfile.resolve(missiles.get(missiles.size() - 1));
    }

    public boolean hasCurrentMissileProfileTag() {
        if (missiles.isEmpty()) {
            return false;
        }
        return MissileProfile.hasProfileTag(missiles.get(missiles.size() - 1));
    }

    public int cycleFuzePreset() {
        for (int preset : FUZE_PRESETS) {
            if (preset > fuzeTicks) {
                fuzeTicks = preset;
                setChanged();
                return fuzeTicks;
            }
        }

        fuzeTicks = FUZE_PRESETS[0];
        setChanged();
        return fuzeTicks;
    }

    public boolean hasBoundRadar() {
        return getBoundRadarEntity() != null;
    }

    public boolean ensureRadarBound() {
        return hasBoundRadar() || bindNearestRadar();
    }

    public BlockPos getBoundRadarPos() {
        return boundRadarPos;
    }

    public void setBoundRadarPos(BlockPos radarPos) {
        this.boundRadarPos = radarPos == null ? null : radarPos.immutable();
        setChanged();
    }

    public boolean isArmStorageTarget() {
        return armStorageTarget;
    }

    public boolean setArmStorageTarget(boolean value) {
        if (armStorageTarget == value) {
            return false;
        }
        armStorageTarget = value;
        setChanged();
        return true;
    }

    public void applyPlacementTag(CompoundTag itemTag) {
        if (itemTag == null) {
            return;
        }

        if (itemTag.contains(LauncherBlockItem.TAG_PREBOUND_RADAR_POS, 10)) {
            setBoundRadarPos(NbtUtils.readBlockPos(itemTag.getCompound(LauncherBlockItem.TAG_PREBOUND_RADAR_POS)));
        }
    }

    public boolean bindNearestRadar() {
        if (level == null) {
            return false;
        }

        BlockPos nearestPos = null;
        double nearestSqr = RADAR_BIND_RANGE * RADAR_BIND_RANGE;

        for (int dx = -RADAR_BIND_RANGE; dx <= RADAR_BIND_RANGE; dx++) {
            for (int dy = -RADAR_BIND_RANGE; dy <= RADAR_BIND_RANGE; dy++) {
                for (int dz = -RADAR_BIND_RANGE; dz <= RADAR_BIND_RANGE; dz++) {
                    BlockPos testPos = worldPosition.offset(dx, dy, dz);
                    if (!(level.getBlockEntity(testPos) instanceof RadarBlockEntity)) {
                        continue;
                    }

                    double distanceSqr = testPos.distSqr(worldPosition);
                    if (distanceSqr < nearestSqr) {
                        nearestSqr = distanceSqr;
                        nearestPos = testPos.immutable();
                    }
                }
            }
        }

        if (nearestPos == null) {
            return false;
        }

        boundRadarPos = nearestPos;
        setChanged();
        return true;
    }

    public UUID cycleRadarTarget() {
        RadarBlockEntity radar = getBoundRadarEntity();
        if (radar == null) {
            return null;
        }
        return radar.cycleTargetSelection();
    }

    public void refreshRadarTargets() {
        RadarBlockEntity radar = getBoundRadarEntity();
        if (radar != null) {
            radar.refreshTargetsNow();
        }
    }

    public UUID getSelectedRadarTargetUuid() {
        RadarBlockEntity radar = getBoundRadarEntity();
        if (radar == null) {
            return null;
        }
        return radar.getSelectedTargetUuid();
    }

    public Vec3 getSelectedRadarTargetPos() {
        RadarBlockEntity radar = getBoundRadarEntity();
        if (radar == null) {
            return null;
        }
        return radar.getSelectedTargetPos();
    }

    public boolean isSelectedRadarPhysicsTarget() {
        RadarBlockEntity radar = getBoundRadarEntity();
        return radar != null && radar.isSelectedPhysicsTarget();
    }

    public int getRadarTrackedCount() {
        RadarBlockEntity radar = getBoundRadarEntity();
        return radar == null ? 0 : radar.getTrackedCount();
    }

    public boolean toggleRadarIff() {
        RadarBlockEntity radar = getBoundRadarEntity();
        if (radar == null) {
            return false;
        }
        return radar.toggleIff();
    }

    public boolean isRadarIffEnabled() {
        RadarBlockEntity radar = getBoundRadarEntity();
        return radar != null && radar.isIffEnabled();
    }

    private RadarBlockEntity getBoundRadarEntity() {
        if (level == null || boundRadarPos == null) {
            return null;
        }

        if (level.getBlockEntity(boundRadarPos) instanceof RadarBlockEntity radar) {
            return radar;
        }

        boundRadarPos = null;
        setChanged();
        return null;
    }

    @Override
    public int getContainerSize() {
        return MAX_MISSILES;
    }

    @Override
    public boolean isEmpty() {
        return missiles.isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        if (slot < 0 || slot >= MAX_MISSILES || slot >= missiles.size()) {
            return ItemStack.EMPTY;
        }
        return missiles.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (slot < 0 || slot >= MAX_MISSILES || slot >= missiles.size() || amount <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack current = missiles.get(slot);
        ItemStack extracted = current.split(Math.min(amount, current.getCount()));
        if (current.isEmpty()) {
            missiles.remove(slot);
        }
        refreshProfileAndMarkChanged();
        return extracted;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (slot < 0 || slot >= MAX_MISSILES || slot >= missiles.size()) {
            return ItemStack.EMPTY;
        }
        ItemStack extracted = missiles.remove(slot);
        refreshProfileAndMarkChanged();
        return extracted;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot < 0 || slot >= MAX_MISSILES) {
            return;
        }

        if (stack.isEmpty()) {
            if (slot < missiles.size()) {
                missiles.remove(slot);
                refreshProfileAndMarkChanged();
            }
            return;
        }

        if (!ModItems.isMissile(stack)) {
            return;
        }

        ItemStack single = stack.copyWithCount(1);
        if (single.getItem() instanceof MissileItem missileItem) {
            MissileProfile.ensureProfileTag(single, missileItem.getDefaultProfile());
        } else if (CbcCompat.isSupportedAmmo(single)) {
            CbcCompat.primeTaovProfileTag(single);
        }

        while (missiles.size() <= slot) {
            missiles.add(ItemStack.EMPTY);
        }
        missiles.set(slot, single);

        for (int i = missiles.size() - 1; i >= 0; i--) {
            if (!missiles.get(i).isEmpty()) {
                break;
            }
            missiles.remove(i);
        }

        refreshProfileAndMarkChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        if (level == null) {
            return false;
        }
        if (level.getBlockEntity(worldPosition) != this) {
            return false;
        }
        return player.distanceToSqr(worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D, worldPosition.getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public void clearContent() {
        missiles.clear();
        refreshProfileAndMarkChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot >= 0 && slot < MAX_MISSILES && ModItems.isMissile(stack);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return armStorageTarget && canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return true;
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> capability, @Nullable Direction side) {
        if (!this.remove && capability == ForgeCapabilities.ITEM_HANDLER) {
            if (side == null) {
                return genericHandler.cast();
            }
            if (side == Direction.UP) {
                return sidedHandlers[0].cast();
            }
            if (side == Direction.DOWN) {
                return sidedHandlers[1].cast();
            }
            return sidedHandlers[2].cast();
        }
        return super.getCapability(capability, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        for (LazyOptional<? extends IItemHandler> handler : sidedHandlers) {
            handler.invalidate();
        }
        genericHandler.invalidate();
    }

    private void refreshProfileAndMarkChanged() {
        int index = lastFilledSlot();
        missileProfile = index >= 0 ? MissileProfile.resolve(missiles.get(index)) : MissileProfile.p1Default();
        setChanged();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // Reserved for future launcher animation controllers.
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geckoCache;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        CompoundTag missilesTag = new CompoundTag();
        missilesTag.putInt("Size", missiles.size());
        for (int i = 0; i < missiles.size(); i++) {
            missilesTag.put("S" + i, missiles.get(i).save(new CompoundTag()));
        }
        tag.put(TAG_MISSILES, missilesTag);
        tag.putInt(TAG_FUZE_TICKS, fuzeTicks);
        tag.putFloat(TAG_PROFILE_MAX_SPEED, missileProfile.maxSpeed());
        tag.putFloat(TAG_PROFILE_STEERING, missileProfile.steering());
        tag.putFloat(TAG_PROFILE_BLAST_POWER, missileProfile.blastPower());
        tag.putFloat(TAG_PROFILE_SEEK_RANGE, missileProfile.seekRange());
        tag.putFloat(TAG_PROFILE_PROXIMITY_FUSE, missileProfile.proximityFuse());
        tag.putFloat(TAG_PROFILE_IFF_SAFE_RADIUS, missileProfile.iffSafeRadius());
        if (boundRadarPos != null) {
            tag.putLong(TAG_BOUND_RADAR_POS, boundRadarPos.asLong());
        }
        tag.putBoolean(TAG_ARM_STORAGE_TARGET, armStorageTarget);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        missiles.clear();
        if (tag.contains(TAG_MISSILES, 10)) {
            CompoundTag missilesTag = tag.getCompound(TAG_MISSILES);
            int size = Math.min(MAX_MISSILES, Math.max(0, missilesTag.getInt("Size")));
            for (int i = 0; i < size; i++) {
                String key = "S" + i;
                if (missilesTag.contains(key, 10)) {
                    ItemStack stack = ItemStack.of(missilesTag.getCompound(key));
                    if (!stack.isEmpty()) {
                        stack.setCount(1);
                        missiles.add(stack);
                    }
                }
            }
        }
        fuzeTicks = tag.contains(TAG_FUZE_TICKS) ? tag.getInt(TAG_FUZE_TICKS) : 200;
        boundRadarPos = tag.contains(TAG_BOUND_RADAR_POS) ? BlockPos.of(tag.getLong(TAG_BOUND_RADAR_POS)) : null;
        armStorageTarget = tag.contains(TAG_ARM_STORAGE_TARGET) && tag.getBoolean(TAG_ARM_STORAGE_TARGET);
        missileProfile = missiles.isEmpty() ? new MissileProfile(
            tag.contains(TAG_PROFILE_MAX_SPEED) ? tag.getFloat(TAG_PROFILE_MAX_SPEED) : MissileProfile.p1Default().maxSpeed(),
            tag.contains(TAG_PROFILE_STEERING) ? tag.getFloat(TAG_PROFILE_STEERING) : MissileProfile.p1Default().steering(),
            tag.contains(TAG_PROFILE_BLAST_POWER) ? tag.getFloat(TAG_PROFILE_BLAST_POWER) : MissileProfile.p1Default().blastPower(),
            tag.contains(TAG_PROFILE_SEEK_RANGE) ? tag.getFloat(TAG_PROFILE_SEEK_RANGE) : MissileProfile.p1Default().seekRange(),
            tag.contains(TAG_PROFILE_PROXIMITY_FUSE) ? tag.getFloat(TAG_PROFILE_PROXIMITY_FUSE) : MissileProfile.p1Default().proximityFuse(),
            tag.contains(TAG_PROFILE_IFF_SAFE_RADIUS) ? tag.getFloat(TAG_PROFILE_IFF_SAFE_RADIUS) : MissileProfile.p1Default().iffSafeRadius())
                : MissileProfile.resolve(missiles.get(missiles.size() - 1));
    }
}