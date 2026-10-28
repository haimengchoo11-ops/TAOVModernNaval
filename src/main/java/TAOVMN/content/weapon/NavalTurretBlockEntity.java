package TAOVMN.content.weapon;

import TAOVMN.content.NavalMissileEntity;
import TAOVMN.content.NavalSystems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class NavalTurretBlockEntity extends BlockEntity {
    private static final double RANGE = 24.0D;
    private int fireCooldown;

    public NavalTurretBlockEntity(BlockPos pos, BlockState blockState) {
        super(TAOVMN.content.NavalBlockEntities.NAVAL_TURRET.get(), pos, blockState);
    }

    public void tickServer() {
        if (fireCooldown > 0) {
            fireCooldown--;
        }

        Level level = this.level;
        if (level == null) {
            return;
        }

        if (fireCooldown == 0) {
            List<LivingEntity> targets = NavalSystems.findTargets(level, this.getBlockPos(), RANGE);
            if (!targets.isEmpty()) {
                fireCooldown = 20;
                fireAt(targets.get(0));
            }
        }
    }

    private void fireAt(LivingEntity target) {
        if (this.level == null || this.level.isClientSide) {
            return;
        }

        Vec3 origin = Vec3.atCenterOf(this.getBlockPos()).add(0.0D, 1.2D, 0.0D);
        Vec3 direction = target.position().subtract(origin).normalize();

        NavalMissileEntity missile = new NavalMissileEntity(this.level, origin.x, origin.y, origin.z);
        missile.launch(direction);
        this.level.addFreshEntity(missile);
    }
}
