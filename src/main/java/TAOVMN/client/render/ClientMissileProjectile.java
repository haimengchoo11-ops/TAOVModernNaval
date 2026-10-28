package TAOVMN.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import net.shao.shaolib.projectile.ShaoEntityType;
import net.shao.shaolib.render.ClientProjectile;

public final class ClientMissileProjectile extends ClientProjectile {
    private final String itemPath;

    public ClientMissileProjectile(ShaoEntityType<?> type, ResourceKey<Level> dimension, String itemPath) {
        super(type, dimension);
        this.itemPath = itemPath;
    }

    @Override
    public void tick() {
        proxy().clientTick();
    }

    @Override
    public void render(float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        var clientLevel = clientLevel();
        if (clientLevel == null) {
            return;
        }
        var item = ForgeRegistries.ITEMS.getValue(new ResourceLocation("taovmodernnaval", itemPath));
        if (item == null) {
            return;
        }

        poseStack.pushPose();
        var motion = velocity();
        double horizontal = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
        float yaw = (float) (Math.atan2(motion.x, motion.z) * 180.0D / Math.PI);
        float pitch = (float) (Math.atan2(motion.y, horizontal) * 180.0D / Math.PI);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch - 90.0F));
        poseStack.translate(0.0D, -0.25D, 0.0D);
        Minecraft.getInstance().getItemRenderer().renderStatic(new ItemStack(item), ItemDisplayContext.FIXED,
                packedLight, OverlayTexture.NO_OVERLAY, poseStack, buffer, clientLevel, (int) id());
        poseStack.popPose();
    }
}