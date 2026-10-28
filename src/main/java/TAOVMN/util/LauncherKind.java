package TAOVMN.util;

import java.util.Locale;

/**
 * 火力通道对应的发射装置类型。
 *
 * <p>{@code missileLauncher} 区分"有弹可打"的导弹发射装置与靠弹链持续射击的近防炮：
 * 界面上前者显示剩余弹量与发射按钮，后者显示炮种与"交战"按钮。
 */
public enum LauncherKind {
    RAK_LAUNCHER("rak_launcher", true),
    LAUNCH_TUBE("launch_tube", true),
    NAVAL_LAUNCH_TUBE("naval_launch_tube", true),
    AK630("ak630", false),
    AK230("ak230", false);

    private final String key;
    private final boolean missileLauncher;

    LauncherKind(String key, boolean missileLauncher) {
        this.key = key;
        this.missileLauncher = missileLauncher;
    }

    /** 稳定的注册名键（与方块注册名一致）。 */
    public String key() {
        return key;
    }

    /** 发射装置类型的语言键。 */
    public String translationKey() {
        return "launcher_type.taovmodernnaval." + key;
    }

    /** 近防炮等固定弹链的语言键；导弹发射装置不使用该键。 */
    public String ammoTranslationKey() {
        return "ammo.taovmodernnaval." + key;
    }

    public boolean isMissileLauncher() {
        return missileLauncher;
    }

    /** 按名称反查（NBT 读取用）；未知值回退到 {@code fallback}。 */
    public static LauncherKind byName(String name, LauncherKind fallback) {
        if (name == null || name.isBlank()) {
            return fallback;
        }
        String normalized = name.trim().toUpperCase(Locale.ROOT);
        for (LauncherKind kind : values()) {
            if (kind.name().equals(normalized)) {
                return kind;
            }
        }
        return fallback;
    }
}
