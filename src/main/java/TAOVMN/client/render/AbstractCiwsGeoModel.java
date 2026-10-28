package TAOVMN.client.render;

import TAOVMN.block.entity.AbstractCiwsBlockEntity;
import net.minecraft.util.Mth;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.model.GeoModel;

/**
 * 近防炮 / 舰炮的公共渲染模型：只负责把服务端同步来的目标角度写进 GeckoLib 骨骼。
 *
 * <p>骨骼名来自各自 {@code CiwsSpec}，因此 AK-230 将来换成独立骨骼时只需改参数，
 * 不必再复制一份模型类。子类只需提供 geo / 贴图 / 动画三个资源路径。
 */
@SuppressWarnings("removal")
public abstract class AbstractCiwsGeoModel<T extends AbstractCiwsBlockEntity> extends GeoModel<T> {

    @Override
    public void setCustomAnimations(T animatable, long instanceId, AnimationState<T> animationState) {
        CoreGeoBone turret = getAnimationProcessor().getBone(animatable.turretBone());
        CoreGeoBone cannon = getAnimationProcessor().getBone(animatable.cannonBone());

        if (turret != null) {
            turret.setRotY(animatable.clientYaw * Mth.DEG_TO_RAD);
        }
        if (cannon != null) {
            cannon.setRotX(animatable.clientPitch * Mth.DEG_TO_RAD);
        }
    }
}
