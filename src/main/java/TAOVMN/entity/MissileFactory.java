package TAOVMN.entity;

import TAOVMN.registry.ModItems;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/** Creates the registered missile entity that matches a loaded missile item. */
public final class MissileFactory {
    private MissileFactory() {
    }

    public static ShaoMissileProjectile create(ItemStack stack, ServerLevel level, Vec3 position) {
        if (!ModItems.isMissile(stack)) {
            return null;
        }
        return new ShaoMissileProjectile(level, stack, position);
    }
}