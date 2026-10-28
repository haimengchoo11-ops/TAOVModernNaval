package TAOVMN.util;

import java.util.UUID;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

/**
 * 「归属」标记：一座装置属于哪个玩家。
 *
 * <p>这是敌我识别（IFF）的数据基础。规则刻意保守：
 * <ul>
 *   <li>只有"被玩家放置"的方块、以及"玩家本人发射"的弹体才会有 owner；</li>
 *   <li>{@code owner == null} 表示归属未知（{@code /setblock}、结构生成、其它模组放置、
 *       CBC 悬挂炮等），后续一律按"可交战"处理，也就是完全保持引入 IFF 之前的行为，
 *       不会让老存档或非本模组生成的装置突然免疫攻击。</li>
 * </ul>
 *
 * <p>实现者只需存一个 UUID 字段，NBT 读写与放置时赋值由本接口的静态方法统一处理。
 */
public interface OwnerTracked {
    /** NBT 键名。改名会破坏存档中的归属信息，勿动。 */
    String TAG_OWNER = "Owner";

    @Nullable
    UUID getOwnerId();

    void setOwnerId(@Nullable UUID ownerId);

    /** 把 owner 写进 NBT；为 null 时不写（读出来仍是 null）。 */
    static void write(CompoundTag tag, @Nullable UUID ownerId) {
        if (ownerId != null) {
            tag.putUUID(TAG_OWNER, ownerId);
        }
    }

    /** 从 NBT 读 owner；缺失或损坏时返回 null。 */
    @Nullable
    static UUID read(CompoundTag tag) {
        return tag != null && tag.hasUUID(TAG_OWNER) ? tag.getUUID(TAG_OWNER) : null;
    }

    /**
     * 方块被放置时把放置者记为该装置的 owner。
     *
     * <p>非玩家放置（发射器、结构方块、{@code /setblock}）时 {@code placer} 不是 {@link Player}，
     * 此时保持 owner 为 null（= 归属未知）。
     */
    static void assign(@Nullable BlockEntity blockEntity, @Nullable LivingEntity placer) {
        if (blockEntity instanceof OwnerTracked tracked && placer instanceof Player player) {
            tracked.setOwnerId(player.getUUID());
        }
    }
}
