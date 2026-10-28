package TAOVMN.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;

/**
 * 火力通道表的纯逻辑：把扫描到的发射装置读数整理成有序的通道列表，并给出默认编组。
 *
 * <p>排序规则为"装置类型 → 坐标"，保证界面上的条块顺序稳定，不会因为目标变化而跳动。
 * 编组优先取玩家显式指定（{@code assignments}，键为 {@link BlockPos#asLong()}），
 * 未指定时按弹药目标域给默认值（防空弹归防空群、其余归反舰群1）。
 */
public final class FireChannelTable {
    private FireChannelTable() {
    }

    /** 服务端扫描一座发射装置得到的一行读数（尚未编组）。 */
    public record Reading(
            BlockPos pos,
            LauncherKind kind,
            String ammoItemId,
            int ammoCount,
            int ammoCapacity,
            boolean ready,
            TargetDomain domain
    ) {
        public Reading {
            int count = Math.max(0, ammoCount);
            ammoCount = count;
            ammoCapacity = Math.max(count, ammoCapacity);
            ammoItemId = ammoItemId == null ? "" : ammoItemId;
            domain = domain == null ? TargetDomain.UNIVERSAL : domain;
        }
    }

    /** 未编组时按弹药目标域给默认编组：防空弹进防空群，其余（含通用）进反舰群1。 */
    public static FireChannelGroup defaultGroup(TargetDomain domain) {
        return domain == TargetDomain.ANTI_AIR ? FireChannelGroup.ANTI_AIR_1 : FireChannelGroup.ANTI_SHIP_1;
    }

    /** 编组循环切换；空值从反舰群1开始。 */
    public static FireChannelGroup cycle(FireChannelGroup current) {
        return current == null ? FireChannelGroup.ANTI_SHIP_1 : current.next();
    }

    /**
     * 整理通道列表。
     *
     * @param readings    扫描到的发射装置读数
     * @param assignments 玩家指定的编组（键为方块坐标的 {@link BlockPos#asLong()}），可为 null
     */
    public static List<FireChannel> assemble(List<Reading> readings, Map<Long, FireChannelGroup> assignments) {
        if (readings == null || readings.isEmpty()) {
            return List.of();
        }

        List<Reading> sorted = new ArrayList<>(readings);
        sorted.sort(Comparator
                .comparingInt((Reading reading) -> reading.kind().ordinal())
                .thenComparingLong(reading -> reading.pos().asLong()));

        List<FireChannel> channels = new ArrayList<>(sorted.size());
        for (Reading reading : sorted) {
            FireChannelGroup group = assignments == null ? null : assignments.get(reading.pos().asLong());
            if (group == null) {
                group = defaultGroup(reading.domain());
            }
            channels.add(new FireChannel(reading.pos().immutable(), reading.kind(), reading.ammoItemId(),
                    reading.ammoCount(), reading.ammoCapacity(), reading.ready(), group, reading.domain()));
        }
        return channels;
    }
}
