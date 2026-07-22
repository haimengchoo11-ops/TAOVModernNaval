package TAOVMN.client.render;

import TAOVMN.entity.P1MissileEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.phys.Vec3;

public class P1MissileRenderer extends EntityRenderer<P1MissileEntity> {
    public P1MissileRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(P1MissileEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();

        float yaw = Mth.lerp(partialTicks, entity.yRotO, entity.getYRot());
        float pitch = -Mth.lerp(partialTicks, entity.xRotO, entity.getXRot());
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - yaw));
        // 导弹模型顶端（+Y）为鼻锥，需减去90°使顶部对准飞行方向
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch - 90.0F));

        // 绕导弹长轴（模型Y轴）自旋，放在方向对齐之后（最内层）
        float roll = (entity.tickCount + partialTicks) * 22.0F;
        poseStack.mulPose(Axis.YP.rotationDegrees(roll));

        float fuzeFactor = Mth.clamp(entity.getRemainingFuzeTicks() / 200.0F, 0.0F, 1.0F);
        float pulse = 1.0F + (1.0F - fuzeFactor) * 0.12F * Mth.sin((entity.tickCount + partialTicks) * 0.9F);

        poseStack.scale(1.25F * pulse, 1.25F, 1.25F * pulse);
        poseStack.translate(0.0D, -0.25D, 0.0D);

        Minecraft.getInstance().getItemRenderer().renderStatic(
                entity.getItem(),
                ItemDisplayContext.FIXED,
                packedLight,
                OverlayTexture.NO_OVERLAY,
                poseStack,
                buffer,
                entity.level(),
                entity.getId());

        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(P1MissileEntity entity) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}