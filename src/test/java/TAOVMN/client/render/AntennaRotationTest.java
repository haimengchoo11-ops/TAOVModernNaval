package TAOVMN.client.render;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 天线旋转角度的纯函数测试。
 *
 * <p>这段逻辑替代了原先"每 tick 累加一个字段再同步"的做法，因此必须保证：
 * 角度严格落在 [0,360)、按固定速率单调推进、并且长时间运行（gameTime 很大）不出现精度抖动。
 */
class AntennaRotationTest {
    private static final float EPSILON = 1.0E-4F;

    @Test
    void angleAdvancesAtConfiguredRate() {
        assertEquals(0.0F, AntennaRotation.degreesFromTime(0L, 3.0F), EPSILON);
        assertEquals(3.0F, AntennaRotation.degreesFromTime(1L, 3.0F), EPSILON);
        assertEquals(30.0F, AntennaRotation.degreesFromTime(10L, 3.0F), EPSILON);
    }

    @Test
    void angleWrapsWithinOneTurn() {
        // 3°/tick → 120 tick 一圈
        assertEquals(0.0F, AntennaRotation.degreesFromTime(120L, 3.0F), EPSILON);
        assertEquals(3.0F, AntennaRotation.degreesFromTime(121L, 3.0F), EPSILON);
        assertEquals(357.0F, AntennaRotation.degreesFromTime(119L, 3.0F), EPSILON);
    }

    @Test
    void angleStaysInRangeAndKeepsAdvancingForHugeGameTime() {
        float previous = -1.0F;
        for (long t = 1_000_000L; t < 1_000_120L; t++) {
            float angle = AntennaRotation.degreesFromTime(t, 3.0F);
            assertTrue(angle >= 0.0F && angle < 360.0F, "角度必须落在 [0,360)，实际 " + angle);
            if (previous >= 0.0F && previous + 3.0F < 360.0F) {
                assertEquals(previous + 3.0F, angle, EPSILON,
                        "长运行下每一步仍应严格推进 3°，不能因 float 精度而抖动");
            }
            previous = angle;
        }
    }

    @Test
    void zeroOrNegativeSpeedDoesNotRotate() {
        assertEquals(0.0F, AntennaRotation.degreesFromTime(12345L, 0.0F), EPSILON);
        assertEquals(0.0F, AntennaRotation.degreesFromTime(12345L, -1.0F), EPSILON);
    }

    @Test
    void negativeTimeIsNormalisedIntoRange() {
        float angle = AntennaRotation.degreesFromTime(-1L, 3.0F);
        assertTrue(angle >= 0.0F && angle < 360.0F, "负数时间也必须规范化到 [0,360)");
    }
}
