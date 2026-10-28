package TAOVMN.util;

import java.util.UUID;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.OwnableEntity;
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
