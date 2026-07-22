package TAOVMN.item;

import TAOVMN.block.entity.RadarBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;

public class LauncherBlockItem extends BlockItem {
    public static final String TAG_PREBOUND_RADAR_POS = "PreBoundRadarPos";

    public LauncherBlockItem(Block block, Properties properties) {
        super(block, properties);
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