package TAOVMN.util;

import net.minecraft.world.phys.Vec3;

/**
 * 目标威胁排序的纯函数实现（不依赖任何 Minecraft 运行期状态，便于单元测试）。
 *
 * <p>雷达/指挥仪用它把"谁最该先打"量化出来，取代原先"只挑最近的一个"：
 * <pre>
 *   threat = 类型权重 × ( 0.6 × 距离归一化 + 0.4 × 接近度归一化 )
 * </pre>
 * 其中距离归一化为 {@code 1 - d/range}（越近越大），接近度为
 * {@code (dot(自身速度方向, 指向目标方向) + 1) / 2}，即"正在接近"得分高、"正在远离"得分低。
 *
 * <p>类型权重让来袭弹体天然压过飞机与水面目标；而"正在远离"的弹体
 * （例如本舰刚刚发射出去的导弹）接近度为 0，配合调用方的
 * {@code approach < 0 即排除} 判断，可以避免雷达接住自己的弹。
 */
public final class ThreatRanking {
    private static final double PROXIMITY_WEIGHT = 0.6D;
    private static final double APPROACH_WEIGHT = 0.4D;
    private static final double MIN_LENGTH_SQR = 1.0E-6D;

    /** 目标类别及其威胁权重：来袭弹 > 飞机 > 水面/地面 > 未知。 */
    public enum ContactKind {
        INCOMING_MISSILE(4.0D),
        AIRCRAFT(2.0D),
        SURFACE(1.0D),
        UNKNOWN(0.5D);

        private final double weight;

        ContactKind(double weight) {
            this.weight = weight;
        }

        public double weight() {
            return weight;
        }
    }

    /**
     * 一个已评估的接触目标。
     *
     * @param position 目标中心坐标
     * @param velocity 目标速度（未知时为零向量）
     * @param kind     目标类别
     * @param distance 到观测中心的距离（用于同位威胁时的稳定打破平局）
     * @param approach 接近度，[-1,1]：&gt;0 表示正在接近
     * @param threat   威胁分数，越大越优先
     */
    public record Contact(Vec3 position, Vec3 velocity, ContactKind kind, double distance, double approach, double threat) {
        /** 本接触对应的目标域（空中目标需要防空弹）。 */
        public TargetDomain targetDomain() {
            return kind == ContactKind.AIRCRAFT || kind == ContactKind.INCOMING_MISSILE
                    ? TargetDomain.ANTI_AIR
                    : TargetDomain.ANTI_SHIP;
        }
    }

    /**
     * 接近度：目标速度在"指向观测中心"方向上的投影，范围 [-1,1]。
     *
     * <p>&gt;0 表示目标正在<b>接近</b>观测中心（例如迎面来袭的导弹），
     * &lt;0 表示正在远离（例如本舰刚发射出去的弹）。调用方用
     * {@code approach < 0} 排除"自己的弹"。
     */
    public static double approach(Vec3 center, Vec3 position, Vec3 velocity) {
        if (center == null || position == null || velocity == null) {
            return 0.0D;
        }
        Vec3 toTarget = position.subtract(center);
        if (toTarget.lengthSqr() <= MIN_LENGTH_SQR || velocity.lengthSqr() <= MIN_LENGTH_SQR) {
            return 0.0D;
        }
        // 取负：速度朝向观测中心时（正在接近）为正
        return clamp(-velocity.normalize().dot(toTarget.normalize()), -1.0D, 1.0D);
    }

    /** 威胁分数。 */
    public static double threat(Vec3 center, Vec3 position, double approach, ContactKind kind, double range) {
        if (center == null || position == null) {
            return 0.0D;
        }
        ContactKind effectiveKind = kind == null ? ContactKind.UNKNOWN : kind;
        double effectiveRange = Math.max(1.0D, range);
        double proximity = clamp(1.0D - center.distanceTo(position) / effectiveRange, 0.0D, 1.0D);
        double approachScore = (clamp(approach, -1.0D, 1.0D) + 1.0D) * 0.5D;
        return effectiveKind.weight() * (PROXIMITY_WEIGHT * proximity + APPROACH_WEIGHT * approachScore);
    }

    /** 一步算出完整接触（距离、接近度、威胁）。 */
    public static Contact of(Vec3 center, Vec3 position, Vec3 velocity, ContactKind kind, double range) {
        Vec3 safeVelocity = velocity == null ? Vec3.ZERO : velocity;
        double approach = approach(center, position, safeVelocity);
        return new Contact(position, safeVelocity, kind, center.distanceTo(position), approach,
                threat(center, position, approach, kind, range));
    }

    /** 候选是否比当前目标更该优先打：威胁分高者胜；同分时距离近者胜。 */
    public static boolean isMoreThreatening(Contact candidate, Contact current) {
        if (current == null) {
            return candidate != null;
        }
        if (candidate == null) {
            return false;
        }
        int byThreat = Double.compare(candidate.threat(), current.threat());
        if (byThreat != 0) {
            return byThreat > 0;
        }
        return candidate.distance() < current.distance();
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private ThreatRanking() {
    }
}
