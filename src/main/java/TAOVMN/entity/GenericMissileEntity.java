package TAOVMN.entity;

import java.util.Optional;
import java.util.UUID;

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
import org.jetbrains.annotations.Nullable;
import rbasamoyai.createbigcannons.munitions.AbstractCannonProjectile;
import rbasamoyai.createbigcannons.munitions.ProjectileContext;
import rbasamoyai.createbigcannons.munitions.big_cannon.he_shell.HEShellProjectile;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * 通用导弹实体基类
 * P1、P15、P30等所有P系列导弹的父类
 * 
 * 提供通用逻辑：
 * - 安全延迟期处理
 * - 制导斜坡计算
 * - 锁定强度管理
 * - 目标追踪
 * - 生命周期管理
 * - 爆炸检测
 */
public abstract class GenericMissileEntity extends HEShellProjectile implements GeoEntity {
    
    // ===== 通用常量 =====
    protected static final int SAFETY_DELAY_TICKS = 30;
    protected static final int GUIDANCE_RAMP_TICKS = 20;
    protected static final int TARGET_MEMORY_TICKS = 100;
    protected static final int MAX_MISSILE_LIFETIME = 600;
    protected static final float MIN_CRUISE_SPEED_RATIO = 1.8F;
    protected static final float ACCELERATION_PER_TICK = 0.11F;
    protected static final double TARGET_SWITCH_DISTANCE = 14.0D;
    protected static final double TRACK_SMOOTH_BLEND = 0.28D;
    protected static final double TRACK_SWITCH_BLEND = 0.55D;
    protected static final int DEFAULT_LOCK_HOLD_TICKS = 16;
    protected static final double DEFAULT_SWITCH_ADVANTAGE_DISTANCE = 8.0D;
    protected static final double DEFAULT_FORCE_SWITCH_DISTANCE = 22.0D;
    protected static final float PHYSICS_TARGET_RANGE_BONUS = 400.0F;
    protected static final double PREDICTIVE_LEAD_DISTANCE = 0.35D;
    protected static final float LOCK_STRENGTH_GAIN = 0.02f;
    protected static final float LOCK_STRENGTH_DECAY = 0.01f;
    
    // ===== EntityDataAccessor =====
    protected static final EntityDataAccessor<Integer> DATA_FUSE = SynchedEntityData.defineId(GenericMissileEntity.class, EntityDataSerializers.INT);
    protected static final EntityDataAccessor<Float> DATA_LAUNCH_X = SynchedEntityData.defineId(GenericMissileEntity.class, EntityDataSerializers.FLOAT);
    protected static final EntityDataAccessor<Float> DATA_LAUNCH_Y = SynchedEntityData.defineId(GenericMissileEntity.class, EntityDataSerializers.FLOAT);
    protected static final EntityDataAccessor<Float> DATA_LAUNCH_Z = SynchedEntityData.defineId(GenericMissileEntity.class, EntityDataSerializers.FLOAT);
    protected static final EntityDataAccessor<Float> DATA_MAX_SPEED = SynchedEntityData.defineId(GenericMissileEntity.class, EntityDataSerializers.FLOAT);
    protected static final EntityDataAccessor<Float> DATA_STEERING = SynchedEntityData.defineId(GenericMissileEntity.class, EntityDataSerializers.FLOAT);
    protected static final EntityDataAccessor<Float> DATA_BLAST_POWER = SynchedEntityData.defineId(GenericMissileEntity.class, EntityDataSerializers.FLOAT);
    protected static final EntityDataAccessor<Float> DATA_SEEK_RANGE = SynchedEntityData.defineId(GenericMissileEntity.class, EntityDataSerializers.FLOAT);
    protected static final EntityDataAccessor<Float> DATA_PROXIMITY_FUSE = SynchedEntityData.defineId(GenericMissileEntity.class, EntityDataSerializers.FLOAT);
    protected static final EntityDataAccessor<Float> DATA_IFF_SAFE_RADIUS = SynchedEntityData.defineId(GenericMissileEntity.class, EntityDataSerializers.FLOAT);
    
    // ===== NBT标签 =====
    protected static final String TAG_FUSE = "FuzeTicks";
    protected static final String TAG_LAUNCH_X = "LaunchX";
    protected static final String TAG_LAUNCH_Y = "LaunchY";
    protected static final String TAG_LAUNCH_Z = "LaunchZ";
    protected static final String TAG_MAX_SPEED = "MaxSpeed";
    protected static final String TAG_STEERING = "Steering";
    protected static final String TAG_BLAST_POWER = "BlastPower";
    protected static final String TAG_SEEK_RANGE = "SeekRange";
    protected static final String TAG_PROXIMITY_FUSE = "ProximityFuse";
    protected static final String TAG_IFF_SAFE_RADIUS = "IffSafeRadius";
    protected static final String TAG_ASSIGNED_TARGET = "AssignedTarget";
    protected static final String TAG_DESIGNATED_TARGET_X = "DesignatedTargetX";
    protected static final String TAG_DESIGNATED_TARGET_Y = "DesignatedTargetY";
    protected static final String TAG_DESIGNATED_TARGET_Z = "DesignatedTargetZ";
    protected static final String TAG_LOCK_HOLD_TICKS = "LockHoldTicks";
    protected static final String TAG_SWITCH_ADVANTAGE_DISTANCE = "SwitchAdvantageDistance";
    protected static final String TAG_FORCE_SWITCH_DISTANCE = "ForceSwitchDistance";
    protected static final String TAG_RENDER_ITEM = "RenderItem";
    protected static final String TAG_FUZE_INSTALLED = "FuzeInstalled";
    protected static final String TAG_LOCK_STRENGTH = "LockStrength";
    protected static final String TAG_TARGET_LOCK_TICKS = "TargetLockTicks";
    
    // ===== 通用字段 =====
    protected final AnimatableInstanceCache geckoCache = GeckoLibUtil.createInstanceCache(this);
    protected ItemStack renderItem;
    protected UUID assignedTarget;
    protected Vec3 designatedTargetPos;
    protected Vec3 inertialTarget;
    protected Vec3 lastTargetVelocity = Vec3.ZERO;
    protected int targetMemoryTicks;
    protected int lockHoldTicks;
    protected int lockHoldTicksConfig = DEFAULT_LOCK_HOLD_TICKS;
    protected double switchAdvantageDistance = DEFAULT_SWITCH_ADVANTAGE_DISTANCE;
    protected double forceSwitchDistance = DEFAULT_FORCE_SWITCH_DISTANCE;
    protected boolean fuzeInstalled = false;
    protected float lockStrength = 0.0f;
    protected int targetLockTicks = 0;
    
    // ===== Director火控参数 =====
    protected Vec3 directorProvidedTarget = null;     // Director指定的目标位置
    protected float directorLockStrength = 0.0f;      // Director的锁定强度（0-1）
    protected net.minecraft.core.BlockPos directorPos = null;  // 提供火控的Director位置
    protected int directorUpdateTicks = 0;            // Director参数更新计时
    
    public GenericMissileEntity(EntityType<? extends GenericMissileEntity> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(true);
        this.renderItem = getDefaultRenderItem();
    }
    
    // ===== 抽象方法 =====
    /**
     * 获取默认的渲染物品
     */
    protected abstract ItemStack getDefaultRenderItem();
    
    /**
     * 获取默认的导弹配置
     */
    protected abstract MissileProfile getDefaultProfile();
    
    // ===== 核心方法 =====
    
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
    protected double getGuidanceRamp() {
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
    protected void updateLockStrength(Vec3 targetPos) {
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

    public void setItem(ItemStack stack) {
        this.renderItem = stack.copy();
    }

    public ItemStack getItem() {
        return renderItem;
    }

    @Override
    protected void defineSynchedData() {
        super.m_8097_();
        MissileProfile defaults = getDefaultProfile();
        entityData.define(DATA_FUSE, 600);
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

    protected Vec3 getLauncherOrigin() {
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

    /**
     * 接收Director火控系统提供的目标信息和火控参数
     * @param targetPos Director锁定的目标位置
     * @param lockStrength Director的锁定强度（0-1）
     * @param directorBlockPos 提供火控的Director方块位置
     */
    public void setDirectedTargetInfo(Vec3 targetPos, float lockStrength, net.minecraft.core.BlockPos directorBlockPos) {
        this.directorProvidedTarget = targetPos;
        this.directorLockStrength = Mth.clamp(lockStrength, 0.0f, 1.0f);
        this.directorPos = directorBlockPos;
        this.directorUpdateTicks = tickCount;
    }

    /**
     * 获取Director提供的目标位置（如果存在）
     */
    @Nullable
    public Vec3 getDirectorProvidedTarget() {
        // 如果Director参数超过200 ticks未更新，则清除
        if (this.directorProvidedTarget != null && (tickCount - this.directorUpdateTicks) > 200) {
            this.directorProvidedTarget = null;
        }
        return this.directorProvidedTarget;
    }

    /**
     * 获取Director的锁定强度
     */
    public float getDirectorLockStrength() {
        return this.directorLockStrength;
    }

    /**
     * 检查是否有有效的Director火控参数
     */
    public boolean hasValidDirectorGuidance() {
        return this.directorProvidedTarget != null && this.directorLockStrength > 0.0f;
    }

    public void setGuidanceTuning(int lockHoldTicks, double switchAdvantageDistance, double forceSwitchDistance) {
        this.lockHoldTicksConfig = Math.max(0, lockHoldTicks);
        this.switchAdvantageDistance = Math.max(0.0D, switchAdvantageDistance);
        this.forceSwitchDistance = Math.max(0.0D, forceSwitchDistance);
    }

    protected float maxSpeed() {
        return entityData.get(DATA_MAX_SPEED);
    }

    protected float steering() {
        return entityData.get(DATA_STEERING);
    }

    protected float blastPower() {
        return entityData.get(DATA_BLAST_POWER);
    }

    protected float seekRange() {
        return entityData.get(DATA_SEEK_RANGE);
    }

    protected float proximityFuse() {
        return entityData.get(DATA_PROXIMITY_FUSE);
    }

    protected float iffSafeRadius() {
        return entityData.get(DATA_IFF_SAFE_RADIUS);
    }

    @Override
    protected void detonate(net.minecraft.core.Position pos) {
        if (!level().isClientSide) {
            level().explode(this, pos.x(), pos.y(), pos.z(), blastPower(),
                    net.minecraft.world.level.Level.ExplosionInteraction.TNT);
        }
        discard();
    }

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
            handleSafetyDelay();
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

        // 执行制导和爆炸逻辑
        handleGuidance();
        handleDetonation();
    }

    protected void handleSafetyDelay() {
        setNoGravity(true);
        Vec3 motion = getDeltaMovement();
        Vec3 newPos = position().add(motion);
        setPos(newPos.x, newPos.y, newPos.z);
        alignToFlightDirection();
        if (level().isClientSide) {
            spawnTrailParticles();
        }
    }

    protected void handleGuidance() {
        Vec3 targetPos = resolveAssignedTargetPos();
        float physicsSeekRange = seekRange() + PHYSICS_TARGET_RANGE_BONUS;
        Optional<Vec3> physicsTarget = PhysicsTargeting.findNearestPhysicsTarget(level(), position(), physicsSeekRange, maxSpeed(), getLauncherOrigin(), iffSafeRadius());

        if (physicsTarget.isPresent()) {
            updateDynamicDesignatedTarget(position(), physicsTarget.get());
            targetPos = designatedTargetPos;
        } else if (targetPos == null && designatedTargetPos != null) {
            targetPos = designatedTargetPos;
        }

        if (targetPos != null) {
            inertialTarget = targetPos;
            targetMemoryTicks = TARGET_MEMORY_TICKS;
        } else if (targetMemoryTicks > 0 && inertialTarget != null) {
            targetMemoryTicks--;
            targetPos = inertialTarget;
        } else {
            inertialTarget = null;
            targetMemoryTicks = 0;
            if (designatedTargetPos != null) {
                targetPos = designatedTargetPos;
            }
        }

        if (targetPos != null) {
            double distance = position().distanceTo(targetPos);
            Vec3 current = getDeltaMovement();
            double currentSpeed = current.length();
            double desiredSpeed = computeNextSpeed(currentSpeed);
            
            updateLockStrength(targetPos);
            
            double guidanceRamp = getGuidanceRamp();
            float effectiveSteeringGain = steering() * (float)guidanceRamp * lockStrength;
            
            double speedRatio = currentSpeed > 0 ? desiredSpeed / currentSpeed : 1.0D;
            
            if (distance < 15.0D) {
                effectiveSteeringGain = Math.min(1.0F, effectiveSteeringGain * (2.0F + (float)(speedRatio - 1.0D)));
            } else if (distance < 30.0D) {
                effectiveSteeringGain = Math.min(1.0F, effectiveSteeringGain * (1.6F + (float)(speedRatio - 1.0D) * 0.5F));
            } else if (distance < 60.0D) {
                effectiveSteeringGain = Math.min(1.0F, effectiveSteeringGain * (1.3F + (float)(speedRatio - 1.0D) * 0.3F));
            } else if (distance < 120.0D) {
                effectiveSteeringGain = Math.min(1.0F, effectiveSteeringGain * 1.1F);
            }

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
            updateLockStrength(null);
            Vec3 currentMotion = getDeltaMovement();
            if (currentMotion.lengthSqr() < 0.01D) {
                setDeltaMovement(getLookAngle().normalize().scale(maxSpeed() * MIN_CRUISE_SPEED_RATIO));
            } else {
                double nextSpeed = computeNextSpeed(currentMotion.length());
                setDeltaMovement(currentMotion.normalize().scale(nextSpeed));
            }
        }
    }

    protected void handleDetonation() {
        if (!fuzeInstalled && tickCount > SAFETY_DELAY_TICKS && tickCount >= MAX_MISSILE_LIFETIME) {
            detonate();
        }
    }

    protected double computeNextSpeed(double currentSpeed) {
        double minSpeed = maxSpeed() * MIN_CRUISE_SPEED_RATIO;
        double normalizedCurrent = Math.max(minSpeed, currentSpeed);
        return Math.min(maxSpeed(), normalizedCurrent + ACCELERATION_PER_TICK);
    }

    protected void updateDynamicDesignatedTarget(Vec3 missilePos, Vec3 candidate) {
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
        lastTargetVelocity = designatedTargetPos.subtract(lastPos);
        lockHoldTicks = Math.max(0, lockHoldTicks - 1);
    }

    protected void alignToFlightDirection() {
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

    protected void spawnTrailParticles() {
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

    protected Vec3 resolveAssignedTargetPos() {
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

        if (hasValidDirectorGuidance()) {
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

    protected LivingEntity findNearestValidTarget(ServerLevel level, double radius) {
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
            renderItem = getDefaultRenderItem();
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
                tag.contains(TAG_MAX_SPEED) ? tag.getFloat(TAG_MAX_SPEED) : getDefaultProfile().maxSpeed(),
                tag.contains(TAG_STEERING) ? tag.getFloat(TAG_STEERING) : getDefaultProfile().steering(),
                tag.contains(TAG_BLAST_POWER) ? tag.getFloat(TAG_BLAST_POWER) : getDefaultProfile().blastPower(),
                tag.contains(TAG_SEEK_RANGE) ? tag.getFloat(TAG_SEEK_RANGE) : getDefaultProfile().seekRange(),
                tag.contains(TAG_PROXIMITY_FUSE) ? tag.getFloat(TAG_PROXIMITY_FUSE) : getDefaultProfile().proximityFuse(),
                tag.contains(TAG_IFF_SAFE_RADIUS) ? tag.getFloat(TAG_IFF_SAFE_RADIUS) : getDefaultProfile().iffSafeRadius()));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // 保留用于未来的飞行中导弹动画控制器
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geckoCache;
    }
}
