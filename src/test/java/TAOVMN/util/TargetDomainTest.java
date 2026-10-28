package TAOVMN.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TargetDomainTest {

    @Test
    void keyRoundTripsForEveryDomain() {
        for (TargetDomain domain : TargetDomain.values()) {
            assertEquals(domain, TargetDomain.byKey(domain.key(), null), "key() 必须能被 byKey 反查");
        }
    }

    @Test
    void unknownKeyFallsBackInsteadOfThrowing() {
        assertAll(
                () -> assertEquals(TargetDomain.UNIVERSAL, TargetDomain.byKey("nonsense", TargetDomain.UNIVERSAL)),
                () -> assertEquals(TargetDomain.ANTI_AIR, TargetDomain.byKey(null, TargetDomain.ANTI_AIR)),
                () -> assertEquals(TargetDomain.ANTI_SHIP, TargetDomain.byKey("   ", TargetDomain.ANTI_SHIP)));
    }

    @Test
    void keyLookupIgnoresCaseAndSurroundingWhitespace() {
        assertEquals(TargetDomain.ANTI_AIR, TargetDomain.byKey("  ANTI_AIR ", TargetDomain.ANTI_SHIP));
    }

    @Test
    void antiShipMustNotEngageAirTargets() {
        assertAll(
                () -> assertTrue(TargetDomain.ANTI_SHIP.engages(TargetDomain.ANTI_SHIP)),
                () -> assertFalse(TargetDomain.ANTI_SHIP.engages(TargetDomain.ANTI_AIR)),
                () -> assertTrue(TargetDomain.ANTI_SHIP.engages(TargetDomain.UNIVERSAL)));
    }

    @Test
    void antiAirMustNotEngageSurfaceTargets() {
        assertAll(
                () -> assertTrue(TargetDomain.ANTI_AIR.engages(TargetDomain.ANTI_AIR)),
                () -> assertFalse(TargetDomain.ANTI_AIR.engages(TargetDomain.ANTI_SHIP)),
                () -> assertTrue(TargetDomain.ANTI_AIR.isAntiAir()),
                () -> assertFalse(TargetDomain.ANTI_SHIP.isAntiAir()));
    }

    @Test
    void universalEngagesEverythingAndNullIsTreatedAsEngageable() {
        for (TargetDomain target : TargetDomain.values()) {
            assertTrue(TargetDomain.UNIVERSAL.engages(target), "通用弹应能交战 " + target);
        }
        assertTrue(TargetDomain.ANTI_AIR.engages(null), "未知目标域不应让导弹拒绝交战");
    }

    @Test
    void airborneClassificationMapsToAirDomain() {
        assertAll(
                () -> assertEquals(TargetDomain.ANTI_AIR, TargetDomain.of(true)),
                () -> assertEquals(TargetDomain.ANTI_SHIP, TargetDomain.of(false)));
    }
}
