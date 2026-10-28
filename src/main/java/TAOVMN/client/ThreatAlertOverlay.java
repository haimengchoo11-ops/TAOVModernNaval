package TAOVMN.client;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

/**
 * 屏幕顶部居中的「来袭导弹」告警条。
 *
 * <p>在此之前，一枚当量 22、速度 1.8 格/tick 的反舰弹会<b>毫无提示</b>地落到舰上——
 * 玩家唯一能察觉的方式是自己被炸死。这个叠加层补上最小的可读性：
 * 数量 + 闪烁 + 淡出，并且尊重 F1（{@code hideGui}）。
 */
public class ThreatAlertOverlay implements IGuiOverlay {
    private static final int TOP = 10;
    private static final int PADDING_X = 8;
    private static final int BACKGROUND_RGB = 0x5A0000;
    private static final int TEXT_RGB = 0xFF4444;
    /** 闪烁周期（毫秒）与其内"亮"的占比。 */
    private static final long BLINK_PERIOD_MILLIS = 500L;
    private static final long BLINK_ON_MILLIS = 350L;
    /** 不闪烁时的亮度系数。 */
    private static final float DIM_FACTOR = 0.45F;

    @Override
    public void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
        if (!ClientThreatAlert.isActive()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui) {
            return;
        }

        Font font = gui.getFont();
        String text = Component.translatable("gui.taovmodernnaval.threat_alert",
                ClientThreatAlert.missileCount()).getString();

        float opacity = ClientThreatAlert.opacity();
        boolean blinkOn = (Util.getMillis() % BLINK_PERIOD_MILLIS) < BLINK_ON_MILLIS;
        int bannerAlpha = (int) (0xAA * opacity);
        int textAlpha = (int) (0xFF * opacity * (blinkOn ? 1.0F : DIM_FACTOR));

        int width = font.width(text);
        int x = (screenWidth - width) / 2;
        int y = TOP;

        graphics.fill(x - PADDING_X, y - 4, x + width + PADDING_X, y + 12,
                (bannerAlpha << 24) | BACKGROUND_RGB);
        graphics.drawString(font, text, x, y, (textAlpha << 24) | TEXT_RGB, false);
    }
}
