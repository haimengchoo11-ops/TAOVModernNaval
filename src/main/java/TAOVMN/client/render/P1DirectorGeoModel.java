package TAOVMN.client.render;

import TAOVMN.TAOVModernNaval;
import TAOVMN.block.entity.P1DirectorBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.model.GeoModel;

/**
 * P-1 火控指挥仪模型。
 *
 * <p>天线转速 3°/tick（约 4 秒一圈，是 MR-800 的两倍，体现"火控跟踪模式"）。
 * 旋转骨骼取 {@code bone2}：p1_director.geo.json 里除 root 之外只有一个骨头，
 * 也就是整机；转它就是整台指挥仪在基座上转动。
 */
@SuppressWarnings("removal")
public class P1DirectorGeoModel extends GeoModel<P1DirectorBlockEntity> {
    /** 负责绕 Y 轴旋转的骨骼。模型更新后骨骼名若变化，改这里即可。 */
    private static final String ROTATING_BONE = "bone2";
    /** 天线转速（度/tick），火控模式下快于普通雷达。 */
    private static final float DEGREES_PER_TICK = 3.0F;

    @Override
    public ResourceLocation getModelResource(P1DirectorBlockEntity animatable) {
        return new ResourceLocation(TAOVModernNaval.MODID, "geo/p1_director.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(P1DirectorBlockEntity animatable) {
        return new ResourceLocation(TAOVModernNaval.MODID, "textures/block/p1_director.png");
    }

    @Override
    public ResourceLocation getAnimationResource(P1DirectorBlockEntity animatable) {
        // 动画仍借用 mr800 的空壳（只做骨骼旋转，不依赖关键帧动画）
        return new ResourceLocation(TAOVModernNaval.MODID, "animations/mr800.animation.json");
    }

    @Override
    public void setCustomAnimations(P1DirectorBlockEntity animatable, long instanceId,
            AnimationState<P1DirectorBlockEntity> animationState) {
        CoreGeoBone turntable = getAnimationProcessor().getBone(ROTATING_BONE);
        if (turntable != null) {
            turntable.setRotY(AntennaRotation.radians(animatable, DEGREES_PER_TICK));
        }
    }
}
