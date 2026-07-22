package TAOVMN.compat.jei;

import TAOVMN.TAOVModernNaval;
import TAOVMN.registry.ModItems;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;

@JeiPlugin
public class TaovJeiPlugin implements IModPlugin {
    private static final ResourceLocation PLUGIN_ID = new ResourceLocation(TAOVModernNaval.MODID, "jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_ID;
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addIngredientInfo(new ItemStack(ModItems.RAK_LAUNCHER_ITEM.get()),
                mezz.jei.api.constants.VanillaTypes.ITEM_STACK,
                Component.translatable("jei.taovmodernnaval.launcher.info"));

        List<ItemStack> missiles = ModItems.MISSILE_TYPES.stream()
                .map(registryObject -> new ItemStack(registryObject.get()))
                .toList();

        registration.addIngredientInfo(missiles,
                mezz.jei.api.constants.VanillaTypes.ITEM_STACK,
                Component.translatable("jei.taovmodernnaval.missiles.info"));
    }
}