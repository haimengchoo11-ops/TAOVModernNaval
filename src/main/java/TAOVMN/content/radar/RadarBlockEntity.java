package TAOVMN.content.radar;

import TAOVMN.content.NavalSystems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class RadarBlockEntity extends BlockEntity {
    private static final double RANGE = 32.0D;
    private int scanCooldown;

    public RadarBlockEntity(BlockPos pos, BlockState state) {
        super(TAOVMN.content.NavalBlockEntities.RADAR.get(), pos, state);
    }

    public void tickServer() {
        if (scanCooldown > 0) {
            scanCooldown--;
            return;
        }

        Level level = this.level;
        if (level == null) {
            return;
        }

        List<LivingEntity> targets = NavalSystems.findTargets(level, this.getBlockPos(), RANGE);
        if (!targets.isEmpty()) {
            this.scanCooldown = 20;
        }
    }

    public List<LivingEntity> scan() {
        if (this.level == null) {
            return List.of();
        }
        return NavalSystems.findTargets(this.level, this.getBlockPos(), RANGE);
    }
}
