package TAOVMN.block.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import TAOVMN.entity.ShaoMissileProjectile;
import TAOVMN.item.LauncherBlockItem;
import TAOVMN.registry.ModBlockEntities;
import TAOVMN.registry.ModItems;
import TAOVMN.util.MissileFireControl;
import TAOVMN.util.MissileProfile;
import TAOVMN.util.OwnerTracked;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * RAK 导弹发射架方块实体。
 *
 * <p>火控基础功能：装填导弹（最多 2 发）→ 绑定雷达 / 火控指挥仪 → 读取其锁定目标 →
 * 收到<b>红石上升沿</b>时发射一发，并按目标指派制导。右键只负责装填 / 卸载 / 绑定 / 查询。
 */
public class RakLauncherBlockEntity extends BlockEntity implements GeoBlockEntity, OwnerTracked {
    public static final int MAX_MISSILES = 2;

    private static final int[] FUZE_PRESETS = {120, 200, 320, 480};
    private static final int NEAREST_RADAR_RANGE = 16;
    private static final int FIRE_COOLDOWN_TICKS = 10;
    private static final String TAG_MISSILES = "Missiles";
    private static final String TAG_FUZE_TICKS = "FuzeTicks";
    private static final String TAG_BOUND_RADAR = "BoundRadarPos";

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final List<ItemStack> missiles = new ArrayList<>();
    private int fuzeTicks = 200;
    private BlockPos boundRadarPos;
    private boolean powered;
    private int fireCooldown;
    /** 放置这座发射架的玩家；null 表示归属未知（不施加 IFF 保护）。 */
    @Nullable
    private UUID ownerId;

    public RakLauncherBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntities.RAK_LAUNCHER.get(), pos, state);
    }

    public RakLauncherBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    // ---- 红石触发发射 ----

    public static void tick(Level level, BlockPos pos, BlockState state, RakLauncherBlockEntity launcher) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        if (launcher.fireCooldown > 0) {
            launcher.fireCooldown--;
        }
        boolean poweredNow = level.hasNeighborSignal(pos);
        if (poweredNow && !launcher.powered && launcher.fireCooldown <= 0) {
            launcher.fireNextMissile(serverLevel, pos);
            launcher.fireCooldown = FIRE_COOLDOWN_TICKS;
        }
        launcher.powered = poweredNow;
    }

    /** 红石上升沿：发射一发已装填导弹（无弹则忽略）。 */
    public boolean fireNextMissile(ServerLevel level, BlockPos pos) {
        if (missiles.isEmpty()) {
            return false;
        }

        ItemStack stack = missiles.remove(0);
        setChanged();

        UUID targetUuid = getSelectedRadarTargetUuid();
        Vec3 targetPos = getSelectedRadarTargetPos();
        Vec3 muzzle = Vec3.atCenterOf(pos).add(0.0D, 0.7D, 0.0D);

        // 垂直冷发射：出膛后由制导转向火控目标
        ShaoMissileProjectile missile = MissileFireControl.launch(level, stack, muzzle,
                new Vec3(0.0D, 1.0D, 0.0D), fuzeTicks, targetUuid, targetPos, muzzle, ownerId);
        if (missile == null) {
            return false;
        }

        level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 1.2F, 0.9F);
        level.sendBlockUpdated(pos, getBlockState(), getBlockState(), 3);
        return true;
    }

    // ---- 装填 / 卸载 ----

    public boolean hasMissile() {
        return !missiles.isEmpty();
    }

    public int getMissileCount() {
        return missiles.size();
    }

    public boolean isFull() {
        return missiles.size() >= MAX_MISSILES;
    }

    public boolean loadMissile(ItemStack stack) {
        if (stack.isEmpty() || isFull() || !ModItems.isMissile(stack)) {
            return false;
        }
        ItemStack loaded = stack.copyWithCount(1);
        if (loaded.getItem() instanceof TAOVMN.item.MissileItem missileItem) {
            MissileProfile.ensureProfileTag(loaded, missileItem.getDefaultProfile());
        }
        missiles.add(loaded);
        setChanged();
        syncToClients();
        return true;
    }

    public ItemStack removeMissile() {
        if (missiles.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack extracted = missiles.remove(missiles.size() - 1);
        setChanged();
        syncToClients();
        return extracted;
    }

    /** 当前最上层（最后装填）的导弹，供总控台显示弹药名；无弹时返回空。 */
    public ItemStack getTopMissile() {
        return missiles.isEmpty() ? ItemStack.EMPTY : missiles.get(missiles.size() - 1);
    }

    public MissileProfile getMissileProfile() {
        return missiles.isEmpty()
                ? MissileProfile.p1Default()
                : MissileProfile.resolve(missiles.get(missiles.size() - 1));
    }

    // ---- 引信预设 ----

    public int getFuzeTicks() {
        return fuzeTicks;
    }

    public int cycleFuzePreset() {
        for (int preset : FUZE_PRESETS) {
            if (preset > fuzeTicks) {
                fuzeTicks = preset;
                setChanged();
                return fuzeTicks;
            }
        }
        fuzeTicks = FUZE_PRESETS[0];
        setChanged();
        return fuzeTicks;
    }

    // ---- 雷达绑定 ----

    public boolean hasBoundRadar() {
        return getBoundRadarEntity() != null;
    }

    public BlockPos getBoundRadarPos() {
        return boundRadarPos;
    }

    public void setBoundRadarPos(BlockPos radarPos) {
        this.boundRadarPos = radarPos == null ? null : radarPos.immutable();
        setChanged();
        syncToClients();
    }

    /** 消费发射架物品上预绑定的雷达坐标（放置时调用）。 */
    public void applyPlacementTag(CompoundTag itemTag) {
        if (itemTag == null || !itemTag.contains(LauncherBlockItem.TAG_PREBOUND_RADAR_POS)) {
            return;
        }
        setBoundRadarPos(NbtUtils.readBlockPos(itemTag.getCompound(LauncherBlockItem.TAG_PREBOUND_RADAR_POS)));
    }

    /** 就近绑定雷达 / 火控指挥仪（{@value #NEAREST_RADAR_RANGE} 格内）。 */
    public boolean bindNearestRadar() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return false;
        }
        RadarBlockEntity radar = MissileFireControl.findNearbyRadar(serverLevel, worldPosition, NEAREST_RADAR_RANGE);
        if (radar == null) {
            return false;
        }
        setBoundRadarPos(radar.getBlockPos());
        return true;
    }

    private RadarBlockEntity getBoundRadarEntity() {
        if (level == null || boundRadarPos == null) {
            return null;
        }
        if (level.getBlockEntity(boundRadarPos) instanceof RadarBlockEntity radar) {
            return radar;
        }
        boundRadarPos = null;
        setChanged();
        return null;
    }

    // ---- 火控目标读取（委托给绑定雷达） ----

    @Nullable
    public UUID getSelectedRadarTargetUuid() {
        RadarBlockEntity radar = getBoundRadarEntity();
        return radar == null ? null : radar.getSelectedTargetUuid();
    }

    @Nullable
    public Vec3 getSelectedRadarTargetPos() {
        RadarBlockEntity radar = getBoundRadarEntity();
        return radar == null ? null : radar.getSelectedTargetPos();
    }

    public int getRadarTrackedCount() {
        RadarBlockEntity radar = getBoundRadarEntity();
        return radar == null ? 0 : radar.getTrackedCount();
    }

    public boolean toggleRadarIff() {
        RadarBlockEntity radar = getBoundRadarEntity();
        return radar != null && radar.toggleIff();
    }

    public boolean isRadarIffEnabled() {
        RadarBlockEntity radar = getBoundRadarEntity();
        return radar != null && radar.isIffEnabled();
    }

    // ---- 归属（IFF） ----

    @Override
    @Nullable
    public UUID getOwnerId() {
        return ownerId;
    }

    @Override
    public void setOwnerId(@Nullable UUID ownerId) {
        this.ownerId = ownerId;
        setChanged();
    }

    private void syncToClients() {
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    // ---- NBT / 网络同步 ----

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        CompoundTag missilesTag = new CompoundTag();
        missilesTag.putInt("Size", missiles.size());
        for (int i = 0; i < missiles.size(); i++) {
            missilesTag.put("S" + i, missiles.get(i).save(new CompoundTag()));
        }
        tag.put(TAG_MISSILES, missilesTag);
        tag.putInt(TAG_FUZE_TICKS, fuzeTicks);
        if (boundRadarPos != null) {
            tag.putLong(TAG_BOUND_RADAR, boundRadarPos.asLong());
        }
        OwnerTracked.write(tag, ownerId);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        missiles.clear();
        if (tag.contains(TAG_MISSILES, 10)) {
            CompoundTag missilesTag = tag.getCompound(TAG_MISSILES);
            int size = Math.min(MAX_MISSILES, Math.max(0, missilesTag.getInt("Size")));
            for (int i = 0; i < size; i++) {
                String key = "S" + i;
                if (missilesTag.contains(key, 10)) {
                    ItemStack stack = ItemStack.of(missilesTag.getCompound(key));
                    if (!stack.isEmpty()) {
                        stack.setCount(1);
                        missiles.add(stack);
                    }
                }
            }
        }
        fuzeTicks = tag.contains(TAG_FUZE_TICKS) ? tag.getInt(TAG_FUZE_TICKS) : 200;
        boundRadarPos = tag.contains(TAG_BOUND_RADAR) ? BlockPos.of(tag.getLong(TAG_BOUND_RADAR)) : null;
        ownerId = OwnerTracked.read(tag);
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        saveAdditional(tag);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    // ---- GeckoLib ----

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // 预留：发射架动画控制器
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
