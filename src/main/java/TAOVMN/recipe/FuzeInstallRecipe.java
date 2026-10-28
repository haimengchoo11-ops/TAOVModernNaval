package TAOVMN.recipe;

import TAOVMN.item.MissileItem;
import TAOVMN.item.MissileFuzeItem;
import com.google.gson.JsonObject;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * 引信安装配方：任意 MissileItem + MissileFuzeItem
 * → 同一枚导弹附加 NBT: {FuzeInstalled: 1b}
 */
public class FuzeInstallRecipe extends CustomRecipe {

    public static final RecipeSerializer<FuzeInstallRecipe> SERIALIZER = new RecipeSerializer<>() {
        @Override
        public FuzeInstallRecipe fromJson(ResourceLocation id, JsonObject json) {
            CraftingBookCategory category = CraftingBookCategory.CODEC.byName(
                    GsonHelper.getAsString(json, "category", null),
                    CraftingBookCategory.MISC);
            return new FuzeInstallRecipe(id, category);
        }

        @Override
        public FuzeInstallRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            CraftingBookCategory category = buf.readEnum(CraftingBookCategory.class);
            return new FuzeInstallRecipe(id, category);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, FuzeInstallRecipe recipe) {
            buf.writeEnum(recipe.category());
        }
    };

    public FuzeInstallRecipe(ResourceLocation id, CraftingBookCategory category) {
        super(id, category);
    }

    @Override
    public boolean matches(CraftingContainer container, Level level) {
        boolean hasMissile = false;
        boolean hasFuze = false;
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (stack.isEmpty()) continue;
            if (stack.getItem() instanceof MissileItem) {
                if (hasMissile) return false;
                hasMissile = true;
            } else if (stack.getItem() instanceof MissileFuzeItem) {
                if (hasFuze) return false;
                hasFuze = true;
            } else {
                return false;
            }
        }
        return hasMissile && hasFuze;
    }

    @Override
    public ItemStack assemble(CraftingContainer container, RegistryAccess registryAccess) {
        ItemStack missile = ItemStack.EMPTY;
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (stack.getItem() instanceof MissileItem) {
                missile = stack.copy();
                break;
            }
        }
        if (missile.isEmpty()) return ItemStack.EMPTY;
        missile.setCount(1);
        missile.getOrCreateTag().putBoolean("FuzeInstalled", true);
        return missile;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return SERIALIZER;
    }
}
