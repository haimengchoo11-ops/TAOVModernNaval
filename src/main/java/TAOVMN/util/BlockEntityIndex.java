package TAOVMN.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import TAOVMN.block.entity.RadarBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * 按"已加载区块"枚举方块实体，替代 {@code BlockPos.betweenClosed} + {@code level.getBlockEntity(pos)} 的死循环。
 *
 * <p><b>为什么必须换</b>：{@code Level#getBlockEntity(BlockPos)} 走的是
 * {@code getChunkAt(pos, ChunkStatus.FULL, true)} —— 最后一个参数是 {@code requireChunk = true}，
 * 也就是<b>对未加载区块会触发同步加载</b>。原先三处火控扫描（22–25 格立方体和 65×33×65 的
 * 悬挂炮选靶）在船移动、区块换入换出时会制造区块加载风暴，代价远超"循环次数多"本身。
 *
 * <p>{@link net.minecraft.server.level.ServerChunkCache#getChunkNow(int, int)} 只返回<b>已经加载</b>
 * 的区块，为 null 时直接跳过，因此既快又不会引发任何加载。行为差异仅在于"未加载区块里的装置
 * 不再被发现"，这对火控是有利的（看不见的雷达本来也不该提供火控）。
 */
public final class BlockEntityIndex {

    private BlockEntityIndex() {
    }

    /**
     * 收集以 {@code center} 为中心、水平半径 {@code radiusXZ}、垂直半径 {@code radiusY} 的立方体内，
     * 满足 {@code filter} 的全部方块实体。
     *
     * <p>结果顺序不保证稳定（取决于区块内的 map 遍历顺序）；需要"最近"语义时请调用方自行按距离挑选。
     */
    public static List<BlockEntity> collectAny(ServerLevel level, BlockPos center, int radiusXZ, int radiusY,
            Predicate<BlockEntity> filter) {
        List<BlockEntity> found = new ArrayList<>();
        if (level == null || center == null || filter == null || radiusXZ < 0 || radiusY < 0) {
            return found;
        }

        int minChunkX = (center.getX() - radiusXZ) >> 4;
        int maxChunkX = (center.getX() + radiusXZ) >> 4;
        int minChunkZ = (center.getZ() - radiusXZ) >> 4;
        int maxChunkZ = (center.getZ() + radiusXZ) >> 4;
        long radiusSqr = (long) radiusXZ * radiusXZ;

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
                if (chunk == null) {
                    // 区块未加载 → 跳过。绝不为了扫描去加载区块。
                    continue;
                }
                for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                    if (blockEntity == null || blockEntity.isRemoved()) {
                        continue;
                    }
                    BlockPos pos = blockEntity.getBlockPos();
                    int dy = pos.getY() - center.getY();
                    if (dy > radiusY || dy < -radiusY) {
                        continue;
                    }
                    long dx = (long) pos.getX() - center.getX();
                    long dz = (long) pos.getZ() - center.getZ();
                    if (dx * dx + dz * dz > radiusSqr) {
                        continue;
                    }
                    if (!filter.test(blockEntity)) {
                        continue;
                    }
                    found.add(blockEntity);
                }
            }
        }
        return found;
    }

    /** 同 {@link #collectAny}，但按类型收窄结果。 */
    public static <T extends BlockEntity> List<T> collect(ServerLevel level, BlockPos center, int radiusXZ, int radiusY,
            Class<T> type) {
        List<T> found = new ArrayList<>();
        for (BlockEntity blockEntity : collectAny(level, center, radiusXZ, radiusY, type::isInstance)) {
            found.add(type.cast(blockEntity));
        }
        return found;
    }

    /**
     * 找到距离 {@code origin} 最近的雷达 / 火控指挥仪（{@link RadarBlockEntity} 及其子类）。
     *
     * <p>等价于原先的 {@code MissileFireControl.findNearbyRadar}，但不会触发区块加载。
     */
    public static Optional<RadarBlockEntity> nearestRadar(ServerLevel level, BlockPos origin, int radiusXZ, int radiusY) {
        RadarBlockEntity nearest = null;
        double bestDistanceSqr = Double.MAX_VALUE;
        for (BlockEntity blockEntity : collectAny(level, origin, radiusXZ, radiusY,
                candidate -> candidate instanceof RadarBlockEntity)) {
            double distanceSqr = blockEntity.getBlockPos().distSqr(origin);
            if (distanceSqr < bestDistanceSqr) {
                bestDistanceSqr = distanceSqr;
                nearest = (RadarBlockEntity) blockEntity;
            }
        }
        return Optional.ofNullable(nearest);
    }
}
