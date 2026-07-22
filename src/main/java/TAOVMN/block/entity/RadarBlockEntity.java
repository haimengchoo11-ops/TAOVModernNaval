package TAOVMN.block.entity;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import TAOVMN.registry.ModBlockEntities;
import TAOVMN.util.PhysicsTargeting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.ModList;

public class RadarBlockEntity extends BlockEntity {
    private static final String TAG_IFF_ENABLED = "IffEnabled";
    private static final String TAG_SELECTED_INDEX = "SelectedIndex";
    private static final int SCAN_INTERVAL_TICKS = 1;
    private static final int DEFAULT_SCAN_RANGE = 1000;
    private static final double EUREKA_SEARCH_MARGIN = 128.0D;

    private int scanCooldown = 0;
    private int selectedIndex = 0;
    private boolean iffEnabled = true;
    private final List<RadarTarget> trackedTargets = new ArrayList<>();

    public RadarBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RADAR_BLOCK.get(), pos, state);
    }

    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, RadarBlockEntity radar) {
        radar.tickServer();
    }

    private void tickServer() {
        if (level == null || level.isClientSide) {
            return;
        }

        if (--scanCooldown > 0) {
            return;
        }

        scanCooldown = SCAN_INTERVAL_TICKS;
        rescanTargets();
    }

    private void rescanTargets() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        Vec3 center = Vec3.atCenterOf(worldPosition);

        List<RadarTarget> physicsTargets = scanPhysicsTargets(serverLevel, center, DEFAULT_SCAN_RANGE)
                .stream()
                .map(pos -> new RadarTarget(null, pos, true))
                .toList();

        List<RadarTarget> eurekaTargets = scanEurekaTargets(serverLevel, center, DEFAULT_SCAN_RANGE)
            .stream()
            .map(pos -> new RadarTarget(null, pos, true))
            .toList();

        trackedTargets.clear();
        trackedTargets.addAll(physicsTargets);
        trackedTargets.addAll(eurekaTargets);
        trackedTargets.sort(Comparator.comparingDouble(target -> target.position.distanceToSqr(center)));

        if (trackedTargets.isEmpty()) {
            selectedIndex = 0;
        } else {
            selectedIndex = Math.floorMod(selectedIndex, trackedTargets.size());
        }

        setChanged();
    }

    public boolean isIffEnabled() {
        return iffEnabled;
    }

    public boolean toggleIff() {
        iffEnabled = !iffEnabled;
        rescanTargets();
        return iffEnabled;
    }

    public void refreshTargetsNow() {
        rescanTargets();
    }

    public int getTrackedCount() {
        return trackedTargets.size();
    }

    public UUID cycleTargetSelection() {
        if (trackedTargets.isEmpty()) {
            return null;
        }
        selectedIndex = (selectedIndex + 1) % trackedTargets.size();
        setChanged();
        return trackedTargets.get(selectedIndex).entityUuid;
    }

    public UUID getSelectedTargetUuid() {
        if (trackedTargets.isEmpty()) {
            rescanTargets();
        }
        if (trackedTargets.isEmpty()) {
            return null;
        }
        selectedIndex = Math.floorMod(selectedIndex, trackedTargets.size());
        return trackedTargets.get(selectedIndex).entityUuid;
    }

    public Vec3 getSelectedTargetPos() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return null;
        }

        if (trackedTargets.isEmpty()) {
            rescanTargets();
        }

        if (trackedTargets.isEmpty()) {
            return null;
        }

        selectedIndex = Math.floorMod(selectedIndex, trackedTargets.size());
        RadarTarget selected = trackedTargets.get(selectedIndex);
        if (selected.entityUuid == null) {
            return selected.position;
        }

        return null;
    }

    public boolean isSelectedPhysicsTarget() {
        if (trackedTargets.isEmpty()) {
            return false;
        }
        selectedIndex = Math.floorMod(selectedIndex, trackedTargets.size());
        return trackedTargets.get(selectedIndex).physicsTarget;
    }

    private List<Vec3> scanPhysicsTargets(Level level, Vec3 center, double range) {
        if (!ModList.get().isLoaded("valkyrienskies")) {
            return List.of();
        }

        Iterable<?> ships = getShips(level);
        if (ships == null) {
            return List.of();
        }

        double maxDistanceSqr = range * range;
        List<Vec3> result = new ArrayList<>();
        for (Object ship : ships) {
            Vec3 shipPos = extractShipWorldPosition(ship).orElse(null);
            if (shipPos == null || shipPos.distanceToSqr(center) > maxDistanceSqr) {
                continue;
            }
            result.add(shipPos);
        }
        return result;
    }

    private Iterable<?> getShips(Level level) {
        try {
            Class<?> utils = Class.forName("org.valkyrienskies.mod.common.VSGameUtilsKt");
            Method getAllShips = utils.getMethod("getAllShips", Level.class);
            Object result = getAllShips.invoke(null, level);
            if (result instanceof Iterable<?> iterable) {
                return iterable;
            }
        } catch (ReflectiveOperationException ignored) {
            return List.of();
        }
        return List.of();
    }

    private List<Vec3> scanEurekaTargets(Level level, Vec3 center, double range) {
        if (!ModList.get().isLoaded("vs_eureka") && !ModList.get().isLoaded("eureka")) {
            return List.of();
        }

        Class<?> helmClass = loadClass("org.valkyrienskies.eureka.blockentity.ShipHelmBlockEntity");
        if (helmClass == null) {
            return List.of();
        }

        double maxDistanceSqr = (range + EUREKA_SEARCH_MARGIN) * (range + EUREKA_SEARCH_MARGIN);
        List<Vec3> result = new ArrayList<>();
        for (BlockEntity blockEntity : collectLoadedBlockEntities(level)) {
            if (blockEntity == null || !helmClass.isInstance(blockEntity)) {
                continue;
            }

            if (!invokeBoolean(blockEntity, "getAssembled")) {
                continue;
            }

            Vec3 shipPos = resolveEurekaShipPosition(blockEntity).orElse(Vec3.atCenterOf(blockEntity.getBlockPos()));
            if (shipPos.distanceToSqr(center) > maxDistanceSqr) {
                continue;
            }

            result.add(shipPos);
        }

        return result;
    }

    private List<BlockEntity> collectLoadedBlockEntities(Level level) {
        Set<BlockEntity> collected = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
        Class<?> current = level.getClass();
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                field.setAccessible(true);
                try {
                    Object value = field.get(level);
                    if (value instanceof Map<?, ?> map) {
                        for (Object entryValue : map.values()) {
                            if (entryValue instanceof BlockEntity blockEntity) {
                                collected.add(blockEntity);
                            }
                        }
                    } else if (value instanceof Collection<?> collection) {
                        for (Object entry : collection) {
                            if (entry instanceof BlockEntity blockEntity) {
                                collected.add(blockEntity);
                            }
                        }
                    }
                } catch (IllegalAccessException ignored) {
                }
            }
            current = current.getSuperclass();
        }
        return new ArrayList<>(collected);
    }

    private Optional<Vec3> resolveEurekaShipPosition(Object helmBlockEntity) {
        try {
            Method getShip = helmBlockEntity.getClass().getDeclaredMethod("getShip");
            getShip.setAccessible(true);
            Object ship = getShip.invoke(helmBlockEntity);
            if (ship == null) {
                return Optional.empty();
            }

            return PhysicsTargeting.resolveShipWorldPosition(ship);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
            return Optional.empty();
        }
    }

    private Class<?> loadClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException ignored) {
            return null;
        }
    }

    private boolean invokeBoolean(Object target, String methodName) {
        try {
            Method method = target.getClass().getMethod(methodName);
            Object result = method.invoke(target);
            return result instanceof Boolean bool && bool;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    private Optional<Vec3> extractShipWorldPosition(Object ship) {
        try {
            Method getTransform = ship.getClass().getMethod("getTransform");
            Object transform = getTransform.invoke(ship);
            if (transform == null) {
                return Optional.empty();
            }

            Method getPositionInWorld = transform.getClass().getMethod("getPositionInWorld");
            Object vec = getPositionInWorld.invoke(transform);
            return Optional.ofNullable(extractVec3(vec));
        } catch (ReflectiveOperationException ignored) {
            return Optional.empty();
        }
    }

    private Vec3 extractVec3(Object vecObj) {
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

    private double readCoordinate(Object vecObj, String directMethodName, String beanMethodName) throws ReflectiveOperationException {
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

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putBoolean(TAG_IFF_ENABLED, iffEnabled);
        tag.putInt(TAG_SELECTED_INDEX, selectedIndex);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        iffEnabled = !tag.contains(TAG_IFF_ENABLED) || tag.getBoolean(TAG_IFF_ENABLED);
        selectedIndex = tag.contains(TAG_SELECTED_INDEX) ? Math.max(0, tag.getInt(TAG_SELECTED_INDEX)) : 0;
    }

    private static final class RadarTarget {
        private final UUID entityUuid;
        private final Vec3 position;
        private final boolean physicsTarget;

        private RadarTarget(UUID entityUuid, Vec3 position, boolean physicsTarget) {
            this.entityUuid = entityUuid;
            this.position = position;
            this.physicsTarget = physicsTarget;
        }
    }
}
