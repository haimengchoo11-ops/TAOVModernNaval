package TAOVMN.compat.jei;

import TAOVMN.TAOVModernNaval;
import TAOVMN.recipe.FuzeInstallRecipe;
import TAOVMN.registry.ModItems;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

@JeiPlugin
public class TaovJeiPlugin implements IModPlugin {
    private static final ResourceLocation PLUGIN_ID = new ResourceLocation(TAOVModernNaval.MODID, "jei_plugin");

    @Override
    public @NotNull ResourceLocation getPluginUid() {
        return PLUGIN_ID;
    }

    @Override
    public void registerCategories(@NotNull IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new FuzeInstallRecipeCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipeCatalysts(@NotNull IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(Objects.requireNonNull(ModItems.RAK_LAUNCHER_ITEM.get())), FuzeInstallRecipeCategory.TYPE);
    }

    @Override
    public void registerRecipes(@NotNull IRecipeRegistration registration) {
        registration.addRecipes(FuzeInstallRecipeCategory.TYPE,
                List.of(new FuzeInstallRecipe(new ResourceLocation(TAOVModernNaval.MODID, "fuze_install"), CraftingBookCategory.MISC)));

        registration.addIngredientInfo(new ItemStack(Objects.requireNonNull(ModItems.RAK_LAUNCHER_ITEM.get())),
                VanillaTypes.ITEM_STACK,
                Component.translatable("jei.taovmodernnaval.launcher.info"));

        List<ItemStack> missiles = ModItems.MISSILE_TYPES.stream()
                .map(registryObject -> new ItemStack(Objects.requireNonNull(registryObject.get())))
                .toList();

        registration.addIngredientInfo(missiles,
                VanillaTypes.ITEM_STACK,
                Component.translatable("jei.taovmodernnaval.missiles.info"));
    }
}