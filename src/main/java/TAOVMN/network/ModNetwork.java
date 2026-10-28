package TAOVMN.network;

import TAOVMN.TAOVModernNaval;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

/** 总控台界面与服务端之间的网络通道。 */
public final class ModNetwork {
    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(TAOVModernNaval.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals);

    private ModNetwork() {
    }

    /** 在 common setup 阶段注册消息。 */
    public static void register() {
        int id = 0;
        CHANNEL.messageBuilder(ServerboundFireChannelPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ServerboundFireChannelPacket::encode)
                .decoder(ServerboundFireChannelPacket::new)
                .consumerMainThread(ServerboundFireChannelPacket::handle)
                .add();
        CHANNEL.messageBuilder(S2CThreatAlertPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(S2CThreatAlertPacket::encode)
                .decoder(S2CThreatAlertPacket::new)
                .consumerMainThread(S2CThreatAlertPacket::handle)
                .add();
    }
}
