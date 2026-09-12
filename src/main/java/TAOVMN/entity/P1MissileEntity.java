package TAOVMN.entity;

import java.util.Optional;
import java.util.UUID;

import TAOVMN.registry.ModEntities;
import TAOVMN.registry.ModItems;
import TAOVMN.util.MissileProfile;
import TAOVMN.util.PhysicsTargeting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import rbasamoyai.createbigcannons.munitions.AbstractCannonProjectile;
import rbasamoyai.createbigcannons.munitions.ProjectileContext;
import rbasamoyai.createbigcannons.munitions.big_cannon.he_shell.HEShellProjectile;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

public class P1MissileEntity extends HEShellProjectile implements GeoEntity {
    private static final EntityDataAccessor<Integer> DATA_FUSE = SynchedEntityData.defineId(P1MissileEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_LAUNCH_X = SynchedEntityData.defineId(P1MissileEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_LAUNCH_Y = SynchedEntityData.defineId(P1MissileEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_LAUNCH_Z = SynchedEntityData.defineId(P1MissileEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_MAX_SPEED = SynchedEntityData.defineId(P1MissileEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_STEERING = SynchedEntityData.defineId(P1MissileEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_BLAST_POWER = SynchedEntityData.defineId(P1MissileEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_SEEK_RANGE = SynchedEntityData.defineId(P1MissileEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_PROXIMITY_FUSE = SynchedEntityData.defineId(P1MissileEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_IFF_SAFE_RADIUS = SynchedEntityData.defineId(P1MissileEntity.class, EntityDataSerializers.FLOAT);
    private static final String TAG_FUSE = "FuzeTicks";
    private static final String TAG_LAUNCH_X = "LaunchX";
    private static final String TAG_LAUNCH_Y = "LaunchY";
    private static final String TAG_LAUNCH_Z = "LaunchZ";
    private static final String TAG_MAX_SPEED = "MaxSpeed";
    private static final String TAG_STEERING = "Steering";
    private static final String TAG_BLAST_POWER = "BlastPower";
    private static final String TAG_SEEK_RANGE = "SeekRange";
    private static final String TAG_PROXIMITY_FUSE = "ProximityFuse";
    private static final String TAG_IFF_SAFE_RADIUS = "IffSafeRadius";
    private static final String TAG_ASSIGNED_TARGET = "AssignedTarget";
    private static final String TAG_DESIGNATED_TARGET_X = "DesignatedTargetX";
    private static final String TAG_DESIGNATED_TARGET_Y = "DesignatedTargetY";
    private static final String TAG_DESIGNATED_TARGET_Z = "DesignatedTargetZ";
    private static final String TAG_LOCK_HOLD_TICKS = "LockHoldTicks";
    private static final String TAG_SWITCH_ADVANTAGE_DISTANCE = "SwitchAdvantageDistance";
    private static final String TAG_FORCE_SWITCH_DISTANCE = "ForceSwitchDistance";
    private static final String TAG_RENDER_ITEM = "RenderItem";
    private static final String TAG_FUZE_INSTALLED = "FuzeInstalled";
    private static final String TAG_LOCK_STRENGTH = "LockStrength";
    private static final String TAG_TARGET_LOCK_TICKS = "TargetLockTicks";
    private static final int TARGET_MEMORY_TICKS = 100;
    private static final float MIN_CRUISE_SPEED_RATIO = 1.8F;
    private static final float ACCELERATION_PER_TICK = 0.11F;
    private static final double TARGET_SWITCH_DISTANCE = 14.0D;
    private static final double TRACK_SMOOTH_BLEND = 0.28D;
    private static final int SAFETY_DELAY_TICKS = 30;
    private static final double TRACK_SWITCH_BLEND = 0.55D;
    private static final int DEFAULT_LOCK_HOLD_TICKS = 16;
    private static final double DEFAULT_SWITCH_ADVANTAGE_DISTANCE = 8.0D;
    private static final double DEFAULT_FORCE_SWITCH_DISTANCE = 22.0D;
    private static final float PHYSICS_TARGET_RANGE_BONUS = 400.0F;
    private static final double PREDICTIVE_LEAD_DISTANCE = 0.35D;
    private static final int MAX_MISSILE_LIFETIME = 600;  // 导弹最大寿命：600 tick = 30秒
    private static final int GUIDANCE_RAMP_TICKS = 20;    // 制导斜坡：前20 ticks逐步建立制导能力
    private static final float LOCK_STRENGTH_GAIN = 0.02f; // 每 tick增加2%锁定强度（50 ticks满锁定）
    private static final float LOCK_STRENGTH_DECAY = 0.01f; // 每 tick衰减1%锁定强度

    private final AnimatableInstanceCache geckoCache = GeckoLibUtil.createInstanceCache(this);
    private ItemStack renderItem = new ItemStack(ModItems.P1_MISSILE.get());
    private UUID assignedTarget;
    private Vec3 designatedTargetPos;
    private Vec3 inertialTarget;
    private Vec3 lastTargetVelocity = Vec3.ZERO;
    private int targetMemoryTicks;
    private int lockHoldTicks;
    private int lockHoldTicksConfig = DEFAULT_LOCK_HOLD_TICKS;
    private double switchAdvantageDistance = DEFAULT_SWITCH_ADVANTAGE_DISTANCE;
    private double forceSwitchDistance = DEFAULT_FORCE_SWITCH_DISTANCE;
    private boolean fuzeInstalled = false;
    private float lockStrength = 0.0f;  // 锁定强度：0-1，影响制导能力
    private int targetLockTicks = 0;    // 目标被锁定的持续时间（用于诊断）

    protected Vec3 directorProvidedTarget = null;
    protected float directorLockStrength = 0.0f;
    protected net.minecraft.core.BlockPos directorPos = null;
    protected int directorUpdateTicks = 0;

    public void setFuzeInstalled(boolean installed) {
        this.fuzeInstalled = installed;
    }

    public boolean isFuzeInstalled() {
        return fuzeInstalled;
    }

    /**
     * 获取制导斜坡（0-1）
     * 前GUIDANCE_RAMP_TICKS内从0逐步增加到1，防止初期过度转向
     */
    private double getGuidanceRamp() {
        if (tickCount < SAFETY_DELAY_TICKS) {
            return 0.0;
        }
        double rampProgress = (tickCount - SAFETY_DELAY_TICKS) / (double) GUIDANCE_RAMP_TICKS;
        return Math.min(1.0, rampProgress);
    }

    /**
     * 更新锁定强度
     * 目标在范围内时增加，超出范围时衰减
     */
    private void updateLockStrength(Vec3 targetPos) {
        if (targetPos != null) {
            double distance = position().distanceTo(targetPos);
            if (distance < seekRange() * 1.5) {  // 有效搜索范围内
                lockStrength = Math.min(1.0f, lockStrength + LOCK_STRENGTH_GAIN);
                targetLockTicks++;
            } else {
                lockStrength = Math.max(0.0f, lockStrength - LOCK_STRENGTH_DECAY);
                targetLockTicks = Math.max(0, targetLockTicks - 1);
            }
        } else {
            lockStrength = Math.max(0.0f, lockStrength - LOCK_STRENGTH_DECAY);
            targetLockTicks = Math.max(0, targetLockTicks - 1);
        }
    }

    public float getLockStrength() {
        return lockStrength;
    }

    public int getTargetLockTicks() {
        return targetLockTicks;
    }

    public P1MissileEntity(EntityType<? extends P1MissileEntity> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(true);
    }

    public P1MissileEntity(Level level, double x, double y, double z) {
        this(ModEntities.P1_MISSILE.get(), level);
        setPos(x, y, z);
    }

    public void setItem(ItemStack stack) {
        this.renderItem = stack.copy();
    }

    public ItemStack getItem() {
        return renderItem;
    }

    @Override
    protected void defineSynchedData() {
        super.m_8097_();
        MissileProfile defaults = MissileProfile.p1Default();
        entityData.define(DATA_FUSE, 600);  // 600 tick = 30秒（半分钟）
        entityData.define(DATA_LAUNCH_X, 0.0F);
        entityData.define(DATA_LAUNCH_Y, 0.0F);
        entityData.define(DATA_LAUNCH_Z, 0.0F);
        entityData.define(DATA_MAX_SPEED, defaults.maxSpeed());
        entityData.define(DATA_STEERING, defaults.steering());
        entityData.define(DATA_BLAST_POWER, defaults.blastPower());
        entityData.define(DATA_SEEK_RANGE, defaults.seekRange());
        entityData.define(DATA_PROXIMITY_FUSE, defaults.proximityFuse());
        entityData.define(DATA_IFF_SAFE_RADIUS, defaults.iffSafeRadius());
    }

    public void setFuzeTicks(int ticks) {
        int clamped = Math.max(1, ticks);
        entityData.set(DATA_FUSE, clamped);
    }

    public void setLauncherOrigin(Vec3 origin) {
        entityData.set(DATA_LAUNCH_X, (float) origin.x);
        entityData.set(DATA_LAUNCH_Y, (float) origin.y);
        entityData.set(DATA_LAUNCH_Z, (float) origin.z);
    }

    private Vec3 getLauncherOrigin() {
        return new Vec3(entityData.get(DATA_LAUNCH_X), entityData.get(DATA_LAUNCH_Y), entityData.get(DATA_LAUNCH_Z));
    }

    private int getFuzeTicks() {
        int countdown = getExplosionCountdown();
        return countdown > 0 ? countdown : entityData.get(DATA_FUSE);
    }

    public int getRemainingFuzeTicks() {
        return getFuzeTicks();
    }

    public void applyProfile(MissileProfile profile) {
        entityData.set(DATA_MAX_SPEED, Math.max(0.2F, profile.maxSpeed()));
        entityData.set(DATA_STEERING, Mth.clamp(profile.steering(), 0.02F, 1.0F));
        entityData.set(DATA_BLAST_POWER, Math.max(0.1F, profile.blastPower()));
        entityData.set(DATA_SEEK_RANGE, Math.max(8.0F, profile.seekRange()));
        entityData.set(DATA_PROXIMITY_FUSE, Mth.clamp(profile.proximityFuse(), 0.2F, 8.0F));
        entityData.set(DATA_IFF_SAFE_RADIUS, Mth.clamp(profile.iffSafeRadius(), 0.0F, 64.0F));
    }

    public void setAssignedTarget(UUID targetUuid) {
        this.assignedTarget = targetUuid;
    }

    public void setDesignatedTargetPos(Vec3 targetPos) {
        this.designatedTargetPos = targetPos;
    }

    public void setGuidanceTuning(int lockHoldTicks, double switchAdvantageDistance, double forceSwitchDistance) {
        this.lockHoldTicksConfig = Math.max(0, lockHoldTicks);
        this.switchAdvantageDistance = Math.max(0.0D, switchAdvantageDistance);
        this.forceSwitchDistance = Math.max(0.0D, forceSwitchDistance);
    }

    public static int defaultLockHoldTicks() {
        return DEFAULT_LOCK_HOLD_TICKS;
    }

    public static double defaultSwitchAdvantageDistance() {
        return DEFAULT_SWITCH_ADVANTAGE_DISTANCE;
    }

    public static double defaultForceSwitchDistance() {
        return DEFAULT_FORCE_SWITCH_DISTANCE;
    }

    private float maxSpeed() {
        return entityData.get(DATA_MAX_SPEED);
    }

    private float steering() {
        return entityData.get(DATA_STEERING);
    }

    private float blastPower() {
        return entityData.get(DATA_BLAST_POWER);
    }

    private float seekRange() {
        return entityData.get(DATA_SEEK_RANGE);
    }

    private float proximityFuse() {
        return entityData.get(DATA_PROXIMITY_FUSE);
    }

    private float iffSafeRadius() {
        return entityData.get(DATA_IFF_SAFE_RADIUS);
    }

    // 重写爆炸：使用 vanilla TNT 爆炸模式，确保破坏方块
    @Override
    protected void detonate(net.minecraft.core.Position pos) {
        if (!level().isClientSide) {
            level().explode(this, pos.x(), pos.y(), pos.z(), blastPower(),
                    net.minecraft.world.level.Level.ExplosionInteraction.TNT);
        }
        discard();
    }

    // 重写碰撞引爆：绕过父类 canDetonate() 检查，需引信已安装，且发射 30 tick 后才生效
    @Override
    protected boolean onImpact(HitResult hitResult,
            AbstractCannonProjectile.ImpactResult impactResult,
            ProjectileContext ctx) {
        if (!level().isClientSide && isAlive() && fuzeInstalled && tickCount > SAFETY_DELAY_TICKS) {
            detonate(hitResult.getLocation());
        }
        return true;
    }

    @Override
    protected boolean onClip(ProjectileContext ctx, Vec3 from, Vec3 to) {
        if (!level().isClientSide && isAlive() && fuzeInstalled && tickCount > SAFETY_DELAY_TICKS) {
            detonate(from.add(to).scale(0.5D));
        }
        return true;
    }

    @Override
    public void tick() {
        // 安全延迟期间（前30个tick）：仅执行基础运动和方向，不运行父类碰撞检测
        if (tickCount <= SAFETY_DELAY_TICKS) {
            tickCount++;
            setNoGravity(true);
            // 直接设置位置，绕过move()的碰撞检测
            Vec3 motion = getDeltaMovement();
            Vec3 newPos = position().add(motion);
            setPos(newPos.x, newPos.y, newPos.z);
            alignToFlightDirection();
            if (level().isClientSide) {
                spawnTrailParticles();
            }
            return;
        }

        // 安全期后：恢复父类 tick 以保留追踪/物理更新，但清掉 CBC 自带倒计时
        setExplosionCountdown(-1);
        super.tick();
        if (!isAlive()) {
            return;
        }
        setNoGravity(true);
        alignToFlightDirection();

        if (level().isClientSide) {
            spawnTrailParticles();
            return;
        }

        // 注意：碰撞引信由 onImpact/onClip 处理，不需要在这里重复检测

        Vec3 targetPos = resolveAssignedTargetPos();
        float physicsSeekRange = seekRange() + PHYSICS_TARGET_RANGE_BONUS;
        Optional<Vec3> physicsTarget = PhysicsTargeting.findNearestPhysicsTarget(level(), position(), physicsSeekRange, maxSpeed(), getLauncherOrigin(), iffSafeRadius());

        // 持续追踪逻辑：每 tick 都尝试寻找目标
        if (physicsTarget.isPresent()) {
            updateDynamicDesignatedTarget(position(), physicsTarget.get());
            targetPos = designatedTargetPos;
        } else if (targetPos == null && designatedTargetPos != null) {
            // 使用上一个指定目标
            targetPos = designatedTargetPos;
        }

        if (targetPos != null) {
            inertialTarget = targetPos;
            targetMemoryTicks = TARGET_MEMORY_TICKS;
        } else if (targetMemoryTicks > 0 && inertialTarget != null) {
            targetMemoryTicks--;
            targetPos = inertialTarget;
        } else {
            // 目标丧失后，清空记忆但继续下一 tick 寻找
            inertialTarget = null;
            targetMemoryTicks = 0;
            // 不立即清空 targetPos，让下一个 tick 继续尝试寻找
            if (designatedTargetPos != null) {
                targetPos = designatedTargetPos;
            }
        }

        if (targetPos != null) {
            // 有目标时：渐进加速 + 动态转向 + 速度预测
            double distance = position().distanceTo(targetPos);
            Vec3 current = getDeltaMovement();
            double currentSpeed = current.length();
            double desiredSpeed = computeNextSpeed(currentSpeed);
            
            // 更新锁定强度
            updateLockStrength(targetPos);
            
            // 获取制导斜坡和锁定强度的综合效果
            double guidanceRamp = getGuidanceRamp();
            float effectiveSteeringGain = steering() * (float)guidanceRamp * lockStrength;
            
            // 动态转向增益：基于距离和速度差
            double speedRatio = currentSpeed > 0 ? desiredSpeed / currentSpeed : 1.0D;
            
            // 距离越近，转向越敏捷；速度差越大，转向越强
            if (distance < 15.0D) {
                effectiveSteeringGain = Math.min(1.0F, effectiveSteeringGain * (2.0F + (float)(speedRatio - 1.0D)));
            } else if (distance < 30.0D) {
                effectiveSteeringGain = Math.min(1.0F, effectiveSteeringGain * (1.6F + (float)(speedRatio - 1.0D) * 0.5F));
            } else if (distance < 60.0D) {
                effectiveSteeringGain = Math.min(1.0F, effectiveSteeringGain * (1.3F + (float)(speedRatio - 1.0D) * 0.3F));
            } else if (distance < 120.0D) {
                effectiveSteeringGain = Math.min(1.0F, effectiveSteeringGain * 1.1F);
            }

            // 预测拦截点：考虑目标速度
            Vec3 targetVelocity = lastTargetVelocity;
            Vec3 predictedPos = targetPos.add(targetVelocity.scale(PREDICTIVE_LEAD_DISTANCE));
            Vec3 toTarget = predictedPos.subtract(position());
            
            if (toTarget.lengthSqr() > 1.0E-6D) {
                Vec3 desired = toTarget.normalize().scale(desiredSpeed);
                Vec3 blended = current.scale(1.0D - effectiveSteeringGain).add(desired.scale(effectiveSteeringGain));
                if (blended.lengthSqr() > 1.0E-6D) {
                    blended = blended.normalize().scale(desiredSpeed);
                    setDeltaMovement(blended);
                }
            }

        } else {
            // 目标丧失时衰减锁定强度
            updateLockStrength(null);
            // 无目标时保持飞行，直到寿命（引信倒计时）结束再自爆
            Vec3 currentMotion = getDeltaMovement();
            if (currentMotion.lengthSqr() < 0.01D) {
                setDeltaMovement(getLookAngle().normalize().scale(maxSpeed() * MIN_CRUISE_SPEED_RATIO));
            } else {
                double nextSpeed = computeNextSpeed(currentMotion.length());
                setDeltaMovement(currentMotion.normalize().scale(nextSpeed));
            }
        }

        // 分离机制：
        // - 有引信导弹：碰撞爆炸（通过引信控制）
        // - 无引信导弹：经过最大寿命（600 tick = 30秒）后自动爆炸，不受getFuzeTicks()影响
        if (!fuzeInstalled && tickCount > SAFETY_DELAY_TICKS && tickCount >= MAX_MISSILE_LIFETIME) {
            detonate();
        }
    }

    private double computeNextSpeed(double currentSpeed) {
        double minSpeed = maxSpeed() * MIN_CRUISE_SPEED_RATIO;
        double normalizedCurrent = Math.max(minSpeed, currentSpeed);
        return Math.min(maxSpeed(), normalizedCurrent + ACCELERATION_PER_TICK);
    }

    private void updateDynamicDesignatedTarget(Vec3 missilePos, Vec3 candidate) {
        if (candidate == null || missilePos == null) {
            return;
        }

        if (designatedTargetPos == null) {
            designatedTargetPos = candidate;
            lockHoldTicks = lockHoldTicksConfig;
            return;
        }

        double currentDistance = missilePos.distanceTo(designatedTargetPos);
        double candidateDistance = missilePos.distanceTo(candidate);
        double separation = designatedTargetPos.distanceTo(candidate);

        boolean significantlyBetter = candidateDistance + switchAdvantageDistance < currentDistance;
        boolean forceSwitch = separation > forceSwitchDistance && candidateDistance < currentDistance;

        if (lockHoldTicks > 0 && !significantlyBetter && !forceSwitch) {
            lockHoldTicks--;
            return;
        }

        if (significantlyBetter || forceSwitch) {
            designatedTargetPos = designatedTargetPos.lerp(candidate, TRACK_SWITCH_BLEND);
            lockHoldTicks = lockHoldTicksConfig;
            return;
        }

        double blend = separation <= TARGET_SWITCH_DISTANCE ? TRACK_SMOOTH_BLEND : TRACK_SWITCH_BLEND;
        Vec3 lastPos = designatedTargetPos;
        designatedTargetPos = designatedTargetPos.lerp(candidate, blend);
        // 更新目标速度用于预测
        lastTargetVelocity = designatedTargetPos.subtract(lastPos);
        lockHoldTicks = Math.max(0, lockHoldTicks - 1);
    }

    private void alignToFlightDirection() {
        Vec3 motion = getDeltaMovement();
        if (motion.lengthSqr() < 1.0E-6D) {
            return;
        }

        double horizontal = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
        float yaw = (float) (Mth.atan2(motion.x, motion.z) * (180.0D / Math.PI));
        float pitch = (float) (Mth.atan2(motion.y, horizontal) * (180.0D / Math.PI));

        yRotO = getYRot();
        xRotO = getXRot();
        setYRot(yaw);
        setXRot(-pitch);
        setYBodyRot(getYRot());
        setYHeadRot(getYRot());
    }

    private void spawnTrailParticles() {
        Vec3 motion = getDeltaMovement();
        Vec3 backwards = motion.lengthSqr() > 1.0E-4D ? motion.normalize().scale(-0.35D) : new Vec3(0.0D, -0.05D, 0.0D);
        double trailX = getX() + backwards.x;
        double trailY = getY() + 0.15D + backwards.y;
        double trailZ = getZ() + backwards.z;

        level().addParticle(ParticleTypes.SMOKE, trailX, trailY, trailZ, 0.0D, 0.01D, 0.0D);
        level().addParticle(ParticleTypes.FLAME, trailX, trailY, trailZ, 0.0D, 0.0D, 0.0D);

        if (getRemainingFuzeTicks() < 20 && tickCount % 2 == 0) {
            level().addParticle(ParticleTypes.LAVA, trailX, trailY, trailZ, 0.0D, 0.02D, 0.0D);
        }
    }

    private Vec3 resolveAssignedTargetPos() {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return null;
        }

        if (assignedTarget != null) {
            var entity = serverLevel.getEntity(assignedTarget);
            if (entity instanceof LivingEntity living && living.isAlive()) {
                Vec3 targetPoint = living.position().add(0.0D, living.getBbHeight() * 0.5D, 0.0D);
                return PhysicsTargeting.predictInterceptPoint(position(), targetPoint, living.getDeltaMovement(), maxSpeed());
            }
            assignedTarget = null;
        }

        if (hasDirectorGuidance()) {
            return directorProvidedTarget;
        }

        if (designatedTargetPos != null) {
            return designatedTargetPos;
        }

        LivingEntity nearest = findNearestValidTarget(serverLevel, seekRange() + 32.0D);
        if (nearest == null) {
            return null;
        }

        Vec3 targetPoint = nearest.position().add(0.0D, nearest.getBbHeight() * 0.5D, 0.0D);
        return PhysicsTargeting.predictInterceptPoint(position(), targetPoint, nearest.getDeltaMovement(), maxSpeed());
    }

    private boolean hasDirectorGuidance() {
        return directorProvidedTarget != null && directorLockStrength > 0.0f && (tickCount - directorUpdateTicks) <= 200;
    }

    private LivingEntity findNearestValidTarget(ServerLevel level, double radius) {
        AABB searchBox = new AABB(position().subtract(radius, radius, radius), position().add(radius, radius, radius));
        LivingEntity best = null;
        double bestDistance = Double.MAX_VALUE;

        for (LivingEntity candidate : level.getEntitiesOfClass(LivingEntity.class, searchBox, entity ->
                entity != null
                        && entity.isAlive()
                        && entity.getId() != this.getId()
                        && !entity.isSpectator()
                        && entity.distanceToSqr(this) <= radius * radius)) {
            double distance = candidate.distanceToSqr(this);
            if (distance < bestDistance) {
                best = candidate;
                bestDistance = distance;
            }
        }

        return best;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt(TAG_FUSE, getFuzeTicks());
        Vec3 launcherOrigin = getLauncherOrigin();
        tag.putDouble(TAG_LAUNCH_X, launcherOrigin.x);
        tag.putDouble(TAG_LAUNCH_Y, launcherOrigin.y);
        tag.putDouble(TAG_LAUNCH_Z, launcherOrigin.z);
        tag.putFloat(TAG_MAX_SPEED, maxSpeed());
        tag.putFloat(TAG_STEERING, steering());
        tag.putFloat(TAG_BLAST_POWER, blastPower());
        tag.putFloat(TAG_SEEK_RANGE, seekRange());
        tag.putFloat(TAG_PROXIMITY_FUSE, proximityFuse());
        tag.putFloat(TAG_IFF_SAFE_RADIUS, iffSafeRadius());
        tag.put(TAG_RENDER_ITEM, renderItem.save(new CompoundTag()));
        if (assignedTarget != null) {
            tag.putUUID(TAG_ASSIGNED_TARGET, assignedTarget);
        }
        if (designatedTargetPos != null) {
            tag.putDouble(TAG_DESIGNATED_TARGET_X, designatedTargetPos.x);
            tag.putDouble(TAG_DESIGNATED_TARGET_Y, designatedTargetPos.y);
            tag.putDouble(TAG_DESIGNATED_TARGET_Z, designatedTargetPos.z);
        }
        tag.putInt(TAG_LOCK_HOLD_TICKS, lockHoldTicksConfig);
        tag.putDouble(TAG_SWITCH_ADVANTAGE_DISTANCE, switchAdvantageDistance);
        tag.putDouble(TAG_FORCE_SWITCH_DISTANCE, forceSwitchDistance);
        tag.putBoolean(TAG_FUZE_INSTALLED, fuzeInstalled);
        tag.putFloat(TAG_LOCK_STRENGTH, lockStrength);
        tag.putInt(TAG_TARGET_LOCK_TICKS, targetLockTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains(TAG_FUSE)) {
            setFuzeTicks(tag.getInt(TAG_FUSE));
        }
        if (tag.contains(TAG_LAUNCH_X) && tag.contains(TAG_LAUNCH_Y) && tag.contains(TAG_LAUNCH_Z)) {
            setLauncherOrigin(new Vec3(tag.getDouble(TAG_LAUNCH_X), tag.getDouble(TAG_LAUNCH_Y), tag.getDouble(TAG_LAUNCH_Z)));
        }
        if (tag.contains(TAG_RENDER_ITEM, 10)) {
            renderItem = ItemStack.of(tag.getCompound(TAG_RENDER_ITEM));
        } else {
            renderItem = new ItemStack(ModItems.P1_MISSILE.get());
        }
        if (tag.hasUUID(TAG_ASSIGNED_TARGET)) {
            assignedTarget = tag.getUUID(TAG_ASSIGNED_TARGET);
        } else {
            assignedTarget = null;
        }
        if (tag.contains(TAG_DESIGNATED_TARGET_X) && tag.contains(TAG_DESIGNATED_TARGET_Y) && tag.contains(TAG_DESIGNATED_TARGET_Z)) {
            designatedTargetPos = new Vec3(tag.getDouble(TAG_DESIGNATED_TARGET_X), tag.getDouble(TAG_DESIGNATED_TARGET_Y), tag.getDouble(TAG_DESIGNATED_TARGET_Z));
        } else {
            designatedTargetPos = null;
        }
        setGuidanceTuning(
            tag.contains(TAG_LOCK_HOLD_TICKS) ? tag.getInt(TAG_LOCK_HOLD_TICKS) : DEFAULT_LOCK_HOLD_TICKS,
            tag.contains(TAG_SWITCH_ADVANTAGE_DISTANCE) ? tag.getDouble(TAG_SWITCH_ADVANTAGE_DISTANCE) : DEFAULT_SWITCH_ADVANTAGE_DISTANCE,
            tag.contains(TAG_FORCE_SWITCH_DISTANCE) ? tag.getDouble(TAG_FORCE_SWITCH_DISTANCE) : DEFAULT_FORCE_SWITCH_DISTANCE
        );
        if (tag.contains(TAG_FUZE_INSTALLED)) {
            fuzeInstalled = tag.getBoolean(TAG_FUZE_INSTALLED);
        }
        if (tag.contains(TAG_LOCK_STRENGTH)) {
            lockStrength = tag.getFloat(TAG_LOCK_STRENGTH);
        }
        if (tag.contains(TAG_TARGET_LOCK_TICKS)) {
            targetLockTicks = tag.getInt(TAG_TARGET_LOCK_TICKS);
        }
        inertialTarget = null;
        targetMemoryTicks = 0;
        lockHoldTicks = 0;

        applyProfile(new MissileProfile(
                tag.contains(TAG_MAX_SPEED) ? tag.getFloat(TAG_MAX_SPEED) : MissileProfile.p1Default().maxSpeed(),
                tag.contains(TAG_STEERING) ? tag.getFloat(TAG_STEERING) : MissileProfile.p1Default().steering(),
                tag.contains(TAG_BLAST_POWER) ? tag.getFloat(TAG_BLAST_POWER) : MissileProfile.p1Default().blastPower(),
                tag.contains(TAG_SEEK_RANGE) ? tag.getFloat(TAG_SEEK_RANGE) : MissileProfile.p1Default().seekRange(),
                tag.contains(TAG_PROXIMITY_FUSE) ? tag.getFloat(TAG_PROXIMITY_FUSE) : MissileProfile.p1Default().proximityFuse(),
                tag.contains(TAG_IFF_SAFE_RADIUS) ? tag.getFloat(TAG_IFF_SAFE_RADIUS) : MissileProfile.p1Default().iffSafeRadius()));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // Reserved for future in-flight missile animation controllers.
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geckoCache;
    }
}