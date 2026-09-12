package TAOVMN.block;

import com.simibubi.create.content.contraptions.ContraptionMovementSetting;

import TAOVMN.block.entity.MissileBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import rbasamoyai.createbigcannons.cannon_control.contraption.AbstractMountedCannonContraption;
import rbasamoyai.createbigcannons.cannons.CannonContraptionProviderBlock;
import rbasamoyai.createbigcannons.crafting.casting.CannonCastShape;
import rbasamoyai.createbigcannons.munitions.config.components.BallisticPropertiesComponent;
import rbasamoyai.createbigcannons.munitions.config.components.EntityDamagePropertiesComponent;
import riftyboi.cbcmodernwarfare.index.CBCModernWarfareMunitionPropertiesHandlers;
import riftyboi.cbcmodernwarfare.munitions.contraptions.MunitionsPhysicsContraptionEntity;
import riftyboi.cbcmodernwarfare.munitions.munitions_contraption_launcher.propulsion.IThrusterBlock;

public class MissileBlock extends BaseEntityBlock implements ContraptionMovementSetting.IMovementSettingProvider,
    IThrusterBlock, CannonContraptionProviderBlock {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    public MissileBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MissileBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public boolean canBeLoaded(BlockState state, Direction.Axis axis) {
        return state.getValue(FACING).getAxis() == axis;
    }

    @Override
    public BlockState onCannonRotate(BlockState state, Direction.Axis axis, Rotation rotation) {
        Direction facing = state.getValue(FACING);
        for (int turns = 0; turns < rotation.ordinal(); turns++) {
            facing = facing.getClockWise(axis);
        }
        return state.setValue(FACING, facing);
    }

    @Override
    public StructureTemplate.StructureBlockInfo getHandloadingInfo(ItemStack stack, BlockPos pos, Direction direction) {
        CompoundTag blockEntityTag = null;
        if (stack.hasTag() && stack.getTag().contains("BlockEntityTag", 10)) {
            blockEntityTag = stack.getTag().getCompound("BlockEntityTag").copy();
            blockEntityTag.remove("x");
            blockEntityTag.remove("y");
            blockEntityTag.remove("z");
        }
        return new StructureTemplate.StructureBlockInfo(pos, defaultBlockState().setValue(FACING, direction.getOpposite()), blockEntityTag);
    }

    @Override
    public ItemStack getExtractedItem(StructureTemplate.StructureBlockInfo blockInfo) {
        ItemStack stack = new ItemStack(this);
        if (blockInfo.nbt() != null) {
            stack.getOrCreateTag().put("BlockEntityTag", blockInfo.nbt().copy());
        }
        return stack;
    }

    @Override
    public Direction.Axis getAxis(BlockState state) {
        return state.getValue(FACING).getAxis();
    }

    @Override
    public ContraptionMovementSetting getContraptionMovementSetting() {
        return ContraptionMovementSetting.MOVABLE;
    }

    private riftyboi.cbcmodernwarfare.munitions.munitions_contraption_launcher.config.FuelThrusterProperties getThrusterProperties() {
        return CBCModernWarfareMunitionPropertiesHandlers.SOLID_FUEL_THRUSTER.getPropertiesOf(defaultBlockState());
    }

    @Override
    public void spawnParticles(BlockPos pos, MunitionsPhysicsContraptionEntity contraption) {
        contraption.level().addParticle(net.minecraft.core.particles.ParticleTypes.FLAME,
                pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                0.0D, 0.0D, 0.0D);
    }

    @Override
    public BallisticPropertiesComponent getBallistics() {
        return getThrusterProperties().ballisticPropertiesComponent();
    }

    @Override
    public EntityDamagePropertiesComponent getDamage() {
        return getThrusterProperties().entityDamagePropertiesComponent();
    }

    @Override
    public float addedGravity() {
        return getThrusterProperties().thrusterProperties().addedGravity();
    }

    @Override
    public int storedFuel() {
        return getThrusterProperties().thrusterProperties().storedFuel();
    }

    @Override
    public float addedSpread() {
        return getThrusterProperties().thrusterProperties().addedSpread();
    }

    @Override
    public float burnRate() {
        return getThrusterProperties().thrusterProperties().burnRate();
    }

    @Override
    public float thrust() {
        return getThrusterProperties().thrusterProperties().thrust();
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return null;
    }

    @Override
    public AbstractMountedCannonContraption getCannonContraption() {
        return new TAOVMN.compat.cbc.MountedLaunchTubeContraption();
    }

    @Override
    public Direction getFacing(BlockState state) {
        return state.getValue(FACING);
    }

    @Override
    public boolean canConnectToSide(BlockState state, Direction side) {
        return state.getValue(FACING).getAxis() == side.getAxis();
    }

    @Override
    public CannonCastShape getCannonShape() {
        return CannonCastShape.VERY_SMALL;
    }

    @Override
    public boolean isComplete(BlockState state) {
        return true;
    }
}