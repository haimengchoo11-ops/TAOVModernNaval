package TAOVMN;

import TAOVMN.core.ModBootstrap;
import TAOVMN.core.NavalCoreCreativeTab;
import TAOVMN.network.ModNetwork;
import TAOVMN.registry.ModBlockEntities;
import TAOVMN.registry.ModBlocks;
import TAOVMN.registry.ModCreativeTabs;
import TAOVMN.registry.ModEntities;
import TAOVMN.registry.ModItems;
import TAOVMN.registry.ModRecipes;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.bernie.geckolib.GeckoLib;

@Mod("taovmodernnaval")
public class TAOVModernNaval {
    private static final Logger LOGGER = LogManager.getLogger();
    public static final String MODID = "taovmodernnaval";

    /**
     * 是否启用「新版重写骨架层」（{@code TAOVMN.content} + {@code TAOVMN.core}）。
     *
     * <p>该层与旧版功能层是两套互不相干的实现：同样注册了雷达 / 发射管 / 导弹，但行为远比旧层粗糙
     * （只打最近生物、无 IFF、无目标域），却同样出现在创意栏里，玩家会看到两个「雷达」两个「发射管」。
     * 在两层合并完成之前，先切断它的注册入口，代码与资源全部保留。
     *
     * <p><b>恢复方式</b>：把这个常量改成 {@code true} 即可（{@link TAOVMN.client.ClientEvents}
     * 里的实体渲染器注册由同一个常量控制，不要只改一边，否则会为未注册的实体类型注册渲染器而崩溃）。
     *
     * <p>该层专属的 4 个配方与 4 个掉落表（{@code naval_core_block} / {@code naval_launch_tube} /
     * {@code naval_turret} / {@code radar}）已随该层一起删除，因为它们引用的物品在停用后并不存在，
     * 会在数据包加载时刷错误日志。恢复该层时需要从 git 历史（commit {@code efa4271} 之前）取回。
     */
    public static final boolean REWRITE_LAYER_ENABLED = false;

    public TAOVModernNaval() {
        GeckoLib.initialize();

        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::setup);

        // 旧版功能层：P 系列导弹 / RAK 发射架 / 火控 / 近防炮
        ModBlocks.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModEntities.initialize();       // 触发 Shao 弹体类型的静态注册
        ModItems.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        ModRecipes.register(modEventBus);

        // 新版重写骨架层（暂不注册，见 REWRITE_LAYER_ENABLED）
        if (REWRITE_LAYER_ENABLED) {
            ModBootstrap.register(modEventBus);
            NavalCoreCreativeTab.register(modEventBus);
        }

        LOGGER.info("TAOVModernNaval bootstrap initialized (legacy layer only, rewrite layer enabled={}).",
                REWRITE_LAYER_ENABLED);
    }

    private void setup(final FMLCommonSetupEvent event) {
        // 网络通道必须在两端同时注册，且要在 common setup 的工作队列里完成
        event.enqueueWork(ModNetwork::register);
        LOGGER.info("TAOVModernNaval common setup completed.");
    }
}