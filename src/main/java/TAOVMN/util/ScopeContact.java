package TAOVMN.util;

import net.minecraft.world.phys.Vec3;

/**
 * 示波器上的一个目标点：以"总控台"为原点的相对位置。
 *
 * <p>点的散布由目标与终端的相对距离和方向决定：
 * {@code dx} 沿世界 +X（东），{@code dz} 沿 +Z（南），{@code dy} 为高差。
 * 屏幕坐标可直接取 {@code (dx, dz)} 映射，北方（-Z）朝上。
 *
 * @param dx      相对 X 偏移（格）
 * @param dy      相对 Y 偏移（格）
 * @param dz      相对 Z 偏移（格）
 * @param kind    接触类别（决定点的颜色）
 * @param threat  威胁分数（越大越优先）
 * @param primary 是否为当前主目标
 */
public record ScopeContact(double dx, double dy, double dz, ThreatRanking.ContactKind kind, double threat, boolean primary) {

    public ScopeContact {
        kind = kind == null ? ThreatRanking.ContactKind.UNKNOWN : kind;
    }

    /** 由世界坐标接触点构造：origin 为总控台位置。 */
    public static ScopeContact of(ThreatRanking.Contact contact, Vec3 origin, boolean primary) {
        if (contact == null || origin == null) {
            return null;
        }
        Vec3 delta = contact.position().subtract(origin);
        return new ScopeContact(delta.x, delta.y, delta.z, contact.kind(), contact.threat(), primary);
    }

    /** 水平距离（格）。 */
    public double horizontalDistance() {
        return Math.sqrt(dx * dx + dz * dz);
    }

    /** 三维距离（格）。 */
    public double distance() {
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /**
     * 真方位角（度）：正北（-Z）为 0，顺时针增大，正东（+X）为 90。
     * 结果归一化到 [0, 360)。
     */
    public double bearingDegrees() {
        double degrees = Math.toDegrees(Math.atan2(dx, -dz));
        return (degrees % 360.0D + 360.0D) % 360.0D;
    }

    /** 本接触需要哪一类弹药交战。 */
    public TargetDomain domain() {
        return kind == ThreatRanking.ContactKind.AIRCRAFT || kind == ThreatRanking.ContactKind.INCOMING_MISSILE
                ? TargetDomain.ANTI_AIR
                : TargetDomain.ANTI_SHIP;
    }
}
