package TAOVMN.client;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * 客户端「来袭导弹」告警状态。
 *
 * <p>由 {@link TAOVMN.network.S2CThreatAlertPacket} 驱动：雷达每次扫描到接近中的弹体就推一条包，
 * 这里只记录数量与过期时刻。用毫秒而非 tick 计时，因此不需要额外的 tick 事件，
 * 也不会因为帧率不同而让告警时长抖动。
 */
public final class ClientThreatAlert {
    /** 告警在最后这段时间内淡出（毫秒）。 */
    private static final long FADE_OUT_MILLIS = 400L;
    /** 告警音效的最小间隔（毫秒），避免多部雷达同时报警时连成一片。 */
    private static final long ALARM_COOLDOWN_MILLIS = 2500L;

    private static int missileCount;
    private static long expiresAtMillis;
    private static long lastAlarmAtMillis = Long.MIN_VALUE;

    private ClientThreatAlert() {
    }

    /** 服务端推送一条告警；从"无告警"转为"有告警"时播放一次告警音。 */
    public static void receive(int count, int ttlTicks) {
        boolean wasIdle = !isActive();
        missileCount = Math.max(0, count);
        expiresAtMillis = Util.getMillis() + Math.max(1, ttlTicks) * 50L;
        if (wasIdle && missileCount > 0) {
            playAlarm();
        }
    }

    public static boolean isActive() {
        return Util.getMillis() < expiresAtMillis;
    }

    public static int missileCount() {
        return missileCount;
    }

    /** 告警的整体不透明度（0–1），用于尾部淡出。 */
    public static float opacity() {
        long remaining = expiresAtMillis - Util.getMillis();
        if (remaining <= 0L) {
            return 0.0F;
        }
        return remaining < FADE_OUT_MILLIS ? (float) remaining / FADE_OUT_MILLIS : 1.0F;
    }

    private static void playAlarm() {
        Minecraft minecraft = Minecraft.getInstance();
        Level level = minecraft.level;
        Player player = minecraft.player;
        if (level == null || player == null) {
            return;
        }
        long now = Util.getMillis();
        if (now - lastAlarmAtMillis < ALARM_COOLDOWN_MILLIS) {
            return;
        }
        lastAlarmAtMillis = now;
        // RAID_HORN 在 1.20.1 是 Holder.Reference<SoundEvent>，需要 .value()
        level.playLocalSound(player.getX(), player.getY(), player.getZ(),
                SoundEvents.RAID_HORN.value(), SoundSource.PLAYERS, 0.5F, 1.0F, false);
    }
}
