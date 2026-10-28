package TAOVMN.client.gui;

import java.util.List;

import TAOVMN.block.entity.TotalControlBlockEntity;
import TAOVMN.network.ModNetwork;
import TAOVMN.network.ServerboundFireChannelPacket;
import TAOVMN.util.FireChannel;
import TAOVMN.util.ScopeContact;
import TAOVMN.util.TargetDomain;
import TAOVMN.util.ThreatRanking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

/**
 * 舰载总控台界面：左侧目标示波器 + 右侧火力通道列。
 *
 * <p>示波器以本机为原点，把雷达接触点按"相对距离与方位"画成不同颜色的点
 * （正北朝上，屏幕坐标即 {@code (dx, dz)}）；右侧每条火力通道是一根长条，
 * 标明发射装置类型、弹药与编组，并带"发射 / 交战"和"编组"按钮。
 */
public class TotalControlScreen extends Screen {
    private static final int PANEL_WIDTH = 452;
    private static final int PANEL_HEIGHT = 252;

    private static final int SCOPE_CENTER_X = 146;
    private static final int SCOPE_CENTER_Y = 146;
    private static final int SCOPE_RADIUS = 74;

    private static final int CHANNEL_X = 286;
    private static final int CHANNEL_WIDTH = 154;
    private static final int CHANNEL_HEADER_Y = 42;
    private static final int CHANNEL_TOP = 56;
    private static final int CHANNEL_ROW_STEP = 32;
    private static final int CHANNEL_BAR_HEIGHT = 30;
    private static final int MAX_CHANNELS = 6;
    private static final int BUTTON_WIDTH = 38;
    private static final int BUTTON_HEIGHT = 12;

    private static final int BACKGROUND = 0xF0061018;
    private static final int PANEL_HEADER = 0xFF4FA3D1;
    private static final int BORDER = 0xFF2A5266;
    private static final int BORDER_BLOCKED = 0xFF5A3630;
    private static final int TEXT = 0xFFE5F2F7;
    private static final int TEXT_DIM = 0xFF8FA8B5;
    private static final int TEXT_READY = 0xFF7FE0A0;
    private static final int TEXT_WARN = 0xFFFF9A5A;
    private static final int SCOPE_BG = 0xFF04121A;
    private static final int SCOPE_RING = 0xFF2E5A70;
    private static final int SCOPE_RING_MINOR = 0xFF1B3D4E;
    private static final int SCOPE_CROSS = 0xFF16323F;
    private static final int BAR_BG_READY = 0xFF10242F;
    private static final int BAR_BG_IDLE = 0xFF161C20;

    private static final double FALLBACK_RANGE = 160.0D;

    private final BlockPos controllerPos;
    private final Button[] fireButtons = new Button[MAX_CHANNELS];
    private final Button[] groupButtons = new Button[MAX_CHANNELS];

    public TotalControlScreen(BlockPos controllerPos) {
        super(Component.translatable("gui.taovmodernnaval.total_control.title"));
        this.controllerPos = controllerPos;
    }

    @Override
    protected void init() {
        int left = panelLeft();
        int top = panelTop();

        for (int i = 0; i < MAX_CHANNELS; i++) {
            final int index = i;
            int barX = left + CHANNEL_X + CHANNEL_WIDTH - BUTTON_WIDTH - 2;
            int barY = top + CHANNEL_TOP + i * CHANNEL_ROW_STEP;

            fireButtons[i] = this.addRenderableWidget(Button.builder(
                    Component.translatable("gui.taovmodernnaval.fire"),
                    button -> sendOrder(index, TotalControlBlockEntity.ACTION_FIRE))
                    .bounds(barX, barY + 2, BUTTON_WIDTH, BUTTON_HEIGHT)
                    .build());

            groupButtons[i] = this.addRenderableWidget(Button.builder(
                    Component.literal("-"),
                    button -> sendOrder(index, TotalControlBlockEntity.ACTION_CYCLE_GROUP))
                    .bounds(barX, barY + 16, BUTTON_WIDTH, BUTTON_HEIGHT)
                    .build());
        }

        this.addRenderableWidget(Button.builder(Component.translatable("gui.taovmodernnaval.close"),
                button -> this.onClose())
                .bounds(left + PANEL_WIDTH - 68, top + 6, 56, 16)
                .build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);

        int left = panelLeft();
        int top = panelTop();
        graphics.fill(left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, BACKGROUND);
        graphics.fill(left, top, left + PANEL_WIDTH, top + 2, PANEL_HEADER);

        TotalControlBlockEntity controller = controller();
        graphics.drawString(this.font, this.title, left + 12, top + 10, TEXT);

        if (controller == null) {
            for (int i = 0; i < MAX_CHANNELS; i++) {
                fireButtons[i].visible = false;
                groupButtons[i].visible = false;
            }
            graphics.drawString(this.font, Component.translatable("gui.taovmodernnaval.no_data"),
                    left + 12, top + 40, TEXT_WARN);
            super.render(graphics, mouseX, mouseY, partialTick);
            return;
        }

        renderStatus(graphics, controller, left, top);
        renderScope(graphics, controller, left, top);
        renderChannels(graphics, controller, left, top);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    // ---- 状态行 ----

    private void renderStatus(GuiGraphics graphics, TotalControlBlockEntity controller, int left, int top) {
        String mode = Component.translatable("gui.taovmodernnaval.mode", controller.getModeName()).getString();
        String targets = Component.translatable("gui.taovmodernnaval.targets", controller.getTargetCount()).getString();
        String range = Component.translatable("gui.taovmodernnaval.scope_range",
                (int) Math.round(controller.getScopeRange())).getString();

        int x = left + 12;
        graphics.drawString(this.font, mode, x, top + 28, TEXT);
        x += this.font.width(mode) + 12;
        graphics.drawString(this.font, targets, x, top + 28, TEXT);
        x += this.font.width(targets) + 12;
        graphics.drawString(this.font, range, x, top + 28, TEXT_DIM);

        String target = controller.getCurrentTargetPos() == null
                ? Component.translatable("gui.taovmodernnaval.no_lock").getString()
                : Component.translatable("gui.taovmodernnaval.locked_target",
                        String.format("%.1f", controller.getCurrentTargetPos().x),
                        String.format("%.1f", controller.getCurrentTargetPos().y),
                        String.format("%.1f", controller.getCurrentTargetPos().z)).getString();
        graphics.drawString(this.font, trim(target, 418), left + 12, top + 40, TEXT_DIM);

        // 威胁分数此前只算不显示（getSelectedThreat 全项目 0 调用点）：
        // 把主目标的排序依据摊开，玩家才知道"为什么先打这个"。
        ScopeContact primary = primaryContact(controller.getContacts());
        String threat = Component.translatable("gui.taovmodernnaval.threat",
                primary == null ? "-" : String.format("%.1f", primary.threat())).getString();
        graphics.drawString(this.font, threat, left + 12, top + 52,
                primary == null ? TEXT_DIM : TEXT_WARN);
    }

    // ---- 示波器 ----

    private void renderScope(GuiGraphics graphics, TotalControlBlockEntity controller, int left, int top) {
        int cx = left + SCOPE_CENTER_X;
        int cy = top + SCOPE_CENTER_Y;
        int radius = SCOPE_RADIUS;
        double range = controller.getScopeRange() > 0.0D ? controller.getScopeRange() : FALLBACK_RANGE;

        for (int y = -radius; y <= radius; y++) {
            int half = (int) Math.sqrt((double) (radius * radius - y * y));
            graphics.fill(cx - half, cy + y, cx + half + 1, cy + y + 1, SCOPE_BG);
        }

        drawCircle(graphics, cx, cy, radius, SCOPE_RING);
        drawCircle(graphics, cx, cy, radius * 2 / 3, SCOPE_RING_MINOR);
        drawCircle(graphics, cx, cy, radius / 3, SCOPE_RING_MINOR);
        graphics.hLine(cx - radius, cx + radius, cy, SCOPE_CROSS);
        graphics.vLine(cx, cy - radius, cy + radius, SCOPE_CROSS);

        graphics.drawString(this.font, "N", cx - 2, cy - radius - 9, TEXT_DIM);
        graphics.drawString(this.font, "S", cx - 2, cy + radius + 2, TEXT_DIM);
        graphics.drawString(this.font, "W", cx - radius - 8, cy - 4, TEXT_DIM);
        graphics.drawString(this.font, "E", cx + radius + 3, cy - 4, TEXT_DIM);

        List<ScopeContact> contacts = controller.getContacts();
        ScopeContact primary = primaryContact(contacts);

        if (primary != null) {
            int[] point = project(cx, cy, radius, primary.dx(), primary.dz(), range);
            drawLine(graphics, cx, cy, point[0], point[1], 0x66FF4444);
        }

        for (ScopeContact contact : contacts) {
            int[] point = project(cx, cy, radius, contact.dx(), contact.dz(), range);
            int color = colorOf(contact.kind());
            graphics.fill(point[0] - 1, point[1] - 1, point[0] + 2, point[1] + 2, color);
            if (contact.primary()) {
                graphics.renderOutline(point[0] - 3, point[1] - 3, 7, 7, 0xFFFFFFFF);
            }
        }

        if (contacts.isEmpty()) {
            graphics.drawString(this.font, Component.translatable("gui.taovmodernnaval.no_contacts"),
                    cx - 26, cy + radius + 12, TEXT_DIM);
        }

        renderLegend(graphics, left, top);
    }

    private void renderLegend(GuiGraphics graphics, int left, int top) {
        int y = top + 228;
        int x = left + 12;
        x = drawLegendEntry(graphics, x, y, ThreatRanking.ContactKind.INCOMING_MISSILE);
        x = drawLegendEntry(graphics, x, y, ThreatRanking.ContactKind.AIRCRAFT);
        drawLegendEntry(graphics, x, y, ThreatRanking.ContactKind.SURFACE);
    }

    private int drawLegendEntry(GuiGraphics graphics, int x, int y, ThreatRanking.ContactKind kind) {
        graphics.fill(x, y + 2, x + 3, y + 5, colorOf(kind));
        String label = Component.translatable(legendKey(kind)).getString();
        graphics.drawString(this.font, label, x + 6, y, TEXT_DIM);
        return x + 6 + this.font.width(label) + 12;
    }

    private static String legendKey(ThreatRanking.ContactKind kind) {
        return switch (kind) {
            case INCOMING_MISSILE -> "gui.taovmodernnaval.contact.missile";
            case AIRCRAFT -> "gui.taovmodernnaval.contact.air";
            case SURFACE -> "gui.taovmodernnaval.contact.surface";
            default -> "gui.taovmodernnaval.contact.unknown";
        };
    }

    // ---- 火力通道列 ----

    private void renderChannels(GuiGraphics graphics, TotalControlBlockEntity controller, int left, int top) {
        int x = left + CHANNEL_X;
        List<FireChannel> channels = controller.getChannels();
        ScopeContact primary = primaryContact(controller.getContacts());
        TargetDomain targetDomain = primary == null ? null : primary.domain();

        graphics.drawString(this.font, Component.translatable("gui.taovmodernnaval.fire_channels"),
                x, top + CHANNEL_HEADER_Y, PANEL_HEADER);
        if (channels.size() > MAX_CHANNELS) {
            graphics.drawString(this.font, "+" + (channels.size() - MAX_CHANNELS),
                    x + CHANNEL_WIDTH - 14, top + CHANNEL_HEADER_Y, TEXT_DIM);
        }
        if (channels.isEmpty()) {
            graphics.drawString(this.font, Component.translatable("gui.taovmodernnaval.no_channels"),
                    x + 2, top + CHANNEL_TOP + 6, TEXT_DIM);
        }

        for (int i = 0; i < MAX_CHANNELS; i++) {
            int barY = top + CHANNEL_TOP + i * CHANNEL_ROW_STEP;
            if (i >= channels.size()) {
                fireButtons[i].visible = false;
                groupButtons[i].visible = false;
                continue;
            }

            FireChannel channel = channels.get(i);
            boolean engageable = channel.engages(targetDomain);
            boolean ready = channel.canLaunch() && engageable;

            graphics.fill(x, barY, x + CHANNEL_WIDTH, barY + CHANNEL_BAR_HEIGHT, ready ? BAR_BG_READY : BAR_BG_IDLE);
            graphics.renderOutline(x, barY, CHANNEL_WIDTH, CHANNEL_BAR_HEIGHT,
                    engageable ? BORDER : BORDER_BLOCKED);

            String kindLabel = Component.translatable(channel.kind().translationKey()).getString();
            graphics.drawString(this.font, trim(kindLabel, 108), x + 4, barY + 3, engageable ? TEXT : TEXT_DIM);

            String ammoLabel = ammoLabel(channel);
            int ammoColor = channel.hasAmmo() ? TEXT_DIM : TEXT_WARN;
            graphics.drawString(this.font, trim(ammoLabel, 108), x + 4, barY + 16, ammoColor);

            fireButtons[i].visible = true;
            fireButtons[i].active = ready;
            fireButtons[i].setMessage(Component.translatable(channel.isMissileChannel()
                    ? "gui.taovmodernnaval.fire"
                    : "gui.taovmodernnaval.engage"));

            groupButtons[i].visible = true;
            groupButtons[i].setMessage(Component.translatable(channel.group().translationKey()));
        }
    }

    // ---- 指令 ----

    private void sendOrder(int index, int action) {
        List<FireChannel> channels = channels();
        if (index < 0 || index >= channels.size() || this.minecraft == null) {
            return;
        }
        BlockPos channelPos = channels.get(index).pos();
        ModNetwork.CHANNEL.sendToServer(new ServerboundFireChannelPacket(controllerPos, channelPos, action));
    }

    // ---- 数据读取 ----

    @Nullable
    private TotalControlBlockEntity controller() {
        if (this.minecraft == null || this.minecraft.level == null) {
            return null;
        }
        BlockEntity blockEntity = this.minecraft.level.getBlockEntity(controllerPos);
        return blockEntity instanceof TotalControlBlockEntity controller ? controller : null;
    }

    private List<FireChannel> channels() {
        TotalControlBlockEntity controller = controller();
        return controller == null ? List.of() : controller.getChannels();
    }

    @Nullable
    private static ScopeContact primaryContact(List<ScopeContact> contacts) {
        for (ScopeContact contact : contacts) {
            if (contact.primary()) {
                return contact;
            }
        }
        return null;
    }

    // ---- 显示辅助 ----

    private String ammoLabel(FireChannel channel) {
        if (!channel.kind().isMissileLauncher()) {
            return Component.translatable(channel.kind().ammoTranslationKey()).getString();
        }
        if (channel.ammoItemId().isBlank() || channel.ammoCount() <= 0) {
            return Component.translatable("gui.taovmodernnaval.no_ammo").getString();
        }
        ResourceLocation id = ResourceLocation.tryParse(channel.ammoItemId());
        Item item = id == null ? null : ForgeRegistries.ITEMS.getValue(id);
        String name = item == null
                ? channel.ammoItemId()
                : Component.translatable(new ItemStack(item).getDescriptionId()).getString();
        return name + " " + channel.ammoCount() + "/" + channel.ammoCapacity();
    }

    private String trim(String text, int maxWidth) {
        if (text == null || text.isEmpty() || this.font.width(text) <= maxWidth) {
            return text == null ? "" : text;
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            if (this.font.width(builder.toString() + text.charAt(i) + "…") > maxWidth) {
                break;
            }
            builder.append(text.charAt(i));
        }
        return builder + "…";
    }

    private static int colorOf(ThreatRanking.ContactKind kind) {
        return switch (kind) {
            case INCOMING_MISSILE -> 0xFFFF4040;
            case AIRCRAFT -> 0xFFFFD24A;
            case SURFACE -> 0xFF49D6FF;
            default -> 0xFF9AA7AD;
        };
    }

    /** 把相对偏移投影到示波器，超出量程的点钉在圆周上。 */
    private static int[] project(int cx, int cy, int radius, double dx, double dz, double range) {
        double safeRange = range <= 0.0D ? FALLBACK_RANGE : range;
        double sx = cx + dx / safeRange * radius;
        double sy = cy + dz / safeRange * radius;

        double vx = sx - cx;
        double vy = sy - cy;
        double distance = Math.sqrt(vx * vx + vy * vy);
        double limit = Math.max(1.0D, radius - 1.0D);
        if (distance > limit) {
            double scale = limit / distance;
            sx = cx + vx * scale;
            sy = cy + vy * scale;
        }
        return new int[]{(int) Math.round(sx), (int) Math.round(sy)};
    }

    /** 任意方向连线（1.20.1 的 GuiGraphics 只有横竖线，这里用 Bresenham 补齐）。 */
    private static void drawLine(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color) {
        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);
        int sx = x1 < x2 ? 1 : -1;
        int sy = y1 < y2 ? 1 : -1;
        int error = dx - dy;

        int x = x1;
        int y = y1;
        while (true) {
            graphics.fill(x, y, x + 1, y + 1, color);
            if (x == x2 && y == y2) {
                return;
            }
            int doubled = error * 2;
            if (doubled > -dy) {
                error -= dy;
                x += sx;
            }
            if (doubled < dx) {
                error += dx;
                y += sy;
            }
        }
    }

    private static void drawCircle(GuiGraphics graphics, int cx, int cy, int radius, int color) {
        if (radius <= 0) {
            return;
        }
        for (int y = -radius; y <= radius; y++) {
            int half = (int) Math.sqrt((double) (radius * radius - y * y));
            graphics.fill(cx - half, cy + y, cx - half + 1, cy + y + 1, color);
            graphics.fill(cx + half, cy + y, cx + half + 1, cy + y + 1, color);
        }
    }

    private int panelLeft() {
        return (this.width - PANEL_WIDTH) / 2;
    }

    private int panelTop() {
        return (this.height - PANEL_HEIGHT) / 2;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
