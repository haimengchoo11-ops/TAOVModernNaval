# Hellfire vs P1 导弹系统完整对标学习指南

## 📋 目录结构

本指南通过对比分析，展示两个导弹系统的架构、算法和设计理念的差异。

---

## 第一部分：架构对比

### Hellfire 架构（模块化设计）

```
Director (火控导演) → Rack (导弹架) → Projectile (导弹)
    ↓                    ↓               ↓
目标搜索、追踪    导弹存储、发射    执行制导、爆炸
```

**职责分离**:
- **HellFireDirectorBlockEntity**: 搜索目标、计算锁定参数、管理精度误差
- **HellFireRackBlockEntity**: 存储导弹、与 Director 通信、发射导弹
- **HellFireProjectile**: 接收锁定信息、执行制导力计算、处理生命周期

**特点**:
- 导弹本身是**"被动的"** - 完全由外部 Director 指挥
- 导弹通过 NBT 同步接收来自 Director 的实时锁定数据
- 支持多导弹由同一 Director 控制

---

### P1 架构（单体自主设计）

```
RakLauncher (发射器)
    ↓
P1MissileEntity (自包含导弹)
    ├─ 目标搜索 (PhysicsTargeting)
    ├─ 目标追踪 (动态切换逻辑)
    ├─ 制导计算 (速度混合)
    └─ 引信管理 (碰撞、近炸、时间)
```

**职责集中**:
- **P1MissileEntity**: 包含所有逻辑 - 搜索、追踪、制导、爆炸
- 独立自主，无需外部指挥
- 每个导弹自己做决策

**特点**:
- 导弹是**"主动的"** - 自己寻找和追踪目标
- 更灵活，适合个体投射
- 更容易部署和使用

---

## 第二部分：目标追踪算法对比

### Hellfire 的目标追踪（Director 侧）

#### 1. 目标搜索阶段
```java
public void updateClosest() {
    // 使用圆锥搜索范围
    // ACQUIRE_CONE_ANGLE = 16° (新目标搜索范围)
    // KEEP_CONE_ANGLE = 24° (已锁定目标保持范围)
    
    Ship candidate = this.pickTrackCandidate(
        from,           // Director 位置
        front,          // Director 前向向量
        level,
        filter          // 过滤条件
    );
    
    if (candidate != null) {
        this.closestTracked = candidate;
        this.lockCount = Math.min(this.lockCount, 25.0);  // 重置锁定计数
        this.graceTicks = 100;  // 设置容限时间（5 秒）
    }
}
```

#### 2. 圆锥目标选择
```java
private Ship pickTrackCandidate(Vec3 from, Vector3dc front, Level level, Predicate<Ship> filter) {
    // 对每个候选目标计算评分
    double allowedAngle = keepingCurrent ? KEEP_CONE_ANGLE : ACQUIRE_CONE_ANGLE;
    double angle = toCenter.angle(front);  // 与前向的夹角
    
    if (angle > allowedAngle) continue;  // 超出圆锥范围，跳过
    
    // 评分 = 夹角占比 + 距离占比 + 可见性惩罚
    double score = angle / allowedAngle;
    score += distToCenter / clipRange * 0.25;
    if (!visible) score += 0.35;  // 被遮挡则惩罚
    
    // 选择评分最低的（最好的）
    if (score < bestScore) {
        bestScore = score;
        best = candidate;
    }
}
```

#### 3. 目标切换条件
```java
public static boolean canSwitchToTrack(@Nullable Ship newTrack, @Nullable Ship oldTrack) {
    if (oldTrack == null || newTrack == null) {
        return true;
    }
    // 不能切换到同一集群的小目标
    boolean same = ConstraintClusterUtil.clusterOf(oldTrack.getId()).contains(newTrack.getId());
    boolean bigger = hasBiggerAABB(newTrack, oldTrack);
    return !same || bigger;
}
```

**特点**:
- 基于**立体角（Solid Angle）**的搜索
- 优先选择更大的目标
- 防止在同一集群内切换

---

### P1 的目标追踪（导弹侧）

#### 1. 物理目标搜索
```java
Optional<Vec3> physicsTarget = PhysicsTargeting.findNearestPhysicsTarget(
    level,
    position(),                          // 导弹位置
    seekRange + PHYSICS_TARGET_RANGE_BONUS,  // 搜索范围扩展
    maxSpeed(),                          // 导弹速度
    getLauncherOrigin(),                 // 发射器位置（IFF 用）
    iffSafeRadius()                      // IFF 安全半径
);
```

#### 2. 目标切换逻辑
```java
private void updateDynamicDesignatedTarget(Vec3 missilePos, Vec3 candidate) {
    if (designatedTargetPos == null) {
        designatedTargetPos = candidate;
        lockHoldTicks = lockHoldTicksConfig;  // 16 ticks
        return;
    }
    
    double distToCurrent = missilePos.distanceTo(designatedTargetPos);
    double distToCandidate = missilePos.distanceTo(candidate);
    
    // 强制切换：新目标比当前目标 forceSwitchDistance(22m) 更近
    if (distToCandidate < distToCurrent - forceSwitchDistance) {
        shouldSwitch = true;
    }
    
    // 有利切换：锁定时间过期后，优势距离(8m) 更近
    if (lockHoldTicks <= 0 && distToCandidate < distToCurrent - switchAdvantageDistance) {
        shouldSwitch = true;
    }
    
    if (shouldSwitch) {
        designatedTargetPos = candidate;
        lockHoldTicks = lockHoldTicksConfig;  // 重置锁定时间
    }
}
```

**特点**:
- 基于**距离**的简单决策
- 有锁定时间保护（16 ticks）
- 优先级：强制切换 > 有利切换

---

## 第三部分：制导力计算对比

### Hellfire 的制导力（复杂）

```java
protected Vec3 getForces(Vec3 position, Vec3 velocity) {
    // 1. 提取基本参数
    float lockRate = this.getLockRate();          // 0-1，锁定进度
    Vector3d targetPos = this.getLock();          // 目标位置
    Vector3d targetVel = this.getLockVel();       // 目标速度
    Vector3d missilePos = position;
    Vector3d missileVel = velocity;
    double missileSpeed = missileVel.length();
    
    // 2. 计算相对位置和速度
    Vector3d r = targetPos.sub(missilePos);       // 相对位置
    Vector3d v_rel = targetVel.sub(missileVel);   // 相对速度
    double distSq = r.lengthSquared();
    
    if (distSq < 4.0) return drag;  // 太近了，停止制导
    
    // 3. 计算制导加速度（使用叉积）
    // omega = (r × v_rel) / |r|²  （导航常数法）
    Vector3d omega = r.cross(v_rel).div(distSq);
    
    // 4. 计算制导斜坡（前 12 tick 从 0 到 1）
    double guidanceRamp = clamp(lifeTime / 12.0, 0, 1);
    
    // 5. 计算制导系数 N
    // N = 5.0 × 锁定率 × 1.0 × 制导斜坡
    double N = 5.0 * lockRate * 1.0 * guidanceRamp;
    
    if (N < 1e-4) return drag;
    
    // 6. 计算命令加速度
    // a_cmd = omega × 导弹速度 × N
    Vector3d a_cmd = omega.cross(missileVel).mul(N);
    
    // 7. 加速度限幅
    // max_accel = 0.08 + 0.27 × 制导斜坡
    // 即：前 12 tick 从 0.08 m/s² 增加到 0.35 m/s²
    double maxGuidanceAccel = 0.08 + 0.27 * guidanceRamp;
    
    if (a_cmd.length() > maxGuidanceAccel) {
        a_cmd.normalize(maxGuidanceAccel);
    }
    
    // 8. 速度限幅（不能超过导弹速度）
    Vector3d predictVel = missileVel.add(a_cmd);
    if (predictVel.length() > missileSpeed) {
        Vector3d clampVel = predictVel.normalize(missileSpeed);
        a_cmd.set(clampVel.sub(missileVel));
    }
    
    // 9. 加诱导拖曳（反作用）
    // induced_drag = -|a_cmd| × 0.4
    Vector3d inducedDrag = normalize(missileVel).mul(-a_cmd.length() * 0.4);
    
    // 10. 返回总力（阻力 + 制导加速度 + 诱导拖曳）
    return drag.add(toMinecraft(a_cmd.add(inducedDrag)));
}
```

**关键算法**:
- **导航常数法 (PN - Proportional Navigation)**
  - `omega = (r × v_rel) / |r|²`
  - 用于计算追踪目标的转向
- **制导斜坡** - 前 12 ticks 内从弱到强的逐步增强
- **加速度限幅** - 防止过度转向
- **诱导拖曳** - 补偿高速转向的能量损失

**优势**:
- 物理上更准确（使用导航常数法）
- 制导能力随时间衰减（防止早期过度转向）
- 考虑目标速度影响
- 能量守恒

---

### P1 的制导力（简化）

```java
if (targetPos != null) {
    // 1. 基本参数
    double distance = position().distanceTo(targetPos);
    Vec3 current = getDeltaMovement();
    double currentSpeed = current.length();
    double desiredSpeed = computeNextSpeed(currentSpeed);  // 每 tick +0.11，上限maxSpeed
    
    // 2. 动态转向增益（距离相关）
    float steeringGain = steering();  // 基础转向增益
    double speedRatio = currentSpeed > 0 ? desiredSpeed / currentSpeed : 1.0;
    
    // 距离越近，转向增益越大
    if (distance < 15.0) {
        steeringGain *= (2.0 + (speedRatio - 1.0));
    } else if (distance < 30.0) {
        steeringGain *= (1.6 + (speedRatio - 1.0) * 0.5);
    } else if (distance < 60.0) {
        steeringGain *= (1.3 + (speedRatio - 1.0) * 0.3);
    } else if (distance < 120.0) {
        steeringGain *= 1.1;
    }
    
    // 3. 预测拦截点
    Vec3 targetVelocity = lastTargetVelocity;
    Vec3 predictedPos = targetPos.add(targetVelocity.scale(0.35));  // 0.35 tick 的领先量
    Vec3 toTarget = predictedPos.subtract(position());
    
    // 4. 速度混合
    if (toTarget.lengthSqr() > 1e-6) {
        Vec3 desired = toTarget.normalize().scale(desiredSpeed);
        Vec3 blended = current.scale(1 - steeringGain)      // 保持 60% 当前速度
                             .add(desired.scale(steeringGain));  // 混入 40% 期望速度
        
        if (blended.lengthSqr() > 1e-6) {
            blended = blended.normalize().scale(desiredSpeed);
            setDeltaMovement(blended);
        }
    }
}
```

**特点**:
- 基于**线性混合（Lerp）**的转向
- 转向增益与距离分段关联
- 预测领先量固定（0.35）
- 计算量小，适合客户端渲染

---

## 第四部分：引信系统对比

### Hellfire 的引信

```
HellFireRackBlockEntity (主动发射)
    ↓
HellFireProjectile 接收导弹参数
    ├─ lockRate (0-1) - 目标锁定进度
    ├─ inaccurate (0-40m) - 爆炸精度误差
    └─ 接收实时更新的目标位置 (Rack → Director → Projectile)
    
爆炸触发：
    ├─ 超期自爆 (400-420 ticks)
    └─ 超出精度误差范围自爆（爆炸就是接近目标10-40m）
```

**特点**:
- **没有传统的"引信"概念**
- 爆炸由 Director 的精度控制 - lockCount 越高，精度越小
- 完全由外部控制爆炸时机

---

### P1 的引信

```
RakLauncher (发射器装配)
    ↓
P1MissileEntity
    ├─ fuzeInstalled: boolean - 引信是否已装
    ├─ setFuzeTicks(200) - 引信延迟（默认 200 tick）
    └─ 自主决策是否爆炸
    
引信类型：
    ├─ 碰撞引信: onImpact() / onClip() - 与实体/方块碰撞时爆炸
    ├─ 近炸引信: proximityFuse() - 靠近目标距离时爆炸
    └─ 时间引信: 600 ticks - 超期自爆
```

**特点**:
- **有明确的"引信"概念** - 可拆卸配件
- 三种独立的爆炸方式
- 灵活的引信管理

---

## 第五部分：生命周期对比

### Hellfire 导弹生命周期

```
t=0 发射
    ↓
t=0-12 制导斜坡期 (从 0.08 到 0.35 m/s² 逐步增强)
    ↓
t=12-400 制导期 (接收 Director 的制导力)
    ↓
t>400 自爆 (寿命终止)
```

**关键数据**:
- 寿命: 400 ticks (20 秒)
- 推进时间: 240 ticks (12 秒)
- 制导斜坡: 12 ticks
- 初速度: 4.5 m/s
- 最大转向加速度: 0.35 m/s²

---

### P1 导弹生命周期

```
t=0 发射
    ├─ SAFETY_DELAY_TICKS = 30 - 安全延迟期，禁止碰撞检测
    ↓
t=30-600 追踪期
    ├─ 寻找物理目标
    ├─ 目标切换逻辑
    ├─ 追踪转向 (距离相关)
    ├─ 每 tick +0.11 加速
    │
    ├─ 碰撞爆炸 (fuzeInstalled && 与实体/方块碰撞)
    ├─ 近炸爆炸 (distance < proximityFuse())
    │
    ↓
t>600 自爆 (仅无引信导弹)
```

**关键数据**:
- 安全延迟: 30 ticks (1.5 秒)
- 寿命: 600 ticks (30 秒)
- 加速度: 0.11 per tick (≈2.2 m/s²)
- 转向增益: 0.5-1.0 (距离相关)
- 预测领先: 0.35 tick
- 目标记忆: 100 ticks (5 秒)

---

## 第六部分：实现建议

### 如何将 Hellfire 的想法融入 P1

#### 建议 1: 添加"制导官"角色（可选块）

创建一个 P1DirectorBlock，功能类似 HellFireDirector:
- 搜索目标船舶
- 计算锁定参数
- 为附近导弹提供制导力增强

```java
// 伪代码
public class P1DirectorBlockEntity extends BlockEntity {
    private Ship closestTracked;
    private double lockCount = 0;  // 0-100
    
    public void updateClosest() {
        // 类似 HellFireDirector 的圆锥搜索
    }
    
    public float getLockStrength() {
        return (float)(lockCount / 100);
    }
    
    public Vec3 getGuidanceBoost(P1MissileEntity missile) {
        // 返回制导加速度向量
    }
}
```

**在 P1MissileEntity 中使用**:
```java
@Nullable
private BlockPos directorPosition;

@Override
protected Vec3 getGuidanceForces(Vec3 targetPos) {
    Vec3 baseForces = super.getGuidanceForces(targetPos);
    
    if (directorPosition != null) {
        P1DirectorBlockEntity director = getDirector(directorPosition);
        if (director != null && director.getLockStrength() > 0.5f) {
            Vec3 boost = director.getGuidanceBoost(this);
            return baseForces.add(boost);
        }
    }
    return baseForces;
}
```

---

#### 建议 2: 改进转向模型

将 P1 的简单 Lerp 改进为 Hellfire 的导航常数法:

```java
private Vec3 getProportionalNavigationForce(Vec3 missilePos, Vec3 missileVel, 
                                             Vec3 targetPos, Vec3 targetVel) {
    // PN 公式
    Vector3d r = toJOML(targetPos).sub(toJOML(missilePos));
    Vector3d v_rel = toJOML(targetVel).sub(toJOML(missileVel));
    double distSq = r.lengthSquared();
    
    if (distSq < 4) return Vec3.ZERO;
    
    // omega = (r × v_rel) / |r|²
    Vector3d omega = r.cross(v_rel).div(distSq);
    
    // a_cmd = omega × v_missile
    Vector3d a_cmd = omega.cross(toJOML(missileVel));
    
    // 限幅
    double maxAccel = 0.35;  // m/s²
    if (a_cmd.length() > maxAccel) {
        a_cmd.normalize(maxAccel);
    }
    
    return toMinecraft(a_cmd);
}
```

---

#### 建议 3: 添加制导斜坡

防止导弹在发射初期转向过猛:

```java
private double getGuidanceRamp() {
    // 前 20 ticks 从 0 到 1
    return Math.min(1.0, (double)tickCount / 20.0);
}

@Override
public void tick() {
    // ... 现有代码
    
    if (targetPos != null && tickCount > SAFETY_DELAY_TICKS) {
        double ramp = getGuidanceRamp();
        steeringGain *= ramp;  // 应用制导斜坡
        // ... 继续追踪
    }
}
```

---

#### 建议 4: 引入"锁定计数"机制

类似 Hellfire，而不是布尔值:

```java
private float lockStrength = 0.0f;  // 0-1

@Override
public void tick() {
    if (targetInRange()) {
        // 增加锁定强度
        lockStrength = Math.min(1.0f, lockStrength + 0.02f);  // 50 ticks 满锁定
    } else {
        // 衰减锁定强度
        lockStrength = Math.max(0.0f, lockStrength - 0.1f);
    }
    
    // 引信爆炸条件
    if (distance < proximityFuse() * lockStrength && tickCount > SAFETY_DELAY_TICKS) {
        detonate();  // 锁定越强，触发距离越近
    }
}
```

---

## 第七部分：性能对比

| 指标 | Hellfire | P1 | 说明 |
|------|----------|--|----|
| **制导计算复杂度** | O(n³) 向量运算 | O(n) 向量运算 | Hellfire 更精准但更耗 CPU |
| **引信系统** | 外部控制 | 自主控制 | Hellfire 全由 Director 决定 |
| **同步开销** | 高（实时目标位置） | 低（初始参数） | Hellfire 每 frame 同步火控数据 |
| **适用场景** | 船舰级精密制导 | 个体快速投射 | Hellfire 用于大型船舰，P1 用于轻型装备 |
| **易用性** | 需配置 Director | 开箱即用 | P1 更简单 |
| **精确性** | ±0-40m（随锁定进度） | ±8m（固定） | Hellfire 可调整 |

---

## 第八部分：学习代码清单

### 必读文件

1. **Hellfire 导弹逻辑**
   - `HellFireProjectile.getForces()` - 制导力计算（PN算法）
   - `HellFireProjectile.getDragForce()` - 拖曳力计算
   
2. **火控导演逻辑**
   - `HellFireDirectorBlockEntity.updateClosest()` - 目标搜索
   - `HellFireDirectorBlockEntity.pickTrackCandidate()` - 圆锥搜索
   - `HellFireDirectorBlockEntity.updateTrack()` - 锁定计数更新
   
3. **P1 导弹逻辑**
   - `P1MissileEntity.tick()` - 生命周期管理
   - `P1MissileEntity.updateDynamicDesignatedTarget()` - 目标切换
   - `P1MissileEntity.getForces()` - 转向力计算（如有的话）

---

## 第九部分：关键代码差异速查表

| 功能 | Hellfire 代码位置 | P1 代码位置 | 核心差异 |
|------|-----------------|-----------|--------|
| 目标搜索 | `updateClosest()` | `PhysicsTargeting.findNearestPhysicsTarget()` | 圆锥 vs 距离 |
| 制导力 | `getForces() omega×v` | 速度混合 Lerp | PN vs Lerp |
| 生命周期 | `m_8119_() lifeTime++` | `tick() tickCount++` | 外部 vs 自主 |
| 引信 | 由 Director 控制精度 | `proximityFuse() & fuzeInstalled` | 外部 vs 内部 |
| 爆炸条件 | `inaccurate` 范围 | 距离 < `proximityFuse()` | 误差范围 vs 固定距离 |

---

## 总结

**Hellfire** 代表的是**高级、精确、模块化**的导弹系统，适合大型船舰级应用。

**P1** 代表的是**简洁、自主、易用**的导弹系统，适合快速部署。

### 学习方向

1. ✅ 理解 **PN 导航常数法** - Hellfire 的核心算法
2. ✅ 理解 **圆锥搜索** vs **距离搜索** - 不同的目标选择策略
3. ✅ 理解 **模块化架构** vs **单体架构** - 不同的系统设计思路
4. ✅ 理解 **制导斜坡** - 防止初期过度转向的机制
5. ✅ 理解 **锁定计数** - 逐步建立和衰减的机制

### 应用建议

- 如果需要**精确的船舰制导**，学习 Hellfire 的 **PN 算法** 和 **Director 架构**
- 如果需要**个体导弹自主作战**，保持 P1 的简洁设计，只参考 **制导斜坡** 概念
- 如果需要**两者兼有**，考虑在 P1 中添加可选的 Director 支持（建议 1）
