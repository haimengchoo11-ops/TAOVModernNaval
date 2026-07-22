package TAOVMN.registry;

import TAOVMN.TAOVModernNaval;
import TAOVMN.entity.P1MissileEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, TAOVModernNaval.MODID);

    public static final RegistryObject<EntityType<P1MissileEntity>> P1_MISSILE = ENTITY_TYPES.register("p1_missile",
            () -> EntityType.Builder.<P1MissileEntity>of(P1MissileEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(8)
                    .updateInterval(1)
                    .build("p1_missile"));

    private ModEntities() {
    }

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}