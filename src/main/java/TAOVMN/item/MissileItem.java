package TAOVMN.item;

import TAOVMN.entity.P1MissileEntity;
import TAOVMN.util.MissileProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class MissileItem extends Item {
    private final MissileProfile defaultProfile;

    public MissileItem(Properties properties, MissileProfile defaultProfile) {
        super(properties);
        this.defaultProfile = defaultProfile;
    }

    public MissileProfile getDefaultProfile() {
        return defaultProfile;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        ItemStack stack = context.getItemInHand();
        Direction face = context.getClickedFace();
        BlockPos spawnPos = context.getClickedPos().relative(face);
        Vec3 dir = Vec3.atLowerCornerOf(face.getNormal()).normalize();

        P1MissileEntity missile = new P1MissileEntity(level,
                spawnPos.getX() + 0.5D,
                spawnPos.getY() + 0.3D,
                spawnPos.getZ() + 0.5D);
        missile.setFuzeTicks(200);
        missile.applyProfile(MissileProfile.resolve(stack));
        missile.setLauncherOrigin(Vec3.atCenterOf(context.getClickedPos()));
        missile.setDeltaMovement(dir.scale(0.9D).add(0.0D, 0.15D, 0.0D));
        missile.setItem(stack.copyWithCount(1));
        level.addFreshEntity(missile);

        level.playSound(null, context.getClickedPos(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.8F, 1.15F);

        if (context.getPlayer() == null || !context.getPlayer().getAbilities().instabuild) {
            stack.shrink(1);
        }

        return InteractionResult.CONSUME;
    }
}