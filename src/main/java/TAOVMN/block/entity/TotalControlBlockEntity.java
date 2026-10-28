package TAOVMN.block.entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import TAOVMN.content.weapon.NavalLaunchBlockEntity;
import TAOVMN.registry.ModBlockEntities;
import TAOVMN.util.FireChannel;
import TAOVMN.util.FireChannelGroup;
import TAOVMN.util.FireChannelTable;
import TAOVMN.util.LauncherKind;
import TAOVMN.util.MissileFireControl;
import TAOVMN.util.MissileProfile;
import TAOVMN.util.ScopeContact;
import TAOVMN.util.TargetDomain;
import TAOVMN.util.ThreatRanking;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

/**
 * 舰载总控台：周期性汇总附近雷达 / 火控指挥仪的目标信息与附近发射装置的备弹状态，
 * 供 GUI 显示"目标示波器 + 火力通道列表"，并接受来自界面的发射 / 编组指令。
 *
 * <p>数据流：
 * <ol>
 *   <li>每 {@value #SYNC_INTERVAL} tick 从最近雷达读取全部接触点，换算成相对本机的坐标后下发；</li>
 *   <li>每 {@value #CHANNEL_SCAN_INTERVAL} tick 扫描 {@value #CHANNEL_RANGE} 格内的发射装置，
 *       组装火力通道并套用玩家指定编组；</li>
 *   <li>界面上的按钮通过 {@code ServerboundFireChannelPacket} 发回服务端，
 *       由 {@link #handleOrder} 转成对应发射装置的实际动作。</li>
 * </ol>
 */
public class TotalControlBlockEntity extends BlockEntity {
    /** 界面刷新间隔（tick）。 */
    private static final int SYNC_INTERVAL = 20;
    /** 发射装置重扫间隔（tick）。 */
    private static final int CHANNEL_SCAN_INTERVAL = 40;
    private static final int RADAR_SEARCH_RANGE = 24;
    /** 火力通道扫描的横向半径（格）。 */
    private static final int CHANNEL_RANGE = 20;
    /** 火力通道扫描的纵向半径（格）。 */
    private static final int CHANNEL_RANGE_Y = 10;
    /** 近防炮收到一次手动交战指令后持续交战的时长（tick）。 */
    private static final int MANUAL_ENGAGE_TICKS = 100;
    private static final double DEFAULT_SCOPE_RANGE = 160.0D;
    private static final int TICK_WRAP = 120;

    /** 请求发射（导弹）或开始交战（近防炮）。 */
    public static final int ACTION_FIRE = 0;
    /** 切换到下一个火力通道编组。 */
    public static final int ACTION_CYCLE_GROUP = 1;

    private static final String TAG_MODE = "Mode";
    private static final String TAG_TARGET_COUNT = "TargetCount";
    private static final String TAG_CHANNEL_GROUPS = "ChannelGroups";
    private static final String TAG_SCOPE_RANGE = "ScopeRange";
    private static final String TAG_CHANNELS = "FireChannels";
    private static final String TAG_CONTACTS = "ScopeContacts";

    private int mode;
    private int targetCount;
    private Vec3 currentTargetPos;
    private int scanTicks;
    private double scopeRange = DEFAULT_SCOPE_RANGE;

    /** 最近一次扫描得到的火力通道（服务端构造、客户端由同步包填充）。 */
    private final List<FireChannel> channels = new ArrayList<>();
    /** 示波器用接触点（相对本机）。 */
    private final List<ScopeContact> contacts = new ArrayList<>();
    /** 玩家显式指定的编组：方块坐标 {@link BlockPos#asLong()} → 编组。 */
    private final Map<Long, FireChannelGroup> groupAssignments = new HashMap<>();

    public TotalControlBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TOTAL_CONTROL_BLOCK.get(), pos, state);
    }

    // ---- 周期扫描 ----

    public void tick() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        scanTicks = (scanTicks + 1) % TICK_WRAP;

        if (scanTicks % CHANNEL_SCAN_INTERVAL == 0) {
            scanChannels(serverLevel);
        }
        if (scanTicks % SYNC_INTERVAL == 0) {
            scanRadar(serverLevel);
            setChanged();
            syncToClients();
        }
    }

    /** 读取最近雷达的全部接触点，换算成相对本机的坐标。 */
    private void scanRadar(ServerLevel serverLevel) {
        RadarBlockEntity radar = MissileFireControl.findNearbyRadar(serverLevel, worldPosition, RADAR_SEARCH_RANGE);
        contacts.clear();
        if (radar == null) {
            targetCount = 0;
            currentTargetPos = null;
            scopeRange = DEFAULT_SCOPE_RANGE;
            return;
        }

        targetCount = radar.getTrackedCount();
        currentTargetPos = radar.getSelectedTargetPos();
        scopeRange = Math.max(32.0D, radar.getScanRange());

        Vec3 origin = Vec3.atCenterOf(worldPosition);
        Vec3 selected = radar.getSelectedTargetPos();
        for (ThreatRanking.Contact contact : radar.getContacts()) {
            boolean primary = selected != null && contact.position().distanceToSqr(selected) < 1.0E-4D;
            ScopeContact snapshot = ScopeContact.of(contact, origin, primary);
            if (snapshot != null) {
                contacts.add(snapshot);
            }
        }
    }

    /** 扫描附近发射装置并套用编组，重建火力通道列表。 */
    private void scanChannels(ServerLevel serverLevel) {
        List<FireChannelTable.Reading> readings = new ArrayList<>();
        BlockPos min = worldPosition.offset(-CHANNEL_RANGE, -CHANNEL_RANGE_Y, -CHANNEL_RANGE);
        BlockPos max = worldPosition.offset(CHANNEL_RANGE, CHANNEL_RANGE_Y, CHANNEL_RANGE);
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            FireChannelTable.Reading reading = readLauncher(serverLevel.getBlockEntity(pos), pos);
            if (reading != null) {
                readings.add(reading);
            }
        }

        channels.clear();
        channels.addAll(FireChannelTable.assemble(readings, groupAssignments));
    }

    /** 把一座方块实体翻译成一条通道读数；不是发射装置时返回 {@code null}。 */
    @Nullable
    private static FireChannelTable.Reading readLauncher(@Nullable BlockEntity blockEntity, BlockPos pos) {
        BlockPos anchor = pos.immutable();
        if (blockEntity instanceof RakLauncherBlockEntity launcher) {
            ItemStack ammo = launcher.getTopMissile();
            return new FireChannelTable.Reading(anchor, LauncherKind.RAK_LAUNCHER, itemId(ammo),
                    launcher.getMissileCount(), RakLauncherBlockEntity.MAX_MISSILES, true, domainOf(ammo));
        }
        if (blockEntity instanceof LaunchTubeBlockEntity tube) {
            ItemStack ammo = tube.getLoadedMissile();
            int count = ammo.isEmpty() ? 0 : 1;
            return new FireChannelTable.Reading(anchor, LauncherKind.LAUNCH_TUBE, itemId(ammo), count, 1, true,
                    domainOf(ammo));
        }
        // AK-630 / AK-230 共用同一套近防炮基类，型号由各自 CiwsSpec 决定
        if (blockEntity instanceof AbstractCiwsBlockEntity ciws) {
            return new FireChannelTable.Reading(anchor, ciws.launcherKind(), "", 0, 0, true, TargetDomain.ANTI_AIR);
        }
        if (blockEntity instanceof NavalLaunchBlockEntity naval) {
            return new FireChannelTable.Reading(anchor, LauncherKind.NAVAL_LAUNCH_TUBE,
                    "taovmodernnaval:naval_missile", 1, 1, naval.isReady(), TargetDomain.UNIVERSAL);
        }
        return null;
    }

    private static String itemId(ItemStack stack) {
        if (stack.isEmpty()) {
            return "";
        }
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return key == null ? "" : key.toString();
    }

    private static TargetDomain domainOf(ItemStack stack) {
        return stack.isEmpty() ? TargetDomain.UNIVERSAL : MissileProfile.resolve(stack).domain();
    }

    // ---- 界面指令 ----

    /** 处理一条来自界面的指令（服务端线程）。 */
    public void handleOrder(ServerPlayer player, BlockPos channelPos, int action) {
        FireChannel channel = channelAt(channelPos);
        if (channel == null) {
            player.displayClientMessage(Component.translatable("message.taovmodernnaval.channel_missing"), true);
            return;
        }

        if (action == ACTION_CYCLE_GROUP) {
            cycleChannelGroup(channelPos);
            FireChannel updated = channelAt(channelPos);
            FireChannelGroup group = updated == null ? channel.group() : updated.group();
            player.displayClientMessage(Component.translatable("message.taovmodernnaval.channel_group_set",
                    Component.translatable(group.translationKey())), true);
            return;
        }

        if (action == ACTION_FIRE) {
            boolean fired = fireChannel(channelPos);
            player.displayClientMessage(Component.translatable(fired
                    ? "message.taovmodernnaval.channel_fired"
                    : "message.taovmodernnaval.channel_fire_failed"), true);
        }
    }

    /** 对指定发射装置下达发射 / 交战命令。 @return 是否真的执行了动作 */
    public boolean fireChannel(BlockPos channelPos) {
        if (!(level instanceof ServerLevel serverLevel) || !isInChannelRange(channelPos)) {
            return false;
        }

        BlockEntity blockEntity = serverLevel.getBlockEntity(channelPos);
        boolean acted = false;
        if (blockEntity instanceof RakLauncherBlockEntity launcher) {
            acted = launcher.fireNextMissile(serverLevel, channelPos);
        } else if (blockEntity instanceof LaunchTubeBlockEntity tube) {
            acted = tube.fire(serverLevel, channelPos, tube.getBlockState());
        } else if (blockEntity instanceof AbstractCiwsBlockEntity gun) {
            gun.orderManualEngagement(MANUAL_ENGAGE_TICKS);
            acted = true;
        } else if (blockEntity instanceof NavalLaunchBlockEntity naval) {
            acted = naval.tryLaunch();
        }

        if (acted) {
            setChanged();
            syncToClients();
        }
        return acted;
    }

    /** 编组循环切换一条火力通道。 */
    public boolean cycleChannelGroup(BlockPos channelPos) {
        FireChannel current = channelAt(channelPos);
        if (current == null) {
            return false;
        }
        groupAssignments.put(channelPos.asLong(), current.group().next());
        setChanged();
        if (level instanceof ServerLevel serverLevel) {
            scanChannels(serverLevel);
            syncToClients();
        }
        return true;
    }

    @Nullable
    public FireChannel channelAt(@Nullable BlockPos pos) {
        if (pos == null) {
            return null;
        }
        for (FireChannel channel : channels) {
            if (channel.sameLauncher(pos)) {
                return channel;
            }
        }
        return null;
    }

    private boolean isInChannelRange(BlockPos pos) {
        return Math.abs(pos.getX() - worldPosition.getX()) <= CHANNEL_RANGE
                && Math.abs(pos.getY() - worldPosition.getY()) <= CHANNEL_RANGE_Y
                && Math.abs(pos.getZ() - worldPosition.getZ()) <= CHANNEL_RANGE;
    }

    // ---- 模式 ----

    public void cycleMode() {
        mode = (mode + 1) % 3;
        setChanged();
        syncToClients();
    }

    public String getModeName() {
        return switch (mode) {
            case 1 -> "防御";
            case 2 -> "全自动";
            default -> "待机";
        };
    }

    // ---- 界面读取 ----

    public int getTargetCount() {
        return targetCount;
    }

    @Nullable
    public Vec3 getCurrentTargetPos() {
        return currentTargetPos;
    }

    /** 火力通道列表（只读视图）。 */
    public List<FireChannel> getChannels() {
        return channels;
    }

    /** 示波器接触点（相对本机）。 */
    public List<ScopeContact> getContacts() {
        return contacts;
    }

    /** 示波器量程（格）。 */
    public double getScopeRange() {
        return scopeRange > 0.0D ? scopeRange : DEFAULT_SCOPE_RANGE;
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
        tag.putInt(TAG_MODE, mode);
        tag.putInt(TAG_TARGET_COUNT, targetCount);
        if (currentTargetPos != null) {
            tag.putDouble("TargetX", currentTargetPos.x);
            tag.putDouble("TargetY", currentTargetPos.y);
            tag.putDouble("TargetZ", currentTargetPos.z);
        }

        CompoundTag groups = new CompoundTag();
        for (Map.Entry<Long, FireChannelGroup> entry : groupAssignments.entrySet()) {
            groups.putString(Long.toString(entry.getKey()), entry.getValue().key());
        }
        tag.put(TAG_CHANNEL_GROUPS, groups);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        mode = tag.getInt(TAG_MODE);
        targetCount = tag.getInt(TAG_TARGET_COUNT);
        currentTargetPos = tag.contains("TargetX")
                ? new Vec3(tag.getDouble("TargetX"), tag.getDouble("TargetY"), tag.getDouble("TargetZ"))
                : null;

        groupAssignments.clear();
        if (tag.contains(TAG_CHANNEL_GROUPS, 10)) {
            CompoundTag groups = tag.getCompound(TAG_CHANNEL_GROUPS);
            for (String key : groups.getAllKeys()) {
                try {
                    groupAssignments.put(Long.parseLong(key),
                            FireChannelGroup.byKey(groups.getString(key), FireChannelGroup.UNASSIGNED));
                } catch (NumberFormatException ignored) {
                    // 非法键（手改存档等）直接忽略
                }
            }
        }

        scopeRange = tag.contains(TAG_SCOPE_RANGE) ? tag.getDouble(TAG_SCOPE_RANGE) : DEFAULT_SCOPE_RANGE;
        readChannels(tag);
        readContacts(tag);
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        saveAdditional(tag);
        tag.putDouble(TAG_SCOPE_RANGE, scopeRange);
        writeChannels(tag);
        writeContacts(tag);
        return tag;
    }

    private void writeChannels(CompoundTag tag) {
        ListTag list = new ListTag();
        for (FireChannel channel : channels) {
            CompoundTag entry = new CompoundTag();
            entry.putLong("Pos", channel.pos().asLong());
            entry.putString("Kind", channel.kind().name());
            entry.putString("Ammo", channel.ammoItemId());
            entry.putInt("Count", channel.ammoCount());
            entry.putInt("Capacity", channel.ammoCapacity());
            entry.putBoolean("Ready", channel.ready());
            entry.putString("Group", channel.group().key());
            entry.putString("Domain", channel.domain().key());
            list.add(entry);
        }
        tag.put(TAG_CHANNELS, list);
    }

    private void readChannels(CompoundTag tag) {
        channels.clear();
        if (!tag.contains(TAG_CHANNELS, 9)) {
            return;
        }
        ListTag list = tag.getList(TAG_CHANNELS, 10);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            channels.add(new FireChannel(
                    BlockPos.of(entry.getLong("Pos")),
                    LauncherKind.byName(entry.getString("Kind"), LauncherKind.RAK_LAUNCHER),
                    entry.getString("Ammo"),
                    entry.getInt("Count"),
                    entry.getInt("Capacity"),
                    entry.getBoolean("Ready"),
                    FireChannelGroup.byKey(entry.getString("Group"), FireChannelGroup.UNASSIGNED),
                    TargetDomain.byKey(entry.getString("Domain"), TargetDomain.UNIVERSAL)));
        }
    }

    private void writeContacts(CompoundTag tag) {
        ListTag list = new ListTag();
        for (ScopeContact contact : contacts) {
            CompoundTag entry = new CompoundTag();
            entry.putDouble("X", contact.dx());
            entry.putDouble("Y", contact.dy());
            entry.putDouble("Z", contact.dz());
            entry.putString("Kind", contact.kind().name());
            entry.putDouble("Threat", contact.threat());
            entry.putBoolean("Primary", contact.primary());
            list.add(entry);
        }
        tag.put(TAG_CONTACTS, list);
    }

    private void readContacts(CompoundTag tag) {
        contacts.clear();
        if (!tag.contains(TAG_CONTACTS, 9)) {
            return;
        }
        ListTag list = tag.getList(TAG_CONTACTS, 10);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            contacts.add(new ScopeContact(
                    entry.getDouble("X"),
                    entry.getDouble("Y"),
                    entry.getDouble("Z"),
                    parseKind(entry.getString("Kind")),
                    entry.getDouble("Threat"),
                    entry.getBoolean("Primary")));
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
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
