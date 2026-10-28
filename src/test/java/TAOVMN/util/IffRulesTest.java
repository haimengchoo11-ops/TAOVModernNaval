package TAOVMN.util;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 归属比较的单元测试。
 *
 * <p>只覆盖纯函数 {@link IffRules#isSameOwner(UUID, UUID)}；{@code canEngage} 依赖
 * {@code Entity}，无法在无服务端的测试环境里构造，改由代码审阅 + 游戏内验证。
 */
class IffRulesTest {

    private static final UUID ALICE = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID BOB = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Test
    void identicalOwnersAreFriendly() {
        assertTrue(IffRules.isSameOwner(ALICE, ALICE), "同一玩家的两枚弹必须被视为友军");
    }

    @Test
    void differentOwnersAreHostile() {
        assertFalse(IffRules.isSameOwner(ALICE, BOB), "不同玩家之间必须可以互相拦截");
    }

    @Test
    void unknownOwnerIsNeverTreatedAsFriendly() {
        // 归属未知时必须"不保护"，否则 CBC 悬挂炮 / 结构生成装置会互相免疫，反导整体失效
        assertAll(
                () -> assertFalse(IffRules.isSameOwner(null, null), "(null, null) 不能算同阵营"),
                () -> assertFalse(IffRules.isSameOwner(null, ALICE), "(null, uuid) 不能算同阵营"),
                () -> assertFalse(IffRules.isSameOwner(ALICE, null), "(uuid, null) 不能算同阵营"));
    }

    @Test
    void comparisonIsUuidBasedNotIdentityBased() {
        // 反序列化 / 网络传输会产生不同的 UUID 对象，比较必须走 equals
        UUID copyOfAlice = UUID.fromString(ALICE.toString());
        assertTrue(IffRules.isSameOwner(ALICE, copyOfAlice));
    }

    @Test
    void nullOrMissingTargetIsNeverHostile() {
        assertAll(
                () -> assertFalse(IffRules.isHostile(null), "null 不是交战对象"),
                () -> assertFalse(IffRules.canEngage(ALICE, null), "null 目标不可交战"));
    }
}
