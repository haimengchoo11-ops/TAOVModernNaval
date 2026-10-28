package TAOVMN.item;

import TAOVMN.entity.MissileFactory;
import TAOVMN.entity.ShaoMissileProjectile;
import TAOVMN.client.render.MissileItemRenderer;
import TAOVMN.util.MissileProfile;
import net.minecraft.core.Direction;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.function.Consumer;

public class MissileItem extends Item implements GeoItem {
    private final MissileProfile defaultProfile;
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    public MissileItem(Properties properties, MissileProfile defaultProfile) {
        super(properties);
        this.defaultProfile = defaultProfile;
    }

    public MissileProfile getDefaultProfile() {
        return defaultProfile;
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private BlockEntityWithoutLevelRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    renderer = new MissileItemRenderer();
                }
                return renderer;
            }
        });
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        ItemStack stack = context.getItemInHand();
        Direction face = context.getClickedFace();
        Vec3 dir = Vec3.atLowerCornerOf(face.getNormal()).normalize();

        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.FAIL;
        }

        var spawnPos = context.getClickedPos().relative(face);
        Vec3 spawnPosition = new Vec3(spawnPos.getX() + 0.5D, spawnPos.getY() + 0.3D, spawnPos.getZ() + 0.5D);
        ShaoMissileProjectile missile = MissileFactory.create(stack, serverLevel, spawnPosition);
        if (missile == null) {
            return InteractionResult.FAIL;
        }
        missile.setFuzeTicks(200);
        missile.applyProfile(MissileProfile.resolve(stack));
        boolean fuzeInstalled = stack.hasTag() && stack.getOrCreateTag().getBoolean("FuzeInstalled");
        missile.setFuzeInstalled(fuzeInstalled);
        missile.setLauncherOrigin(Vec3.atCenterOf(context.getClickedPos()));
        // 玩家徒手投掷的导弹归属该玩家，避免刚出手就回头追自己
        missile.setOwnerId(context.getPlayer() == null ? null : context.getPlayer().getUUID());
        missile.setVelocity(dir.scale(0.9D).add(0.0D, 0.15D, 0.0D));
        missile.spawn();

        level.playSound(null, context.getClickedPos(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.8F, 1.15F);

        if (context.getPlayer() == null || !context.getPlayer().getAbilities().instabuild) {
            stack.shrink(1);
        }

        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        MissileProfile profile = MissileProfile.resolve(stack);
        boolean fuzeInstalled = stack.hasTag() && stack.getOrCreateTag().getBoolean("FuzeInstalled");
        tooltip.add(Component.translatable(fuzeInstalled
                        ? "tooltip.taovmodernnaval.fuze_installed"
                        : "tooltip.taovmodernnaval.fuze_not_installed")
                .withStyle(fuzeInstalled ? ChatFormatting.GREEN : ChatFormatting.YELLOW));
        tooltip.add(Component.translatable("tooltip.taovmodernnaval.max_speed")
            .append(Component.literal(String.format("  %.2f blk/t", profile.maxSpeed()))));
        tooltip.add(Component.translatable("tooltip.taovmodernnaval.steering")
            .append(Component.literal(String.format("  %.2f", profile.steering()))));
        tooltip.add(Component.translatable("tooltip.taovmodernnaval.blast_power")
            .append(Component.literal(String.format("  %.1f", profile.blastPower()))));
        tooltip.add(Component.translatable("tooltip.taovmodernnaval.seek_range")
            .append(Component.literal(String.format("  %.0f blk", profile.seekRange()))));
        tooltip.add(Component.translatable("tooltip.taovmodernnaval.proximity_fuse")
            .append(Component.literal(String.format("  %.1f blk", profile.proximityFuse()))));
        tooltip.add(Component.translatable("tooltip.taovmodernnaval.target_domain")
                .append(Component.literal("  "))
                .append(Component.translatable(profile.domain().translationKey())));
    }
}