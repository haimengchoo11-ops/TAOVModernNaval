package TAOVMN.util;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

/**
 * 苏俄海军反舰导弹规格表（P0-2）。检索基准：2026-10-30，来源见
 * {@code docs/RUSSIAN_NAVAL_ARMAMENT_REVIEW.md} §6。
 *
 * <p>只收录**反舰弹**；防空弹的参数仍在 {@link MissileProfile} 里手工维护
 * （防空弹的杀伤机理是破片，与反舰弹的装药当量不同源，不能共用 /45 的换算）。
 *
 * <p>物品注册 id 一律不改（改 id 会砸存档与配方）。四个查不到实装的旧 id
 * （{@code p30/p45/p100/p300}）保留 id，改挂到最接近的真实型号上，并在 {@code dataNote}
 * 里标「数据待核」——tooltip 会把这一条显示给玩家，避免把估算值当真。
 */
public final class WeaponSpecTable {

    /** 未登记物品（CBC 弹药、未知弹）使用的回退档位。 */
    private static final WeaponSpec FALLBACK = new WeaponSpec(
            "p1_missile", "P-1「箭」SS-N-1", 1958, 0.9D, 40.0D, 450.0D, 0.24D, 1.8D,
            TargetDomain.UNIVERSAL, "P-1 参数本轮未抓页，占位");

    private static final Map<String, WeaponSpec> SPECS = new LinkedHashMap<>();

    private WeaponSpecTable() {
    }

    private static void put(WeaponSpec spec) {
        SPECS.put(spec.itemPath(), spec);
    }

    static {
        // ---- 已核对：速度/射程/战斗部均来自本轮抓取的页面 ----

        // P-15 白蚁（SS-N-2）：Mach 0.9；Termit-M 射程 80 km。战斗部 globalsec 该页未给，取 450 kg 占位。
        put(new WeaponSpec("p15_missile", "P-15「白蚁」SS-N-2（Termit-M）", 1960, 0.9D, 80.0D, 450.0D,
                0.24D, 1.8D, TargetDomain.UNIVERSAL, "战斗部质量未核实，按 450 kg 占位"));

        // P-120 孔雀石（SS-N-9）：Mach 0.9（部分来源 1.4）；水面舰 110 km / 潜艇 70 km；500 kg HE 或 200 kt。
        put(new WeaponSpec("p120_missile", "P-120「孔雀石」SS-N-9", 1969, 0.9D, 110.0D, 500.0D,
                0.24D, 1.9D, TargetDomain.UNIVERSAL, null));

        // P-270 蚊（SS-N-22）：Mach 2–3（取 2.5）；舰射 90–120 km（取 120）；300 kg 穿透型（装药 150 kg）。
        put(new WeaponSpec("p270_missile", "3M80 P-270「蚊」SS-N-22", 1984, 2.5D, 120.0D, 300.0D,
                0.20D, 2.0D, TargetDomain.UNIVERSAL, null));

        // P-500 玄武岩（SS-N-12）：Mach 2.5；550 km；1000 kg HE 或 350 kt。
        put(new WeaponSpec("p500_missile", "P-500「玄武岩」SS-N-12", 1973, 2.5D, 550.0D, 1000.0D,
                0.15D, 2.4D, TargetDomain.UNIVERSAL, null));

        // P-700 花岗岩（SS-N-19）：Mach 1.6；550 km；750 kg HE 或 500 kt。服役年份本轮未核。
        put(new WeaponSpec("p700_missile", "3M45 P-700「花岗岩」SS-N-19", 0, 1.6D, 550.0D, 750.0D,
                0.14D, 2.5D, TargetDomain.UNIVERSAL, "服役年份未核实"));

        // P-800 缟玛瑙（SS-N-26）：Mach 2.0–2.5（取 2.25）；300 km 联合弹道 / 120 km 全程低空；200 kg。
        put(new WeaponSpec("p800_missile", "3M55「缟玛瑙」SS-N-26", 0, 2.25D, 300.0D, 200.0D,
                0.14D, 2.1D, TargetDomain.UNIVERSAL, "服役年份未核实"));

        // ---- 四个「查无实装」的旧 id：保留 id，改挂最接近的真实型号，数值待核 ----

        // P-35 进步（SS-N-3）：高空巡航、Mach 1.4 级、300 km 级、1000 kg 级战斗部——本轮未抓页，全部估算。
        put(new WeaponSpec("p30_missile", "P-35「进步」SS-N-3", 1962, 1.4D, 300.0D, 1000.0D,
                0.20D, 2.0D, TargetDomain.UNIVERSAL, "数据待核（本轮未抓页，按同代弹估算）"));

        // P-70 紫晶（SS-N-7）：潜射亚音速、70–80 km、500 kg 级。
        put(new WeaponSpec("p45_missile", "P-70「紫晶」SS-N-7", 1968, 0.9D, 80.0D, 500.0D,
                0.24D, 1.9D, TargetDomain.UNIVERSAL, "数据待核（本轮未抓页，按同代弹估算）"));

        // P-1000 火山（SS-N-12B）：玄武岩增程型，Mach 2.5+、700 km 级、1000 kg 级。
        put(new WeaponSpec("p100_missile", "P-1000「火山」SS-N-12B", 1987, 2.5D, 700.0D, 1000.0D,
                0.15D, 2.4D, TargetDomain.UNIVERSAL, "数据待核（射程/战斗部本轮未核实）"));

        // 3M-54 口径（SS-N-27）：亚音速巡航 + 超音速末段，综合按 Mach 2.1 折算。
        put(new WeaponSpec("p300_missile", "3M-54「口径」SS-N-27", 0, 2.1D, 300.0D, 300.0D,
                0.20D, 2.1D, TargetDomain.UNIVERSAL, "数据待核（亚超结合弹道，速度取等效值）"));
    }

    /** 按物品路径查规格；未登记返回 {@code null}。 */
    @Nullable
    public static WeaponSpec find(String itemPath) {
        return SPECS.get(itemPath);
    }

    /**
     * 按物品路径取参数；未登记时回退到 P-1 档位。
     *
     * <p>回退行为与引入本表之前一致：未知弹（CBC 弹药等）走 {@code p1Default()}。
     */
    public static MissileProfile profile(String itemPath) {
        WeaponSpec spec = SPECS.get(itemPath);
        return (spec == null ? FALLBACK : spec).toProfile();
    }

    public static Map<String, WeaponSpec> all() {
        return Collections.unmodifiableMap(SPECS);
    }
}
