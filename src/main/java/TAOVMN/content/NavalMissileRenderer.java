package TAOVMN.content;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class NavalMissileRenderer extends EntityRenderer<NavalMissileEntity> {
    public NavalMissileRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(NavalMissileEntity entity, float entityYaw, float partialTicks, PoseStack poseStack,
                      MultiBufferSource buffer, int packedLight) {
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);

        poseStack.pushPose();
        poseStack.translate(0.0D, 0.0D, 0.0D);
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F - entity.getYRot()));
        poseStack.mulPose(Axis.ZP.rotationDegrees(-entity.getXRot()));

        ItemStack stack = new ItemStack(NavalItems.NAVAL_MISSILE.get());
        Minecraft.getInstance().getItemRenderer().renderStatic(
                stack,
                ItemDisplayContext.GROUND,
                packedLight,
                OverlayTexture.NO_OVERLAY,
                poseStack,
                buffer,
                entity.level(),
                (int) entity.getId());

        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(NavalMissileEntity entity) {
        return new ResourceLocation("taovmodernnaval", "textures/item/naval_missile.png");
    }
}
