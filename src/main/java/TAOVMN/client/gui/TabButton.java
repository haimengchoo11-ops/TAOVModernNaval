package TAOVMN.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/**
 * 标签页按钮
 */
public class TabButton extends Button {
    private boolean active;

    public TabButton(int x, int y, int width, int height, Component component, Button.OnPress onPress) {
        super(x, y, width, height, component, onPress, DEFAULT_NARRATION);
        this.active = false;
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 背景颜色
        int bgColor = this.active ? 0xFF4488FF : 0xFF888888;
        guiGraphics.fill(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), bgColor);

        // 边框
        int borderColor = this.isHovered ? 0xFFFFFFFF : 0xFF000000;
        guiGraphics.fill(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + 1, borderColor);
        guiGraphics.fill(this.getX(), this.getY() + this.getHeight() - 1, this.getX() + this.getWidth(), this.getY() + this.getHeight(), borderColor);

        // 文字
        int textColor = this.active ? 0xFFFFFF : 0xFFCCCCCC;
        guiGraphics.drawCenteredString(Minecraft.getInstance().font, this.getMessage(), this.getX() + this.getWidth() / 2, this.getY() + (this.getHeight() - 8) / 2, textColor);
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isActive() {
        return active;
    }
}
