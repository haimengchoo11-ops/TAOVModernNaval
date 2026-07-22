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
import net.minecraft.world.phys.Vec3;
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
    private static final int TARGET_MEMORY_TICKS = 20;
    private static final float MIN_CRUISE_SPEED_RATIO = 2.0F;
    private static final float ACCELERATION_PER_TICK = 0.055F;
    private static final double TARGET_SWITCH_DISTANCE = 14.0D;
    private static final double TRACK_SMOOTH_BLEND = 0.28D;
    private static final double TRACK_SWITCH_BLEND = 0.55D;
    private static final int DEFAULT_LOCK_HOLD_TICKS = 16;
    private static final double DEFAULT_SWITCH_ADVANTAGE_DISTANCE = 8.0D;
    private static final double DEFAULT_FORCE_SWITCH_DISTANCE = 22.0D;
    private static final float PHYSICS_TARGET_RANGE_BONUS = 500.0F;

    private final AnimatableInstanceCache geckoCache = GeckoLibUtil.createInstanceCache(this);
    private ItemStack renderItem = new ItemStack(ModItems.P1_MISSILE.get());
    private UUID assignedTarget;
    private Vec3 designatedTargetPos;
    private Vec3 inertialTarget;
    private int targetMemoryTicks;
    private int lockHoldTicks;
    private int lockHoldTicksConfig = DEFAULT_LOCK_HOLD_TICKS;
    private double switchAdvantageDistance = DEFAULT_SWITCH_ADVANTAGE_DISTANCE;
    private double forceSwitchDistance = DEFAULT_FORCE_SWITCH_DISTANCE;

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
        entityData.define(DATA_FUSE, 200);
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
        setExplosionCountdown(clamped);
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

    @Override
    public void tick() {
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

        entityData.set(DATA_FUSE, Math.max(0, getExplosionCountdown()));
        if (getExplosionCountdown() <= 0) {
            detonate();
            return;
        }

        Vec3 targetPos = resolveAssignedTargetPos();
        float physicsSeekRange = seekRange() + PHYSICS_TARGET_RANGE_BONUS;
        Optional<Vec3> physicsTarget = PhysicsTargeting.findNearestPhysicsTarget(level(), position(), physicsSeekRange, maxSpeed(), getLauncherOrigin(), iffSafeRadius());

        if (targetPos == null) {
            if (physicsTarget.isPresent()) {
                updateDynamicDesignatedTarget(position(), physicsTarget.get());
                targetPos = designatedTargetPos;
            } else if (designatedTargetPos != null) {
                targetPos = designatedTargetPos;
            }
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
        }

        if (targetPos != null) {
            // 有目标时：渐进加速 + 末段增强转向
            double distance = position().distanceTo(targetPos);
            float steeringGain = steering();
            if (distance < 18.0D) {
                steeringGain = Math.min(1.0F, steeringGain * 1.6F);
            } else if (distance < 36.0D) {
                steeringGain = Math.min(1.0F, steeringGain * 1.3F);
            }

            Vec3 current = getDeltaMovement();
            double currentSpeed = current.length();
            double desiredSpeed = computeNextSpeed(currentSpeed);

            Vec3 toTarget = targetPos.subtract(position());
            if (toTarget.lengthSqr() > 1.0E-6D) {
                Vec3 desired = toTarget.normalize().scale(desiredSpeed);
                Vec3 blended = current.scale(1.0D - steeringGain).add(desired.scale(steeringGain));
                if (blended.lengthSqr() > 1.0E-6D) {
                    blended = blended.normalize().scale(desiredSpeed);
                    setDeltaMovement(blended);
                }
            }

            if (distance < proximityFuse()) {
                detonate();
                return;
            }
        } else {
            // 无目标时保持飞行，直到寿命（引信倒计时）结束再自爆
            Vec3 currentMotion = getDeltaMovement();
            if (currentMotion.lengthSqr() < 0.01D) {
                setDeltaMovement(getLookAngle().normalize().scale(maxSpeed() * MIN_CRUISE_SPEED_RATIO));
            } else {
                double nextSpeed = computeNextSpeed(currentMotion.length());
                setDeltaMovement(currentMotion.normalize().scale(nextSpeed));
            }
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
        designatedTargetPos = designatedTargetPos.lerp(candidate, blend);
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
        if (assignedTarget == null || !(level() instanceof ServerLevel serverLevel)) {
            return null;
        }

        var entity = serverLevel.getEntity(assignedTarget);
        if (!(entity instanceof LivingEntity living) || !living.isAlive()) {
            assignedTarget = null;
            return null;
        }

        Vec3 targetPoint = living.position().add(0.0D, living.getBbHeight() * 0.5D, 0.0D);
        return PhysicsTargeting.predictInterceptPoint(position(), targetPoint, living.getDeltaMovement(), maxSpeed());
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