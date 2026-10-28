package TAOVMN.content;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class NavalSystemsTest {
    @Test
    void findTargetsReturnsEmptyWhenNoLevelProvided() {
        assertTrue(NavalSystems.findTargets(null, BlockPos.ZERO, 32.0D).isEmpty());
    }
}
