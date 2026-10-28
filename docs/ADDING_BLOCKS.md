# 添加新方块规范（TAOV 精简版）

> 本工程加方块/装置时的强制流程。**先问，再写代码** —— 第 0 节没问清楚之前不要开始实现：
> 多方块尺寸与挂载方式改起来会牵动模型、碰撞、掉落、UI 与测试，返工成本最高。

---

## 0. 动手前必须先问用户

1. **要不要做成多方块？** 单格能撑住模型就不做；模型明显超出 1×1×1（尤其高度 > 1）就必须做，
   否则碰撞、交互、掉落、放置全对不上。
2. **多方块尺寸**：长（沿朝向）× 宽（侧向）× 高，例如 4×2×3。
   偶数边长必然有半格问题 —— 提前确认允许模型略微探出，还是必须严格落在框内。
3. **挂载方式**（发射类装置）：能挂哪一档弹药
   - 轻型防空（9K33 档）/ 轻型反舰（Kh-35 档）/ 重型反舰（P 系列）/ S-300 专用（5V55RM）；
   - 载弹量几发；
   - 只收某一档吗，拒收提示哪句文案。
4. **发射方向**：RAIL（沿朝向上翘 30°）/ VERTICAL（一律向上）/ TUBE（完全按 FACING）。
5. **是否联动**：能否被火控终端绑定、是否参与火控网选举、是否需要火力通道。

问题一次问完，答案写进实现与 CHANGELOG，不要先做着看。

---

## 1. 多方块结构（沿用工程既有做法）

### 1.1 声明（发射类走统一基类）

```java
public class XxxBlock extends MultiblockLauncherBlock {
    public static final MultiblockStructure.Shape SHAPE = new MultiblockStructure.Shape(1, 1, 3);

    public XxxBlock(Properties properties) { super(properties, SHAPE); }

    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return isPart(state) ? null : new XxxBlockEntity(pos, state);
    }

    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        if (isPart(state)) return null;
        return createTickerHelper(type, SlimRegistry.XXX_BE.get(), LauncherBlockEntity::tick);
    }
}
```

非发射类的多方块（HMT / S300 / 指挥仪）可以直接 implements MultiblockStructure.Controller，
但装配、拆除、部件转发这六段逻辑必须与 MultiblockLauncherBlock 保持一致（优先复用）。

### 1.2 硬性约定

| 项目 | 约定 |
|---|---|
| 控制器位置 | 最底格，结构向上长 |
| 部件格 | part=true；不建方块实体、不 tick、不渲染方块模型（getRenderShape = INVISIBLE） |
| 部件格右键 | 转发到控制器（MultiblockInteraction.forwardToController） |
| 放置 | canFit 不通过就整个不放，不允许控制器在、结构缺一半 |
| 拆除 | 拆任一格整组消失，只掉一台 |
| 掉落表 | 必须有 block_state_property + part=false 条件 |
| blockstate | 四向 × part 真假 = 8 个变体 |
| 偶数边长 | cells() 从 -size/2 起算，格子中心在控制器中心 ±0.5 格；需要整格偏移用 Shape(..., forwardOffset, sideOffset) |

### 1.3 对齐（模型 ↔ 格子）

- 模型中心必须与格子均值重合，**先算再改**：
  - 前后：格子均值 == -Z(geo 中心)（Basis.NORTH 时前是 −Z）；
  - 侧向：以**实测**为准（LAL 就是归零向右偏一格、+1 才对，不能按公式硬推）。
- 已有几何守门测试的装置（如 LalAlignmentTest）改任何一侧都要同步跑。
- 渲染顺序：**先 applyFacing，再 super.preRender**（反了会绕方块角旋转）。
- GeckoLib 自带 translate(0.5, 0, 0.5)：geo 坐标 0 = 方块中心，不要重复平移。

---

## 2. 挂载方式（发射类装置）

1. 弹药档位由 SlimMissileItem.MountClass 决定：LIGHT（轻型防空）、LIGHT_ANTI_SHIP、HEAVY_ANTI_SHIP、HEAVY_ANTI_AIR。
2. 装置侧覆写 acceptsAntiShipMissiles / acceptsLightAntiAirMissiles / acceptsLightAntiShipMissiles；
   统一由 MissileMountRule.accepts(...) 判定，不要自己写 if。
3. 拒收必须给对应 lang 键（如 message.taovmnslim.light_anti_ship_needs_light_launcher），
   否则玩家只看到装不进去，不知道为什么。
4. 载弹量用 maxMissiles() + CAPACITY 常量，与 CHANGELOG 里的数字一致。

---

## 3. 发射方向

```java
@Override public LaunchMode launchMode() { return LaunchMode.VERTICAL; }   // 或 RAIL / TUBE
```

- 方向与出膛点统一走 LaunchDirection.of(facing, mode) / .muzzle(...)，不要在 fireNextMissile 里自己算向量；
- 发射架类默认 RAIL（上翘 30°）；垂发一律 VERTICAL；普通定向管 TUBE；
- VS 船上：查询与瞄准用世界坐标（VsCompat.toWorldBox / toWorld），出膛点与目标存储用本地坐标。

---

## 4. 资源清单（缺一项就是紫黑方块或创造页缺条目）

| 资源 | 路径 |
|---|---|
| 方块状态 | assets/taovmnslim/blockstates/<id>.json（四向 / part 变体） |
| 方块模型 | assets/taovmnslim/models/block/<id>.json |
| 物品模型 | assets/taovmnslim/models/item/<id>.json（builtin/entity 或 item/generated） |
| 几何体 | assets/taovmnslim/geo/<id>.geo.json（identifier 规范为 geometry.<id>） |
| 贴图 | assets/taovmnslim/textures/block/<id>.png（或 item / entity） |
| 动画 | assets/taovmnslim/animations/<id>.animation.json |
| 掉落表 | data/taovmnslim/loot_tables/blocks/<id>.json（多方块必须 part=false） |
| 配方 | data/taovmnslim/recipes/<id>.json |
| 语言 | lang/zh_cn.json + lang/en_us.json（block.taovmnslim.<id>） |
| 创造页 | SlimRegistry 的 output.accept(...) |
| 渲染器 | SlimClientEvents 注册（GeckoLib 走 SlimBlockRenderer 家族） |
| 注册 | 方块 / 物品 / 方块实体三件套（实体字段名 <X>_BE，供守门测试比对） |

加完跑一次资源审计（注册表 ↔ 资源）。

---

## 5. 必须跑的守门测试

| 测试 | 它守什么 |
|---|---|
| BlockEntityBindingTest | 方块必须创建注册表声明的方块实体（放下去 ClassCastException 的坑） |
| FacingRenderConsistencyTest | 有 FACING 的方块必须覆盖四向 blockstate；发射装置方向必须来自朝向（舰炮家族除外） |
| MultiblockLaunchersAndClickTargetingTest | 装配 / 拆除 / 部件转发 / 掉落 part=false / 结构尺寸 |
| CreativeTabCoverageTest | 每个物品都要进创造页 |
| MissileMountRuleTest | 每个装置能挂 / 不能挂的档位 |
| LalAlignmentTest 一类几何测试 | 模型中心 ↔ 格子均值（改模型或尺寸都会失败） |

新增装置时顺手加一条守门测试：把这次用户给的具体数值与交互写成断言
（尺寸、载弹量、发射模式、拒收规则），否则下次改动会悄悄改坏且没人发现。

---

## 6. 常见坑（都踩过）

1. VS 坐标系：船上方块在船坞坐标里，直接用本地方块坐标查询实体永远查不到 → 用 toWorldBox。
2. 偶数尺寸半格：模型居中在方块中心、格子居中在方块角；整格偏移与移模型二选一，别两边都动。
3. 渲染顺序：super.preRender 在 applyFacing 之前 → 模型绕角旋转（格子偏了）。
4. level 字段是 private：实体里用 level()，按 UUID 查实体用 ServerLevel#getEntity(UUID)。
5. 注释剥离型测试：源码断言前会去注释，别指望注释里的词能匹配上。
6. 高速弹：位移 > 1 格/tick 必须分步（MAX_MOVE_STEP），否则穿墙 / 穿船 / 近炸不触发。
7. 贴图三件套：blockstate、block model、item model 任一缺失，游戏里就是紫黑或隐形。

---

## 7. 交付标准

- gradlew build 全绿（含新增守门测试）；
- dist 刷新 + 两个测试实例换 jar + 真实工程镜像；
- CHANGELOG 写清：结构尺寸、挂载档位、发射模式、资源清单、测试名。
