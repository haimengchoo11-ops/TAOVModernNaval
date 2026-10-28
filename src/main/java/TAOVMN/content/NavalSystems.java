package TAOVMN.content;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class NavalSystems {
    private NavalSystems() {
    }

    public static List<LivingEntity> findTargets(Level level, BlockPos origin, double radius) {
        if (level == null || level.isClientSide) {
            return List.of();
        }

        Vec3 center = Vec3.atCenterOf(origin);
        AABB area = new AABB(center.x() - radius, center.y() - radius, center.z() - radius,
                center.x() + radius, center.y() + radius, center.z() + radius);

        List<LivingEntity> targets = new ArrayList<>();
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, area)) {
            double distance = entity.position().distanceToSqr(center);
            if (distance <= radius * radius) {
                targets.add(entity);
            }
        }

        targets.sort(Comparator.comparingDouble(entity -> entity.position().distanceToSqr(center)));
        return targets;
    }
}
