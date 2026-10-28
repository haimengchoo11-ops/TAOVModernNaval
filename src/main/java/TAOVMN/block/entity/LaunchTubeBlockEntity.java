package TAOVMN.block.entity;

import TAOVMN.block.LaunchTubeBlock;
import TAOVMN.entity.ShaoMissileProjectile;
import TAOVMN.registry.ModBlockEntities;
import TAOVMN.registry.ModItems;
import TAOVMN.util.MissileFireControl;
import TAOVMN.util.OwnerTracked;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

public class LaunchTubeBlockEntity extends BlockEntity implements GeoBlockEntity, OwnerTracked {
    private static final String TAG_MISSILE = "LoadedMissile";
    private static final int FIRE_COOLDOWN_TICKS = 10;
    private static final int RADAR_SEARCH_RANGE = 32;
    private static final int DEFAULT_FUZE_TICKS = 200;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private ItemStack loadedMissile = ItemStack.EMPTY;
    private boolean powered;
    private int fireCooldown;
    /** 放置这根发射管的玩家；null 表示归属未知（不施加 IFF 保护）。 */
    @Nullable
    private UUID ownerId;

    public LaunchTubeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LAUNCH_TUBE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, LaunchTubeBlockEntity tube) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        if (tube.fireCooldown > 0) {
            tube.fireCooldown--;
        }
        boolean poweredNow = level.hasNeighborSignal(pos);
        if (poweredNow && !tube.powered && tube.fireCooldown <= 0) {
            tube.fire(serverLevel, pos, state);
            tube.fireCooldown = FIRE_COOLDOWN_TICKS;
        }
        tube.powered = poweredNow;
    }

    /** 红石上升沿：按发射管朝向发射已装填导弹，并就近取雷达/火控目标。 */
    public boolean fire(ServerLevel level, BlockPos pos, BlockState state) {
        if (loadedMissile.isEmpty()) {
            return false;
        }

        ItemStack stack = loadedMissile;
        loadedMissile = ItemStack.EMPTY;
        setChanged();

        Vec3 direction = Vec3.atLowerCornerOf(state.getValue(LaunchTubeBlock.FACING).getNormal());
        Vec3 muzzle = Vec3.atCenterOf(pos).add(direction.scale(0.8D));

        UUID targetUuid = null;
        Vec3 targetPos = null;
        RadarBlockEntity radar = MissileFireControl.findNearbyRadar(level, pos, RADAR_SEARCH_RANGE);
        if (radar != null) {
            targetUuid = radar.getSelectedTargetUuid();
            targetPos = radar.getSelectedTargetPos();
        }

        ShaoMissileProjectile missile = MissileFireControl.launch(level, stack, muzzle, direction,
                DEFAULT_FUZE_TICKS, targetUuid, targetPos, muzzle, ownerId);
        if (missile == null) {
            return false;
        }

        level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
        return true;
    }

    public boolean hasMissile() {
        return !loadedMissile.isEmpty();
    }

    public ItemStack getLoadedMissile() {
        return loadedMissile;
    }

    public boolean loadMissile(ItemStack stack) {
        if (hasMissile() || stack.isEmpty() || !isAntiShipMissile(stack)) {
            return false;
        }
        loadedMissile = stack.copyWithCount(1);
        setChanged();
        return true;
    }

    public static boolean isAntiShipMissile(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        return stack.is(ModItems.P1_MISSILE.get())
                || stack.is(ModItems.P15_MISSILE.get())
                || stack.is(ModItems.P120_MISSILE.get())
                || stack.is(ModItems.P270_MISSILE.get())
                || stack.is(ModItems.P500_MISSILE.get())
                || stack.is(ModItems.P700_MISSILE.get())
                || stack.is(ModItems.P800_MISSILE.get());
    }

    public ItemStack unloadMissile() {
        ItemStack result = loadedMissile;
        loadedMissile = ItemStack.EMPTY;
        setChanged();
        return result;
    }

    @Override
    @Nullable
    public UUID getOwnerId() {
        return ownerId;
    }

    @Override
    public void setOwnerId(@Nullable UUID ownerId) {
        this.ownerId = ownerId;
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (hasMissile()) {
            tag.put(TAG_MISSILE, loadedMissile.save(new CompoundTag()));
        }
        OwnerTracked.write(tag, ownerId);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        loadedMissile = tag.contains(TAG_MISSILE, 10) ? ItemStack.of(tag.getCompound(TAG_MISSILE)) : ItemStack.EMPTY;
        ownerId = OwnerTracked.read(tag);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}