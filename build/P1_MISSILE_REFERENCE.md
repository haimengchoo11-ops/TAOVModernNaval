# P1 导弹系统完整代码参考

## 类层级结构

```
HEShellProjectile (CreateBigCannons)
  ↓
P1MissileEntity (TAOVMN)
  ├── 实现: GeoEntity (GeckoLib 动画)
  ├── 字段: 导弹参数、追踪目标、引信状态
  ├── 方法: tick()、追踪逻辑、爆炸处理
  └── 回调: onImpact()、onClip()
```

---

## 核心常量定义

```java
// 时间控制
private static final int SAFETY_DELAY_TICKS = 30;        // 发射后安全延迟 (1.5秒)
private static final int MAX_MISSILE_LIFETIME = 600;     // 最大寿命 (30秒)
private static final int TARGET_MEMORY_TICKS = 100;      // 目标记忆时长 (5秒)
private static final int DEFAULT_LOCK_HOLD_TICKS = 16;   // 锁定保持时长 (0.8秒)

// 性能参数
private static final float MIN_CRUISE_SPEED_RATIO = 1.8F;        // 最小巡航速度比例
private static final float ACCELERATION_PER_TICK = 0.11F;        // 每 tick 加速度
private static final float PHYSICS_TARGET_RANGE_BONUS = 400.0F;  // 物理追踪范围加成

// 追踪调优
private static final double TARGET_SWITCH_DISTANCE = 14.0D;       // 目标切换距离
private static final double TRACK_SMOOTH_BLEND = 0.28D;          // 轨迹平滑混合系数
private static final double TRACK_SWITCH_BLEND = 0.55D;          // 轨迹切换混合系数
private static final double DEFAULT_SWITCH_ADVANTAGE_DISTANCE = 8.0D;   // 切换优势距离
private static final double DEFAULT_FORCE_SWITCH_DISTANCE = 22.0D;      // 强制切换距离
private static final double PREDICTIVE_LEAD_DISTANCE = 0.35D;    // 预测铅量
```

---

## 关键字段

### NBT 同步字段 (EntityDataAccessor)
```java
// 发射参数
private static final EntityDataAccessor<Float> DATA_MAX_SPEED;        // 最大速度
private static final EntityDataAccessor<Float> DATA_STEERING;         // 转向增益
private static final EntityDataAccessor<Float> DATA_BLAST_POWER;      // 爆炸威力
private static final EntityDataAccessor<Float> DATA_SEEK_RANGE;       // 搜索范围
private static final EntityDataAccessor<Float> DATA_PROXIMITY_FUSE;   // 近炸距离

// 位置和追踪
private static final EntityDataAccessor<Float> DATA_LAUNCH_X/Y/Z;     // 发射点坐标
private static final EntityDataAccessor<Float> DATA_IFF_SAFE_RADIUS;  // IFF 安全半径
```

### 游戏状态字段 (运行时)
```java
private UUID assignedTarget;           // 雷达指定目标 (UUID)
private Vec3 designatedTargetPos;      // 雷达指定目标位置
private Vec3 inertialTarget;           // 惯性目标记忆（目标丧失后用）
private Vec3 lastTargetVelocity;       // 上一帧目标速度（用于预测拦截）
private int targetMemoryTicks;         // 目标记忆剩余时长
private int lockHoldTicks;             // 锁定保持剩余 ticks
private boolean fuzeInstalled;         // 引信是否已安装
```

---

## 主要方法

### 1. tick() 方法 - 生命周期核心

#### 流程图
```
tick() called every frame
    ↓
    [安全延迟期 (0-30 ticks)?]
    ├─ YES: 基础运动 + 方向对齐 → return
    └─ NO:
        ↓
        [禁用 CBC 倒计时]
        setExplosionCountdown(-1)  // 防止 CBC 自动爆炸
        ↓
        [恢复父类物理引擎]
        super.tick()  // 处理碰撞、速度更新等
        ↓
        [死亡判断]
        if (!isAlive()) return;  // 如已死亡则停止
        ↓
        [方向对齐 + 粒子效果]
        alignToFlightDirection()
        spawnTrailParticles()
        ↓
        [目标追踪 (服务端)]
        ├─ 物理目标搜索 (PhysicsTargeting)
        ├─ 目标记忆管理
        ├─ 目标切换逻辑
        └─ 追踪转向 + 速度控制
        ↓
        [引信检测]
        ├─ 碰撞引信: onImpact/onClip 处理
        ├─ 近炸引信: 距离检测
        └─ 时间引信: 600 ticks 自爆
```

#### 完整代码注解版本

```java
@Override
public void tick() {
    // =========== 阶段 1: 安全延迟期 ===========
    // 前 30 tick 内：禁止碰撞检测，保护发射器
    if (tickCount <= SAFETY_DELAY_TICKS) {
        setNoGravity(true);  // 禁用重力
        
        // 绕过 move() 的碰撞检测，直接设置位置
        Vec3 motion = getDeltaMovement();
        Vec3 newPos = position().add(motion);
        setPos(newPos.x, newPos.y, newPos.z);
        
        // 对齐飞行方向（使模型面向运动方向）
        alignToFlightDirection();
        
        // 客户端粒子效果
        if (level().isClientSide) {
            spawnTrailParticles();
        }
        return;  // 立即返回，不执行后续逻辑
    }

    // =========== 阶段 2: 恢复物理引擎 ===========
    // 禁用 CBC 的自动爆炸倒计时
    // CBC 的 FuzedBigCannonProjectile 有一个 explosionCountdown 字段
    // 如果不禁用，它会在 ~60 ticks 时自动爆炸（冲突）
    setExplosionCountdown(-1);  // CRITICAL: 每 frame 重置为 -1
    
    // 调用父类 tick() 以保持物理引擎运行
    // 这会处理：
    // - 碰撞检测（触发 onImpact/onClip 回调）
    // - 速度应用
    // - 重力（虽然我们禁用了 NoGravity，但这是安全的）
    super.tick();
    
    if (!isAlive()) {
        return;  // 导弹已失活，停止处理
    }

    // =========== 阶段 3: 方向和粒子 ===========
    setNoGravity(true);  // 继续禁用重力
    alignToFlightDirection();  // 每 frame 对齐方向

    // 客户端只执行粒子，不执行逻辑
    if (level().isClientSide) {
        spawnTrailParticles();
        return;
    }

    // =========== 阶段 4: 服务端目标追踪 ===========
    // 注意：碰撞引信由 onImpact/onClip 处理，这里不重复检测
    
    // 获取指定的目标位置（可能来自雷达或预设）
    Vec3 targetPos = resolveAssignedTargetPos();
    
    // 定义物理搜索范围（基础搜索范围 + 额外加成）
    float physicsSeekRange = seekRange() + PHYSICS_TARGET_RANGE_BONUS;  // +400
    
    // 使用 PhysicsTargeting 搜索最近的物理目标
    Optional<Vec3> physicsTarget = PhysicsTargeting.findNearestPhysicsTarget(
        level(),                    // 世界
        position(),                 // 导弹位置
        physicsSeekRange,           // 搜索范围
        maxSpeed(),                 // 最大速度（用于预测）
        getLauncherOrigin(),        // 发射器位置（IFF 用）
        iffSafeRadius()             // IFF 安全半径
    );

    // =========== 阶段 5: 目标管理逻辑 ===========
    if (physicsTarget.isPresent()) {
        // 物理目标存在：更新指定目标
        updateDynamicDesignatedTarget(position(), physicsTarget.get());
        targetPos = designatedTargetPos;  // 使用动态更新的目标
    } else if (targetPos == null && designatedTargetPos != null) {
        // 物理目标不存在，但有之前的指定目标：使用它
        targetPos = designatedTargetPos;
    }

    // 更新目标记忆
    if (targetPos != null) {
        inertialTarget = targetPos;         // 保存为惯性目标
        targetMemoryTicks = TARGET_MEMORY_TICKS;  // 重置记忆计数
    } else if (targetMemoryTicks > 0 && inertialTarget != null) {
        // 目标丧失但记忆未过期：使用记忆中的目标
        targetMemoryTicks--;
        targetPos = inertialTarget;
    } else {
        // 记忆也过期了：清空
        inertialTarget = null;
        targetMemoryTicks = 0;
        // 但如果还有指定位置，继续使用
        if (designatedTargetPos != null) {
            targetPos = designatedTargetPos;
        }
    }

    // =========== 阶段 6A: 有目标时的追踪 ===========
    if (targetPos != null) {
        // 保存当前速度和加速
        inertialTarget = targetPos;
        targetMemoryTicks = TARGET_MEMORY_TICKS;
        
        // 计算基本参数
        double distance = position().distanceTo(targetPos);
        Vec3 current = getDeltaMovement();
        double currentSpeed = current.length();
        double desiredSpeed = computeNextSpeed(currentSpeed);  // 渐进加速
        
        // --------- 动态转向增益计算 ---------
        // 转向增益基于距离：距离越近越敏捷
        float steeringGain = steering();  // 基础转向增益 (0-1)
        double speedRatio = currentSpeed > 0 ? desiredSpeed / currentSpeed : 1.0D;
        
        // 距离分段增强转向
        if (distance < 15.0D) {
            steeringGain = Math.min(1.0F, steeringGain * (2.0F + (float)(speedRatio - 1.0D)));
        } else if (distance < 30.0D) {
            steeringGain = Math.min(1.0F, steeringGain * (1.6F + (float)(speedRatio - 1.0D) * 0.5F));
        } else if (distance < 60.0D) {
            steeringGain = Math.min(1.0F, steeringGain * (1.3F + (float)(speedRatio - 1.0D) * 0.3F));
        } else if (distance < 120.0D) {
            steeringGain = Math.min(1.0F, steeringGain * 1.1F);
        }

        // --------- 预测拦截点 ---------
        // 考虑目标的速度，提前计算拦截位置
        Vec3 targetVelocity = lastTargetVelocity;
        Vec3 predictedPos = targetPos.add(targetVelocity.scale(PREDICTIVE_LEAD_DISTANCE));
        Vec3 toTarget = predictedPos.subtract(position());
        
        // --------- 速度混合与转向 ---------
        if (toTarget.lengthSqr() > 1.0E-6D) {
            // 目标方向的期望速度
            Vec3 desired = toTarget.normalize().scale(desiredSpeed);
            
            // 混合当前速度和期望速度
            // blended = current * (1 - gain) + desired * gain
            Vec3 blended = current.scale(1.0D - steeringGain)
                                   .add(desired.scale(steeringGain));
            
            // 确保混合后的速度有效
            if (blended.lengthSqr() > 1.0E-6D) {
                // 规范化并应用期望速度
                blended = blended.normalize().scale(desiredSpeed);
                setDeltaMovement(blended);
            }
        }

        // --------- 近炸引信检测 ---------
        // 如果距离 < proximityFuse() 且引信已安装且过了安全期
        if (distance < proximityFuse() && fuzeInstalled && tickCount > SAFETY_DELAY_TICKS) {
            detonate();  // 爆炸！
            return;
        }
    } 
    
    // =========== 阶段 6B: 无目标时的巡航 ===========
    else {
        Vec3 currentMotion = getDeltaMovement();
        
        if (currentMotion.lengthSqr() < 0.01D) {
            // 速度太小：重新设置为最小巡航速度
            setDeltaMovement(
                getLookAngle()
                    .normalize()
                    .scale(maxSpeed() * MIN_CRUISE_SPEED_RATIO)  // 1.8x
            );
        } else {
            // 继续当前方向，应用加速逻辑
            double nextSpeed = computeNextSpeed(currentMotion.length());
            setDeltaMovement(
                currentMotion.normalize().scale(nextSpeed)
            );
        }
    }

    // =========== 阶段 7: 寿命引爆 ===========
    // 无引信导弹：经过最大寿命后自动爆炸
    // 不受 getFuzeTicks() 影响（与引信系统独立）
    if (!fuzeInstalled && tickCount > SAFETY_DELAY_TICKS && tickCount >= MAX_MISSILE_LIFETIME) {
        detonate();  // 600 ticks 后强制爆炸
    }
}
```

---

### 2. onImpact() 和 onClip() - 碰撞检测

```java
// 实体碰撞触发
@Override
protected boolean onImpact(HitResult hitResult,
        AbstractCannonProjectile.ImpactResult impactResult,
        ProjectileContext ctx) {
    // 仅在服务端、导弹活跃、引信已安装、过了安全期时爆炸
    if (!level().isClientSide && isAlive() && fuzeInstalled && tickCount > SAFETY_DELAY_TICKS) {
        detonate(hitResult.getLocation());
    }
    return true;  // 返回 true 表示消耗碰撞事件
}

// 方块碰撞触发
@Override
protected boolean onClip(ProjectileContext ctx, Vec3 from, Vec3 to) {
    if (!level().isClientSide && isAlive() && fuzeInstalled && tickCount > SAFETY_DELAY_TICKS) {
        // 使用碰撞中点作为爆炸位置
        detonate(from.add(to).scale(0.5D));
    }
    return true;
}
```

**调用流程**: `super.tick()` → 碰撞检测 → `onImpact()`/`onClip()`

---

### 3. updateDynamicDesignatedTarget() - 目标切换逻辑

```java
private void updateDynamicDesignatedTarget(Vec3 missilePos, Vec3 candidate) {
    if (candidate == null || missilePos == null) {
        return;
    }

    // 首次目标设置
    if (designatedTargetPos == null) {
        designatedTargetPos = candidate;
        lockHoldTicks = lockHoldTicksConfig;  // 16 ticks
        return;
    }

    // 当前目标与候选目标的距离
    double distToCurrent = missilePos.distanceTo(designatedTargetPos);
    double distToCandidate = missilePos.distanceTo(candidate);

    // 判断是否应该切换目标
    boolean shouldSwitch = false;

    // 已有锁定时间
    if (lockHoldTicks > 0) {
        lockHoldTicks--;
        // 强制切换距离检查：新目标更近很多
        if (distToCandidate < distToCurrent - forceSwitchDistance) {
            shouldSwitch = true;
        }
    } else {
        // 锁定时间已过：允许根据距离优势切换
        if (distToCandidate < distToCurrent - switchAdvantageDistance) {
            shouldSwitch = true;
        }
    }

    if (shouldSwitch) {
        designatedTargetPos = candidate;
        lockHoldTicks = lockHoldTicksConfig;  // 重置锁定时间
    }
}
```

**目标切换逻辑**:
1. 新目标比当前目标 **强制切换距离** (22m) 更近 → 立即切换
2. 锁定保持时间 (16 ticks) 过期后，新目标比当前目标 **优势距离** (8m) 更近 → 切换

---

### 4. resolveAssignedTargetPos() - 获取目标位置

```java
private Vec3 resolveAssignedTargetPos() {
    // 如果有雷达指定目标 (UUID)
    if (assignedTarget != null && level() instanceof ServerLevel serverLevel) {
        Entity targetEntity = serverLevel.getEntity(assignedTarget);
        if (targetEntity != null && targetEntity.isAlive()) {
            // 记录目标速度（用于预测拦截）
            lastTargetVelocity = targetEntity.getDeltaMovement();
            return targetEntity.getEyePosition();  // 使用眼睛位置作为目标
        }
    }
    // 否则返回预设的目标位置
    return designatedTargetPos;
}
```

---

### 5. computeNextSpeed() - 加速逻辑

```java
private double computeNextSpeed(double currentSpeed) {
    double minSpeed = maxSpeed() * MIN_CRUISE_SPEED_RATIO;  // 1.8x 最大速度
    double normalizedCurrent = Math.max(minSpeed, currentSpeed);
    
    // 每 tick 加速 0.11，上限为最大速度
    return Math.min(maxSpeed(), normalizedCurrent + ACCELERATION_PER_TICK);
}
```

**说明**: 
- 最小速度 = 最大速度 × 1.8
- 每 tick 加速 0.11
- 加速到最大速度后保持

---

## 发射流程 (RakLauncherBlock)

```java
private static void launch(ServerLevel level, BlockPos pos, BlockState state) {
    // 1. 验证和获取导弹
    BlockEntity blockEntity = level.getBlockEntity(pos);
    if (!(blockEntity instanceof RakLauncherBlockEntity launcher) || !launcher.hasMissile()) {
        return;
    }
    ItemStack ammoStack = launcher.removeMissile();

    // 2. 计算发射方向和位置
    Direction facing = state.getValue(FACING);
    Vec3 dir = Vec3.atLowerCornerOf(facing.getNormal()).normalize();
    Vec3 center = Vec3.atCenterOf(pos);
    Vec3 muzzlePos = center
        .add(dir.scale(MUZZLE_FORWARD_OFFSET))     // 向前偏移
        .add(0.0D, MUZZLE_UP_OFFSET, 0.0D)         // 向上偏移
        .add(right.scale(MUZZLE_SIDE_OFFSET));     // 向侧偏移

    // 3. 创建导弹实体
    P1MissileEntity missile = new P1MissileEntity(level, muzzlePos.x, muzzlePos.y, muzzlePos.z);

    // 4. 应用引信参数
    int resolvedFuze = CbcCompat.resolveFuzeTicks(ammoStack, launcher.getFuzeTicks());
    missile.setFuzeTicks(resolvedFuze);
    
    // 5. 应用性能配置 (MissileProfile)
    missile.applyProfile(MissileProfile.resolve(ammoStack));
    
    // 6. 应用追踪调优参数
    missile.setGuidanceTuning(
        MissileProfile.resolveLockHoldTicks(ammoStack, P1MissileEntity.defaultLockHoldTicks()),
        MissileProfile.resolveSwitchAdvantageDistance(ammoStack, P1MissileEntity.defaultSwitchAdvantageDistance()),
        MissileProfile.resolveForceSwitchDistance(ammoStack, P1MissileEntity.defaultForceSwitchDistance())
    );

    // 7. 绑定雷达目标
    if (launcher.ensureRadarBound()) {
        launcher.refreshRadarTargets();
        missile.setAssignedTarget(launcher.getSelectedRadarTargetUuid());
        Vec3 selectedPos = launcher.getSelectedRadarTargetPos();
        if (selectedPos != null) {
            missile.setDesignatedTargetPos(selectedPos);
        }
    }

    // 8. 设置发射参数
    missile.setLauncherOrigin(center);
    missile.setDeltaMovement(dir.scale(1.0D).add(0.0D, 0.16D, 0.0D));  // 初速度
    missile.setItem(ammoStack.copyWithCount(1));
    
    // 9. 应用引信安装状态
    boolean fuzeInstalled = ammoStack.hasTag() && ammoStack.getOrCreateTag().getBoolean("FuzeInstalled");
    missile.setFuzeInstalled(fuzeInstalled);

    // 10. 添加到世界
    level.addFreshEntity(missile);
    level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
}
```

---

## 关键依赖类

### PhysicsTargeting
```java
public static Optional<Vec3> findNearestPhysicsTarget(
    Level level,
    Vec3 position,
    float searchRange,
    float maxSpeed,
    Vec3 launcherOrigin,
    float iffSafeRadius
) -> Optional<Vec3>
```
**功能**: 搜索搜索范围内的最近物理目标（移动实体）

### MissileProfile
```java
public static MissileProfile resolve(ItemStack stack)
// 从导弹物品中读取配置：
// - MaxSpeed, Steering, BlastPower
// - SeekRange, ProximityFuse
// - IFFSafeRadius
// - GuidanceTuning (LockHoldTicks, SwitchAdvantageDistance, ForceSwitchDistance)
```

### CbcCompat
```java
public static int resolveFuzeTicks(ItemStack ammoStack, int defaultFuzeTicks)
// 从 CBC 弹药 NBT 读取引信延迟，失败则用默认值 (200 ticks)
```

---

## 引信系统总结

| 引信类型 | 触发条件 | 代码位置 | 控制参数 |
|---------|--------|---------|---------|
| **碰撞** | 与实体碰撞 | `onImpact()` | `fuzeInstalled` |
| **碰撞** | 与方块碰撞 | `onClip()` | `fuzeInstalled` |
| **近炸** | 距离 < `proximityFuse()` | `tick()` | `proximityFuse` (float) |
| **时间** | `tickCount >= 600` | `tick()` | `MAX_MISSILE_LIFETIME` (常量) |

---

## 关键参数默认值

| 参数 | 默认值 | 单位/含义 |
|------|--------|----------|
| MaxSpeed | ? | block/tick |
| Steering | ? | 0-1 比例 |
| BlastPower | ? | TNT 当量 |
| SeekRange | ? | blocks |
| ProximityFuse | ? | blocks |
| IFFSafeRadius | ? | blocks |
| FuzeTicks | 200 | ticks (10 秒) |
| LockHoldTicks | 16 | ticks (0.8 秒) |
| SwitchAdvantageDistance | 8.0 | blocks |
| ForceSwitchDistance | 22.0 | blocks |

*(具体值在 MissileProfile 或发射器配置中)*

---

## Hellfire vs P1 对比表

| 特性 | 实现位置 | Hellfire (推断) |
|------|--------|----------------|
| **基类** | `class P1MissileEntity extends HEShellProjectile` | 类似结构 |
| **安全延迟** | `SAFETY_DELAY_TICKS = 30` | 应该相同 |
| **生命周期** | `MAX_MISSILE_LIFETIME = 600` | 可能不同 |
| **追踪** | `PhysicsTargeting.findNearestPhysicsTarget()` | 应该相同 |
| **预测** | `PREDICTIVE_LEAD_DISTANCE = 0.35` | 可能不同 |
| **转向** | `steeringGain * (distance factor)` | 可能有变化 |
| **引信** | `fuzeInstalled`, `onImpact()`, `onClip()` | 应该相同 |

---

## 性能优化要点

1. **CBC 倒计时禁用**: `setExplosionCountdown(-1)` 每 frame 重置
   - 防止 CBC 的 ~60 tick 自动爆炸机制

2. **安全延迟**: 前 30 ticks 绕过碰撞检测
   - 保护发射器免受友伤

3. **目标记忆**: 100 ticks 内保持对离线目标的追踪
   - 避免频繁目标丧失导致的不稳定

4. **转向增益分段**: 距离越近转向越敏捷
   - 近距离 2.0x～2.6x，远距离 1.1x～1.3x

5. **速度平滑**: 每 tick ≤0.11 的加速
   - 避免急加速的不稳定性

---

## 下一步学习

1. **查阅 Hellfire 源代码** (如果可获得)
2. **比对关键方法**: `tick()`, `onImpact()`, `updateDynamicDesignatedTarget()`
3. **测试参数差异**: 速度、加速度、转向增益等
4. **反编译 Hellfire class** 获取完整实现
