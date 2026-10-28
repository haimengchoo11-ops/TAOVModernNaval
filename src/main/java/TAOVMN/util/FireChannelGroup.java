package TAOVMN.util;

import java.util.Locale;

/**
 * 火力通道编组：总控台把舰上发射装置按作战用途分组，例如"反舰群1""反舰群2""防空群"。
 *
 * <p>编组只影响 UI 呈现与指挥习惯，不改变导弹本身的制导行为；但每一组的
 * {@link #domain()} 给出了该组默认交战的目标域，界面据此提示"这一群打的是水面目标还是空中目标"。
 * {@link #UNASSIGNED} 不限制目标域。
 */
public enum FireChannelGroup {
    ANTI_SHIP_1("anti_ship_1", TargetDomain.ANTI_SHIP),
    ANTI_SHIP_2("anti_ship_2", TargetDomain.ANTI_SHIP),
    ANTI_AIR_1("anti_air_1", TargetDomain.ANTI_AIR),
    ANTI_AIR_2("anti_air_2", TargetDomain.ANTI_AIR),
    UNASSIGNED("unassigned", null);

    private final String key;
    private final TargetDomain domain;

    FireChannelGroup(String key, TargetDomain domain) {
        this.key = key;
        this.domain = domain;
    }

    /** NBT / 配置中使用的稳定键名（小写下划线，改名会破坏存档，勿动）。 */
    public String key() {
        return key;
    }

    /** 本组默认交战的目标域；{@code null} 表示不限。 */
    public TargetDomain domain() {
        return domain;
    }

    /** 语言键（用于界面显示）。 */
    public String translationKey() {
        return "channel_group.taovmodernnaval." + key;
    }

    /** 编组轮换顺序中的下一个（用于界面"编组"按钮循环切换）。 */
    public FireChannelGroup next() {
        FireChannelGroup[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    /** 按 {@link #key()} 反查；未知键回退到 {@code fallback}（保证旧存档可读）。 */
    public static FireChannelGroup byKey(String key, FireChannelGroup fallback) {
        if (key == null || key.isBlank()) {
            return fallback;
        }
        String normalized = key.trim().toLowerCase(Locale.ROOT);
        for (FireChannelGroup group : values()) {
            if (group.key.equals(normalized)) {
                return group;
            }
        }
        return fallback;
    }
}
