# Hellfire 导弹系统学习指南

## 概述

**Hellfire Missile** 是 TAOV（The Adventure of Valkyrien-Skies）mod 中的一个导弹系统。基于资源文件和 P1 导弹的对比分析，这里是 Hellfire 的结构推断。

## 核心组件

### 1. Hellfire 导弹实体
**Registry ID**: `taov_core:hellfire_missile`  
**显示名称**: "Hellfire Missile"

#### 推断的基类结构
```
HellFireMissileEntity extends FuzedBigCannonProjectile
  ├── 继承自 CreateBigCannons mod 的导弹基类
  ├── 支持引信机制（fuze system）
  └── 支持碰撞爆炸
```

### 2. Hellfire 块系统（三个相关块）

#### a. Hell Fire 主块
**Block ID**: `taov_core:hell_fire`  
**配置文件**: `assets/taov_core/blockstates/hell_fire.json`

**属性**（blockstate variants）:
- `facing`: 方向（up, down, north, south, east, west）
- `waterlogged`: 水中状态（true/false）

**用途**: 可能是导弹发射点或引信触发装置

---

#### b. Hell Fire Director（方向控制器）
**Block ID**: `taov_core:hell_fire_director`  
**配置文件**: `assets/taov_core/blockstates/hell_fire_director.json`

**属性**（blockstate variants）:
- `facing`: 方向（6个方向）

**推断功能**:
- 类似于 P1 导弹的 **RakLauncher** 或 **火控系统**
- 用于设置导弹的初始方向和轨迹
- 可能提供 **目标指定** 或 **预瞄** 功能

---

#### c. Hell Fire Rack（导弹架/装弹架）
**Block ID**: `taov_core:hell_fire_rack`  
**配置文件**: `assets/taov_core/blockstates/hell_fire_rack.json`

**属性**（blockstate variants）:
- `facing`: 方向（6个方向）

**推断功能**:
- 类似于 P1 导弹的 **MissileBlock**
- 用于存储和装弹导弹
- 可能提供导弹装备/组装功能

---

## 对比分析：P1 导弹 vs Hellfire 导弹

| 特性 | P1 导弹 (TAOVModernNaval) | Hellfire (TAOV) |
|------|------------------------|-----------------|
| **基类** | HEShellProjectile | FuzedBigCannonProjectile（推断） |
| **引信类型** | 碰撞引信、近炸引信、时间引信 | 推断支持碰撞引信 |
| **追踪系统** | PhysicsTargeting（物理目标搜索） | 推断有类似的追踪 |
| **发射器** | RakLauncher（火箭发射器） | Hell Fire Director（推断） |
| **存储块** | MissileBlock | Hell Fire Rack（推断） |
| **安全延迟** | 30 ticks（保护发射器） | 推断类似 |
| **最大寿命** | 600 ticks（30秒） | 未知，需查证 |

---

## 推断的类结构（基于资源和P1对比）

### HellFireMissileEntity（推断）
```java
public class HellFireMissileEntity extends FuzedBigCannonProjectile {
    
    // 常量
    private static final int SAFETY_DELAY_TICKS = 30;      // 安全延迟
    private static final int MAX_MISSILE_LIFETIME = ???;   // 最大寿命
    private static final float PHYSICS_TARGET_RANGE_BONUS = 400.0F; // 追踪范围加成
    private static final double PREDICTIVE_LEAD_DISTANCE = 0.35D;   // 预测铅量
    
    // 字段
    private boolean fuzeInstalled;           // 引信已安装
    private UUID assignedTarget;             // 指定目标（来自雷达）
    private Vec3 designatedTargetPos;        // 目标位置
    private int tickCount;                   // 运行计时
    
    // 主要方法
    @Override
    public void tick() {
        // 1. 安全延迟期 (0-30 ticks)：基础运动，无碰撞检测
        if (tickCount <= SAFETY_DELAY_TICKS) {
            // 直接设置位置，保护发射器
            return;
        }
        
        // 2. 禁用CBC自动爆炸
        setExplosionCountdown(-1);
        
        // 3. 恢复父类物理引擎
        super.tick();
        
        // 4. 碰撞引信（由 onImpact/onClip 回调处理）
        if (fuzeInstalled && tickCount > SAFETY_DELAY_TICKS) {
            // 碰撞时爆炸（通过 onImpact/onClip）
        }
        
        // 5. 追踪目标
        Vec3 targetPos = resolveAssignedTargetPos();
        Optional<Vec3> physicsTarget = PhysicsTargeting.findNearestPhysicsTarget(...);
        
        if (targetPos != null) {
            // 6. 近炸引信
            if (distance < proximityFuse() && fuzeInstalled) {
                detonate();
            }
            
            // 7. 动态转向、速度控制
            // 使用 steering gain、predictive lead 等
        }
        
        // 8. 无引信导弹的自爆逻辑
        if (!fuzeInstalled && tickCount >= MAX_MISSILE_LIFETIME) {
            detonate();
        }
    }
    
    @Override
    protected boolean onImpact(HitResult hitResult, 
            AbstractCannonProjectile.ImpactResult impactResult,
            ProjectileContext ctx) {
        // 碰撞时的爆炸逻辑
        if (fuzeInstalled && tickCount > SAFETY_DELAY_TICKS) {
            detonate(hitResult.getLocation());
        }
        return true;
    }
    
    @Override
    protected boolean onClip(ProjectileContext ctx, Vec3 from, Vec3 to) {
        // 方块碰撞时的爆炸逻辑
        if (fuzeInstalled && tickCount > SAFETY_DELAY_TICKS) {
            detonate(from.add(to).scale(0.5D));
        }
        return true;
    }
}
```

---

## 关键函数推断

### 1. 导弹生命周期
```
发射 (t=0)
  ↓
安全延迟 (t=0-30 ticks) → 保护发射器，禁止引信
  ↓
追踪激活 (t>30 ticks) → 寻找目标，启用引信
  ↓
爆炸方式 (三选一):
  ├─ 碰撞爆炸 (fuze on, 碰撞到实体)
  ├─ 近炸爆炸 (fuze on, 靠近目标)
  └─ 寿命爆炸 (fuze off, 600 ticks 后)
```

### 2. 引信系统
- **碰撞引信** (Impact Fuze): 与实体碰撞时立即爆炸
- **近炸引信** (Proximity Fuze): 靠近目标到一定距离时爆炸
- **无引信模式** (No Fuze): 按寿命到期自爆

### 3. 追踪系统
```java
// 目标搜索
PhysicsTargeting.findNearestPhysicsTarget(
    level,
    position,
    seekRange + PHYSICS_TARGET_RANGE_BONUS,  // 扩展追踪范围
    maxSpeed,
    launcherOrigin,
    iffSafeRadius
)

// 预测拦截
Vec3 targetVelocity = lastTargetVelocity;
Vec3 predictedPos = targetPos.add(
    targetVelocity.scale(PREDICTIVE_LEAD_DISTANCE)  // 0.35 的速度值
);

// 转向增益（距离越近越敏捷）
float steeringGain = steering();
if (distance < 15.0D) {
    steeringGain *= 2.0F;  // 非常敏捷
} else if (distance < 30.0D) {
    steeringGain *= 1.6F;  // 较为敏捷
} else if (distance < 60.0D) {
    steeringGain *= 1.3F;  // 中等敏捷
} else if (distance < 120.0D) {
    steeringGain *= 1.1F;  // 略微敏捷
}
```

---

## Hellfire 专有特征（推断）

基于命名和组件对比：

1. **Hell Fire Director** - 指挥控制
   - 可能提供高级火控功能
   - 多导弹协同控制？
   - 目标分配系统？

2. **Hell Fire Rack** - 导弹存储
   - 可能支持多种引信配置
   - 快速装弹系统？

3. **发射参数**
   - 可能有不同于 P1 的速度/加速度
   - 可能有更大的爆炸范围
   - 可能有特殊的追踪增强

---

## 使用参考（与 P1 类比）

### 发射流程
```
Hell Fire Director (瞄准) 
  → Hell Fire Rack (装弹)
    → 消耗导弹物品 (HellFireMissile item)
      → 生成 HellFireMissileEntity
        → 应用发射参数和目标信息
          → 进入游戏世界
```

### 配置参数（推断）
```java
// 可能的 NBT 标签
{
    FuzeInstalled: 1 (boolean),      // 引信是否安装
    FuzeTicks: 200 (int),            // 引信延迟滴答数
    AssignedTarget: [UUID] (UUID),   // 指定目标（来自雷达）
    DesignatedTargetPos: [x, y, z],  // 预设目标位置
    Profile: {...}                   // MissileProfile（性能调优）
}
```

---

## 进一步研究方向

如果需要获取完整的 Hellfire 源代码：

1. **查找 TAOV mod 源项目**
   - GitHub 仓库：verr1/TAOV (推断)
   - 获取源代码以查看完整实现

2. **反编译 Hellfire 类**
   ```bash
   # 使用 CFR 或 Fernflower 反编译 TAOV jar
   cfr taov_core-0.6.18.jar --outputdir src/
   ```

3. **比对关键代码**
   - `HellFireMissileEntity.tick()`
   - `HellFireMissileEntity.onImpact()`
   - `HellFireDirectorBlock` 火控逻辑
   - `HellFireRackBlock` 装弹逻辑

---

## 总结

Hellfire 导弹系统（基于推断）：
- ✅ 继承自 CBC 的导弹基类
- ✅ 支持三种引信模式（碰撞、近炸、时间）
- ✅ 支持主动追踪和预测拦截
- ✅ 配备火控系统（Director）和装弹系统（Rack）
- ✅ 遵循与 P1 导弹类似的生命周期逻辑

**推荐对比学习**: P1MissileEntity.java 与其 Hellfire 对应实现
