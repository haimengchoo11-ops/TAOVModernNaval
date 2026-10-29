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
        float iffSafeRadius,
        TargetDomain domain
) {
    /**
     * 向后兼容的 6 参数构造器：目标域默认 {@link TargetDomain#UNIVERSAL}。
     *
     * <p>既有 P 系列弹药全部走这条路径，因此本轮引入目标域不会改变它们的行为。
     */
    public MissileProfile(float maxSpeed, float steering, float blastPower, float seekRange,
            float proximityFuse, float iffSafeRadius) {
        this(maxSpeed, steering, blastPower, seekRange, proximityFuse, iffSafeRadius, TargetDomain.UNIVERSAL);
    }
    private static final String TAG_ROOT = "TaovMissile";
    private static final String TAG_MAX_SPEED = "MaxSpeed";
    private static final String TAG_STEERING = "Steering";
    private static final String TAG_BLAST_POWER = "BlastPower";
    private static final String TAG_SEEK_RANGE = "SeekRange";
    private static final String TAG_PROXIMITY_FUSE = "ProximityFuse";
    private static final String TAG_IFF_SAFE_RADIUS = "IffSafeRadius";
    private static final String TAG_DOMAIN = "TargetDomain";
    private static final String TAG_LOCK_HOLD_TICKS = "LockHoldTicks";
    private static final String TAG_SWITCH_ADVANTAGE_DISTANCE = "SwitchAdvantageDistance";
    private static final String TAG_FORCE_SWITCH_DISTANCE = "ForceSwitchDistance";

    public static String rootTagName() {
        return TAG_ROOT;
    }

    // ---- 反舰弹：数值全部来自 WeaponSpecTable（真实数据 + 统一换算），此处不再手工排档 ----

    public static MissileProfile p1Default() {
        return WeaponSpecTable.profile("p1_missile");
    }

    public static MissileProfile p15() {
        return WeaponSpecTable.profile("p15_missile");
    }

    /** 旧 id 保留，改挂 P-35「进步」（SS-N-3）。 */
    public static MissileProfile p30() {
        return WeaponSpecTable.profile("p30_missile");
    }

    /** 旧 id 保留，改挂 P-70「紫晶」（SS-N-7）。 */
    public static MissileProfile p45() {
        return WeaponSpecTable.profile("p45_missile");
    }

    /** 旧 id 保留，改挂 P-1000「火山」（SS-N-12B）。 */
    public static MissileProfile p100() {
        return WeaponSpecTable.profile("p100_missile");
    }

    /** 旧 id 保留，改挂 3M-54「口径」（SS-N-27）。 */
    public static MissileProfile p300() {
        return WeaponSpecTable.profile("p300_missile");
    }

    public static MissileProfile p270() {
        return WeaponSpecTable.profile("p270_missile");
    }

    public static MissileProfile p500() {
        return WeaponSpecTable.profile("p500_missile");
    }

    public static MissileProfile p700() {
        return WeaponSpecTable.profile("p700_missile");
    }

    public static MissileProfile p120() {
        return WeaponSpecTable.profile("p120_missile");
    }

    public static MissileProfile p800() {
        return WeaponSpecTable.profile("p800_missile");
    }

    // ---- 防空弹（目标域 = ANTI_AIR；高速度、高转向、小当量、以近炸为主）----
    // 防空弹的杀伤机理是破片，不套用反舰弹的「战斗部/45」当量换算，参数仍按手感维护。

    /** S-300F 的 5V55RM：远程舰空弹。射程未核实，保留原档位。 */
    public static MissileProfile s300() {
        return new MissileProfile(3.20F, 0.44F, 6.0F, 260.0F, 4.0F, 12.0F, TargetDomain.ANTI_AIR);
    }

    /** 4K33 Osa-M 的 9M33M3：15 km / 12 km 射高，转向最快。射程按换算表落到下限档。 */
    public static MissileProfile nineK33() {
        return new MissileProfile(2.75F, 0.52F, 3.5F, 120.0F, 3.0F, 10.0F, TargetDomain.ANTI_AIR);
    }

    /** 9M33M1（Osa 早期弹）：近程舰空弹，弹型编号沿用旧 id。 */
    public static MissileProfile nineK60() {
        return new MissileProfile(2.60F, 0.55F, 3.0F, 120.0F, 3.0F, 10.0F, TargetDomain.ANTI_AIR);
    }

    /** Shtil-1 的 9M317ME：2.5–50 km、5 m–15 km 射高、62 kg 战斗部（旧 id 保留）。 */
    public static MissileProfile nineM38() {
        return new MissileProfile(3.00F, 0.46F, 4.5F, 260.0F, 3.5F, 12.0F, TargetDomain.ANTI_AIR);
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
        } else if (isTaovMissileItem(stack, "p120_missile")) {
            base = p120();
        } else if (isTaovMissileItem(stack, "p270_missile")) {
            base = p270();
        } else if (isTaovMissileItem(stack, "p800_missile")) {
            base = p800();
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
                readFloat(cfg, TAG_IFF_SAFE_RADIUS, fallback.iffSafeRadius),
                TargetDomain.byKey(cfg.contains(TAG_DOMAIN) ? cfg.getString(TAG_DOMAIN) : null, fallback.domain()));
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
        cfg.putString(TAG_DOMAIN, profile.domain().key());
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