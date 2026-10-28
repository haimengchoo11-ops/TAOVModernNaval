package TAOVMN.util;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThreatRankingTest {
    private static final Vec3 ORIGIN = Vec3.ZERO;
    private static final double RANGE = 100.0D;

    @Test
    void approachingTargetScoresHigherThanIdenticalRecedingTarget() {
        Vec3 position = new Vec3(0.0D, 0.0D, 50.0D);
        double incoming = ThreatRanking.threat(ORIGIN, position, 1.0D, ThreatRanking.ContactKind.AIRCRAFT, RANGE);
        double outgoing = ThreatRanking.threat(ORIGIN, position, -1.0D, ThreatRanking.ContactKind.AIRCRAFT, RANGE);

        assertTrue(incoming > outgoing, "同样位置，迎面来的目标威胁必须高于背向离开的目标");
    }

    @Test
    void closerTargetScoresHigherWhenApproachIsEqual() {
        double near = ThreatRanking.threat(ORIGIN, new Vec3(0.0D, 0.0D, 10.0D), 0.0D,
                ThreatRanking.ContactKind.SURFACE, RANGE);
        double far = ThreatRanking.threat(ORIGIN, new Vec3(0.0D, 0.0D, 90.0D), 0.0D,
                ThreatRanking.ContactKind.SURFACE, RANGE);

        assertTrue(near > far, "同样接近度下，越近威胁越高");
    }

    @Test
    void incomingMissileOutranksCloserSurfaceTarget() {
        ThreatRanking.Contact missile = ThreatRanking.of(ORIGIN, new Vec3(0.0D, 20.0D, 60.0D),
                new Vec3(0.0D, -0.2D, -1.0D), ThreatRanking.ContactKind.INCOMING_MISSILE, RANGE);
        ThreatRanking.Contact boat = ThreatRanking.of(ORIGIN, new Vec3(0.0D, 0.0D, 20.0D),
                new Vec3(0.0D, 0.0D, 0.0D), ThreatRanking.ContactKind.SURFACE, RANGE);

        assertTrue(ThreatRanking.isMoreThreatening(missile, boat),
                "来袭弹体即使更远，也应压过更近的水面目标（威胁排序而非最近优先）");
        assertEquals(TargetDomain.ANTI_AIR, missile.targetDomain());
        assertEquals(TargetDomain.ANTI_SHIP, boat.targetDomain());
    }

    @Test
    void equalThreatBreaksTieByDistance() {
        ThreatRanking.Contact near = new ThreatRanking.Contact(new Vec3(1.0D, 0.0D, 0.0D), Vec3.ZERO,
                ThreatRanking.ContactKind.SURFACE, 1.0D, 0.0D, 5.0D);
        ThreatRanking.Contact far = new ThreatRanking.Contact(new Vec3(5.0D, 0.0D, 0.0D), Vec3.ZERO,
                ThreatRanking.ContactKind.SURFACE, 5.0D, 0.0D, 5.0D);

        assertAll(
                () -> assertTrue(ThreatRanking.isMoreThreatening(near, far), "同分时近者优先"),
                () -> assertFalse(ThreatRanking.isMoreThreatening(far, near)));
    }

    @Test
    void nullCandidateNeverReplacesCurrentTarget() {
        ThreatRanking.Contact current = ThreatRanking.of(ORIGIN, new Vec3(0.0D, 0.0D, 10.0D), Vec3.ZERO,
                ThreatRanking.ContactKind.SURFACE, RANGE);

        assertAll(
                () -> assertTrue(ThreatRanking.isMoreThreatening(current, null), "首个目标应被接受"),
                () -> assertFalse(ThreatRanking.isMoreThreatening(null, current), "空候选不得顶掉现有目标"),
                () -> assertFalse(ThreatRanking.isMoreThreatening(null, null)));
    }

    @Test
    void approachSignDistinguishesClosingFromReceding() {
        Vec3 center = Vec3.ZERO;
        Vec3 position = new Vec3(0.0D, 0.0D, 30.0D);

        assertAll(
                () -> assertTrue(ThreatRanking.approach(center, position, new Vec3(0.0D, 0.0D, -1.0D)) > 0.0D,
                        "朝雷达飞来应为正接近度"),
                () -> assertTrue(ThreatRanking.approach(center, position, new Vec3(0.0D, 0.0D, 1.0D)) < 0.0D,
                        "远离雷达应为负接近度"),
                () -> assertEquals(0.0D, ThreatRanking.approach(center, center, new Vec3(0.0D, 0.0D, 1.0D)),
                        "与观测点重合时接近度必须安全归零"));
    }

    @Test
    void degenerateInputsDoNotThrowOrProduceNaN() {
        assertAll(
                () -> assertEquals(0.0D, ThreatRanking.approach(null, null, null)),
                () -> assertEquals(0.0D, ThreatRanking.threat(null, null, 0.0D, null, 0.0D)),
                () -> assertFalse(Double.isNaN(ThreatRanking.threat(ORIGIN, new Vec3(1.0D, 0.0D, 0.0D), 0.0D, null, 0.0D))));
    }
}
