package TAOVMN.util;

import java.util.Locale;

/**
 * 目标域：一枚弹药可以交战哪一类目标。
 *
 * <p>反舰弹（P 系列）为 {@link #ANTI_SHIP}，不应去追空中目标；防空弹为 {@link #ANTI_AIR}，
 * 只交战空中目标（在袭来袭弹体、飞行生物/载具）；{@link #UNIVERSAL} 两者皆可。
 *
 * <p><b>向后兼容</b>：所有既有弹药的 {@link MissileProfile} 都由 6 参数构造器建立，
 * 其目标域为 {@link #UNIVERSAL}，因此本枚举上线不会改变既有 P 系列弹药的行为；
 * 只有显式声明了目标域的新弹种（防空弹）会受 {@link #engages(TargetDomain)} 约束。
 */
public enum TargetDomain {
    /** 反舰：只交战水面/地面目标。 */
    ANTI_SHIP,
    /** 防空：只交战空中目标。 */
    ANTI_AIR,
    /** 通用：两者皆可（既有弹药的默认值）。 */
    UNIVERSAL;

    /** 本目标域能否交战 {@code target} 这一类目标。 */
    public boolean engages(TargetDomain target) {
        if (target == null) {
            return true;
        }
        return switch (this) {
            case UNIVERSAL -> true;
            // 目标域为 UNIVERSAL 表示"未知/不限"，不能因为未知就拒绝交战
            case ANTI_SHIP -> target != ANTI_AIR;
            case ANTI_AIR -> target != ANTI_SHIP;
        };
    }

    /** 军事上是否属于防空弹（用于 UI/提示）。 */
    public boolean isAntiAir() {
        return this == ANTI_AIR;
    }

    /** NBT / 配置里使用的稳定键名（小写下划线，改名会破坏存档，勿动）。 */
    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** 语言键（用于 tooltip 展示）。 */
    public String translationKey() {
        return "domain.taovmodernnaval." + key();
    }

    /** 按 {@link #key()} 反查；未知键回退到 {@code fallback}（保证旧存档可读）。 */
    public static TargetDomain byKey(String key, TargetDomain fallback) {
        if (key == null || key.isBlank()) {
            return fallback;
        }
        for (TargetDomain domain : values()) {
            if (domain.key().equalsIgnoreCase(key.trim())) {
                return domain;
            }
        }
        return fallback;
    }

    /** 按"目标是否在空中"归类：空中目标需要防空弹，水面/地面目标归反舰弹。 */
    public static TargetDomain of(boolean airborne) {
        return airborne ? ANTI_AIR : ANTI_SHIP;
    }
}
