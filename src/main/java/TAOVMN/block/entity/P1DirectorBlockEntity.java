package TAOVMN.block.entity;

import java.util.List;
import java.util.UUID;

import TAOVMN.entity.ShaoMissileProjectile;
import TAOVMN.registry.ModBlockEntities;
import TAOVMN.util.TargetDomain;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * P-1 火控指挥仪：在普通雷达基础上扩大扫描半径（256 格）、加快刷新（10 tick），
 * 并作为舰上火控节点向射程内的在飞导弹<b>广播火控参数</b>（目标位置 + 锁定强度）。
 *
 * <p>工作周期：
 * <ol>
 *   <li>每 tick 旋转雷达天线（{@value #RADAR_SCAN_SPEED_DEGREES}°/tick）；</li>
 *   <li>每 {@link #scanInterval()} tick 扫描并刷新被跟踪目标；</li>
 *   <li>每 tick 更新主目标锁定强度；</li>
 *   <li>每 {@link #BROADCAST_INTERVAL} tick 向 {@value #FIRE_CONTROL_RANGE} 格内的在飞导弹分发火控参数。</li>
 * </ol>
 *
 * <p>导弹端由 {@link ShaoMissileProjectile#setDirectorTarget} 接收；参数超过
 * {@value #FIRE_CONTROL_MEMORY_TICKS} tick 未更新即自动过期，参见导弹端的判定逻辑。
 */
public class P1DirectorBlockEntity extends RadarBlockEntity {
    /** 火控参数广播范围（格）。 */
    public static final double FIRE_CONTROL_RANGE = 64.0D;
    /** 广播间隔（tick）。 */
    public static final int BROADCAST_INTERVAL = 5;
    /** 锁定强度每 tick 增长量（约 20 tick 完全锁定）。 */
    public static final float LOCK_STRENGTH_GAIN = 0.05F;
    /** 切换主目标后锁定强度回退到的基线（保留部分锁定，避免导弹丢失制导）。 */
    public static final float LOCK_STRENGTH_RETAINED = 0.3F;
    /** 导弹侧认为火控参数仍然有效的时长（tick），与导弹端保持一致。 */
    public static final int FIRE_CONTROL_MEMORY_TICKS = 200;
    /** 雷达天线扫描速度（度/tick）。 */
    private static final float RADAR_SCAN_SPEED_DEGREES = 3.0F;
    /** 判定"主目标被替换"的位置容差（格）：目标正常移动不应重置锁定，只有明显跳变才算切换。 */
    private static final double TARGET_SWITCH_TOLERANCE = 4.0D;
    /** 位置容差的平方，用于与 distanceToSqr 同量纲比较。 */
    private static final double TARGET_SWITCH_TOLERANCE_SQR = TARGET_SWITCH_TOLERANCE * TARGET_SWITCH_TOLERANCE;

    private static final String TAG_RADAR_ANGLE = "RadarAngle";
    private static final String TAG_LOCK_STRENGTH = "LockStrength";
    private static final String TAG_TRACKED_COUNT = "TrackedCount";
    private static final String TAG_PRIMARY_TARGET = "PrimaryTarget";

    private UUID primaryTargetUuid;
    private Vec3 primaryTargetPos;
    private float lockStrength;
    private float radarAngle;
    private int broadcastTicks;

    public P1DirectorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.P1_DIRECTOR.get(), pos, state);
    }

    @Override
    protected double scanRange() {
        return 256.0D;
    }

    @Override
    protected int scanInterval() {
        return 10;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, P1DirectorBlockEntity director) {
        director.radarAngle = (director.radarAngle + RADAR_SCAN_SPEED_DEGREES) % 360.0F;
        RadarBlockEntity.tick(level, pos, state, director);

        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        director.updateLockStrength();
        if (++director.broadcastTicks >= BROADCAST_INTERVAL) {
            director.broadcastTicks = 0;
            director.broadcastFireControl(serverLevel, pos);
        }
    }

    // ---- 目标跟踪 / 锁定强度 ----

    /** 主目标在扫描周期之间保持有效时逐步提升锁定强度；丢失后逐步衰减。 */
    private void updateLockStrength() {
        lockStrength = nextLockStrength(lockStrength, primaryTargetPos != null);
    }

    /**
     * 锁定强度的纯函数推进（便于单元测试）：
     * 有目标时每 tick 增长 {@link #LOCK_STRENGTH_GAIN}，无目标时以两倍速率衰减，结果截断在 [0,1]。
     */
    public static float nextLockStrength(float current, boolean hasTarget) {
        float next = hasTarget
                ? current + LOCK_STRENGTH_GAIN
                : current - LOCK_STRENGTH_GAIN * 2.0F;
        return Mth.clamp(next, 0.0F, 1.0F);
    }

    /** 切换主目标时锁定强度回退到基线（保留部分锁定，避免在飞导弹瞬间丢失制导）。 */
    public static float retainedLockStrength(float current) {
        return Math.min(current, LOCK_STRENGTH_RETAINED);
    }

    /**
     * 主目标是否发生切换：UUID 变化、原位置丢失，或目标已移动超过
     * {@link #TARGET_SWITCH_TOLERANCE} 格（同一目标的正常移动不算切换）。
     */
    public static boolean isTargetSwitch(@Nullable UUID currentUuid, @Nullable Vec3 currentPos,
            @Nullable UUID nextUuid, @Nullable Vec3 nextPos) {
        if (currentUuid == null) {
            return false;
        }
        if (!currentUuid.equals(nextUuid) || nextPos == null) {
            return true;
        }
        return currentPos == null || currentPos.distanceToSqr(nextPos) > TARGET_SWITCH_TOLERANCE_SQR;
    }

    /** 扫描结果落库：把雷达选定的目标提升为主目标，并维护锁定强度连续性。 */
    @Override
    protected void scan(ServerLevel level) {
        super.scan(level);
        applyScanResult(getSelectedTargetUuid(), getSelectedTargetPos());
    }

    /**
     * 直接把雷达选定的接触提升为主目标。
     *
     * <p>这里刻意<strong>不再</strong>回查 {@code level.getEntity(uuid)}：在袭来袭弹体
     * 并不是普通世界实体（由 {@code ShaoMissileProjectile.activeWithin} 以静态集合维护），
     * 回查会得到 {@code null}，导致防空反导时指挥仪"看得见却锁不住"。
     */
    private void applyScanResult(@Nullable UUID nextUuid, @Nullable Vec3 nextPos) {
        if (isTargetSwitch(primaryTargetUuid, primaryTargetPos, nextUuid, nextPos)) {
            lockStrength = retainedLockStrength(lockStrength);
        }

        primaryTargetUuid = nextUuid;
        primaryTargetPos = nextPos;
        setChanged();
    }

    // ---- 火控参数分发 ----

    /**
     * 向火控范围内所有在飞导弹广播当前主目标位置与锁定强度。
     * 导弹端在 {@value ShaoMissileProjectile#DIRECTOR_GUIDANCE_MEMORY_TICKS} tick 内认为参数有效。
     */
    private void broadcastFireControl(ServerLevel level, BlockPos pos) {
        if (primaryTargetPos == null || lockStrength <= 0.0F) {
            return;
        }

        Vec3 center = Vec3.atCenterOf(pos);
        TargetDomain targetDomain = getSelectedTargetDomain();
        List<ShaoMissileProjectile> missiles = ShaoMissileProjectile.activeWithin(level, center, FIRE_CONTROL_RANGE);
        for (ShaoMissileProjectile missile : missiles) {
            // 连目标域一起广播：防空弹只认空中目标，反舰弹只认水面目标，避免弹种互相抢错目标
            missile.setDirectorTarget(primaryTargetPos, lockStrength, pos, targetDomain);
        }

        // 锁定强度足够高时给出可见的火控照射效果
        if (lockStrength >= 0.8F && level.getGameTime() % 4L == 0L) {
            spawnLockBeam(level, center);
        }
    }

    private void spawnLockBeam(ServerLevel level, Vec3 center) {
        Vec3 from = center.add(0.0D, 1.2D, 0.0D);
        int steps = 8;
        for (int i = 1; i <= steps; i++) {
            Vec3 point = from.lerp(primaryTargetPos, i / (double) steps);
            level.sendParticles(ParticleTypes.END_ROD, point.x, point.y, point.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    /** 是否有可分发的主目标。 */
    public boolean hasPrimaryTarget() {
        return primaryTargetPos != null;
    }

    public float getLockStrength() {
        return lockStrength;
    }

    /** 雷达天线当前角度（度），供渲染与火控界面使用。 */
    public float getRadarAngle() {
        return radarAngle;
    }

    @Nullable
    public Vec3 getPrimaryTargetPos() {
        return primaryTargetPos;
    }

    @Nullable
    public UUID getPrimaryTargetUuid() {
        return primaryTargetUuid;
    }

    /** 火控总控台 / 状态提示用的可读状态描述。 */
    public Component describeStatus() {
        if (primaryTargetPos == null) {
            return Component.translatable("message.taovmodernnaval.director_no_target",
                    getTrackedCount());
        }
        return Component.translatable("message.taovmodernnaval.director_status",
                String.format("%.2f", lockStrength),
                String.format("%.1f", primaryTargetPos.x),
                String.format("%.1f", primaryTargetPos.y),
                String.format("%.1f", primaryTargetPos.z));
    }

    // ---- NBT / 网络同步 ----

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putFloat(TAG_RADAR_ANGLE, radarAngle);
        tag.putFloat(TAG_LOCK_STRENGTH, lockStrength);
        tag.putInt(TAG_TRACKED_COUNT, getTrackedCount());
        if (primaryTargetUuid != null) {
            tag.putUUID(TAG_PRIMARY_TARGET, primaryTargetUuid);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        radarAngle = Mth.clamp(tag.getFloat(TAG_RADAR_ANGLE), 0.0F, 360.0F);
        lockStrength = Mth.clamp(tag.getFloat(TAG_LOCK_STRENGTH), 0.0F, 1.0F);
        primaryTargetUuid = tag.hasUUID(TAG_PRIMARY_TARGET) ? tag.getUUID(TAG_PRIMARY_TARGET) : null;
        primaryTargetPos = null;
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        tag.putFloat(TAG_LOCK_STRENGTH, lockStrength);
        tag.putFloat(TAG_RADAR_ANGLE, radarAngle);
        return tag;
    }
}
