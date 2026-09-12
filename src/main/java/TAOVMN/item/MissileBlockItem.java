package TAOVMN.item;

import java.util.List;
import java.util.function.Consumer;

import TAOVMN.client.render.MissileItemRenderer;
import TAOVMN.util.MissileProfile;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

public class MissileBlockItem extends BlockItem implements GeoItem {
    private final MissileProfile defaultProfile;
        private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    public MissileBlockItem(Block block, Properties properties, MissileProfile defaultProfile) {
        super(block, properties);
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
                                if (this.renderer == null) {
                                        this.renderer = new MissileItemRenderer();
                                }
                                return this.renderer;
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

    /** 写入默认 NBT，若物品已有自定义数据则不覆盖 */
    public static ItemStack withDefaultNbt(ItemStack stack, MissileProfile profile) {
        MissileProfile.ensureProfileTag(stack, profile);
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);

        MissileProfile p = MissileProfile.resolve(stack);

        // 引信安装状态
        boolean fuzeInstalled = stack.hasTag() && stack.getOrCreateTag().getBoolean("FuzeInstalled");
        if (fuzeInstalled) {
            tooltip.add(Component.literal("✔ ")
                    .withStyle(ChatFormatting.GREEN)
                    .append(Component.translatable("tooltip.taovmodernnaval.fuze_installed")
                            .withStyle(ChatFormatting.GREEN)));
        } else {
            tooltip.add(Component.literal("⚠ ")
                    .withStyle(ChatFormatting.YELLOW)
                    .append(Component.translatable("tooltip.taovmodernnaval.fuze_not_installed")
                            .withStyle(ChatFormatting.YELLOW)));
        }

        tooltip.add(Component.literal("──────────────").withStyle(ChatFormatting.DARK_GRAY));

        tooltip.add(Component.literal("⚡ ")
                .withStyle(ChatFormatting.YELLOW)
                .append(Component.translatable("tooltip.taovmodernnaval.max_speed")
                        .withStyle(ChatFormatting.GOLD))
                .append(Component.literal(String.format("  %.2f blk/t", p.maxSpeed()))
                        .withStyle(ChatFormatting.WHITE)));

        tooltip.add(Component.literal("↗ ")
                .withStyle(ChatFormatting.AQUA)
                .append(Component.translatable("tooltip.taovmodernnaval.steering")
                        .withStyle(ChatFormatting.AQUA))
                .append(Component.literal(String.format("  %.2f", p.steering()))
                        .withStyle(ChatFormatting.WHITE)));

        tooltip.add(Component.literal("💥 ")
                .withStyle(ChatFormatting.RED)
                .append(Component.translatable("tooltip.taovmodernnaval.blast_power")
                        .withStyle(ChatFormatting.RED))
                .append(Component.literal(String.format("  %.1f", p.blastPower()))
                        .withStyle(ChatFormatting.WHITE)));

        tooltip.add(Component.literal("📡 ")
                .withStyle(ChatFormatting.GREEN)
                .append(Component.translatable("tooltip.taovmodernnaval.seek_range")
                        .withStyle(ChatFormatting.GREEN))
                .append(Component.literal(String.format("  %.0f blk", p.seekRange()))
                        .withStyle(ChatFormatting.WHITE)));

        tooltip.add(Component.literal("◉ ")
                .withStyle(ChatFormatting.LIGHT_PURPLE)
                .append(Component.translatable("tooltip.taovmodernnaval.proximity_fuse")
                        .withStyle(ChatFormatting.LIGHT_PURPLE))
                .append(Component.literal(String.format("  %.1f blk", p.proximityFuse()))
                        .withStyle(ChatFormatting.WHITE)));

        // 若物品有自定义 NBT，显示提示
        if (MissileProfile.hasProfileTag(stack)) {
            tooltip.add(Component.literal("★ ")
                    .withStyle(ChatFormatting.GOLD)
                    .append(Component.translatable("tooltip.taovmodernnaval.custom_profile")
                            .withStyle(ChatFormatting.YELLOW)));
        }
    }
}
