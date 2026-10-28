package TAOVMN.util;

import java.util.UUID;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/**
 * 敌我识别（IFF）判定规则。
 *
 * <p>本模组此前完全没有"归属"概念，导致三处严重误伤：
 * <ul>
 *   <li>导弹的自主寻的只排除"距发射点 N 格内的实体"，出膛一秒后就会去追自己的船；</li>
 *   <li>防空弹的近炸对<b>任意</b>在飞弹体生效，同舰齐射的两发会互相引爆；</li>
 *   <li>近防炮拦截时直接 {@code discard()} 掉己方在飞导弹。</li>
 * </ul>
 *
 * <p>这里给出最小闭环所需的两个判定：
 * <ol>
 *   <li>{@link #isSameOwner(UUID, UUID)} —— 纯函数，用于"两枚弹 / 炮与弹"之间的归属比较；</li>
 *   <li>{@link #canEngage(UUID, Entity)} —— 用于"武器能否对这个实体开火"。</li>
 * </ol>
 *
 * <p><b>当前版本的能力边界（刻意为之，不是遗漏）</b>：
 * <ul>
 *   <li>只认「同一玩家 UUID」与「{@link OwnableEntity} 的归属」（驯服生物、船等）；</li>
 *   <li><b>不</b>支持计分板队伍。同队玩家互相之间仍会误伤。队伍支持需要维护
 *       {@code owner UUID → team} 的映射（需要 ServerLevel 查询），留作后续；
 *       接入时应只改本类，不要在每个判定点各写一份逻辑。</li>
 *   <li>任一方 owner 为 null 时一律视为"可交战"（fail-open），保证非本模组放置的
 *       装置与旧存档行为不变。</li>
 * </ul>
 */
public final class IffRules {

    private IffRules() {
    }

    /**
     * 两个归属标记是否指向同一个玩家。
     *
     * <p>任一为 {@code null} 都返回 {@code false}：归属未知时不做保护，
     * 否则 CBC 悬挂炮、结构生成装置等"无主"发射源会互相免疫，反导和拦截会整体失效。
     */
    public static boolean isSameOwner(@Nullable UUID a, @Nullable UUID b) {
        return a != null && b != null && a.equals(b);
    }

    /**
     * 这个实体是否属于「交战对象」，即战舰火控应当把它当目标看待。
     *
     * <p>这是本模组此前缺失的一环：雷达与近防炮原先硬编码 {@code entity instanceof Enemy}，
     * 而 {@link Enemy} 只覆盖原版敌对生物——<b>玩家永远不在其中</b>。结果是这套为舰队对抗设计的
     * 武器系统在 PvP 场景下"看不见敌舰上的人"，雷达显示 0 个目标。
     *
     * <p>现在定义两类交战对象：
     * <ul>
     *   <li>{@link Enemy}：原版敌对生物（僵尸、骷髅等），保留 PvE 能力；</li>
     *   <li>{@link Player}：玩家。是否真的可以开火由 {@link #canEngage(UUID, Entity)} 进一步按归属过滤，
     *       因此友军玩家仍不会被锁定。</li>
     * </ul>
     *
     * <p>中立生物（牛、羊等）不算交战对象——否则导弹会去追一头牛。
     */
    public static boolean isHostile(@Nullable Entity target) {
        if (target == null || !target.isAlive() || target.isSpectator()) {
            return false;
        }
        return target instanceof Enemy || target instanceof Player;
    }

    /**
     * 归属为 {@code ownerId} 的武器能否对 {@code target} 开火。
     *
     * @return {@code false} 仅当能确定目标是友方（同一玩家 / 该玩家拥有的实体）；
     *         归属未知、目标为 null 之外的其它情况一律 {@code true}
     */
    public static boolean canEngage(@Nullable UUID ownerId, @Nullable Entity target) {
        if (target == null) {
            return false;
        }
        if (ownerId == null) {
            // 归属未知 → 不施加任何保护，保持引入 IFF 之前的行为
            return true;
        }
        if (target instanceof Player player && ownerId.equals(player.getUUID())) {
            return false;
        }
        if (target instanceof OwnableEntity ownable && ownerId.equals(ownable.getOwnerUUID())) {
            return false;
        }
        return true;
    }
}
