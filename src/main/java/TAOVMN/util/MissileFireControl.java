package TAOVMN.util;

import java.util.UUID;

import TAOVMN.block.entity.RadarBlockEntity;
import TAOVMN.entity.MissileFactory;
import TAOVMN.entity.ShaoMissileProjectile;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * 火控发射公共逻辑：把装填好的导弹按火控目标指派后出膛。
 * 供 RAK 发射架、发射管等复用，保证行为一致。
 */
public final class MissileFireControl {
    private static final String TAG_FUZE_INSTALLED = "FuzeInstalled";

    private MissileFireControl() {
    }

    /** 导弹物品是否已装引信（引信决定碰撞/近炸起爆）。 */
    public static boolean isFuzeInstalled(ItemStack stack) {
        return stack.hasTag() && stack.getOrCreateTag().getBoolean(TAG_FUZE_INSTALLED);
    }

    /**
     * 在 origin 附近查找最近的雷达 / 火控指挥仪（水平 radius 格、垂直 radius/2 格）。
     *
     * <p>实现已改为按已加载区块枚举（见 {@link BlockEntityIndex}）：原先逐坐标调用
     * {@code level.getBlockEntity} 在 r=32 时要做 13.9 万次查找，且对未加载区块会触发同步加载。
     */
    public static RadarBlockEntity findNearbyRadar(ServerLevel level, BlockPos origin, int radius) {
        int verticalRadius = Math.max(1, radius / 2);
        return BlockEntityIndex.nearestRadar(level, origin, radius, verticalRadius).orElse(null);
    }

    /**
     * 发射一发导弹。
     *
     * @param missileStack  已装填的导弹物品
     * @param muzzle        出膛世界坐标
     * @param launchDirection 出膛方向（会被归一化）
     * @param targetUuid    火控锁定的目标实体 UUID（可为 null）
     * @param targetPos     火控锁定的目标坐标（可为 null）
     * @param ownerId       发射方的归属（放置该发射架的玩家）；为 null 时弹体不受 IFF 保护，
     *                      行为与引入归属概念之前完全一致
     */
    public static ShaoMissileProjectile launch(ServerLevel level, ItemStack missileStack, Vec3 muzzle,
            Vec3 launchDirection, int fuzeTicks, UUID targetUuid, Vec3 targetPos, Vec3 launcherOrigin,
            @Nullable UUID ownerId) {
        if (missileStack.isEmpty()) {
            return null;
        }

        MissileProfile profile = MissileProfile.resolve(missileStack);
        ShaoMissileProjectile missile = MissileFactory.create(missileStack, level, muzzle);
        if (missile == null) {
            return null;
        }

        missile.setFuzeTicks(fuzeTicks);
        missile.applyProfile(profile);
        missile.setGuidanceTuning(
                MissileProfile.resolveLockHoldTicks(missileStack, ShaoMissileProjectile.defaultLockHoldTicks()),
                MissileProfile.resolveSwitchAdvantageDistance(missileStack, ShaoMissileProjectile.defaultSwitchAdvantageDistance()),
                MissileProfile.resolveForceSwitchDistance(missileStack, ShaoMissileProjectile.defaultForceSwitchDistance()));
        if (targetUuid != null) {
            missile.setAssignedTarget(targetUuid);
        }
        if (targetPos != null) {
            missile.setDesignatedTargetPos(targetPos);
        }
        missile.setLauncherOrigin(launcherOrigin);
        missile.setOwnerId(ownerId);

        Vec3 direction = launchDirection.lengthSqr() > 1.0E-6D
                ? launchDirection.normalize()
                : new Vec3(0.0D, 1.0D, 0.0D);
        double launchSpeed = Math.max(0.6D, profile.maxSpeed() * 0.5D);
        missile.setVelocity(direction.scale(launchSpeed).add(0.0D, 0.15D, 0.0D));
        missile.setFuzeInstalled(isFuzeInstalled(missileStack));
        missile.spawn();
        return missile;
    }
}
