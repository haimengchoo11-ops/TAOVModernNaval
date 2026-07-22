package TAOVMN.client.render;

import TAOVMN.entity.P1MissileEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class P1MissileGeoRenderer extends GeoEntityRenderer<P1MissileEntity> {
    public P1MissileGeoRenderer(EntityRendererProvider.Context context) {
        super(context, new P1MissileGeoModel());
        this.shadowRadius = 0.2F;
    }

    @Override
    public void render(P1MissileEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        float yaw = Mth.lerp(partialTick, entity.yRotO, entity.getYRot());
        float pitch = -Mth.lerp(partialTick, entity.xRotO, entity.getXRot());
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch - 90.0F));
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }
}
