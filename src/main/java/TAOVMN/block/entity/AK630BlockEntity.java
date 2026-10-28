package TAOVMN.block.entity;

import TAOVMN.registry.ModBlockEntities;
import TAOVMN.util.LauncherKind;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * AK-630 六管 30mm 近防炮。
 *
 * <p>逻辑全部在 {@link AbstractCiwsBlockEntity}，这里只声明本型号的性能参数：
 * 搜索 54 格、炮塔插值 4°/tick、每 4 tick 一发、单发 4 点伤害、炮口抬升 0.65 格。
 */
public class AK630BlockEntity extends AbstractCiwsBlockEntity {
    private static final CiwsSpec SPEC = new CiwsSpec(
            54.0D,
            4.0F,
            4,
            4.0F,
            0.65D,
            "AK630TURRET",
            "AK630CANNON",
            LauncherKind.AK630);

    public AK630BlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public AK630BlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.AK630_BLOCK.get(), pos, state);
    }

    @Override
    protected CiwsSpec spec() {
        return SPEC;
    }
}
