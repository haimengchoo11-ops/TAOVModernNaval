package TAOVMN.entity;

import com.verr1.cbcswap.projectile.AbstractCannonShaojectile;
import com.verr1.cbcswap.projectile.ProjectileContext;
import com.verr1.cbcswap.projectile.big_cannon.HEShellShaojectile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.shao.shaolib.projectile.ShaoEntityType;
import org.jetbrains.annotations.Nullable;
import rbasamoyai.createbigcannons.munitions.big_cannon.he_shell.HEShellProjectile;
import TAOVMN.registry.ModEntities;
import TAOVMN.util.IffRules;
import TAOVMN.util.MissileProfile;
import TAOVMN.util.PhysicsTargeting;
import TAOVMN.util.TargetDomain;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ShaoMissileProjectile extends HEShellShaojectile {
    private static final EntityType<HEShellProjectile> DELEGATE_TYPE = EntityType.Builder
            .<HEShellProjectile>of(HEShellProjectile::new, MobCategory.MISC)
            .sized(0.5F, 0.5F)
            .build("taovmodernnaval:missile_delegate");
    private static final Set<ShaoMissileProjectile> ACTIVE = ConcurrentHashMap.newKeySet();
    private static final int TARGET_MEMORY_TICKS = 100;
    private static final int SAFETY_DELAY_TICKS = 30;
    private static final int MAX_MISSILE_LIFETIME = 600;
    private static final int MISSILE_LOCAL_SCAN_INTERVAL = 2;
    private static final double MISSILE_LOCAL_SCAN_RADIUS = 24.0D;
    private static final int PHYSICS_TARGET_SCAN_INTERVAL = 2;
    private static final float PHYSICS_TARGET_RANGE_BONUS = 400.0F;
    private static final float ACCELERATION_PER_TICK = 0.11F;
    private static final double PREDICTIVE_LEAD_DISTANCE = 0.35D;
    private static final double TARGET_SWITCH_DISTANCE = 14.0D;
    private static final double STEERING_DISTANCE_SCALE = 15.0D;
    private static final double TRACK_SMOOTH_BLEND = 0.28D;
    private static final double TRACK_SWITCH_BLEND = 0.55D;
    private static final int DEFAULT_LOCK_HOLD_TICKS = 16;
    private static final double DEFAULT_SWITCH_ADVANTAGE_DISTANCE = 8.0D;
    private static final double DEFAULT_FORCE_SWITCH_DISTANCE = 22.0D;
    /** 指挥仪锁定强度低于该阈值时视为无效火控（优先使用弹载自主寻的）。 */
    private static final float DIRECTOR_LOCK_THRESHOLD = 0.3F;
    /** 指挥仪火控参数的有效期（tick）；与指挥仪广播周期解耦，允许短暂丢包。 */
    private static final int DIRECTOR_GUIDANCE_MEMORY_TICKS = 200;
    /** 满锁定时的转向精度系数：火控数据把末端转向收敛到基础增益的该比例。 */
    private static final float DIRECTOR_PRECISION_BASE = 0.85F;

    private final MissileProfile defaultProfile;
    private final ItemStack renderItem;
    private MissileProfile profile;
    private UUID assignedTarget;
    private Vec3 designatedTargetPos;
    private Vec3 inertialTarget;
    private Vec3 lastTargetVelocity = Vec3.ZERO;
    private Vec3 launcherOrigin = Vec3.ZERO;
    /** 发射方归属（放置发射装置的玩家 / 投掷导弹的玩家）；null = 归属未知，不受 IFF 保护。 */
    @Nullable
    private UUID ownerId;
    private Optional<Vec3> cachedPhysicsTarget = Optional.empty();
    private int age;
    private int fuseTicks = MAX_MISSILE_LIFETIME;
    private int targetMemoryTicks;
    private int lockHoldTicks;
    private int lockHoldTicksConfig = DEFAULT_LOCK_HOLD_TICKS;
    private int localScanCooldown;
    private int physicsTargetScanCooldown;
    private double switchAdvantageDistance = DEFAULT_SWITCH_ADVANTAGE_DISTANCE;
    private double forceSwitchDistance = DEFAULT_FORCE_SWITCH_DISTANCE;
    private boolean fuzeInstalled;
    private Vec3 directorProvidedTarget;
    private float directorLockStrength;
    private BlockPos directorSourcePos;
    private int directorUpdateTicks;
    private TargetDomain directorTargetDomain = TargetDomain.UNIVERSAL;

    public ShaoMissileProjectile(ServerLevel level, ItemStack stack, Vec3 position) {
        this(level, stack, position, MissileProfile.resolve(stack));
    }

    public ShaoMissileProjectile(ServerLevel level, ItemStack stack, Vec3 position, MissileProfile profile) {
        super(new HEShellProjectile(DELEGATE_TYPE, level),
                ModEntities.getShaoType(stack), level);
        this.renderItem = stack.copyWithCount(1);
        this.defaultProfile = profile;
        this.profile = profile;
        setPosition(position);
        setNoGravity(true);
        syncDelegatePosition();
    }

    public static List<ShaoMissileProjectile> activeWithin(ServerLevel level, Vec3 center, double radius) {
        ACTIVE.removeIf(projectile -> !projectile.isAlive());
        double radiusSqr = radius * radius;
        return ACTIVE.stream()
                .filter(projectile -> projectile.isAlive()
                        && projectile.level() == level
                        && projectile.position().distanceToSqr(center) <= radiusSqr)
                .toList();
    }

    @Override
    public void spawn() {
        super.spawn();
        ACTIVE.add(this);
    }

    @Override
    public void discard() {
        ACTIVE.remove(this);
        super.discard();
    }

    public void setFuzeTicks(int ticks) {
        fuseTicks = Math.max(1, ticks);
    }

    public int getRemainingFuzeTicks() {
        return Math.max(0, fuseTicks - age);
    }

    public void setFuzeInstalled(boolean installed) {
        fuzeInstalled = installed;
    }

    public boolean isFuzeInstalled() {
        return fuzeInstalled;
    }

    public void setLauncherOrigin(Vec3 origin) {
        launcherOrigin = origin;
    }

    /** 发射方归属；用于敌我识别（见 {@link IffRules}）。 */
    @Nullable
    public UUID getOwnerId() {
        return ownerId;
    }

    /**
     * 设置发射方归属。
     *
     * <p>{@code null} 表示归属未知（CBC 悬挂炮、结构生成装置等），此时弹体不享受任何
     * IFF 保护，相关判定与引入归属概念之前完全一致。
     */
    public void setOwnerId(@Nullable UUID ownerId) {
        this.ownerId = ownerId;
    }

    public void applyProfile(MissileProfile nextProfile) {
        MissileProfile resolved = nextProfile != null ? nextProfile : defaultProfile;
        profile = new MissileProfile(
                clampFinite(resolved.maxSpeed(), defaultProfile.maxSpeed(), 0.2F, 16.0F),
                clampFinite(resolved.steering(), defaultProfile.steering(), 0.02F, 1.0F),
                clampFinite(resolved.blastPower(), defaultProfile.blastPower(), 0.1F, 64.0F),
                clampFinite(resolved.seekRange(), defaultProfile.seekRange(), 8.0F, 4096.0F),
                clampFinite(resolved.proximityFuse(), defaultProfile.proximityFuse(), 0.2F, 8.0F),
                clampFinite(resolved.iffSafeRadius(), defaultProfile.iffSafeRadius(), 0.0F, 64.0F));
    }

    public void setAssignedTarget(UUID targetUuid) {
        assignedTarget = targetUuid;
    }

    public void setDesignatedTargetPos(Vec3 targetPos) {
        designatedTargetPos = targetPos;
    }

    public void setGuidanceTuning(int lockHoldTicks, double switchAdvantageDistance, double forceSwitchDistance) {
        lockHoldTicksConfig = Math.max(0, lockHoldTicks);
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

    /**
     * 接收 P-1 火控指挥仪广播的火控参数。
     *
     * @param target       指挥仪锁定的目标中心坐标
     * @param lockStrength 指挥仪锁定强度（0-1）
     * @param directorPos  指挥仪方块坐标（仅用于诊断/后续扩展）
     */
    public void setDirectorTarget(Vec3 target, float lockStrength, BlockPos directorPos) {
        setDirectorTarget(target, lockStrength, directorPos, TargetDomain.UNIVERSAL);
    }

    /**
     * 接收 P-1 火控指挥仪广播的火控参数（含目标域）。
     *
     * <p>目标域用于飞行途中校验：防空弹不会去追指挥仪锁定的水面目标，
     * 反舰弹也不会被空中目标带走。这样一部指挥仪可以同时为两类弹提供火控，
     * 由弹体自己按 {@link TargetDomain#engages(TargetDomain)} 取用。
     *
     * @param target       指挥仪锁定的目标中心坐标
     * @param lockStrength 指挥仪锁定强度（0-1）
     * @param directorPos  指挥仪方块坐标（仅用于诊断/后续扩展）
     * @param targetDomain 被锁定目标的目标域；{@code null} 视为通用
     */
    public void setDirectorTarget(Vec3 target, float lockStrength, BlockPos directorPos, TargetDomain targetDomain) {
        directorProvidedTarget = target;
        directorLockStrength = Mth.clamp(lockStrength, 0.0F, 1.0F);
        directorSourcePos = directorPos;
        directorTargetDomain = targetDomain == null ? TargetDomain.UNIVERSAL : targetDomain;
        directorUpdateTicks = age;
    }

    @Override
    public void tickDetonate() {
    }

    @Override
    public void tick() {
        if (!isAlive()) {
            return;
        }

        age++;
        setNoGravity(true);
        if (age <= SAFETY_DELAY_TICKS) {
            setPosition(position().add(velocity()));
        } else {
            super.tick();
            if (!isAlive()) {
                return;
            }
        }
        syncDelegatePosition();
        spawnTrailParticles();

        ServerLevel serverLevel = level();
        maybeRefreshLocalTargetScan(serverLevel);
        Vec3 targetPos = resolveGuidanceTarget(serverLevel);
        if (targetPos != null) {
            guideToward(targetPos);
        } else {
            maintainCruiseFlight();
        }

        if (fuzeInstalled && age > SAFETY_DELAY_TICKS && checkProximityFuse(serverLevel)) {
            return;
        }

        if (!fuzeInstalled && age >= MAX_MISSILE_LIFETIME) {
            detonate(position());
        }
    }

    @Override
    protected boolean onImpact(HitResult hitResult, AbstractCannonShaojectile.ImpactResult impactResult,
            ProjectileContext context) {
        if (isAlive() && fuzeInstalled && age > SAFETY_DELAY_TICKS) {
            detonate(hitResult.getLocation());
        }
        return true;
    }

    @Override
    protected boolean onClip(ProjectileContext context, Vec3 from, Vec3 to) {
        if (isAlive() && fuzeInstalled && age > SAFETY_DELAY_TICKS) {
            detonate(from.add(to).scale(0.5D));
        }
        return true;
    }

    @Override
    protected void detonate(Position position) {
        if (isAlive()) {
            level().explode(original(), position.x(), position.y(), position.z(), profile.blastPower(),
                    Level.ExplosionInteraction.TNT);
            discard();
        }
    }

    private Vec3 resolveGuidanceTarget(ServerLevel serverLevel) {
        Vec3 targetPos = resolveAssignedTargetPos(serverLevel);
        Optional<Vec3> physicsTarget = findPhysicsTarget(serverLevel, profile.seekRange() + PHYSICS_TARGET_RANGE_BONUS);

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
            targetPos = designatedTargetPos;
        }
        return targetPos;
    }

    private Vec3 resolveAssignedTargetPos(ServerLevel serverLevel) {
        // 优先级 1：P-1 火控指挥仪广播的火控目标（最新鲜、最可信）
        Vec3 directorTarget = resolveDirectorTarget();
        if (directorTarget != null) {
            return directorTarget;
        }

        if (assignedTarget != null) {
            var entity = serverLevel.getEntity(assignedTarget);
            // 火控指派也要过 IFF：否则雷达把友军目标指派下来，弹体侧的寻的过滤等于白做
            if (entity instanceof LivingEntity living && living.isAlive() && IffRules.canEngage(ownerId, living)) {
                Vec3 targetPoint = living.position().add(0.0D, living.getBbHeight() * 0.5D, 0.0D);
                return PhysicsTargeting.predictInterceptPoint(position(), targetPoint, living.getDeltaMovement(), profile.maxSpeed());
            }
            assignedTarget = null;
        }

        if (designatedTargetPos != null) {
            return designatedTargetPos;
        }

        LivingEntity nearest = findNearestValidTarget(serverLevel, profile.seekRange() + 32.0D);
        if (nearest == null) {
            return null;
        }
        Vec3 targetPoint = nearest.position().add(0.0D, nearest.getBbHeight() * 0.5D, 0.0D);
        return PhysicsTargeting.predictInterceptPoint(position(), targetPoint, nearest.getDeltaMovement(), profile.maxSpeed());
    }

    /**
     * 指挥仪火控参数是否仍然有效：锁定强度高于阈值，且未超过
     * {@link TAOVMN.block.entity.P1DirectorBlockEntity#FIRE_CONTROL_MEMORY_TICKS} tick 未更新。
     */
    public boolean hasValidDirectorGuidance() {
        return directorProvidedTarget != null
                && directorLockStrength > DIRECTOR_LOCK_THRESHOLD
                && age - directorUpdateTicks <= DIRECTOR_GUIDANCE_MEMORY_TICKS;
    }

    @Nullable
    private Vec3 resolveDirectorTarget() {
        if (!hasValidDirectorGuidance()) {
            directorProvidedTarget = null;
            return null;
        }
        // 目标域不匹配（例如反舰弹收到指挥仪的空中目标）时视为没有火控，回退到自主寻的
        if (!profile.domain().engages(directorTargetDomain)) {
            return null;
        }
        return directorProvidedTarget;
    }

    /** 指挥仪火控带来的末段转向精度加成（锁定越强转向越精确）。 */
    private float directorPrecisionBoost() {
        if (!hasValidDirectorGuidance()) {
            return 1.0F;
        }
        return DIRECTOR_PRECISION_BASE + (1.0F - DIRECTOR_PRECISION_BASE) * directorLockStrength;
    }

    private void guideToward(Vec3 targetPos) {
        double distance = position().distanceTo(targetPos);
        Vec3 current = velocity();
        double currentSpeed = current.length();
        double desiredSpeed = Math.min(profile.maxSpeed(), currentSpeed + ACCELERATION_PER_TICK);
        float steeringGain = adjustSteeringGain(profile.steering(), distance) * directorPrecisionBoost();
        Vec3 predictedPos = targetPos.add(lastTargetVelocity.scale(PREDICTIVE_LEAD_DISTANCE));
        Vec3 toTarget = predictedPos.subtract(position());
        if (toTarget.lengthSqr() <= 1.0E-6D) {
            return;
        }

        Vec3 desired = toTarget.normalize().scale(desiredSpeed);
        Vec3 blended = current.scale(1.0D - steeringGain).add(desired.scale(steeringGain));
        if (blended.lengthSqr() > 1.0E-6D) {
            setVelocity(blended.normalize().scale(desiredSpeed));
        }
    }

    private float adjustSteeringGain(float steeringGain, double distance) {
        double proximityMultiplier = 1.0D + STEERING_DISTANCE_SCALE / (STEERING_DISTANCE_SCALE + distance);
        return Mth.clamp(steeringGain * (float) proximityMultiplier, 0.0F, 1.0F);
    }

    private void maintainCruiseFlight() {
        Vec3 motion = velocity();
        if (motion.lengthSqr() < 0.01D) {
            return;
        }
        setVelocity(motion.normalize().scale(Math.min(profile.maxSpeed(), motion.length() + ACCELERATION_PER_TICK)));
    }

    private void updateDynamicDesignatedTarget(Vec3 missilePos, Vec3 candidate) {
        if (candidate == null) {
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

        Vec3 lastPos = designatedTargetPos;
        double blend = separation <= TARGET_SWITCH_DISTANCE ? TRACK_SMOOTH_BLEND : TRACK_SWITCH_BLEND;
        designatedTargetPos = designatedTargetPos.lerp(candidate, blend);
        lastTargetVelocity = designatedTargetPos.subtract(lastPos);
        lockHoldTicks = Math.max(0, lockHoldTicks - 1);
    }

    private void maybeRefreshLocalTargetScan(ServerLevel serverLevel) {
        if (localScanCooldown > 0) {
            localScanCooldown--;
            return;
        }
        localScanCooldown = MISSILE_LOCAL_SCAN_INTERVAL;
        double radius = Math.min(MISSILE_LOCAL_SCAN_RADIUS, Math.max(12.0D, profile.seekRange() * 0.4D));
        LivingEntity nearest = findNearestValidTarget(serverLevel, radius);
        if (nearest == null) {
            return;
        }
        Vec3 targetPoint = nearest.position().add(0.0D, nearest.getBbHeight() * 0.5D, 0.0D);
        designatedTargetPos = PhysicsTargeting.predictInterceptPoint(position(), targetPoint,
                nearest.getDeltaMovement(), profile.maxSpeed());
        lastTargetVelocity = nearest.getDeltaMovement();
        targetMemoryTicks = Math.max(targetMemoryTicks, 8);
    }

    private Optional<Vec3> findPhysicsTarget(ServerLevel serverLevel, float range) {
        if (physicsTargetScanCooldown > 0) {
            physicsTargetScanCooldown--;
            if (physicsTargetScanCooldown > 0) {
                return cachedPhysicsTarget;
            }
        }
        cachedPhysicsTarget = PhysicsTargeting.findNearestPhysicsTarget(serverLevel, position(), range,
                profile.maxSpeed(), launcherOrigin, profile.iffSafeRadius());
        physicsTargetScanCooldown = PHYSICS_TARGET_SCAN_INTERVAL;
        return cachedPhysicsTarget;
    }

    private LivingEntity findNearestValidTarget(ServerLevel serverLevel, double radius) {
        Vec3 origin = position();
        AABB searchBox = new AABB(origin.subtract(radius, radius, radius), origin.add(radius, radius, radius));
        return serverLevel.getEntitiesOfClass(LivingEntity.class, searchBox, candidate -> candidate.isAlive()
                        && !candidate.isSpectator()
                        && candidate.distanceToSqr(origin) <= radius * radius
                        && candidate.position().distanceToSqr(launcherOrigin) > profile.iffSafeRadius() * profile.iffSafeRadius()
                        // 只自主交战「交战对象」（敌对生物或玩家）：否则弹体会去追一头牛
                        && IffRules.isHostile(candidate)
                        && IffRules.canEngage(ownerId, candidate)
                        && engagesLivingTarget(candidate, origin))
                .stream()
                .min((left, right) -> Double.compare(left.distanceToSqr(origin), right.distanceToSqr(origin)))
                .orElse(null);
    }

    /**
     * 目标域过滤：防空弹只交战空中目标，反舰弹只交战水面/地面目标。
     *
     * <p>"空中"的判定与雷达保持一致（不在地面且明显高于海平面参考点）。参考高度优先取
     * {@link #launcherOrigin}（发射舰甲板），未设置时退回本弹当前位置，保证默认行为不受影响。
     */
    private boolean engagesLivingTarget(LivingEntity candidate, Vec3 origin) {
        double referenceY = launcherOrigin.lengthSqr() > 1.0E-6D ? launcherOrigin.y : origin.y;
        boolean airborne = !candidate.onGround() && candidate.getY() > referenceY + 3.0D;
        return profile.domain().engages(TargetDomain.of(airborne));
    }

    private boolean checkProximityFuse(ServerLevel serverLevel) {
        double radius = profile.proximityFuse();
        LivingEntity target = findNearestValidTarget(serverLevel, radius);
        if (target != null) {
            Vec3 targetPoint = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
            if (targetPoint.distanceToSqr(position()) <= radius * radius) {
                detonate(targetPoint);
                return true;
            }
        }
        return checkMissileProximityFuse(serverLevel, radius);
    }

    /**
     * 反导近炸：防空弹在近炸半径内遇到<b>另一枚在飞弹体</b>即起爆。
     *
     * <p>这是"防空反导"能真正打到来袭导弹的关键——来袭弹体不是 {@code LivingEntity}，
     * 只靠对生物的近炸判定永远打不到。命中点取两弹中点，避免爆炸偏心。
     */
    private boolean checkMissileProximityFuse(ServerLevel serverLevel, double radius) {
        if (!profile.domain().engages(TargetDomain.ANTI_AIR)) {
            return false;
        }
        for (ShaoMissileProjectile other : activeWithin(serverLevel, position(), radius)) {
            if (other == this) {
                continue;
            }
            // 不打自己人发射的弹：否则同舰齐射的防空弹会互相引爆，只有首发能飞出去
            if (IffRules.isSameOwner(ownerId, other.getOwnerId())) {
                continue;
            }
            Vec3 impactPoint = position().add(other.position()).scale(0.5D);
            detonate(impactPoint);
            return true;
        }
        return false;
    }

    private void syncDelegatePosition() {
        original().setPos(position().x, position().y, position().z);
    }

    private void spawnTrailParticles() {
        ServerLevel serverLevel = level();
        if (age % 2 != 0) {
            return;
        }
        Vec3 motion = velocity();
        Vec3 backwards = motion.lengthSqr() > 1.0E-4D ? motion.normalize().scale(-0.35D) : Vec3.ZERO;
        Vec3 trail = position().add(backwards).add(0.0D, 0.15D, 0.0D);
        serverLevel.sendParticles(ParticleTypes.SMOKE, trail.x, trail.y, trail.z, 1, 0.0D, 0.01D, 0.0D, 0.0D);
        if (getRemainingFuzeTicks() < 20) {
            serverLevel.sendParticles(ParticleTypes.LAVA, trail.x, trail.y, trail.z, 1, 0.0D, 0.02D, 0.0D, 0.0D);
        }
    }

    private static float clampFinite(float value, float fallback, float min, float max) {
        return Mth.clamp(Float.isFinite(value) ? value : fallback, min, max);
    }
}