package TAOVMN.compat.jei;

import TAOVMN.TAOVModernNaval;
import TAOVMN.recipe.FuzeInstallRecipe;
import TAOVMN.registry.ModItems;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class FuzeInstallRecipeCategory implements IRecipeCategory<FuzeInstallRecipe> {
    public static final RecipeType<FuzeInstallRecipe> TYPE =
            RecipeType.create(TAOVModernNaval.MODID, "fuze_install", FuzeInstallRecipe.class);

    private final IDrawable background;
    private final IDrawable icon;
    private final Component title;

    public FuzeInstallRecipeCategory(@NotNull IGuiHelper guiHelper) {
        this.background = guiHelper.createBlankDrawable(160, 70);
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(Objects.requireNonNull(ModItems.MISSILE_FUZE.get())));
        this.title = Component.translatable("jei.taovmodernnaval.fuze_install");
    }

    @Override
    public @NotNull RecipeType<FuzeInstallRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public @NotNull Component getTitle() {
        return title;
    }

    @Override
    public @NotNull IDrawable getBackground() {
        return background;
    }

    @Override
    public @NotNull IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(@NotNull IRecipeLayoutBuilder builder, @NotNull FuzeInstallRecipe recipe, @NotNull IFocusGroup focuses) {
        Ingredient missileIngredient = Ingredient.of(
                ModItems.MISSILE_TYPES.stream()
                        .map(registryObject -> new ItemStack(Objects.requireNonNull(registryObject.get())))
                        .toArray(ItemStack[]::new)
        );
        Ingredient fuzeIngredient = Ingredient.of(Objects.requireNonNull(ModItems.MISSILE_FUZE.get()));

        builder.addSlot(RecipeIngredientRole.INPUT, 20, 16).addIngredients(missileIngredient);
        builder.addSlot(RecipeIngredientRole.INPUT, 20, 46).addIngredients(fuzeIngredient);
        builder.addSlot(RecipeIngredientRole.OUTPUT, 118, 31)
                .addItemStack(new ItemStack(Objects.requireNonNull(ModItems.P1_MISSILE.get())));
    }

    @Override
    public void draw(@NotNull FuzeInstallRecipe recipe, @NotNull IRecipeSlotsView recipeSlotsView, @NotNull GuiGraphics guiGraphics, double mouseX, double mouseY) {
        IRecipeCategory.super.draw(recipe, recipeSlotsView, guiGraphics, mouseX, mouseY);
    }
}
