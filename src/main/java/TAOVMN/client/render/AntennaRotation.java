package TAOVMN.client.render;

import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 雷达天线旋转角度的统一计算。
 *
 * <p>此前 {@code P1DirectorBlockEntity} 每 tick 累加一个 {@code radarAngle}，写进 NBT、
 * 随方块实体包同步到客户端——但<b>没有任何渲染器读它</b>。那条数据链是完整的，
 * 只差渲染端一行 {@code setCustomAnimations}，于是"雷达天线每 tick 转 3°"这件事
 * 在游戏里完全看不见，还白白占了同步包。
 *
 * <p>现在改为客户端纯本地推算：
 * <ul>
 *   <li>不需要网络字节，也不再需要 NBT 字段；</li>
 *   <li>与游戏时间严格同步，不会因为 10~20 tick 一次的同步包而一跳一跳；</li>
 *   <li>断线重连、区块重载都不会出现角度跳变（不会从 0° 重新开始）。</li>
 * </ul>
 */
final class AntennaRotation {

    private AntennaRotation() {
    }

    /**
     * 按游戏时间推算当前旋转角（度）。
     *
     * <p>先对一圈所需 tick 数取模再相乘，避免 {@code gameTime} 很大时 float 精度不足导致角度抖动。
     */
    static float degrees(BlockEntity blockEntity, float degreesPerTick) {
        Level level = blockEntity.getLevel();
        long time = level == null ? 0L : level.getGameTime();
        return degreesFromTime(time, degreesPerTick);
    }

    /** 纯函数版本，便于单元测试。 */
    static float degreesFromTime(long gameTime, float degreesPerTick) {
        if (degreesPerTick <= 0.0F) {
            return 0.0F;
        }
        long periodTicks = Math.max(1L, Math.round(360.0F / degreesPerTick));
        // 负数 gameTime 理论上不会出现，这里仍然做一次规范化，保证结果落在 [0, 360)
        long phase = ((gameTime % periodTicks) + periodTicks) % periodTicks;
        return phase * degreesPerTick;
    }

    /** 弧度版，直接喂给 GeckoLib 的 {@code CoreGeoBone#setRotY}。 */
    static float radians(BlockEntity blockEntity, float degreesPerTick) {
        return degrees(blockEntity, degreesPerTick) * Mth.DEG_TO_RAD;
    }
}
