package TAOVMN.util;

import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public final class PhysicsTargeting {
    private static final double MAX_LEAD_TICKS = 45.0D;
    private static final double SEARCH_RADIUS_MULTIPLIER = 2.6D;
    private static final double SEARCH_BOUND_MARGIN = 56.0D;

    private PhysicsTargeting() {
    }

    public static Optional<Vec3> findNearestPhysicsTarget(Level level, Vec3 origin, double maxDistance, double projectileSpeed, Vec3 friendlyCenter, double friendlyRadius) {
        if (!ModList.get().isLoaded("valkyrienskies")) {
            return Optional.empty();
        }

        Iterable<?> ships = getShips(level);
        if (ships == null) {
            return Optional.empty();
        }

        Vec3 nearest = null;
        double effectiveMaxDistance = Math.max(maxDistance, maxDistance * SEARCH_RADIUS_MULTIPLIER);
        double bestSqr = effectiveMaxDistance * effectiveMaxDistance;
        double friendlyRadiusSqr = friendlyRadius * friendlyRadius;

        for (Object ship : ships) {
            Vec3 shipCenter = extractShipWorldPosition(ship);
            if (shipCenter == null) {
                continue;
            }

            AABB shipBounds = extractShipBounds(ship);
            Vec3 targetPoint = shipBounds != null
                    ? nearestPointOnBounds(shipBounds.inflate(SEARCH_BOUND_MARGIN), origin)
                    : shipCenter;

            if (friendlyCenter != null && targetPoint.distanceToSqr(friendlyCenter) <= friendlyRadiusSqr) {
                continue;
            }

            double distanceSqr = targetPoint.distanceToSqr(origin);
            if (distanceSqr < bestSqr) {
                bestSqr = distanceSqr;
                nearest = predictAimPoint(origin, targetPoint, extractShipVelocity(ship), projectileSpeed, distanceSqr);
            }
        }

        return Optional.ofNullable(nearest);
    }

    public static Vec3 predictInterceptPoint(Vec3 origin, Vec3 targetPoint, Vec3 targetVelocity, double projectileSpeed) {
        return predictAimPoint(origin, targetPoint, targetVelocity, projectileSpeed, targetPoint.distanceToSqr(origin));
    }

    public static Optional<Vec3> resolveShipWorldPosition(Object ship) {
        return Optional.ofNullable(extractShipWorldPosition(ship));
    }

    private static Iterable<?> getShips(Level level) {
        try {
            Class<?> utils = Class.forName("org.valkyrienskies.mod.common.VSGameUtilsKt");
            Method getAllShips = utils.getMethod("getAllShips", Level.class);
            Object result = getAllShips.invoke(null, level);
            if (result instanceof Iterable<?> iterable) {
                return iterable;
            }
        } catch (ReflectiveOperationException ignored) {
            return Collections.emptyList();
        }

        return Collections.emptyList();
    }

    private static Vec3 extractShipWorldPosition(Object ship) {
        try {
            Method getTransform = ship.getClass().getMethod("getTransform");
            Object transform = getTransform.invoke(ship);
            if (transform == null) {
                return null;
            }

            Method getPositionInWorld = transform.getClass().getMethod("getPositionInWorld");
            Object vec = getPositionInWorld.invoke(transform);
            return extractVec3(vec);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static Vec3 extractShipVelocity(Object ship) {
        List<String> shipMethods = List.of("getVelocity", "getVelocityInWorld", "getShipVelocity", "getLinearVelocity");
        for (String methodName : shipMethods) {
            Vec3 velocity = invokeVec3Method(ship, methodName);
            if (velocity != null) {
                return velocity;
            }
        }

        try {
            Method getTransform = ship.getClass().getMethod("getTransform");
            Object transform = getTransform.invoke(ship);
            if (transform == null) {
                return Vec3.ZERO;
            }

            List<String> transformMethods = List.of("getVelocity", "getVelocityInWorld", "getShipVelocity", "getLinearVelocity");
            for (String methodName : transformMethods) {
                Vec3 velocity = invokeVec3Method(transform, methodName);
                if (velocity != null) {
                    return velocity;
                }
            }
        } catch (ReflectiveOperationException ignored) {
            return Vec3.ZERO;
        }

        return Vec3.ZERO;
    }

    private static AABB extractShipBounds(Object ship) {
        List<String> methods = List.of("getShipAABB", "getWorldAABB", "getRenderAABB", "getAABB", "getShipVoxelAABB", "getVoxelAABB");
        for (String methodName : methods) {
            AABB bounds = invokeAabbMethod(ship, methodName);
            if (bounds != null) {
                return bounds;
            }
        }

        try {
            Method getTransform = ship.getClass().getMethod("getTransform");
            Object transform = getTransform.invoke(ship);
            if (transform == null) {
                return null;
            }

            for (String methodName : methods) {
                AABB bounds = invokeAabbMethod(transform, methodName);
                if (bounds != null) {
                    return bounds;
                }
            }
        } catch (ReflectiveOperationException ignored) {
            return null;
        }

        return null;
    }

    private static Vec3 invokeVec3Method(Object target, String methodName) {
        try {
            Method method = target.getClass().getMethod(methodName);
            Object result = method.invoke(target);
            return extractVec3(result);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static AABB invokeAabbMethod(Object target, String methodName) {
        try {
            Method method = target.getClass().getMethod(methodName);
            Object result = method.invoke(target);
            return extractAabb(result);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static AABB extractAabb(Object aabbObj) {
        if (aabbObj == null) {
            return null;
        }

        if (aabbObj instanceof AABB aabb) {
            return aabb;
        }

        try {
            double minX = readCoordinate(aabbObj, "minX", "getMinX");
            double minY = readCoordinate(aabbObj, "minY", "getMinY");
            double minZ = readCoordinate(aabbObj, "minZ", "getMinZ");
            double maxX = readCoordinate(aabbObj, "maxX", "getMaxX");
            double maxY = readCoordinate(aabbObj, "maxY", "getMaxY");
            double maxZ = readCoordinate(aabbObj, "maxZ", "getMaxZ");
            return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static Vec3 nearestPointOnBounds(AABB bounds, Vec3 origin) {
        return new Vec3(
                Mth.clamp(origin.x, bounds.minX, bounds.maxX),
                Mth.clamp(origin.y, bounds.minY, bounds.maxY),
                Mth.clamp(origin.z, bounds.minZ, bounds.maxZ));
    }

    private static Vec3 predictAimPoint(Vec3 origin, Vec3 targetPoint, Vec3 targetVelocity, double projectileSpeed, double distanceSqr) {
        if (targetVelocity == null || targetVelocity.lengthSqr() < 1.0E-6D || projectileSpeed <= 1.0E-4D) {
            return targetPoint;
        }

        Vec3 toTarget = targetPoint.subtract(origin);
        double speedSqr = projectileSpeed * projectileSpeed;
        double targetSpeedSqr = targetVelocity.lengthSqr();
        double b = 2.0D * toTarget.dot(targetVelocity);
        double c = toTarget.lengthSqr();
        double a = targetSpeedSqr - speedSqr;

        double leadTicks;
        if (Math.abs(a) < 1.0E-6D) {
            double distance = Math.sqrt(distanceSqr);
            leadTicks = distance / projectileSpeed;
        } else {
            double discriminant = b * b - 4.0D * a * c;
            if (discriminant < 0.0D) {
                double distance = Math.sqrt(distanceSqr);
                leadTicks = distance / projectileSpeed;
            } else {
                double sqrt = Math.sqrt(discriminant);
                double t1 = (-b - sqrt) / (2.0D * a);
                double t2 = (-b + sqrt) / (2.0D * a);
                leadTicks = pickInterceptTime(t1, t2);
            }
        }

        leadTicks = Mth.clamp(leadTicks, 0.0D, MAX_LEAD_TICKS);
        return targetPoint.add(targetVelocity.scale(leadTicks));
    }

    private static double pickInterceptTime(double t1, double t2) {
        double best = Double.POSITIVE_INFINITY;
        if (t1 > 0.0D) {
            best = t1;
        }
        if (t2 > 0.0D && t2 < best) {
            best = t2;
        }
        return Double.isFinite(best) ? best : 0.0D;
    }

    private static Vec3 extractVec3(Object vecObj) {
        if (vecObj == null) {
            return null;
        }

        try {
            double x = readCoordinate(vecObj, "x", "getX");
            double y = readCoordinate(vecObj, "y", "getY");
            double z = readCoordinate(vecObj, "z", "getZ");
            return new Vec3(x, y, z);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static double readCoordinate(Object vecObj, String directMethodName, String beanMethodName) throws ReflectiveOperationException {
        try {
            Method direct = vecObj.getClass().getMethod(directMethodName);
            Object val = direct.invoke(vecObj);
            if (val instanceof Number n) {
                return n.doubleValue();
            }
        } catch (NoSuchMethodException ignored) {
            Method bean = vecObj.getClass().getMethod(beanMethodName);
            Object val = bean.invoke(vecObj);
            if (val instanceof Number n) {
                return n.doubleValue();
            }
        }

        throw new ReflectiveOperationException("Cannot read coordinate");
    }
}