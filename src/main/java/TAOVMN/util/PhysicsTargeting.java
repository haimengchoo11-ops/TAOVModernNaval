package TAOVMN.util;

import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.ModList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class PhysicsTargeting {
    private static final Logger LOGGER = LogManager.getLogger(PhysicsTargeting.class);
    private static final double MAX_LEAD_TICKS = 45.0D;
    private static final double SEARCH_RADIUS_MULTIPLIER = 2.6D;
    private static final double SEARCH_BOUND_MARGIN = 56.0D;
    private static final Map<Class<?>, Map<String, Optional<Method>>> NO_ARG_METHOD_CACHE = new ConcurrentHashMap<>();
    private static final Set<String> REFLECTION_FAILURE_LOGS = ConcurrentHashMap.newKeySet();

    private PhysicsTargeting() {
    }

    private static void logReflectionIssue(String phase, Throwable error) {
        String signature = phase + ":" + error.getClass().getSimpleName() + ":" + (error.getMessage() == null ? "unknown" : error.getMessage());
        if (REFLECTION_FAILURE_LOGS.add(signature)) {
            LOGGER.debug("Valkyrien Skies fallback [{}] failed gracefully: {}", phase, error.toString());
        }
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
            if (ship == null) {
                continue;
            }
            
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
                Vec3 shipVelocity = extractShipVelocity(ship);
                nearest = predictAimPoint(origin, targetPoint, shipVelocity, projectileSpeed, distanceSqr);
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
        if (level == null) {
            return Collections.emptyList();
        }

        // 方案1：VSGameUtilsKt.getAllShips (通常 API)
        try {
            Class<?> utils = Class.forName("org.valkyrienskies.mod.common.VSGameUtilsKt");
            Method getAllShips = findNoArgMethod(utils, "getAllShips").orElse(null);
            if (getAllShips == null) {
                getAllShips = findMethodBySignature(utils, "getAllShips", Level.class).orElse(null);
            }
            if (getAllShips != null) {
                Object result = safeInvoke(getAllShips, null, level);
                if (result instanceof Iterable<?> iterable) {
                    return iterable;
                }
            }
        } catch (Throwable t) {
            logReflectionIssue("VSGameUtilsKt.getAllShips", t);
            // Third-party API may be absent or incompatible; fail closed and continue to the next strategy.
        }

        // 方案2：从 ServerLevel 直接访问 VS 对象管理器
        try {
            if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                Class<?> vsDataClass = Class.forName("org.valkyrienskies.mod.common.VSData");
                Method getInstance = findMethodBySignature(vsDataClass, "getInstance").orElse(null);
                if (getInstance == null) {
                    return Collections.emptyList();
                }
                Object vsData = safeInvoke(getInstance, null);
                if (vsData == null) {
                    return Collections.emptyList();
                }

                Method serverDataGetter = findMethodBySignature(vsData.getClass(), "getServerData").orElse(null);
                if (serverDataGetter == null) {
                    return Collections.emptyList();
                }
                Object serverData = safeInvoke(serverDataGetter, vsData);
                if (serverData == null) {
                    return Collections.emptyList();
                }

                Method getLoadedShips = findMethodBySignature(serverData.getClass(), "getLoadedShips").orElse(null);
                if (getLoadedShips == null) {
                    return Collections.emptyList();
                }
                Object result = safeInvoke(getLoadedShips, serverData);
                if (result instanceof Iterable<?> iterable) {
                    return iterable;
                }
            }
        } catch (Throwable t) {
            logReflectionIssue("VSData.getLoadedShips", t);
            // The VS API may have changed between versions; a graceful fallback is safer than crashing the game.
        }

        return Collections.emptyList();
    }

    private static Vec3 extractShipWorldPosition(Object ship) {
        if (ship == null) {
            return null;
        }

        try {
            Method getTransform = findNoArgMethod(ship.getClass(), "getTransform").orElse(null);
            if (getTransform == null) {
                return null;
            }

            Object transform = safeInvoke(getTransform, ship);
            if (transform == null) {
                return null;
            }

            Method getPositionInWorld = findNoArgMethod(transform.getClass(), "getPositionInWorld").orElse(null);
            if (getPositionInWorld == null) {
                return null;
            }

            Object vec = safeInvoke(getPositionInWorld, transform);
            return extractVec3(vec);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Vec3 extractShipVelocity(Object ship) {
        if (ship == null) {
            return Vec3.ZERO;
        }

        List<String> shipMethods = List.of("getVelocity", "getVelocityInWorld", "getShipVelocity", "getLinearVelocity");
        for (String methodName : shipMethods) {
            Vec3 velocity = invokeVec3Method(ship, methodName);
            if (velocity != null) {
                return velocity;
            }
        }

        try {
            Method getTransform = findNoArgMethod(ship.getClass(), "getTransform").orElse(null);
            if (getTransform == null) {
                return Vec3.ZERO;
            }

            Object transform = safeInvoke(getTransform, ship);
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
        } catch (Throwable t) {
            logReflectionIssue("extractShipVelocity", t);
            return Vec3.ZERO;
        }

        return Vec3.ZERO;
    }

    private static AABB extractShipBounds(Object ship) {
        if (ship == null) {
            return null;
        }

        List<String> methods = List.of("getShipAABB", "getWorldAABB", "getRenderAABB", "getAABB", "getShipVoxelAABB", "getVoxelAABB", "getShipBB");
        for (String methodName : methods) {
            try {
                AABB bounds = invokeAabbMethod(ship, methodName);
                if (bounds != null && isValidBounds(bounds)) {
                    return bounds;
                }
            } catch (Exception ignored) {
                // 继续尝试下一个方法
            }
        }

        try {
            Method getTransform = findNoArgMethod(ship.getClass(), "getTransform").orElse(null);
            if (getTransform == null) {
                return null;
            }

            Object transform = safeInvoke(getTransform, ship);
            if (transform == null) {
                return null;
            }

            for (String methodName : methods) {
                try {
                    AABB bounds = invokeAabbMethod(transform, methodName);
                    if (bounds != null && isValidBounds(bounds)) {
                        return bounds;
                    }
                } catch (Exception ignored) {
                    // 继续尝试下一个方法
                }
            }
        } catch (Throwable t) {
            logReflectionIssue("extractShipBounds", t);
            return null;
        }

        return null;
    }

    private static boolean isValidBounds(AABB bounds) {
        return bounds != null && bounds.minX < bounds.maxX && bounds.minY < bounds.maxY && bounds.minZ < bounds.maxZ;
    }

    private static Vec3 invokeVec3Method(Object target, String methodName) {
        if (target == null || methodName == null || methodName.isBlank()) {
            return null;
        }

        try {
            Method method = findNoArgMethod(target.getClass(), methodName).orElse(null);
            if (method == null) {
                return null;
            }

            Object result = safeInvoke(method, target);
            return extractVec3(result);
        } catch (Throwable t) {
            logReflectionIssue("invokeVec3Method:" + methodName, t);
            return null;
        }
    }

    private static AABB invokeAabbMethod(Object target, String methodName) {
        if (target == null || methodName == null || methodName.isBlank()) {
            return null;
        }

        try {
            Method method = findNoArgMethod(target.getClass(), methodName).orElse(null);
            if (method == null) {
                return null;
            }

            Object result = safeInvoke(method, target);
            return extractAabb(result);
        } catch (Throwable t) {
            logReflectionIssue("invokeAabbMethod:" + methodName, t);
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
        } catch (ReflectiveOperationException t) {
            logReflectionIssue("extractVec3", t);
            return null;
        }
    }

    private static double readCoordinate(Object vecObj, String directMethodName, String beanMethodName) throws ReflectiveOperationException {
        try {
            Method direct = findNoArgMethod(vecObj.getClass(), directMethodName).orElse(null);
            if (direct == null) {
                throw new NoSuchMethodException(directMethodName);
            }

            Object val = safeInvoke(direct, vecObj);
            if (val instanceof Number n) {
                return n.doubleValue();
            }
        } catch (NoSuchMethodException ignored) {
            Method bean = findNoArgMethod(vecObj.getClass(), beanMethodName).orElse(null);
            if (bean == null) {
                throw new ReflectiveOperationException("Cannot read coordinate");
            }

            Object val = safeInvoke(bean, vecObj);
            if (val instanceof Number n) {
                return n.doubleValue();
            }
        }

        throw new ReflectiveOperationException("Cannot read coordinate");
    }

    private static Object safeInvoke(Method method, Object target, Object... args) {
        if (method == null) {
            return null;
        }

        try {
            if (args == null || args.length == 0) {
                return method.invoke(target);
            }
            return method.invoke(target, args);
        } catch (Throwable t) {
            logReflectionIssue("safeInvoke:" + method.getName(), t);
            return null;
        }
    }

    private static Optional<Method> findNoArgMethod(Class<?> owner, String methodName) {
        if (owner == null || methodName == null || methodName.isBlank()) {
            return Optional.empty();
        }

        Map<String, Optional<Method>> byName = NO_ARG_METHOD_CACHE.computeIfAbsent(owner, ignored -> new ConcurrentHashMap<>());
        return byName.computeIfAbsent(methodName, name -> lookupNoArgMethod(owner, name));
    }

    private static Optional<Method> lookupNoArgMethod(Class<?> owner, String methodName) {
        try {
            Method method = findMethodBySignature(owner, methodName).orElse(null);
            if (method == null) {
                return Optional.empty();
            }

            try {
                method.setAccessible(true);
            } catch (RuntimeException ignored) {
                return Optional.empty();
            }

            return Optional.of(method);
        } catch (Throwable ignored) {
            return Optional.empty();
        }
    }

    private static Optional<Method> findMethodBySignature(Class<?> owner, String methodName, Class<?>... paramTypes) {
        if (owner == null || methodName == null || methodName.isBlank()) {
            return Optional.empty();
        }

        try {
            Method method = owner.getMethod(methodName, paramTypes);
            if (method.getParameterCount() == 0 || paramTypes.length > 0) {
                return Optional.of(method);
            }
            return Optional.of(method);
        } catch (NoSuchMethodException ignored) {
            for (Class<?> current = owner; current != null; current = current.getSuperclass()) {
                for (Method candidate : current.getDeclaredMethods()) {
                    if (candidate.getName().equals(methodName) && candidate.getParameterCount() == paramTypes.length) {
                        if (paramTypes.length == 0 || isAssignableTo(candidate.getParameterTypes(), paramTypes)) {
                            return Optional.of(candidate);
                        }
                    }
                }
            }
        } catch (Throwable t) {
            logReflectionIssue("findMethodBySignature:" + methodName, t);
            // ignored; caller will treat this as a missing API and fail closed.
        }

        return Optional.empty();
    }

    private static boolean isAssignableTo(Class<?>[] candidateParams, Class<?>[] expectedParams) {
        if (candidateParams.length != expectedParams.length) {
            return false;
        }

        for (int i = 0; i < candidateParams.length; i++) {
            if (!candidateParams[i].isAssignableFrom(expectedParams[i])) {
                return false;
            }
        }

        return true;
    }
}
