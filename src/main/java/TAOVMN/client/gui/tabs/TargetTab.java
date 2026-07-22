package TAOVMN.client.gui.tabs;

import TAOVMN.client.gui.ITab;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * 目标设置标签页
 */
public class TargetTab implements ITab {
    private EditBox targetXInput;
    private EditBox targetYInput;
    private EditBox targetZInput;
    private Screen screen;

    public TargetTab(Screen screen) {
        this.screen = screen;
    }

    @Override
    public String getTitle() {
        return "Target";
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 标题
        guiGraphics.drawString(screen.getMinecraft().font, "目标坐标", 30, 50, 0xFFFFFF);

        // X 坐标输入框
        guiGraphics.drawString(screen.getMinecraft().font, "X:", 30, 70, 0xAAAAAA);
        if (targetXInput != null) {
            targetXInput.render(guiGraphics, mouseX, mouseY, partialTick);
        }

        // Y 坐标输入框
        guiGraphics.drawString(screen.getMinecraft().font, "Y:", 30, 100, 0xAAAAAA);
        if (targetYInput != null) {
            targetYInput.render(guiGraphics, mouseX, mouseY, partialTick);
        }

        // Z 坐标输入框
        guiGraphics.drawString(screen.getMinecraft().font, "Z:", 30, 130, 0xAAAAAA);
        if (targetZInput != null) {
            targetZInput.render(guiGraphics, mouseX, mouseY, partialTick);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        return false;
    }

    @Override
    public void init() {
        targetXInput = new EditBox(screen.getMinecraft().font, 60, 68, 80, 16, Component.literal("X"));
        targetXInput.setMaxLength(10);
        targetXInput.setValue("0");

        targetYInput = new EditBox(screen.getMinecraft().font, 60, 98, 80, 16, Component.literal("Y"));
        targetYInput.setMaxLength(10);
        targetYInput.setValue("0");

        targetZInput = new EditBox(screen.getMinecraft().font, 60, 128, 80, 16, Component.literal("Z"));
        targetZInput.setMaxLength(10);
        targetZInput.setValue("0");
    }

    public float getTargetX() {
        try {
            return Float.parseFloat(targetXInput.getValue());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public float getTargetY() {
        try {
            return Float.parseFloat(targetYInput.getValue());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public float getTargetZ() {
        try {
            return Float.parseFloat(targetZInput.getValue());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
