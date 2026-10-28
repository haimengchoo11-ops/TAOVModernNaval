package TAOVMN.util;

import java.util.Optional;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PhysicsTargetingTest {
    private static class PrivateTransform {
        private Vec3 getPositionInWorld() {
            return new Vec3(1.0D, 2.0D, 3.0D);
        }
    }

    private static class PrivateShip {
        private PrivateTransform getTransform() {
            return new PrivateTransform();
        }
    }

    /** 反射链上完全没有 getTransform 的对象，用于验证"安全失败"路径 */
    private static class ShipWithoutAccessors {
    }

    @Test
    void resolveShipWorldPositionReadsNonPublicAccessors() {
        Optional<Vec3> resolved = assertDoesNotThrow(() -> PhysicsTargeting.resolveShipWorldPosition(new PrivateShip()));

        assertTrue(resolved.isPresent(), "非 public 访问器应通过 setAccessible 正常读取");
        assertAll(
                () -> assertEquals(1.0D, resolved.get().x),
                () -> assertEquals(2.0D, resolved.get().y),
                () -> assertEquals(3.0D, resolved.get().z));
    }

    @Test
    void resolveShipWorldPositionFailsClosedWhenAccessorsAreMissing() {
        assertDoesNotThrow(() -> {
            assertTrue(PhysicsTargeting.resolveShipWorldPosition(new ShipWithoutAccessors()).isEmpty());
        });
    }

    @Test
    void predictInterceptPointFallsBackToTargetWhenVelocityIsInvalid() {
        Vec3 origin = new Vec3(0.0D, 0.0D, 0.0D);
        Vec3 target = new Vec3(10.0D, 0.0D, 0.0D);

        Vec3 predicted = PhysicsTargeting.predictInterceptPoint(origin, target, Vec3.ZERO, 5.0D);

        assertAll(
                () -> assertEquals(target.x, predicted.x),
                () -> assertEquals(target.y, predicted.y),
                () -> assertEquals(target.z, predicted.z)
        );
    }
}
