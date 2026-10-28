package TAOVMN.network;

import java.util.function.Supplier;

import TAOVMN.client.ClientThreatAlert;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * 服务端 → 客户端：来袭导弹告警。
 *
 * <p>弹体本身不是原版实体（由 Shao 库维护，且 {@code ACTIVE} 集合只在服务端维护），
 * 客户端没有可靠途径自己发现"有导弹朝我飞来"。因此由雷达在扫描到
 * {@code INCOMING_MISSILE} 接触点时主动推送一条告警，客户端只负责显示与自动过期。
 *
 * @param missileCount 当前朝本舰接近的在飞弹体数量
 * @param ttlTicks     告警残留时长（tick）；雷达每 20 tick 扫一次，取值略大于扫描间隔，
 *                      这样威胁消失后告警会自然淡出，而不是闪一下就没了
 */
public record S2CThreatAlertPacket(int missileCount, int ttlTicks) {

    public S2CThreatAlertPacket(FriendlyByteBuf buffer) {
        this(buffer.readVarInt(), buffer.readVarInt());
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeVarInt(missileCount);
        buffer.writeVarInt(ttlTicks);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        // ClientThreatAlert 只在物理客户端存在，用 DistExecutor 隔离，避免专用服务端解析到客户端类
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientThreatAlert.receive(missileCount, ttlTicks)));
        context.setPacketHandled(true);
    }
}
