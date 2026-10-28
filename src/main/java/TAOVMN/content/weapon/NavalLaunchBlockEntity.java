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

public class NavalLaunchBlockEntity extends BlockEntity {
    private static final double RANGE = 32.0D;
    private static final int COOLDOWN_TICKS = 40;

    private int cooldown;

    public NavalLaunchBlockEntity(BlockPos pos, BlockState state) {
        super(TAOVMN.content.NavalBlockEntities.NAVAL_LAUNCH.get(), pos, state);
    }

    public void tickServer() {
        if (cooldown > 0) {
            cooldown--;
        }
    }

    /** 冷却结束，可接受总控台的发射命令。 */
    public boolean isReady() {
        return cooldown <= 0;
    }

    /**
     * 向范围内最近目标发射一枚导弹。
     *
     * @return 是否成功发射（有目标且冷却完毕）
     */
    public boolean tryLaunch() {
        if (cooldown > 0 || this.level == null || this.level.isClientSide) {
            return false;
        }

        List<LivingEntity> targets = NavalSystems.findTargets(this.level, this.getBlockPos(), RANGE);
        if (targets.isEmpty()) {
            return false;
        }

        Vec3 origin = Vec3.atCenterOf(this.getBlockPos()).add(0.0D, 1.0D, 0.0D);
        LivingEntity target = targets.get(0);
        Vec3 direction = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D)
                .subtract(origin).normalize();

        NavalMissileEntity missile = new NavalMissileEntity(this.level, origin.x, origin.y, origin.z);
        missile.launch(direction);
        this.level.addFreshEntity(missile);

        cooldown = COOLDOWN_TICKS;
        this.setChanged();
        return true;
    }
}