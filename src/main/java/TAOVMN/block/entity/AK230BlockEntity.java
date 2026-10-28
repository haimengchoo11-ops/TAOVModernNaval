package TAOVMN.block.entity;

import TAOVMN.registry.ModBlockEntities;
import TAOVMN.util.LauncherKind;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * AK-230 双联 30mm 舰炮。
 *
 * <p>逻辑与 AK-630 完全一致（见 {@link AbstractCiwsBlockEntity}），只有性能参数不同：
 * 搜索 40 格、炮塔插值 5°/tick、每 2 tick 一发、单发 3 点伤害、炮口抬升 0.55 格。
 *
 * <p>模型骨骼名沿用 {@code AK630TURRET} / {@code AK630CANNON}（ak230.geo.json 与 ak630 共用命名）。
 */
public class AK230BlockEntity extends AbstractCiwsBlockEntity {
    private static final CiwsSpec SPEC = new CiwsSpec(
            40.0D,
            5.0F,
            2,
            3.0F,
            0.55D,
            "AK630TURRET",
            "AK630CANNON",
            LauncherKind.AK230);

    public AK230BlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public AK230BlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.AK230_BLOCK.get(), pos, state);
    }

    @Override
    protected CiwsSpec spec() {
        return SPEC;
    }
}
