package TAOVMN.util;

import TAOVMN.compat.cbc.CbcCompat;
import TAOVMN.item.MissileItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

public record MissileProfile(
        float maxSpeed,
        float steering,
        float blastPower,
        float seekRange,
        float proximityFuse,
        float iffSafeRadius
) {
    private static final String TAG_ROOT = "TaovMissile";
    private static final String TAG_MAX_SPEED = "MaxSpeed";
    private static final String TAG_STEERING = "Steering";
    private static final String TAG_BLAST_POWER = "BlastPower";
    private static final String TAG_SEEK_RANGE = "SeekRange";
    private static final String TAG_PROXIMITY_FUSE = "ProximityFuse";
    private static final String TAG_IFF_SAFE_RADIUS = "IffSafeRadius";
    private static final String TAG_LOCK_HOLD_TICKS = "LockHoldTicks";
    private static final String TAG_SWITCH_ADVANTAGE_DISTANCE = "SwitchAdvantageDistance";
    private static final String TAG_FORCE_SWITCH_DISTANCE = "ForceSwitchDistance";

    public static String rootTagName() {
        return TAG_ROOT;
    }

    public static MissileProfile p1Default() {
        return new MissileProfile(1.2F, 0.20F, 4.0F, 96.0F, 1.8F, 10.0F);
    }

    public static MissileProfile p15() {
        return new MissileProfile(1.24F, 0.24F, 6.0F, 100.0F, 1.8F, 10.0F);
    }

    public static MissileProfile p30() {
        return new MissileProfile(1.20F, 0.22F, 7.0F, 104.0F, 1.9F, 10.0F);
    }

    public static MissileProfile p45() {
        return new MissileProfile(1.16F, 0.20F, 8.0F, 108.0F, 2.0F, 10.0F);
    }

    public static MissileProfile p100() {
        return new MissileProfile(1.08F, 0.18F, 10.0F, 116.0F, 2.1F, 10.0F);
    }

    public static MissileProfile p300() {
        return new MissileProfile(1.00F, 0.16F, 13.0F, 124.0F, 2.2F, 10.0F);
    }

    public static MissileProfile p500() {
        return new MissileProfile(0.94F, 0.15F, 16.0F, 132.0F, 2.4F, 10.0F);
    }

    public static MissileProfile p700() {
        return new MissileProfile(0.88F, 0.14F, 20.0F, 140.0F, 2.6F, 10.0F);
    }

    public static MissileProfile resolve(ItemStack stack) {
        MissileProfile base;
        if (stack.getItem() instanceof MissileItem missileItem) {
            base = missileItem.getDefaultProfile();
        } else if (isTaovMissileItem(stack, "p1_missile")) {
            base = p1Default();
        } else if (isTaovMissileItem(stack, "p15_missile")) {
            base = p15();
        } else if (isTaovMissileItem(stack, "p30_missile")) {
            base = p30();
        } else if (isTaovMissileItem(stack, "p45_missile")) {
            base = p45();
        } else if (isTaovMissileItem(stack, "p100_missile")) {
            base = p100();
        } else if (isTaovMissileItem(stack, "p300_missile")) {
            base = p300();
        } else if (isTaovMissileItem(stack, "p500_missile")) {
            base = p500();
        } else if (isTaovMissileItem(stack, "p700_missile")) {
            base = p700();
        } else if (CbcCompat.isSupportedAmmo(stack)) {
            base = CbcCompat.resolveProfile(stack, p300());
        } else {
            base = p1Default();
        }
        return fromItem(stack, base);
    }

    private static boolean isTaovMissileItem(ItemStack stack, String path) {
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return key != null && "taovmodernnaval".equals(key.getNamespace()) && path.equals(key.getPath());
    }

    public static MissileProfile fromItem(ItemStack stack, MissileProfile fallback) {
        if (!stack.hasTag()) {
            return fallback;
        }

        CompoundTag root = stack.getTag();
        if (root == null || !root.contains(TAG_ROOT, 10)) {
            return fallback;
        }

        CompoundTag cfg = root.getCompound(TAG_ROOT);
        return new MissileProfile(
                readFloat(cfg, TAG_MAX_SPEED, fallback.maxSpeed),
                readFloat(cfg, TAG_STEERING, fallback.steering),
                readFloat(cfg, TAG_BLAST_POWER, fallback.blastPower),
                readFloat(cfg, TAG_SEEK_RANGE, fallback.seekRange),
                readFloat(cfg, TAG_PROXIMITY_FUSE, fallback.proximityFuse),
                readFloat(cfg, TAG_IFF_SAFE_RADIUS, fallback.iffSafeRadius));
    }

    public static boolean hasProfileTag(ItemStack stack) {
        if (!stack.hasTag()) {
            return false;
        }
        CompoundTag root = stack.getTag();
        return root != null && root.contains(TAG_ROOT, 10);
    }

    public static void writeToItem(ItemStack stack, MissileProfile profile) {
        CompoundTag root = stack.getOrCreateTag();
        CompoundTag cfg = root.contains(TAG_ROOT, 10) ? root.getCompound(TAG_ROOT) : new CompoundTag();
        cfg.putFloat(TAG_MAX_SPEED, profile.maxSpeed());
        cfg.putFloat(TAG_STEERING, profile.steering());
        cfg.putFloat(TAG_BLAST_POWER, profile.blastPower());
        cfg.putFloat(TAG_SEEK_RANGE, profile.seekRange());
        cfg.putFloat(TAG_PROXIMITY_FUSE, profile.proximityFuse());
        cfg.putFloat(TAG_IFF_SAFE_RADIUS, profile.iffSafeRadius());
        root.put(TAG_ROOT, cfg);
    }

    public static void ensureProfileTag(ItemStack stack, MissileProfile fallback) {
        if (!hasProfileTag(stack)) {
            writeToItem(stack, fallback);
        }
    }

    public static int resolveLockHoldTicks(ItemStack stack, int fallback) {
        CompoundTag cfg = getRootConfig(stack);
        if (cfg == null) {
            return fallback;
        }
        return Math.max(0, cfg.contains(TAG_LOCK_HOLD_TICKS) ? cfg.getInt(TAG_LOCK_HOLD_TICKS) : fallback);
    }

    public static double resolveSwitchAdvantageDistance(ItemStack stack, double fallback) {
        CompoundTag cfg = getRootConfig(stack);
        if (cfg == null) {
            return fallback;
        }
        return Math.max(0.0D, cfg.contains(TAG_SWITCH_ADVANTAGE_DISTANCE) ? cfg.getDouble(TAG_SWITCH_ADVANTAGE_DISTANCE) : fallback);
    }

    public static double resolveForceSwitchDistance(ItemStack stack, double fallback) {
        CompoundTag cfg = getRootConfig(stack);
        if (cfg == null) {
            return fallback;
        }
        return Math.max(0.0D, cfg.contains(TAG_FORCE_SWITCH_DISTANCE) ? cfg.getDouble(TAG_FORCE_SWITCH_DISTANCE) : fallback);
    }

    private static CompoundTag getRootConfig(ItemStack stack) {
        if (!stack.hasTag()) {
            return null;
        }
        CompoundTag root = stack.getTag();
        if (root == null || !root.contains(TAG_ROOT, 10)) {
            return null;
        }
        return root.getCompound(TAG_ROOT);
    }

    private static float readFloat(CompoundTag tag, String key, float fallback) {
        return tag.contains(key) ? tag.getFloat(key) : fallback;
    }
}