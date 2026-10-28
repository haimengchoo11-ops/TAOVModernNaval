package TAOVMN.content;

import TAOVMN.TAOVModernNaval;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class NavalEntityTypes {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, TAOVModernNaval.MODID);

    public static final RegistryObject<EntityType<NavalMissileEntity>> NAVAL_MISSILE = ENTITY_TYPES.register(
            "naval_missile",
            () -> EntityType.Builder.<NavalMissileEntity>of(NavalMissileEntity::new, MobCategory.MISC)
                    .sized(0.4F, 0.4F)
                    .clientTrackingRange(32)
                    .updateInterval(1)
                    .build("taovmodernnaval:naval_missile"));

    private NavalEntityTypes() {
    }

    public static void register(IEventBus modEventBus) {
        ENTITY_TYPES.register(modEventBus);
    }
}
