package TAOVMN.item;

import TAOVMN.block.entity.RadarBlockEntity;
import TAOVMN.client.render.RakLauncherItemRenderer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.function.Consumer;

// 让物品（手持/背包/地面）用与方块相同的GeckoLib几何体渲染，避免与实际放置外观不一致
public class LauncherBlockItem extends BlockItem implements GeoItem {
    public static final String TAG_PREBOUND_RADAR_POS = "PreBoundRadarPos";

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    public LauncherBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private BlockEntityWithoutLevelRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (this.renderer == null) {
                    this.renderer = new RakLauncherItemRenderer();
                }
                return this.renderer;
            }
        });
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // 物品展示为静态姿态，无需额外动画控制器
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getPlayer() == null || !context.getPlayer().isShiftKeyDown()) {
            return super.useOn(context);
        }

        if (context.getLevel().isClientSide) {
            return InteractionResult.SUCCESS;
        }

        BlockPos clickedPos = context.getClickedPos();
        if (!(context.getLevel().getBlockEntity(clickedPos) instanceof RadarBlockEntity)) {
            return super.useOn(context);
        }

        ItemStack stack = context.getItemInHand();
        CompoundTag tag = stack.getOrCreateTag();
        tag.put(TAG_PREBOUND_RADAR_POS, NbtUtils.writeBlockPos(clickedPos));

        context.getPlayer().displayClientMessage(
                Component.translatable("message.taovmodernnaval.launcher_item_radar_bound", clickedPos.getX(), clickedPos.getY(), clickedPos.getZ()),
                true
        );
        return InteractionResult.CONSUME;
    }
}