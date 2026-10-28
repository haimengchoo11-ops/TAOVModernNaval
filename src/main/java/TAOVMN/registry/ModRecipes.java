package TAOVMN.registry;

import TAOVMN.TAOVModernNaval;
import TAOVMN.recipe.FuzeInstallRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, TAOVModernNaval.MODID);

    public static final RegistryObject<RecipeSerializer<FuzeInstallRecipe>> FUZE_INSTALL =
            RECIPE_SERIALIZERS.register("fuze_install", () -> FuzeInstallRecipe.SERIALIZER);

    private ModRecipes() {}

    public static void register(IEventBus eventBus) {
        RECIPE_SERIALIZERS.register(eventBus);
    }
}
