package TAOVMN.client.gui;

import net.minecraft.client.gui.GuiGraphics;

/**
 * 标签页接口
 */
public interface ITab {
    /**
     * 获取标签页标题
     */
    String getTitle();

    /**
     * 渲染标签页内容
     */
    void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick);

    /**
     * 处理鼠标点击事件
     */
    boolean mouseScrolled(double mouseX, double mouseY, double delta);

    /**
     * 初始化标签页中的小部件
     */
    void init();
}
