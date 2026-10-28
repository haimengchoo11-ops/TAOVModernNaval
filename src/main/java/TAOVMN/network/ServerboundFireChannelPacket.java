package TAOVMN.network;

import java.util.function.Supplier;

import TAOVMN.block.entity.TotalControlBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

/**
 * 客户端 → 服务端：对某条火力通道下达指令。
 *
 * <p>发送的是发射装置的方块坐标而不是列表下标，避免两端扫描不同步时打错目标。
 *
 * <p>服务端在本包内做三道前置校验（权威校验仍由
 * {@link TotalControlBlockEntity#handleOrder} 完成）：
 * <ol>
 *   <li>发送者必须存活且非旁观；</li>
 *   <li>发送者到总控台的距离不得超过 {@link TotalControlBlockEntity#MAX_ORDER_DISTANCE}，
 *       否则界面开着也能从任意位置伪造发射指令；</li>
 *   <li>距离不合格时连方块实体都不查，省掉一次无意义的查找。</li>
 * </ol>
 *
 * @param controllerPos 总控台坐标
 * @param channelPos    火力通道（发射装置）坐标
 * @param action        {@link TotalControlBlockEntity#ACTION_FIRE} 或
 *                      {@link TotalControlBlockEntity#ACTION_CYCLE_GROUP}
 */
public record ServerboundFireChannelPacket(BlockPos controllerPos, BlockPos channelPos, int action) {

    private static final double MAX_ORDER_DISTANCE_SQR =
            TotalControlBlockEntity.MAX_ORDER_DISTANCE * TotalControlBlockEntity.MAX_ORDER_DISTANCE;

    public ServerboundFireChannelPacket(FriendlyByteBuf buffer) {
        this(buffer.readBlockPos(), buffer.readBlockPos(), buffer.readVarInt());
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeBlockPos(controllerPos);
        buffer.writeBlockPos(channelPos);
        buffer.writeVarInt(action);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || player.isSpectator() || !player.isAlive()) {
                return;
            }
            if (controllerPos == null || channelPos == null) {
                return;
            }
            if (player.distanceToSqr(Vec3.atCenterOf(controllerPos)) > MAX_ORDER_DISTANCE_SQR) {
                return;
            }
            if (player.level().getBlockEntity(controllerPos) instanceof TotalControlBlockEntity controller) {
                controller.handleOrder(player, channelPos, action);
            }
        });
        context.setPacketHandled(true);
    }
}
