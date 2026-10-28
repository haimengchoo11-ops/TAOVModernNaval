package TAOVMN.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 火力通道表的纯逻辑测试：默认编组、编组轮换、排序、玩家编组覆盖与发射资格。
 */
class FireChannelTableTest {
    private static FireChannelTable.Reading reading(BlockPos pos, LauncherKind kind, TargetDomain domain,
            int count, int capacity) {
        return new FireChannelTable.Reading(pos, kind, "taovmodernnaval:p1_missile", count, capacity, true, domain);
    }

    @Test
    void defaultGroupFollowsAmmoDomain() {
        assertAll(
                () -> assertEquals(FireChannelGroup.ANTI_AIR_1, FireChannelTable.defaultGroup(TargetDomain.ANTI_AIR),
                        "防空弹默认编入防空群1"),
                () -> assertEquals(FireChannelGroup.ANTI_SHIP_1, FireChannelTable.defaultGroup(TargetDomain.ANTI_SHIP),
                        "反舰弹默认编入反舰群1"),
                () -> assertEquals(FireChannelGroup.ANTI_SHIP_1, FireChannelTable.defaultGroup(TargetDomain.UNIVERSAL),
                        "通用弹默认按反舰编组"),
                () -> assertEquals(FireChannelGroup.ANTI_SHIP_1, FireChannelTable.defaultGroup(null)));
    }

    @Test
    void cycleVisitsEveryGroupAndWrapsAround() {
        FireChannelGroup group = FireChannelGroup.ANTI_SHIP_1;
        assertEquals(FireChannelGroup.ANTI_SHIP_2, group.next());

        FireChannelGroup walked = group;
        for (int i = 0; i < FireChannelGroup.values().length; i++) {
            walked = walked.next();
        }
        assertEquals(group, walked, "轮换一整圈后应回到起点");
        assertEquals(FireChannelGroup.ANTI_SHIP_1, FireChannelTable.cycle(null), "空编组从反舰群1开始");
    }

    @Test
    void assembleAppliesPlayerAssignmentOverDefault() {
        BlockPos pos = new BlockPos(4, 64, 4);
        Map<Long, FireChannelGroup> assignments = new HashMap<>();
        assignments.put(pos.asLong(), FireChannelGroup.ANTI_AIR_2);

        List<FireChannel> channels = FireChannelTable.assemble(
                List.of(reading(pos, LauncherKind.RAK_LAUNCHER, TargetDomain.ANTI_SHIP, 2, 2)), assignments);

        assertEquals(1, channels.size());
        assertEquals(FireChannelGroup.ANTI_AIR_2, channels.get(0).group(), "玩家编组必须覆盖默认编组");
    }

    @Test
    void assembleOrdersByLauncherKindThenPosition() {
        BlockPos far = new BlockPos(9, 64, 0);
        BlockPos near = new BlockPos(1, 64, 0);
        List<FireChannel> channels = FireChannelTable.assemble(List.of(
                reading(far, LauncherKind.AK230, TargetDomain.ANTI_AIR, 0, 0),
                reading(near, LauncherKind.RAK_LAUNCHER, TargetDomain.ANTI_SHIP, 1, 2),
                reading(far, LauncherKind.RAK_LAUNCHER, TargetDomain.ANTI_SHIP, 1, 2)), null);

        assertEquals(3, channels.size());
        assertEquals(LauncherKind.RAK_LAUNCHER, channels.get(0).kind());
        assertEquals(LauncherKind.RAK_LAUNCHER, channels.get(1).kind());
        assertEquals(LauncherKind.AK230, channels.get(2).kind(), "导弹发射装置排在近防炮之前");
        assertEquals(near, channels.get(0).pos());
    }

    @Test
    void emptyOrNullReadingsYieldNoChannels() {
        assertTrue(FireChannelTable.assemble(null, null).isEmpty());
        assertTrue(FireChannelTable.assemble(List.of(), new HashMap<>()).isEmpty());
    }

    @Test
    void missileChannelNeedsAmmoToLaunch() {
        FireChannel empty = new FireChannel(BlockPos.ZERO, LauncherKind.RAK_LAUNCHER, "",
                0, 2, true, FireChannelGroup.ANTI_SHIP_1, TargetDomain.ANTI_SHIP);
        FireChannel loaded = new FireChannel(BlockPos.ZERO, LauncherKind.RAK_LAUNCHER, "taovmodernnaval:p1_missile",
                1, 2, true, FireChannelGroup.ANTI_SHIP_1, TargetDomain.ANTI_SHIP);

        assertAll(
                () -> assertFalse(empty.hasAmmo()),
                () -> assertFalse(empty.canLaunch(), "空发射架不得执行发射"),
                () -> assertTrue(loaded.canLaunch()));
    }

    @Test
    void gunChannelIsAlwaysLoadedButNotReadyWhenOffline() {
        FireChannel gun = new FireChannel(BlockPos.ZERO, LauncherKind.AK630, "", 0, 0, true,
                FireChannelGroup.ANTI_AIR_1, TargetDomain.ANTI_AIR);
        FireChannel offline = new FireChannel(BlockPos.ZERO, LauncherKind.AK630, "", 0, 0, false,
                FireChannelGroup.ANTI_AIR_1, TargetDomain.ANTI_AIR);

        assertAll(
                () -> assertTrue(gun.hasAmmo(), "近防炮视为持续有弹"),
                () -> assertTrue(gun.canLaunch()),
                () -> assertFalse(offline.canLaunch()));
    }

    @Test
    void channelEngagementRespectsTargetDomain() {
        FireChannel antiShip = new FireChannel(BlockPos.ZERO, LauncherKind.RAK_LAUNCHER, "x",
                1, 2, true, FireChannelGroup.ANTI_SHIP_1, TargetDomain.ANTI_SHIP);
        FireChannel antiAir = new FireChannel(BlockPos.ZERO, LauncherKind.LAUNCH_TUBE, "x",
                1, 1, true, FireChannelGroup.ANTI_AIR_1, TargetDomain.ANTI_AIR);

        assertAll(
                () -> assertTrue(antiShip.engages(TargetDomain.ANTI_SHIP)),
                () -> assertFalse(antiShip.engages(TargetDomain.ANTI_AIR), "反舰弹不得交战空中目标"),
                () -> assertTrue(antiAir.engages(TargetDomain.ANTI_AIR)),
                () -> assertFalse(antiAir.engages(TargetDomain.ANTI_SHIP), "防空弹不得交战水面目标"),
                () -> assertTrue(antiShip.engages(null), "目标域未知时不拒绝交战"));
    }
}
