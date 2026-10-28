package TAOVMN.client.render;

import java.util.Map;

/**
 * 资源命名规范（以素材库 asset stor/WEAPON/MISSILE 为准）。
 *
 * <p>P 系列导弹的模型/贴图/动画统一使用 {@code p-<型号>} 作为资源名，与库内
 * {@code ANTI SHIP/P120/p-120.geo.json}、{@code ANTI SHIP/P700/p-700.png} 保持一致：
 * 方块注册名 {@code p270_missile} -&gt; {@code geo/p-270.geo.json}、
 * {@code textures/item/p-270.png}、{@code animations/p-270.animation.json}。
 *
 * <p>方块 ID、物品 ID、语言键仍然使用 {@code pX_missile}（注册名不变），只有资源文件名走本规范。
 */
final class MissileAssetNames {
    private static final String MISSILE_SUFFIX = "_missile";
    private static final String DASH_PREFIX = "p-";
    private static final String DEFAULT_BASE = "p-1";

    /**
     * 注册名基 -&gt; 资源基名的显式别名。
     *
     * <p>{@code s300_missile} 的弹体模型在素材库里叫 {@code 5v55rm}（S-300 的弹种型号
     * 5V55RM），与发射车/发射架目录名 {@code s300} 不同，故需要显式映射。
     */
    private static final Map<String, String> ALIASES = Map.of(
            "s300", "5v55rm");

    private MissileAssetNames() {
    }

    /**
     * 注册名 -&gt; 资源基名：{@code p1_missile -> p-1}、{@code p270_missile -> p-270}。
     * 非 P 系列名称（如 {@code ak630}）原样返回。
     */
    static String assetBase(String registryName) {
        if (registryName == null || registryName.isBlank()) {
            return DEFAULT_BASE;
        }

        String base = registryName.endsWith(MISSILE_SUFFIX)
                ? registryName.substring(0, registryName.length() - MISSILE_SUFFIX.length())
                : registryName;

        String alias = ALIASES.get(base);
        if (alias != null) {
            return alias;
        }

        if (base.length() > 1 && base.charAt(0) == 'p' && Character.isDigit(base.charAt(1))) {
            return DASH_PREFIX + base.substring(1);
        }

        return base;
    }
}
