package TAOVMN.compat.cbc;

import TAOVMN.util.MissileProfile;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

public final class CbcCompat {
    private static final String CBC_MODID = "createbigcannons";
    private static final String ALT_CBC_MODID = "cbc";

    private CbcCompat() {
    }

    public static boolean isLoaded() {
        return ModList.get().isLoaded(CBC_MODID) || ModList.get().isLoaded(ALT_CBC_MODID);
    }

    public static boolean isSupportedAmmo(ItemStack stack) {
        if (stack.isEmpty() || !isLoaded()) {
            return false;
        }

        ResourceLocation key = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (key == null) {
            return false;
        }

        String namespace = key.getNamespace();
        if (!CBC_MODID.equals(namespace) && !ALT_CBC_MODID.equals(namespace)) {
            return false;
        }

        String path = key.getPath();
        return path.contains("shell") || path.contains("shot") || path.contains("projectile") || path.contains("autocannon") || path.contains("munition");
    }

    public static MissileProfile resolveProfile(ItemStack stack, MissileProfile fallback) {
        MissileProfile base = deriveFromItemId(stack, fallback);
        return readFromCommonCbcTags(stack, base);
    }

    public static int resolveFuzeTicks(ItemStack stack, int fallbackTicks) {
        if (!isSupportedAmmo(stack)) {
            return Math.max(1, fallbackTicks);
        }

        CompoundTag root = stack.getTag();
        if (root == null) {
            return Math.max(1, fallbackTicks);
        }

        Integer direct = readPossibleFuzeTicks(root);
        if (direct != null) {
            return Math.max(1, direct);
        }

        if (root.contains("Fuze", 10)) {
            CompoundTag fuzeTag = root.getCompound("Fuze");
            Integer nested = readPossibleFuzeTicks(fuzeTag);
            if (nested != null) {
                return Math.max(1, nested);
            }
        }

        if (root.contains("Projectile", 10)) {
            CompoundTag projectile = root.getCompound("Projectile");
            Integer projectileDirect = readPossibleFuzeTicks(projectile);
            if (projectileDirect != null) {
                return Math.max(1, projectileDirect);
            }
            if (projectile.contains("Fuze", 10)) {
                Integer projectileNested = readPossibleFuzeTicks(projectile.getCompound("Fuze"));
                if (projectileNested != null) {
                    return Math.max(1, projectileNested);
                }
            }
        }

        return Math.max(1, fallbackTicks);
    }

    public static ItemStack resolveFuzeItem(ItemStack stack) {
        if (!isSupportedAmmo(stack)) {
            return ItemStack.EMPTY;
        }

        CompoundTag root = stack.getTag();
        if (root == null) {
            return ItemStack.EMPTY;
        }

        ItemStack fuze = readPossibleFuzeItem(root);
        if (!fuze.isEmpty()) {
            return fuze;
        }

        if (root.contains("Projectile", 10)) {
            ItemStack projectileFuze = readPossibleFuzeItem(root.getCompound("Projectile"));
            if (!projectileFuze.isEmpty()) {
                return projectileFuze;
            }
        }

        return ItemStack.EMPTY;
    }

    public static void primeTaovProfileTag(ItemStack stack) {
        if (!isSupportedAmmo(stack) || MissileProfile.hasProfileTag(stack)) {
            return;
        }
        MissileProfile.writeToItem(stack, resolveProfile(stack, MissileProfile.p300()));
    }

    private static MissileProfile deriveFromItemId(ItemStack stack, MissileProfile fallback) {
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (key == null) {
            return fallback;
        }

        String path = key.getPath();
        if (path.contains("he") || path.contains("high_explosive")) {
            return new MissileProfile(1.02F, 0.16F, 16.0F, 130.0F, 2.6F, 10.0F);
        }
        if (path.contains("ap") || path.contains("armor_piercing")) {
            return new MissileProfile(1.18F, 0.18F, 10.0F, 145.0F, 2.4F, 10.0F);
        }
        if (path.contains("autocannon")) {
            return new MissileProfile(1.25F, 0.24F, 8.0F, 115.0F, 1.8F, 10.0F);
        }
        return fallback;
    }

    private static MissileProfile readFromCommonCbcTags(ItemStack stack, MissileProfile fallback) {
        CompoundTag root = stack.getTag();
        if (root == null) {
            return fallback;
        }

        float blast = fallback.blastPower();
        float seekRange = fallback.seekRange();
        float proximity = fallback.proximityFuse();

        if (root.contains("ExplosionPower")) {
            blast = Math.max(0.1F, root.getFloat("ExplosionPower"));
        } else if (root.contains("Damage")) {
            blast = Math.max(0.1F, root.getFloat("Damage") * 0.5F);
        }

        if (root.contains("Range")) {
            seekRange = Math.max(8.0F, root.getFloat("Range"));
        }

        Integer fuzeTicks = readPossibleFuzeTicks(root);
        if (fuzeTicks == null && root.contains("Projectile", 10)) {
            fuzeTicks = readPossibleFuzeTicks(root.getCompound("Projectile"));
        }
        if (fuzeTicks != null) {
            proximity = Math.max(0.2F, fuzeTicks / 40.0F);
        }

        return new MissileProfile(
                fallback.maxSpeed(),
                fallback.steering(),
                blast,
                seekRange,
                proximity,
                fallback.iffSafeRadius());
    }

    private static Integer readPossibleFuzeTicks(CompoundTag tag) {
        if (tag.contains("ExplosionCountdown", 3)) {
            return tag.getInt("ExplosionCountdown");
        }
        if (tag.contains("FuzeTicks", 3)) {
            return tag.getInt("FuzeTicks");
        }
        if (tag.contains("FuzeTime", 3)) {
            return tag.getInt("FuzeTime");
        }
        if (tag.contains("TimeToDetonate", 3)) {
            return tag.getInt("TimeToDetonate");
        }
        if (tag.contains("Fuze", 3)) {
            return tag.getInt("Fuze");
        }
        if (tag.contains("Timer", 3)) {
            return tag.getInt("Timer");
        }
        if (tag.contains("Delay", 3)) {
            return tag.getInt("Delay");
        }
        return null;
    }

    private static ItemStack readPossibleFuzeItem(CompoundTag root) {
        if (root.contains("Fuze", 10)) {
            ItemStack fuze = itemFromTag(root.getCompound("Fuze"));
            if (!fuze.isEmpty()) {
                return fuze;
            }
        }
        if (root.contains("FuzeItem", 10)) {
            ItemStack fuze = itemFromTag(root.getCompound("FuzeItem"));
            if (!fuze.isEmpty()) {
                return fuze;
            }
        }
        if (root.contains("FuzeStack", 10)) {
            ItemStack fuze = itemFromTag(root.getCompound("FuzeStack"));
            if (!fuze.isEmpty()) {
                return fuze;
            }
        }
        return ItemStack.EMPTY;
    }

    private static ItemStack itemFromTag(CompoundTag tag) {
        if (!tag.contains("id", 8)) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = ItemStack.of(tag);
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return stack.copyWithCount(1);
    }
}