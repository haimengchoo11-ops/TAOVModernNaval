package TAOVMN.util;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 示波器接触点的纯逻辑测试：相对偏移、方位角与目标域映射。
 */
class ScopeContactTest {
    private static final double EPSILON = 1.0E-6D;

    private static ScopeContact at(double dx, double dy, double dz, ThreatRanking.ContactKind kind) {
        return new ScopeContact(dx, dy, dz, kind, 1.0D, false);
    }

    @Test
    void bearingIsZeroToTheNorthAndIncreasesClockwise() {
        assertEquals(0.0D, at(0.0D, 0.0D, -10.0D, ThreatRanking.ContactKind.SURFACE).bearingDegrees(), EPSILON,
                "正北（-Z）为 0 度");
        assertEquals(90.0D, at(10.0D, 0.0D, 0.0D, ThreatRanking.ContactKind.SURFACE).bearingDegrees(), EPSILON,
                "正东（+X）为 90 度");
        assertEquals(180.0D, at(0.0D, 0.0D, 10.0D, ThreatRanking.ContactKind.SURFACE).bearingDegrees(), EPSILON,
                "正南（+Z）为 180 度");
        assertEquals(270.0D, at(-10.0D, 0.0D, 0.0D, ThreatRanking.ContactKind.SURFACE).bearingDegrees(), EPSILON,
                "正西（-X）为 270 度");
    }

    @Test
    void bearingStaysWithinZeroToThreeSixty() {
        for (double angle = 0.0D; angle < 360.0D; angle += 7.0D) {
            double radians = Math.toRadians(angle);
            ScopeContact contact = at(Math.sin(radians) * 20.0D, 0.0D, -Math.cos(radians) * 20.0D,
                    ThreatRanking.ContactKind.AIRCRAFT);
            double bearing = contact.bearingDegrees();
            assertTrue(bearing >= 0.0D && bearing < 360.0D, "方位角必须归一化到 [0,360)：" + bearing);
            assertEquals(angle, bearing, 1.0E-6D);
        }
    }

    @Test
    void ofConvertsWorldPositionIntoRelativeOffset() {
        Vec3 origin = new Vec3(100.0D, 64.0D, 200.0D);
        ThreatRanking.Contact contact = new ThreatRanking.Contact(new Vec3(130.0D, 70.0D, 160.0D),
                Vec3.ZERO, ThreatRanking.ContactKind.SURFACE, 50.0D, 0.0D, 3.0D);

        ScopeContact snapshot = ScopeContact.of(contact, origin, true);

        assertEquals(30.0D, snapshot.dx(), EPSILON);
        assertEquals(6.0D, snapshot.dy(), EPSILON);
        assertEquals(-40.0D, snapshot.dz(), EPSILON);
        assertEquals(50.0D, snapshot.horizontalDistance(), EPSILON);
        assertTrue(snapshot.primary());
    }

    @Test
    void domainFollowsContactKind() {
        assertEquals(TargetDomain.ANTI_AIR,
                at(0.0D, 5.0D, -10.0D, ThreatRanking.ContactKind.INCOMING_MISSILE).domain());
        assertEquals(TargetDomain.ANTI_AIR,
                at(0.0D, 5.0D, -10.0D, ThreatRanking.ContactKind.AIRCRAFT).domain());
        assertEquals(TargetDomain.ANTI_SHIP,
                at(0.0D, 0.0D, -10.0D, ThreatRanking.ContactKind.SURFACE).domain());
    }

    @Test
    void degenerateInputsDoNotThrow() {
        assertEquals(null, ScopeContact.of(null, Vec3.ZERO, false));
        assertEquals(null, ScopeContact.of(new ThreatRanking.Contact(Vec3.ZERO, Vec3.ZERO,
                ThreatRanking.ContactKind.SURFACE, 0.0D, 0.0D, 0.0D), null, false));
    }
}
