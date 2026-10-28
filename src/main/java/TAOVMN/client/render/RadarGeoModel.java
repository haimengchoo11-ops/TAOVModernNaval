package TAOVMN.client.render;

import TAOVMN.TAOVModernNaval;
import TAOVMN.block.entity.RadarBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.model.GeoModel;

/**
 * MR-800 舰载雷达模型。
 *
 * <p>天线转速 1.5°/tick（约 8 秒转一圈）。旋转骨骼取 {@code bone2}：它是桅杆顶端那个
 * 1 个 cube 的转台，{@code bone3}/{@code bone4}（反射面与馈源）都挂在它下面，
 * 因此转 {@code bone2} 就是整副天线绕桅杆旋转。
 */
@SuppressWarnings("removal")
public class RadarGeoModel extends GeoModel<RadarBlockEntity> {
    /** 负责绕 Y 轴旋转的骨骼。模型更新后骨骼名若变化，改这里即可。 */
    private static final String ROTATING_BONE = "bone2";
    /** 天线转速（度/tick）。 */
    private static final float DEGREES_PER_TICK = 1.5F;

    @Override
    public ResourceLocation getModelResource(RadarBlockEntity animatable) {
        return new ResourceLocation(TAOVModernNaval.MODID, "geo/mr800.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(RadarBlockEntity animatable) {
        return new ResourceLocation(TAOVModernNaval.MODID, "textures/block/mr800.png");
    }

    @Override
    public ResourceLocation getAnimationResource(RadarBlockEntity animatable) {
        return new ResourceLocation(TAOVModernNaval.MODID, "animations/mr800.animation.json");
    }

    @Override
    public void setCustomAnimations(RadarBlockEntity animatable, long instanceId,
            AnimationState<RadarBlockEntity> animationState) {
        CoreGeoBone turntable = getAnimationProcessor().getBone(ROTATING_BONE);
        if (turntable != null) {
            turntable.setRotY(AntennaRotation.radians(animatable, DEGREES_PER_TICK));
        }
    }
}
