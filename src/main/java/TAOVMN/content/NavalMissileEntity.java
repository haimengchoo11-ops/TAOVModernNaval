package TAOVMN.content;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class NavalMissileEntity extends Projectile {
    private static final double MAX_SPEED = 0.95D;
    private static final double SEEK_STRENGTH = 0.12D;
    private static final int MAX_LIFE = 200;
    /** 爆炸当量（TNT 当量）。 */
    private static final float BLAST_POWER = 4.0F;

    private int lifeTicks;

    public NavalMissileEntity(EntityType<? extends Projectile> type, Level level) {
        super(type, level);
    }

    public NavalMissileEntity(Level level, double x, double y, double z) {
        this(NavalEntityTypes.NAVAL_MISSILE.get(), level);
        this.setPos(x, y, z);
        this.setDeltaMovement(0.0D, 0.0D, 0.0D);
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compoundTag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compoundTag) {
    }

    public void launch(Vec3 direction) {
        Vec3 motion = direction.normalize().scale(MAX_SPEED);
        this.setDeltaMovement(motion);
        updateRotation(motion);
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide) {
            lifeTicks++;
            if (lifeTicks >= MAX_LIFE) {
                this.discard();
                return;
            }

            // 仅服务器端制导：向 24 格内最近目标修正航向
            Vec3 velocity = this.getDeltaMovement();
            LivingEntity target = findNearestTarget();
            if (target != null) {
                Vec3 desired = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D)
                        .subtract(this.position()).normalize();
                velocity = velocity.add(desired.scale(SEEK_STRENGTH));
            }

            if (velocity.lengthSqr() > MAX_SPEED * MAX_SPEED) {
                velocity = velocity.normalize().scale(MAX_SPEED);
            }
            this.setDeltaMovement(velocity);
        }

        updateRotation(this.getDeltaMovement());

        // 命中检测（方块 + 实体，沿飞行轨迹射线检测），命中后起爆（爆炸仅服务器端执行）
        HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
        if (hit.getType() != HitResult.Type.MISS) {
            this.onHit(hit);
            if (!this.isAlive()) {
                return;
            }
        }

        // 使用 move 保持与碰撞箱一致的位移（替代 setPos，避免穿墙）
        this.move(MoverType.SELF, this.getDeltaMovement());

        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SMOKE,
                    this.getX(), this.getY(), this.getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        detonate();
    }

    @Override
    protected void onHitBlock(net.minecraft.world.phys.BlockHitResult result) {
        super.onHitBlock(result);
        detonate();
    }

    private void detonate() {
        if (this.level().isClientSide || this.isRemoved()) {
            return;
        }
        this.level().explode(this, this.getX(), this.getY(), this.getZ(), BLAST_POWER,
                Level.ExplosionInteraction.TNT);
        this.discard();
    }

    private void updateRotation(Vec3 velocity) {
        if (velocity.lengthSqr() < 1.0E-6D) {
            return;
        }
        double horizontal = velocity.horizontalDistance();
        this.setYRot((float) (Mth.atan2(velocity.x, velocity.z) * Mth.RAD_TO_DEG));
        this.setXRot((float) (Mth.atan2(velocity.y, horizontal) * Mth.RAD_TO_DEG));
        this.yRotO = this.getYRot();
        this.xRotO = this.getXRot();
    }

    private LivingEntity findNearestTarget() {
        LivingEntity nearest = null;
        double bestDistance = Double.MAX_VALUE;

        for (LivingEntity entity : this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(24.0D))) {
            if (entity == this.getOwner() || !entity.isAlive()) {
                continue;
            }

            double distance = entity.distanceToSqr(this);
            if (distance < bestDistance) {
                bestDistance = distance;
                nearest = entity;
            }
        }

        return nearest;
    }

    /** 供渲染端/调试使用：导弹当前是否仍在飞行。 */
    public boolean isFlying() {
        return this.isAlive() && lifeTicks < MAX_LIFE;
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        return super.canHitEntity(target) && target != this.getOwner();
    }
}
