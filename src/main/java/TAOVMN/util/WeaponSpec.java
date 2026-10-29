package TAOVMN.util;

/**
 * 真实世界规格 → 游戏参数 的唯一换算入口（P0-2）。
 *
 * <p>背景：此前 {@link MissileProfile} 的 11 型「P 系列」参数按「代次越新越强」手工排，
 * 结果与真实世界**排序相反**——亚音速的 P-15 是全场最快，超音速的 P-270 / P-500 / P-800 反而更慢；
 * 战斗部质量也被做成了单调递增，而真实的 P-800 战斗部（200 kg）是重型弹里最轻的。
 *
 * <p>这里把「真实数据」与「换算」分开：{@code mach / rangeKm / warheadKg} 是检索到的事实，
 * 以下三个函数才是玩法调整旋钮。改平衡只动常量，改史实只动 {@link WeaponSpecTable}。
 *
 * <pre>
 * 速度（格/tick）= clamp(1.40 + 0.50 × Mach, 0.90, 3.20)
 * 射程（格）     = clamp(200 + 120 × ln(km / 30), 120, 600)
 * 当量（TNT）    = clamp(战斗部质量kg / 45, 2, 24)
 * </pre>
 *
 * <p>口径：1 格 = 1 m、1 格/tick = 20 m/s。所以上表得到的是**对数压缩后的玩法值**，
 * 不是真实射程——MC 撑不住 550 km。换算刻意保留真实排序与档位差，这是本表存在的意义。
 *
 * @param itemPath      物品注册路径（如 {@code p270_missile}），也是查表键
 * @param realDesignation 真实型号（含 GRAU / NATO 代号），显示在 tooltip
 * @param serviceYear   服役年份；0 表示本轮未核实
 * @param mach          最大速度（Mach）；与真实弹道档位对应
 * @param rangeKm       射程（km）；同一弹型有低空/联合弹道时取舰射常用档
 * @param warheadKg     战斗部质量（kg，含装药与壳体，按公开数据）
 * @param steering      转向增益（0–1，纯手感值，不由真实数据推导）
 * @param proximityFuse 近炸半径（格，纯手感值）
 * @param domain        目标域
 * @param dataNote      数据可信度备注；非空表示本轮未逐页核实
 */
public record WeaponSpec(
        String itemPath,
        String realDesignation,
        int serviceYear,
        double mach,
        double rangeKm,
        double warheadKg,
        double steering,
        double proximityFuse,
        TargetDomain domain,
        String dataNote) {

    // ---- 换算常量：调平衡只改这里 ----
    public static final double SPEED_BASE = 1.40D;
    public static final double SPEED_PER_MACH = 0.50D;
    public static final double SPEED_MIN = 0.90D;
    public static final double SPEED_MAX = 3.20D;

    public static final double RANGE_BASE = 200.0D;
    public static final double RANGE_KM_PER_LN = 120.0D;
    public static final double RANGE_REF_KM = 30.0D;
    public static final double RANGE_MIN = 120.0D;
    public static final double RANGE_MAX = 600.0D;

    public static final double BLAST_KG_DIVISOR = 45.0D;
    public static final double BLAST_MIN = 2.0D;
    public static final double BLAST_MAX = 24.0D;

    /** 通用反舰弹的 IFF 安全半径（格）：防止刚出膛就追自己的发射舰。 */
    public static final float DEFAULT_IFF_SAFE_RADIUS = 10.0F;

    public float maxSpeedBlocks() {
        return (float) clamp(SPEED_BASE + SPEED_PER_MACH * mach, SPEED_MIN, SPEED_MAX);
    }

    public float seekRangeBlocks() {
        return (float) clamp(RANGE_BASE + RANGE_KM_PER_LN * Math.log(rangeKm / RANGE_REF_KM), RANGE_MIN, RANGE_MAX);
    }

    public float blastPower() {
        return (float) clamp(warheadKg / BLAST_KG_DIVISOR, BLAST_MIN, BLAST_MAX);
    }

    public MissileProfile toProfile() {
        return new MissileProfile(maxSpeedBlocks(), (float) steering, blastPower(), seekRangeBlocks(),
                (float) proximityFuse, DEFAULT_IFF_SAFE_RADIUS, domain);
    }

    /** 数据是否已核实（{@code dataNote} 为空即视为核实）。 */
    public boolean verified() {
        return dataNote == null || dataNote.isBlank();
    }

    static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
