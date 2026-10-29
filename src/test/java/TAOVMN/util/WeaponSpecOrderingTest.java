package TAOVMN.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * P0-2 的防回归测试：真实世界的**排序不变量**。
 *
 * <p>引入本表之前，参数是「代次越新越强」手工排的，结果速度排序与真实相反
 * （亚音速 P-15 最快、超音速 P-800 最慢）。这些断言就是为了让那种错误不可能再悄悄回来：
 * 谁把 {@link WeaponSpecTable} 里的数字改成"更好玩但反史实"，这里就会红。
 *
 * <p>容差取 1e-3：参数是 float，换算是 double，直接 == 会因精度抖动误报。
 */
class WeaponSpecOrderingTest {

    private static final float EPS = 1.0E-3F;

    private static MissileProfile antiShip(String itemPath) {
        WeaponSpec spec = WeaponSpecTable.find(itemPath);
        assertNotNull(spec, "缺少规格登记：" + itemPath);
        return spec.toProfile();
    }

    @Test
    void speedOrderingFollowsRealWorld() {
        // 真实：P-15 / P-120 亚音速（Mach 0.9）< P-700（1.6）< P-270 / P-500 / P-800（2.0–3.0）
        float p15 = antiShip("p15_missile").maxSpeed();
        float p120 = antiShip("p120_missile").maxSpeed();
        float p700 = antiShip("p700_missile").maxSpeed();
        float p270 = antiShip("p270_missile").maxSpeed();
        float p500 = antiShip("p500_missile").maxSpeed();
        float p800 = antiShip("p800_missile").maxSpeed();

        assertAll(
                () -> assertTrue(p15 < p700, "亚音速弹必须慢于 P-700"),
                () -> assertTrue(p120 < p700, "亚音速弹必须慢于 P-700"),
                () -> assertTrue(p700 < p270, "P-700（Mach 1.6）必须慢于 P-270（Mach 2.5）"),
                () -> assertTrue(p700 < p800, "P-700 必须慢于 P-800"),
                () -> assertEquals(p270, p500, EPS, "P-270 与 P-500 同为 Mach 2.5，应同档"),
                () -> assertTrue(p800 > p700 && p800 <= p500 + EPS,
                        "P-800（Mach 2.25 折算）应落在 P-700 与 P-500 之间"));
    }

    @Test
    void rangeOrderingIsNotMonotonicByGeneration() {
        // 真实射程：P-15 80 km < P-120 110 < P-270 120 < P-800 300 < P-500 / P-700 550
        float p15 = antiShip("p15_missile").seekRange();
        float p120 = antiShip("p120_missile").seekRange();
        float p270 = antiShip("p270_missile").seekRange();
        float p800 = antiShip("p800_missile").seekRange();
        float p500 = antiShip("p500_missile").seekRange();
        float p700 = antiShip("p700_missile").seekRange();

        assertAll(
                () -> assertTrue(p15 < p120),
                () -> assertTrue(p120 < p270),
                () -> assertTrue(p270 < p800, "P-800 射程必须长于近程的 P-270"),
                () -> assertTrue(p800 < p500, "P-800 射程必须短于 P-500"),
                () -> assertTrue(p800 < p700),
                () -> assertEquals(p500, p700, EPS, "P-500 与 P-700 同为 550 km，应同档"));
    }

    @Test
    void warheadOrderingReflectsRealMass() {
        // 真实战斗部：P-800 200 kg < P-270 300 kg < P-15 450 kg < P-120 500 kg < P-700 750 kg < P-500 1000 kg
        float p800 = antiShip("p800_missile").blastPower();
        float p270 = antiShip("p270_missile").blastPower();
        float p15 = antiShip("p15_missile").blastPower();
        float p120 = antiShip("p120_missile").blastPower();
        float p700 = antiShip("p700_missile").blastPower();
        float p500 = antiShip("p500_missile").blastPower();

        assertAll(
                () -> assertTrue(p800 < p270, "P-800 是重型弹里战斗部最轻的"),
                () -> assertTrue(p270 < p15),
                () -> assertTrue(p15 < p120),
                () -> assertTrue(p120 < p700),
                () -> assertTrue(p700 < p500));
    }

    @Test
    void conversionFormulaIsPinned() {
        // P-800：Mach 2.25 → 1.40 + 0.50 × 2.25 = 2.525；300 km → 200 + 120·ln(10) ≈ 476.3
        MissileProfile p800 = antiShip("p800_missile");
        assertAll(
                () -> assertEquals(2.525F, p800.maxSpeed(), EPS),
                () -> assertEquals(476.3F, p800.seekRange(), 0.5F),
                () -> assertEquals(4.444F, p800.blastPower(), 0.01F));
    }

    @Test
    void everySpecStaysInsideClampedBands() {
        for (WeaponSpec spec : WeaponSpecTable.all().values()) {
            MissileProfile profile = spec.toProfile();
            assertAll(
                    () -> assertTrue(profile.maxSpeed() >= WeaponSpec.SPEED_MIN - EPS
                                    && profile.maxSpeed() <= WeaponSpec.SPEED_MAX + EPS,
                            spec.itemPath() + " 速度越界：" + profile.maxSpeed()),
                    () -> assertTrue(profile.seekRange() >= WeaponSpec.RANGE_MIN - EPS
                                    && profile.seekRange() <= WeaponSpec.RANGE_MAX + EPS,
                            spec.itemPath() + " 射程越界：" + profile.seekRange()),
                    () -> assertTrue(profile.blastPower() >= WeaponSpec.BLAST_MIN - EPS
                                    && profile.blastPower() <= WeaponSpec.BLAST_MAX + EPS,
                            spec.itemPath() + " 当量越界：" + profile.blastPower()));
        }
    }

    @Test
    void antiShipSpecsStayAntiShip() {
        for (WeaponSpec spec : WeaponSpecTable.all().values()) {
            assertEquals(TargetDomain.UNIVERSAL, spec.domain(), spec.itemPath() + " 不应改目标域");
        }
    }

    @Test
    void unknownItemFallsBackInsteadOfThrowing() {
        assertAll(
                () -> assertNull(WeaponSpecTable.find("no_such_missile")),
                () -> assertEquals(MissileProfile.p1Default().maxSpeed(),
                        WeaponSpecTable.profile("no_such_missile").maxSpeed(), EPS));
    }

    @Test
    void verifiedFlagMatchesDataNote() {
        for (WeaponSpec spec : WeaponSpecTable.all().values()) {
            assertEquals(spec.dataNote() == null || spec.dataNote().isBlank(), spec.verified(),
                    spec.itemPath() + " 的 verified() 与 dataNote 不一致");
        }
    }
}
