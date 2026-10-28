package TAOVMN.block.entity;

import java.util.UUID;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * P-1 火控指挥仪的纯逻辑测试：锁定强度推进、切换回退、主目标切换判定。
 * 这些规则决定了导弹端接收到的火控参数是否连续可靠。
 */
class P1DirectorBlockEntityTest {
    private static final float EPSILON = 1.0E-6F;

    @Test
    void lockStrengthRampsUpToFullLockWithinTwentyTicks() {
        float strength = 0.0F;
        int ticksToFullLock = -1;

        for (int tick = 1; tick <= 40; tick++) {
            strength = P1DirectorBlockEntity.nextLockStrength(strength, true);
            if (strength >= 1.0F && ticksToFullLock < 0) {
                ticksToFullLock = tick;
            }
        }

        assertEquals(1.0F, strength, EPSILON, "锁定强度不应超过 1.0");
        assertEquals(Math.round(1.0F / P1DirectorBlockEntity.LOCK_STRENGTH_GAIN), ticksToFullLock,
                "满锁定耗时应与 LOCK_STRENGTH_GAIN 一致");
    }

    @Test
    void lockStrengthDecaysTwiceAsFastWithoutTargetAndNeverGoesNegative() {
        float lost = P1DirectorBlockEntity.nextLockStrength(1.0F, false);
        assertEquals(1.0F - P1DirectorBlockEntity.LOCK_STRENGTH_GAIN * 2.0F, lost, EPSILON,
                "目标丢失后的衰减速率应为增长速率的两倍");

        float strength = 0.5F;
        for (int tick = 0; tick < 100; tick++) {
            strength = P1DirectorBlockEntity.nextLockStrength(strength, false);
        }
        assertEquals(0.0F, strength, EPSILON, "锁定强度不应变成负数");
    }

    @Test
    void switchingPrimaryTargetRetreatsToRetainedBaseline() {
        assertEquals(P1DirectorBlockEntity.LOCK_STRENGTH_RETAINED,
                P1DirectorBlockEntity.retainedLockStrength(1.0F), EPSILON);
        assertEquals(0.1F, P1DirectorBlockEntity.retainedLockStrength(0.1F), EPSILON,
                "低于基线的锁定强度不应被抬高");
    }

    @Test
    void sameTargetKeepingPositionIsNotASwitch() {
        UUID uuid = UUID.randomUUID();
        Vec3 before = new Vec3(10.0D, 64.0D, 10.0D);

        assertFalse(P1DirectorBlockEntity.isTargetSwitch(uuid, before, uuid, new Vec3(10.5D, 64.0D, 10.5D)),
                "同一目标的小幅移动不是主目标切换");
        assertFalse(P1DirectorBlockEntity.isTargetSwitch(uuid, before, uuid, new Vec3(11.5D, 66.0D, 10.0D)),
                "容差范围内的高速目标位移不应重置锁定强度");
    }

    @Test
    void targetJumpingBeyondToleranceCountsAsASwitch() {
        UUID uuid = UUID.randomUUID();
        Vec3 before = new Vec3(0.0D, 64.0D, 0.0D);

        assertTrue(P1DirectorBlockEntity.isTargetSwitch(uuid, before, uuid, new Vec3(0.0D, 80.0D, 0.0D)),
                "同 UUID 但位置大幅跳变（换乘载具/传送）应视为切换");
    }

    @Test
    void changedUuidOrLostPositionCountsAsASwitch() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        Vec3 position = new Vec3(0.0D, 64.0D, 0.0D);

        assertTrue(P1DirectorBlockEntity.isTargetSwitch(first, position, second, position),
                "目标 UUID 变化即为切换");
        assertTrue(P1DirectorBlockEntity.isTargetSwitch(first, position, null, null),
                "目标丢失即为切换");
        assertTrue(P1DirectorBlockEntity.isTargetSwitch(first, position, first, null),
                "位置消失即为切换");
    }

    @Test
    void noSwitchIsReportedWhenNoTargetWasTracked() {
        assertFalse(P1DirectorBlockEntity.isTargetSwitch(null, null, UUID.randomUUID(), Vec3.ZERO),
                "首次建立跟踪不算切换，不应触发锁定强度回退");
    }

    @Test
    void fireControlRangeExceedsRadarBindingRange() {
        assertTrue(P1DirectorBlockEntity.FIRE_CONTROL_RANGE >= 32.0D,
                "火控分发范围应覆盖舰上发射装置到目标的常见距离");
    }
}
