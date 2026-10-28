# P系列导弹系统完整更新计划

## 📋 更新概览

本更新基于Hellfire导弹系统的设计理念，将P1导弹进行优化，并扩展至整个P系列。

---

## 第一阶段：P1导弹优化

### 1.1 添加制导斜坡（Guidance Ramp）

**目的**：防止导弹在发射初期转向过猛，确保平稳的制导曲线。

**实现方式**：
```java
private static final int GUIDANCE_RAMP_TICKS = 20;  // 前20 ticks建立制导能力

private double getGuidanceRamp() {
    return Math.min(1.0, (double)tickCount / GUIDANCE_RAMP_TICKS);
}

// 在apply转向时乘以这个因子
float steeringGain = steering() * (float)getGuidanceRamp();
```

**预期效果**：
- t=0-20: 制导能力从0%增加到100%
- 避免导弹发射后立即做出激进转向

---

### 1.2 添加锁定计数系统

**目的**：用渐进式的"锁定强度"代替布尔型的目标状态，增强现实感。

**实现方式**：
```java
private float lockStrength = 0.0f;  // 0-1，锁定强度

private void updateLockStrength(Vec3 targetPos) {
    if (targetPos != null && position().distanceTo(targetPos) < seekRange()) {
        // 增加锁定强度，50 ticks内达到满锁定
        lockStrength = Math.min(1.0f, lockStrength + 0.02f);
    } else {
        // 衰减锁定强度，100 ticks内完全丧失锁定
        lockStrength = Math.max(0.0f, lockStrength - 0.01f);
    }
}

// 用锁定强度调整转向增益
float effectiveSteeringGain = steering() * lockStrength;
```

**影响因子**：
- 制导能力随锁定强度线性增加
- 锁定强度可用于诊断（渲染、声音反馈等）

---

### 1.3 改进转向模型（PN导航常数法可选）

**当前方式**：线性混合（Lerp）

**升级方式**（可选）：PN导航常数法
```java
private Vec3 computeProportionalNavigation(Vec3 missilePos, Vec3 missileVel, 
                                            Vec3 targetPos, Vec3 targetVel) {
    // 相对位置和速度
    double dx = targetPos.x - missilePos.x;
    double dy = targetPos.y - missilePos.y;
    double dz = targetPos.z - missilePos.z;
    double distSq = dx*dx + dy*dy + dz*dz;
    
    if (distSq < 4.0) return Vec3.ZERO;  // 太近，停止制导
    
    // 相对速度
    double vx_rel = targetVel.x - missileVel.x;
    double vy_rel = targetVel.y - missileVel.y;
    double vz_rel = targetVel.z - missileVel.z;
    
    // omega = (r × v_rel) / |r|²
    double omega_x = (dy * vz_rel - dz * vy_rel) / distSq;
    double omega_y = (dz * vx_rel - dx * vz_rel) / distSq;
    double omega_z = (dx * vy_rel - dy * vx_rel) / distSq;
    
    // a_cmd = omega × v_missile × N
    double N = 5.0 * lockStrength * getGuidanceRamp();
    double a_cmd_x = (omega_y * missileVel.z - omega_z * missileVel.y) * N;
    double a_cmd_y = (omega_z * missileVel.x - omega_x * missileVel.z) * N;
    double a_cmd_z = (omega_x * missileVel.y - omega_y * missileVel.x) * N;
    
    // 限幅：最大加速度从0.2增加到0.5 m/s²
    double maxAccel = 0.2 + 0.3 * getGuidanceRamp();
    double accelMag = Math.sqrt(a_cmd_x*a_cmd_x + a_cmd_y*a_cmd_y + a_cmd_z*a_cmd_z);
    if (accelMag > maxAccel) {
        a_cmd_x = a_cmd_x / accelMag * maxAccel;
        a_cmd_y = a_cmd_y / accelMag * maxAccel;
        a_cmd_z = a_cmd_z / accelMag * maxAccel;
    }
    
    return new Vec3(a_cmd_x, a_cmd_y, a_cmd_z);
}
```

**保守方案**（不改写核心逻辑）：
```java
// 只增强距离相关的转向增益和制导斜坡
float steeringGain = steering() * getGuidanceRamp() * lockStrength;
```

---

### 1.4 优化碰撞检测

**当前问题**：碰撞检测由`onImpact()`和`onClip()`处理，但可能不够精准。

**改进方向**：
- 添加`DEBUG_COLLISION_MODE`标志用于测试
- 在引信安装检查中添加"硬化区"（加强碰撞响应）

```java
private static final int HARDENED_COLLISION_TICKS = 100;  // 发射后100 ticks内引信硬化

@Override
protected boolean onImpact(HitResult hitResult,
        AbstractCannonProjectile.ImpactResult impactResult,
        ProjectileContext ctx) {
    if (!level().isClientSide && isAlive() && fuzeInstalled && 
        tickCount > SAFETY_DELAY_TICKS) {
        // 硬化引信确保碰撞时必然爆炸
        if (tickCount < SAFETY_DELAY_TICKS + HARDENED_COLLISION_TICKS) {
            detonate(hitResult.getLocation());
        }
    }
    return true;
}
```

---

### 1.5 完整的P1改进代码（关键部分）

**需要修改的部分**（在P1MissileEntity中）：

```java
// 新增字段
private static final int GUIDANCE_RAMP_TICKS = 20;
private float lockStrength = 0.0f;
private int targetLockTicks = 0;  // 目标被锁定的持续时间

// 新增方法
private double getGuidanceRamp() {
    return Math.min(1.0, (double)(tickCount - SAFETY_DELAY_TICKS) / GUIDANCE_RAMP_TICKS);
}

private void updateLockStrength(Vec3 targetPos) {
    if (targetPos != null) {
        double distance = position().distanceTo(targetPos);
        if (distance < seekRange() * 1.5) {  // 有效搜索范围内
            lockStrength = Math.min(1.0f, lockStrength + 0.02f);  // 50 ticks满锁定
            targetLockTicks++;
        } else {
            lockStrength = Math.max(0.0f, lockStrength - 0.02f);
            targetLockTicks = 0;
        }
    } else {
        lockStrength = Math.max(0.0f, lockStrength - 0.01f);
        targetLockTicks = Math.max(0, targetLockTicks - 1);
    }
}

// 在tick()中应用
@Override
public void tick() {
    // ... 现有代码 ...
    
    if (targetPos != null) {
        updateLockStrength(targetPos);
        
        // 应用制导斜坡和锁定强度
        double ramp = getGuidanceRamp();
        double effectiveSteeringGain = steering() * (float)ramp * lockStrength;
        
        // 使用effectiveSteeringGain替代原始steering()
        // ... 制导逻辑 ...
    } else {
        updateLockStrength(null);
    }
}
```

---

## 第二阶段：P系列导弹基类

### 2.1 创建GenericMissileEntity基类

**文件**：`src/main/java/TAOVMN/entity/GenericMissileEntity.java`

**目的**：提取P1、P15、P30等通用逻辑

**核心设计**：
```java
public abstract class GenericMissileEntity extends HEShellProjectile implements GeoEntity {
    // 通用常量
    protected static final int SAFETY_DELAY_TICKS = 30;
    protected static final int GUIDANCE_RAMP_TICKS = 20;
    protected static final int TARGET_MEMORY_TICKS = 100;
    protected static final int MAX_MISSILE_LIFETIME = 600;
    
    // 通用字段
    protected ItemStack renderItem;
    protected MissileProfile profile;
    protected Vec3 designatedTargetPos;
    protected float lockStrength = 0.0f;
    
    // 抽象方法（由子类实现）
    protected abstract ItemStack getDefaultRenderItem();
    protected abstract MissileProfile getDefaultProfile();
    
    // 通用逻辑
    protected double getGuidanceRamp() { ... }
    protected void updateLockStrength(Vec3 targetPos) { ... }
    protected Vec3 computeGuidanceForces(Vec3 targetPos) { ... }
    
    @Override
    public void tick() {
        // 通用生命周期
        if (tickCount <= SAFETY_DELAY_TICKS) {
            handleSafetyDelay();
            return;
        }
        
        // 制导逻辑（子类可覆盖）
        handleGuidance();
        
        // 爆炸检测
        handleDetonation();
    }
    
    protected void handleSafetyDelay() { ... }
    protected void handleGuidance() { ... }
    protected void handleDetonation() { ... }
}
```

---

### 2.2 基于基类的P系列实现

**文件**：
- `src/main/java/TAOVMN/entity/P1MissileEntity.java` - 改为继承GenericMissileEntity
- `src/main/java/TAOVMN/entity/P15MissileEntity.java` - 新建
- `src/main/java/TAOVMN/entity/P30MissileEntity.java` - 新建
- 等等...

**实现模式**（每个都简洁）：
```java
public class P15MissileEntity extends GenericMissileEntity {
    @Override
    protected ItemStack getDefaultRenderItem() {
        return new ItemStack(ModItems.P15_MISSILE.get());
    }
    
    @Override
    protected MissileProfile getDefaultProfile() {
        return MissileProfile.p15();
    }
    
    // 可选：覆盖特定行为
    @Override
    protected void handleGuidance() {
        // P15特殊逻辑（如果有的话）
        super.handleGuidance();
    }
}
```

---

## 第三阶段：Director支持（可选模块化架构）

### 3.1 创建P1DirectorBlock

**文件**：`src/main/java/TAOVMN/block/P1DirectorBlock.java`

**功能**：
- 搜索附近的敌方目标
- 计算锁定参数
- 为附近导弹提供制导增强

**实现代码框架**：
```java
public class P1DirectorBlockEntity extends BlockEntity {
    private static final int SEARCH_RADIUS = 640;  // 32 chunks
    private static final double ACQUIRE_CONE_ANGLE = Math.toRadians(16);  // 16°
    private static final double KEEP_CONE_ANGLE = Math.toRadians(24);     // 24°
    
    private double lockCount = 0.0;  // 0-100，锁定进度
    private double inaccurate = 0.0;  // 精度误差（0-40m）
    @Nullable
    private Ship closestTracked;
    private Vec3 lockPosition = Vec3.ZERO;
    private Vec3 lockVelocity = Vec3.ZERO;
    
    public void tick() {
        if (level == null || level.isClientSide) return;
        
        // 搜索目标
        updateClosest();
        
        // 更新锁定计数
        if (closestTracked != null) {
            lockCount = Math.min(100.0, lockCount + 2.0);  // 50 ticks满锁定
            inaccurate = 40.0 - (lockCount / 100.0) * 40.0;  // 从40m衰减到0m
        } else {
            lockCount = Math.max(0.0, lockCount - 5.0);  // 快速丧失锁定
            inaccurate = 40.0;  // 重置精度
        }
    }
    
    private void updateClosest() {
        // 类似HellFireDirectorBlockEntity的圆锥搜索
        Vec3 from = Vec3.atCenterOf(worldPosition);
        Vector3dc front = getDirection();  // 获取方向向量
        
        // ... 搜索逻辑 ...
    }
    
    // 导弹查询接口
    public float getLockStrength() {
        return (float)(lockCount / 100.0);
    }
    
    public Vec3 getLockPosition() {
        return lockPosition;
    }
    
    public Vec3 getLockVelocity() {
        return lockVelocity;
    }
    
    public double getInaccuracy() {
        return inaccurate;
    }
}
```

### 3.2 集成P1导弹到Director

**在P1MissileEntity中添加**：
```java
@Nullable
private BlockPos linkedDirector;

private P1DirectorBlockEntity getDirector() {
    if (linkedDirector == null || !(level() instanceof ServerLevel)) {
        return null;
    }
    
    BlockEntity be = ((ServerLevel)level()).getChunkSource()
        .getChunkAtIfLoadedImmediately(linkedDirector.getX() >> 4, 
                                       linkedDirector.getZ() >> 4)
        ?.getBlockEntity(linkedDirector);
    
    return be instanceof P1DirectorBlockEntity dir ? dir : null;
}

private void applyDirectorGuidance() {
    P1DirectorBlockEntity director = getDirector();
    if (director == null) return;
    
    // 获取导演提供的制导力
    float directorLockStrength = director.getLockStrength();
    
    // 增强本地锁定强度
    lockStrength = Math.max(lockStrength, directorLockStrength);
    
    // 使用Director的目标位置进行制导
    // ... 实现细节 ...
}
```

---

## 第四阶段：性能优化

### 4.1 缓存优化

```java
// 缓存PhysicsTargeting结果，避免每 tick都搜索
private int lastTargetSearchTick = -100;
private static final int TARGET_SEARCH_INTERVAL = 5;  // 每5 ticks搜索一次

private void updateTargetIfNeeded() {
    if (tickCount - lastTargetSearchTick < TARGET_SEARCH_INTERVAL) {
        return;  // 使用缓存的目标
    }
    
    // 执行新的搜索
    lastTargetSearchTick = tickCount;
    // ... 搜索逻辑 ...
}
```

### 4.2 向量运算优化

```java
// 使用向量长度平方而不是长度（避免sqrt）
if (toTarget.lengthSqr() > 1e-6) {
    // ...
}

// 预计算常用向量
private Vec3 cachedMotion = Vec3.ZERO;
private double cachedMotionLength = 0.0;

private void cacheMotion() {
    cachedMotion = getDeltaMovement();
    cachedMotionLength = cachedMotion.length();
}
```

### 4.3 网络同步优化

```java
// 减少不必要的EntityDataAccessor同步
// 只在值变化时标记为脏
private float lastSyncedSteeringGain = -1;

if (Math.abs(steeringGain - lastSyncedSteeringGain) > 0.01f) {
    entityData.set(DATA_STEERING, steeringGain);
    lastSyncedSteeringGain = steeringGain;
}
```

---

## 第五阶段：测试清单

### 5.1 单元测试

- [ ] P1导弹制导斜坡：验证0-20 ticks制导能力正确衰减
- [ ] 锁定计数：验证目标进入范围时锁定强度增加
- [ ] 碰撞爆炸：验证fuzeInstalled=true时碰撞爆炸
- [ ] 近炸爆炸：验证proximity距离爆炸
- [ ] 时间爆炸：验证600 ticks自动爆炸（无引信）
- [ ] 目标切换：验证目标动态切换逻辑正确

### 5.2 集成测试

- [ ] P15/P30导弹能正确发射和追踪
- [ ] Director能搜索目标并计算锁定
- [ ] Director的导弹制导增强正常工作
- [ ] 多导弹由同一Director控制

### 5.3 性能测试

- [ ] 100个导弹飞行：监测CPU使用
- [ ] 10个Director块：监测搜索性能
- [ ] 网络同步开销：监测带宽消耗

---

## 实现优先级

### 🔴 必做（本次更新）
1. ✅ P1优化：添加制导斜坡 + 锁定计数
2. ✅ 编译验证：确保P1修改无误
3. ✅ 创建基类：GenericMissileEntity
4. ✅ P15/P30等：基于基类的简洁实现

### 🟡 可选（后续）
1. ⚠️ Director块：模块化架构
2. ⚠️ PN导航常数法：高级制导模型
3. ⚠️ 性能优化：缓存和同步优化

### 🟢 未来（探索）
1. 🔮 多导弹编队控制
2. 🔮 AI目标预测
3. 🔮 自适应制导参数

---

## 文件清单

### 新建文件
- `src/main/java/TAOVMN/entity/GenericMissileEntity.java` - 导弹基类
- `src/main/java/TAOVMN/entity/P15MissileEntity.java` - P15导弹
- `src/main/java/TAOVMN/entity/P30MissileEntity.java` - P30导弹
- `src/main/java/TAOVMN/block/P1DirectorBlock.java` - Director方块
- `src/main/java/TAOVMN/block/entity/P1DirectorBlockEntity.java` - Director逻辑
- `src/main/resources/.../blockstates/p1_director.json` - 方块状态
- `src/main/resources/.../models/block/p1_director.json` - 模型

### 修改文件
- `src/main/java/TAOVMN/entity/P1MissileEntity.java` - 优化 + 继承基类
- `src/main/java/TAOVMN/registry/ModEntities.java` - 注册新导弹实体
- `src/main/java/TAOVMN/registry/ModBlocks.java` - 注册Director块

---

## 时间估计

| 任务 | 估计时间 |
|------|--------|
| P1优化（制导斜坡+锁定计数） | 1-2小时 |
| 编译测试 | 0.5小时 |
| 基类重构 | 1-1.5小时 |
| P15/P30等实现 | 1小时 |
| Director设计 | 2-3小时 |
| 全面测试 | 2小时 |
| **总计** | **~10小时** |

---

## 验收标准

✅ P1导弹优化成功，制导更平滑  
✅ P15/P30等所有P系列导弹都能正常发射和追踪  
✅ Director块（如果实现）能搜索目标并提供制导增强  
✅ 编译无错误，运行无异常  
✅ 性能在可接受范围内（< 2% CPU额外消耗）  
✅ 所有爆炸模式（碰撞、近炸、时间）正常工作  
