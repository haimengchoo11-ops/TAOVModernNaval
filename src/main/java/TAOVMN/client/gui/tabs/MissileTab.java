package TAOVMN.client.gui.tabs;

import TAOVMN.client.gui.ITab;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * 导弹配置标签页
 */
public class MissileTab implements ITab {
    private Screen screen;
    private Checkbox autoTargetCheckbox;

    public MissileTab(Screen screen) {
        this.screen = screen;
    }

    @Override
    public String getTitle() {
        return "Missile";
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 标题
        guiGraphics.drawString(screen.getMinecraft().font, "导弹配置", 30, 50, 0xFFFFFF);

        // 自动瞄准选项
        guiGraphics.drawString(screen.getMinecraft().font, "自动瞄准:", 30, 80, 0xAAAAAA);
        if (autoTargetCheckbox != null) {
            autoTargetCheckbox.render(guiGraphics, mouseX, mouseY, partialTick);
        }

        // 导弹类型说明
        guiGraphics.drawString(screen.getMinecraft().font, "当前导弹: P1 导弹", 30, 110, 0xAAAAAA);
        guiGraphics.drawString(screen.getMinecraft().font, "状态: 就绪", 30, 130, 0xFF00FF00);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        return false;
    }

    @Override
    public void init() {
        autoTargetCheckbox = new Checkbox(50, 78, 20, 20, Component.literal("自动瞄准"), false);
    }

    public boolean isAutoTarget() {
        return autoTargetCheckbox != null && autoTargetCheckbox.selected();
    }
}
