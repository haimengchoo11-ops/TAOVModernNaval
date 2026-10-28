package TAOVMN.registry;

import TAOVMN.TAOVModernNaval;
import TAOVMN.client.render.ClientMissileProjectile;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.resources.ResourceLocation;
import net.shao.shaolib.projectile.ShaoEntityType;

import java.util.Map;

public final class ModEntities {
    public static final ShaoEntityType<ClientMissileProjectile> P1_SHAO = createShaoType("p1_missile");
    public static final ShaoEntityType<ClientMissileProjectile> P15_SHAO = createShaoType("p15_missile");
    public static final ShaoEntityType<ClientMissileProjectile> P30_SHAO = createShaoType("p30_missile");
    public static final ShaoEntityType<ClientMissileProjectile> P45_SHAO = createShaoType("p45_missile");
    public static final ShaoEntityType<ClientMissileProjectile> P100_SHAO = createShaoType("p100_missile");
    public static final ShaoEntityType<ClientMissileProjectile> P120_SHAO = createShaoType("p120_missile");
    public static final ShaoEntityType<ClientMissileProjectile> P270_SHAO = createShaoType("p270_missile");
    public static final ShaoEntityType<ClientMissileProjectile> P300_SHAO = createShaoType("p300_missile");
    public static final ShaoEntityType<ClientMissileProjectile> P500_SHAO = createShaoType("p500_missile");
    public static final ShaoEntityType<ClientMissileProjectile> P700_SHAO = createShaoType("p700_missile");
    public static final ShaoEntityType<ClientMissileProjectile> P800_SHAO = createShaoType("p800_missile");
    public static final ShaoEntityType<ClientMissileProjectile> S300_SHAO = createShaoType("s300_missile");
    public static final ShaoEntityType<ClientMissileProjectile> K33_SHAO = createShaoType("9k33_missile");
    public static final ShaoEntityType<ClientMissileProjectile> K60_SHAO = createShaoType("9k60_missile");
    public static final ShaoEntityType<ClientMissileProjectile> M38_SHAO = createShaoType("9m38_missile");

    private static final Map<String, ShaoEntityType<ClientMissileProjectile>> SHAO_TYPES = Map.ofEntries(
            Map.entry("p1_missile", P1_SHAO),
            Map.entry("p15_missile", P15_SHAO),
            Map.entry("p30_missile", P30_SHAO),
            Map.entry("p45_missile", P45_SHAO),
            Map.entry("p100_missile", P100_SHAO),
            Map.entry("p120_missile", P120_SHAO),
            Map.entry("p270_missile", P270_SHAO),
            Map.entry("p300_missile", P300_SHAO),
            Map.entry("p500_missile", P500_SHAO),
            Map.entry("p700_missile", P700_SHAO),
            Map.entry("p800_missile", P800_SHAO),
            Map.entry("s300_missile", S300_SHAO),
            Map.entry("9k33_missile", K33_SHAO),
            Map.entry("9k60_missile", K60_SHAO),
            Map.entry("9m38_missile", M38_SHAO));

    private static ShaoEntityType<ClientMissileProjectile> createShaoType(String itemPath) {
        return new ShaoEntityType.Builder<ClientMissileProjectile>()
                .namespace(TAOVModernNaval.MODID)
                .id(itemPath)
                .clientTypes(() -> (type, dimension) -> new ClientMissileProjectile(type, dimension, itemPath))
                .build();
    }

    public static ShaoEntityType<ClientMissileProjectile> getShaoType(ItemStack stack) {
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(stack.getItem());
        String itemPath = key != null && TAOVModernNaval.MODID.equals(key.getNamespace())
                ? key.getPath()
                : "p300_missile";
        return SHAO_TYPES.getOrDefault(itemPath, P300_SHAO);
    }

    private ModEntities() {
    }

    public static void initialize() {
    }

}