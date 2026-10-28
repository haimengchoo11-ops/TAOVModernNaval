package TAOVMN.block.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import TAOVMN.entity.ShaoMissileProjectile;
import TAOVMN.registry.ModBlockEntities;
import TAOVMN.util.TargetDomain;
import TAOVMN.util.ThreatRanking;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

public class RadarBlockEntity extends BlockEntity implements GeoBlockEntity {
    private static final String TAG_TARGET = "SelectedTarget";
    private static final String TAG_IFF = "IffEnabled";
    private static final String TAG_AIR_TRACKED = "AirTrackedCount";
    private static final String TAG_THREAT = "SelectedThreat";
    private static final String TAG_DOMAIN = "SelectedDomain";
    private static final String TAG_CONTACTS = "Contacts";
    /** 同步到客户端的接触点数量上限，避免大型空战时同步包过大。 */
    private static final int MAX_SYNCED_CONTACTS = 24;
    /** 判定"空中目标"的高度带（格）：离雷达中心高出这么多且不在地面，即视为空中目标。 */
    private static final double AIRBORNE_ALTITUDE_BAND = 3.0D;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private UUID selectedTargetUuid;
    private Vec3 selectedTargetPos;
    /** 本次扫描得到的所有接触点（世界坐标），供总控台示波器绘制。仅随同步包下发，不落盘。 */
    private final List<ThreatRanking.Contact> contacts = new ArrayList<>();
    private int trackedCount;
    private int airTrackedCount;
    private double selectedThreat;
    private TargetDomain selectedDomain;
    private boolean iffEnabled = true;
    private int scanTicks;

    public RadarBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntities.RADAR_BLOCK.get(), pos, state);
    }

    protected RadarBlockEntity(net.minecraft.world.level.block.entity.BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /** 扫描半径（方块）。子类可覆盖以扩大范围。 */
    protected double scanRange() {
        return 160.0D;
    }

    /** 扫描间隔（tick）。子类可覆盖以加快刷新。 */
    protected int scanInterval() {
        return 20;
    }

    public int getTrackedCount() {
        return trackedCount;
    }

    public boolean isIffEnabled() {
        return iffEnabled;
    }

    /** 切换 IFF（开启=只锁定敌对生物）。 @return 切换后的状态 */
    public boolean toggleIff() {
        iffEnabled = !iffEnabled;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
        return iffEnabled;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, RadarBlockEntity radar) {
        if (level.isClientSide || ++radar.scanTicks < radar.scanInterval()) {
            return;
        }
        radar.scanTicks = 0;
        radar.scan((ServerLevel) level);
    }

    protected void scan(ServerLevel level) {
        Vec3 radarCenter = Vec3.atCenterOf(worldPosition);
        double range = scanRange();
        double rangeSqr = range * range;
        AABB bounds = new AABB(radarCenter.subtract(range, range, range),
                radarCenter.add(range, range, range));

        List<LivingEntity> candidates = level.getEntitiesOfClass(LivingEntity.class, bounds, entity ->
                entity.isAlive()
                        && !entity.isSpectator()
                        && (!iffEnabled || entity instanceof Enemy)
                        && entity.distanceToSqr(radarCenter) <= rangeSqr);

        // 按威胁排序选主目标（而非"最近优先"）：来袭弹体 > 飞机 > 水面/地面
        ThreatRanking.Contact best = null;
        UUID bestUuid = null;
        int airborne = 0;
        contacts.clear();

        for (LivingEntity entity : candidates) {
            boolean isAirborne = isAirborne(entity.onGround(), entity.getY(), radarCenter.y);
            if (isAirborne) {
                airborne++;
            }
            ThreatRanking.Contact contact = ThreatRanking.of(radarCenter, targetPoint(entity), entity.getDeltaMovement(),
                    isAirborne ? ThreatRanking.ContactKind.AIRCRAFT : ThreatRanking.ContactKind.SURFACE, range);
            contacts.add(contact);
            if (ThreatRanking.isMoreThreatening(contact, best)) {
                best = contact;
                bestUuid = entity.getUUID();
            }
        }

        int incomingMissiles = 0;
        for (ShaoMissileProjectile missile : ShaoMissileProjectile.activeWithin(level, radarCenter, range)) {
            Vec3 missilePos = missile.position();
            // 只有"正在接近"的在飞弹体构成威胁；本舰刚发射、正在远离的弹体接近度为负，天然被排除
            if (ThreatRanking.approach(radarCenter, missilePos, missile.velocity()) < 0.0D) {
                continue;
            }
            incomingMissiles++;
            airborne++;
            ThreatRanking.Contact contact = ThreatRanking.of(radarCenter, missilePos, missile.velocity(),
                    ThreatRanking.ContactKind.INCOMING_MISSILE, range);
            contacts.add(contact);
            if (ThreatRanking.isMoreThreatening(contact, best)) {
                best = contact;
                bestUuid = null;
            }
        }

        trimContacts();
        trackedCount = candidates.size() + incomingMissiles;
        airTrackedCount = airborne;
        selectedTargetUuid = bestUuid;
        selectedTargetPos = best == null ? null : best.position();
        selectedDomain = best == null ? null : best.targetDomain();
        selectedThreat = best == null ? 0.0D : best.threat();
        setChanged();
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    /** 接触点过多时只保留最近的若干个（同步包大小可控，界面点也不会糊成一团）。 */
    private void trimContacts() {
        if (contacts.size() <= MAX_SYNCED_CONTACTS) {
            return;
        }
        contacts.sort(java.util.Comparator.comparingDouble(ThreatRanking.Contact::distance));
        contacts.subList(MAX_SYNCED_CONTACTS, contacts.size()).clear();
    }

    /** 本次扫描的全部接触点（可能为空，不会为 null）。 */
    public List<ThreatRanking.Contact> getContacts() {
        return contacts;
    }

    /** 扫描半径（格），总控台用它确定示波器量程。 */
    public double getScanRange() {
        return scanRange();
    }

    /** 空中目标判定：不在地面，且高于雷达中心 {@value #AIRBORNE_ALTITUDE_BAND} 格。 */
    public static boolean isAirborne(boolean onGround, double entityY, double radarCenterY) {
        return !onGround && entityY > radarCenterY + AIRBORNE_ALTITUDE_BAND;
    }

    private static Vec3 targetPoint(LivingEntity entity) {
        return entity.position().add(0.0D, entity.getBbHeight() * 0.5D, 0.0D);
    }

    /** 被跟踪目标中被判定为空中目标的数量。 */
    public int getAirTrackedCount() {
        return airTrackedCount;
    }

    /** 当前主目标的目标域（空中目标 = {@link TargetDomain#ANTI_AIR}）；无目标时为 {@code null}。 */
    @Nullable
    public TargetDomain getSelectedTargetDomain() {
        return selectedDomain;
    }

    /** 当前主目标的威胁分数，供火控界面展示排序依据。 */
    public double getSelectedThreat() {
        return selectedThreat;
    }

    public UUID getSelectedTargetUuid() {
        return selectedTargetUuid;
    }

    public Vec3 getSelectedTargetPos() {
        return selectedTargetPos;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (selectedTargetUuid != null) {
            tag.putUUID(TAG_TARGET, selectedTargetUuid);
        }
        tag.putBoolean(TAG_IFF, iffEnabled);
        tag.putInt(TAG_AIR_TRACKED, airTrackedCount);
        tag.putDouble(TAG_THREAT, selectedThreat);
        if (selectedDomain != null) {
            tag.putString(TAG_DOMAIN, selectedDomain.key());
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        selectedTargetUuid = tag.hasUUID(TAG_TARGET) ? tag.getUUID(TAG_TARGET) : null;
        iffEnabled = !tag.contains(TAG_IFF) || tag.getBoolean(TAG_IFF);
        airTrackedCount = tag.getInt(TAG_AIR_TRACKED);
        selectedThreat = tag.getDouble(TAG_THREAT);
        selectedDomain = tag.contains(TAG_DOMAIN)
                ? TargetDomain.byKey(tag.getString(TAG_DOMAIN), null)
                : null;
        readContacts(tag);
    }

    /** 接触点只随同步包下发（不写进存档），因此单独读写。 */
    private void readContacts(CompoundTag tag) {
        contacts.clear();
        if (!tag.contains(TAG_CONTACTS, 9)) {
            return;
        }
        ListTag list = tag.getList(TAG_CONTACTS, 10);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            Vec3 position = new Vec3(entry.getDouble("X"), entry.getDouble("Y"), entry.getDouble("Z"));
            contacts.add(new ThreatRanking.Contact(position, Vec3.ZERO, parseKind(entry.getString("Kind")),
                    0.0D, 0.0D, entry.getDouble("Threat")));
        }
    }

    private static ThreatRanking.ContactKind parseKind(String name) {
        for (ThreatRanking.ContactKind kind : ThreatRanking.ContactKind.values()) {
            if (kind.name().equals(name)) {
                return kind;
            }
        }
        return ThreatRanking.ContactKind.UNKNOWN;
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        saveAdditional(tag);
        ListTag contactList = new ListTag();
        for (ThreatRanking.Contact contact : contacts) {
            CompoundTag entry = new CompoundTag();
            entry.putDouble("X", contact.position().x);
            entry.putDouble("Y", contact.position().y);
            entry.putDouble("Z", contact.position().z);
            entry.putString("Kind", contact.kind().name());
            entry.putDouble("Threat", contact.threat());
            contactList.add(entry);
        }
        tag.put(TAG_CONTACTS, contactList);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}