package TAOVMN.util;

import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

/**
 * 一条"火力通道"：舰上一座可与目标交战的发射装置，加上它当前的编组与备弹。
 *
 * <p>这是纯数据载体：服务端扫描装填状态后构造，序列化进方块实体同步包供总控台界面显示。
 * 不含任何 World 依赖，便于单元测试。
 *
 * @param pos           发射装置所在方块坐标
 * @param kind          发射装置类型
 * @param ammoItemId    弹药物品的注册名（如 {@code taovmodernnaval:p500_missile}）；无弹或非导弹装置为空串
 * @param ammoCount     已装填弹药数
 * @param ammoCapacity  载弹上限
 * @param ready         装置当前是否可立即开火（冷却/缺弹/未就绪时为 false）
 * @param group         火力通道编组
 * @param domain        本通道弹药可交战的目标域
 */
public record FireChannel(
        BlockPos pos,
        LauncherKind kind,
        String ammoItemId,
        int ammoCount,
        int ammoCapacity,
        boolean ready,
        FireChannelGroup group,
        TargetDomain domain
) {
    public FireChannel {
        ammoCount = Math.max(0, ammoCount);
        ammoCapacity = Math.max(ammoCount, ammoCapacity);
        ammoItemId = ammoItemId == null ? "" : ammoItemId;
        group = group == null ? FireChannelGroup.UNASSIGNED : group;
        domain = domain == null ? TargetDomain.UNIVERSAL : domain;
    }

    /** 是否有可发射的弹药。近防炮视为持续有弹。 */
    public boolean hasAmmo() {
        return !kind.isMissileLauncher() || ammoCount > 0;
    }

    /** 通道现在能否接受发射命令。 */
    public boolean canLaunch() {
        return ready && hasAmmo();
    }

    /** 本通道能否与 {@code contactDomain} 描述的目标交战。 */
    public boolean engages(TargetDomain contactDomain) {
        return domain.engages(contactDomain);
    }

    /** 是否是已装填的导弹发射装置（用于界面区分"发射"与"交战"）。 */
    public boolean isMissileChannel() {
        return kind.isMissileLauncher();
    }

    /** 与另一条通道是否指向同一座发射装置。 */
    public boolean sameLauncher(@Nullable BlockPos other) {
        return other != null && pos.equals(other);
    }
}
