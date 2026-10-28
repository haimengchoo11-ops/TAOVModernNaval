package TAOVMN.block.entity;

import java.util.Comparator;
import java.util.UUID;

import TAOVMN.entity.ShaoMissileProjectile;
import TAOVMN.util.IffRules;
import TAOVMN.util.LauncherKind;
import TAOVMN.util.OwnerTracked;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * 近防炮（CIWS）通用逻辑：红石 / 总控台手动指令驱动 → 优先拦截来袭在飞弹体、其次敌对生物 →
 * 命中判定走方块射线遮挡 → 炮塔朝向同步给客户端做阻尼插值。
 *
 * <p>原本 {@code AK630BlockEntity} 与 {@code AK230BlockEntity} 是两份逐字复制的 286 行实现，
 * 只有扫描半径 / 射速 / 伤害 / 炮口高度 / 旋转速度五项不同。这里把这五项收进 {@link CiwsSpec}，
 * 子类只声明参数。
 *
 * <p><b>同步策略</b>：炮塔朝向每 tick 都在变，原先"角度变化 &gt; 0.1° 就发一个全量方块实体包"
 * 会在追踪移动目标时退化成每 tick 一次广播。现在服务端仍每 tick 更新朝向字段，但只在累计到
 * {@link #AIM_SYNC_INTERVAL_TICKS} tick 时同步一次；客户端已有 {@link #smoothRotateOnClient()}
 * 按 {@link CiwsSpec#rotateSpeedDegPerTick()} 插值，4 tick 足以补上 16–20°。
 */
public abstract class AbstractCiwsBlockEntity extends BlockEntity implements GeoBlockEntity, OwnerTracked {
    /**
     * 各型号近防炮的性能参数。
     *
     * @param scanRange             搜索半径（格）
     * @param rotateSpeedDegPerTick 客户端炮塔插值速度（度/tick）
     * @param fireCooldownTicks     射速间隔（tick）
     * @param damagePerHit          对生物目标的单发伤害
     * @param muzzleHeightOffset    炮口相对方块中心的抬升高度
     * @param turretBone            GeckoLib 炮塔水平旋转骨骼名
     * @param cannonBone            GeckoLib 炮管俯仰骨骼名
     * @param launcherKind          总控台火力通道类型
     */
    public record CiwsSpec(
            double scanRange,
            float rotateSpeedDegPerTick,
            int fireCooldownTicks,
            float damagePerHit,
            double muzzleHeightOffset,
            String turretBone,
            String cannonBone,
            LauncherKind launcherKind) {
    }

    /** 炮塔朝向同步到客户端的间隔（tick）。 */
    public static final int AIM_SYNC_INTERVAL_TICKS = 4;

    private static final String TAG_TARGET_YAW = "TargetYaw";
    private static final String TAG_TARGET_PITCH = "TargetPitch";
    /** 角度变化小于该值视为没变，不触发同步。 */
    private static final float AIM_EPSILON_DEGREES = 0.1F;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    // 渲染器需要读取的实时目标角度（角度制）
    public float targetYaw = 0.0F;
    public float targetPitch = 0.0F;

    // 客户端渲染使用的当前插值角度（提供丝滑的转动阻尼感）
    public float clientYaw = 0.0F;
    public float clientPitch = 0.0F;

    private Entity targetEntity;
    private ShaoMissileProjectile targetMissile;
    private int fireCooldown;
    /** 总控台手动交战指令剩余 tick；>0 时等效于收到红石信号。 */
    private int manualOrderTicks;
    /** 累计未同步的瞄准变化 tick 数。 */
    private int aimSyncTicks;
    /** 客户端插值角度是否已经从 NBT 播种过（避免每次同步包都把插值打回目标角）。 */
    private boolean clientAnglesSeeded;
    /** 放置这座炮的玩家；null 表示归属未知（不施加 IFF 保护）。 */
    @Nullable
    private UUID ownerId;

    protected AbstractCiwsBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /** 本型号的性能参数；每个子类返回一个 static final 常量。 */
    protected abstract CiwsSpec spec();

    /** 总控台火力通道里显示的类型。 */
    public LauncherKind launcherKind() {
        return spec().launcherKind();
    }

    /** GeckoLib 炮塔水平旋转骨骼名（供渲染模型使用）。 */
    public String turretBone() {
        return spec().turretBone();
    }

    /** GeckoLib 炮管俯仰骨骼名（供渲染模型使用）。 */
    public String cannonBone() {
        return spec().cannonBone();
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

    // ---- 主循环 ----

    /**
     * 统一的方块 Ticker。
     *
     * <p>参数类型取 {@link AbstractCiwsBlockEntity}（父类型），因此可以直接以
     * {@code AbstractCiwsBlockEntity::tick} 作为 {@code BlockEntityTicker<子类>} 传入
     * {@code createTickerHelper}，子类无需各自再写一遍转发。
     */
    public static void tick(@NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState state,
            @NotNull AbstractCiwsBlockEntity be) {
        if (level.isClientSide()) {
            be.smoothRotateOnClient();
            return;
        }

        boolean engaged = level.hasNeighborSignal(pos) || be.manualOrderTicks > 0;
        if (be.manualOrderTicks > 0) {
            be.manualOrderTicks--;
        }
        if (!engaged) {
            be.stopTargeting(level, pos);
            return;
        }

        be.serverTrackTarget(level, pos);
    }

    /** 总控台下发的"手动交战"指令：在接下来 {@code ticks} tick 内等效于收到红石信号。 */
    public void orderManualEngagement(int ticks) {
        this.manualOrderTicks = Math.max(this.manualOrderTicks, Math.max(0, ticks));
    }

    /** 是否处于（红石或手动下达的）交战状态。 */
    public boolean isEngaged() {
        return this.manualOrderTicks > 0
                || (this.level != null && this.level.hasNeighborSignal(this.worldPosition));
    }

    private void stopTargeting(@NotNull Level level, @NotNull BlockPos pos) {
        this.targetEntity = null;
        this.targetMissile = null;
        if (this.targetPitch != 0.0F || this.targetYaw != 0.0F) {
            this.targetYaw = 0.0F;
            this.targetPitch = 0.0F;
            // 停火归零立即同步一次，否则炮塔会卡在旧角度直到下一次交战
            this.aimSyncTicks = 0;
            this.setChanged();
            level.sendBlockUpdated(pos, this.getBlockState(), this.getBlockState(), 3);
        }
    }

    // ---- 服务端：选靶与瞄准 ----

    private void serverTrackTarget(@NotNull Level level, @NotNull BlockPos pos) {
        BlockPos currentPos = pos.immutable();
        CiwsSpec spec = spec();
        double scanRange = spec.scanRange();

        AABB searchBox = new AABB(
                currentPos.getX() - scanRange,
                currentPos.getY() - scanRange,
                currentPos.getZ() - scanRange,
                currentPos.getX() + 1.0D + scanRange,
                currentPos.getY() + 1.0D + scanRange,
                currentPos.getZ() + 1.0D + scanRange);

        // 近防炮的现实定位：优先拦截来袭导弹，其次才是敌对生物
        Vec3 turretCenter = Vec3.atCenterOf(currentPos);
        this.targetMissile = level instanceof ServerLevel serverLevel
                ? ShaoMissileProjectile.activeWithin(serverLevel, turretCenter, scanRange).stream()
                        // 不打自己人发射的弹：否则同舰齐射/连续拦截会互相抵消
                        .filter(projectile -> !IffRules.isSameOwner(this.ownerId, projectile.getOwnerId()))
                        .min(Comparator.comparingDouble(projectile -> projectile.position().distanceToSqr(turretCenter)))
                        .orElse(null)
                : null;
        this.targetEntity = this.targetMissile == null
                ? level.getEntitiesOfClass(LivingEntity.class, searchBox,
                        entity -> IffRules.isHostile(entity) && IffRules.canEngage(this.ownerId, entity)).stream()
                        .min(Comparator.comparingDouble(entity -> entity.distanceToSqr(turretCenter)))
                        .orElse(null)
                : null;

        if (this.targetEntity == null && this.targetMissile == null) {
            if (this.targetPitch != 0.0F || this.targetYaw != 0.0F) {
                this.targetYaw = 0.0F;
                this.targetPitch = 0.0F;
                this.setChanged();
                level.sendBlockUpdated(currentPos, this.getBlockState(), this.getBlockState(), 3);
            }
            tryFire(level, currentPos);
            return;
        }

        Vec3 aimPoint = this.targetMissile != null ? this.targetMissile.position() : aimPointOf(this.targetEntity);
        double dx = aimPoint.x - (currentPos.getX() + 0.5D);
        double dy = aimPoint.y - (currentPos.getY() + 0.5D);
        double dz = aimPoint.z - (currentPos.getZ() + 0.5D);

        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);

        // 计算得出绝对角度
        float calcYaw = (float) (Math.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
        float calcPitch = (float) -(Math.atan2(dy, horizontalDistance) * (180.0 / Math.PI));

        if (Math.abs(this.targetYaw - calcYaw) > AIM_EPSILON_DEGREES
                || Math.abs(this.targetPitch - calcPitch) > AIM_EPSILON_DEGREES) {
            this.targetYaw = calcYaw;
            this.targetPitch = calcPitch;
            syncAimIfDue(level, currentPos);
        }

        tryFire(level, currentPos);
    }

    /**
     * 瞄准节流同步：每 {@link #AIM_SYNC_INTERVAL_TICKS} tick 才写一次 NBT 标记并广播方块实体包。
     *
     * <p>只在朝向确实发生变化时由 {@link #serverTrackTarget} 调用，因此静止目标不会产生任何包。
     */
    private void syncAimIfDue(@NotNull Level level, @NotNull BlockPos pos) {
        if (++this.aimSyncTicks < AIM_SYNC_INTERVAL_TICKS) {
            return;
        }
        this.aimSyncTicks = 0;
        this.setChanged();
        level.sendBlockUpdated(pos, this.getBlockState(), this.getBlockState(), 3);
    }

    private static Vec3 aimPointOf(Entity entity) {
        if (entity instanceof LivingEntity living) {
            return living.getEyePosition();
        }
        return entity.position().add(0.0D, entity.getBbHeight() * 0.5D, 0.0D);
    }

    // ---- 服务端：开火判定 ----

    private void tryFire(@NotNull Level level, @NotNull BlockPos pos) {
        if (fireCooldown > 0) {
            fireCooldown--;
        }

        Entity target = this.targetEntity;
        ShaoMissileProjectile missileTarget = this.targetMissile;
        if ((target == null && missileTarget == null)
                || (target != null && !target.isAlive())
                || (missileTarget != null && !missileTarget.isAlive())
                || fireCooldown > 0) {
            return;
        }

        // 防御性 IFF 复检：选靶阶段已过滤，这里再挡一道，避免将来改动选靶逻辑时误伤友军
        if (target != null && !IffRules.canEngage(this.ownerId, target)) {
            return;
        }
        if (missileTarget != null && IffRules.isSameOwner(this.ownerId, missileTarget.getOwnerId())) {
            return;
        }

        CiwsSpec spec = spec();
        Vec3 muzzle = Vec3.atCenterOf(pos).add(0.0D, spec.muzzleHeightOffset(), 0.0D);
        Vec3 aimPoint = missileTarget != null ? missileTarget.position() : aimPointOf(target);
        if (!hasClearLineOfSight(level, muzzle, aimPoint)) {
            return;
        }

        fireCooldown = spec.fireCooldownTicks();
        if (target instanceof LivingEntity living) {
            living.hurt(level.damageSources().generic(), spec.damagePerHit());
        } else if (missileTarget != null) {
            missileTarget.discard();
        }

        spawnTracer(level, muzzle, aimPoint);
        level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 1.4F, 1.6F);
    }

    private boolean hasClearLineOfSight(@NotNull Level level, Vec3 from, Vec3 to) {
        ClipContext ctx = new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, null);
        HitResult result = level.clip(ctx);
        return result.getType() == HitResult.Type.MISS;
    }

    private void spawnTracer(@NotNull Level level, Vec3 from, Vec3 to) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        Vec3 path = to.subtract(from);
        double length = path.length();
        if (length < 1.0E-4D) {
            return;
        }

        Vec3 step = path.scale(1.0D / length);
        int steps = (int) Math.min(48, length);
        for (int i = 1; i <= steps; i++) {
            Vec3 point = from.add(step.scale(i));
            serverLevel.sendParticles(ParticleTypes.CRIT, point.x, point.y, point.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    // ---- 客户端：机械转动阻尼插值 ----

    private void smoothRotateOnClient() {
        float rotateSpeed = spec().rotateSpeedDegPerTick();
        this.clientYaw = approachAngle(this.clientYaw, this.targetYaw, rotateSpeed);
        this.clientPitch = approachAngle(this.clientPitch, this.targetPitch, rotateSpeed);
    }

    private static float approachAngle(float current, float target, float maxStep) {
        float delta = (target - current) % 360.0F;
        if (delta > 180.0F) {
            delta -= 360.0F;
        }
        if (delta < -180.0F) {
            delta += 360.0F;
        }

        if (delta > maxStep) {
            return current + maxStep;
        }
        if (delta < -maxStep) {
            return current - maxStep;
        }
        return target;
    }

    // ---- 网络数据同步 ----

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        this.targetYaw = tag.getFloat(TAG_TARGET_YAW);
        this.targetPitch = tag.getFloat(TAG_TARGET_PITCH);
        this.ownerId = OwnerTracked.read(tag);
        // 只在第一次拿到服务端朝向时播种插值角度；之后每次同步包都不能重置，
        // 否则客户端的阻尼插值会被 4 tick 一次的同步反复打回目标角，看起来就是瞬移。
        if (!this.clientAnglesSeeded) {
            this.clientYaw = this.targetYaw;
            this.clientPitch = this.targetPitch;
            this.clientAnglesSeeded = true;
        }
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putFloat(TAG_TARGET_YAW, this.targetYaw);
        tag.putFloat(TAG_TARGET_PITCH, this.targetPitch);
        OwnerTracked.write(tag, this.ownerId);
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        saveAdditional(tag);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    // ---- GeckoLib ----

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // 炮塔朝向由 setCustomAnimations 直接写骨骼旋转，不走关键帧动画
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
