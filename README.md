# TAOVModernNaval（正式版）—— 已归档

> **本仓库已冻结，不再添加功能。主线开发在 [`TAOVModernNavalSlim/`](TAOVModernNavalSlim/)。**
> 合并决策与差距分析见 [MERGE_PLAN.md](MERGE_PLAN.md)。

## 为什么归档

2026-10-29 核对了两边的实际能力后确认：**精简版是严格更优的实现**，正式版不再有独立价值。

| | 正式版（本仓库） | 精简版 |
|---|---|---|
| mod id | `taovmodernnaval` | `taovmnslim` |
| 外部依赖 | GeckoLib + Shao 弹道库 + CBC + Create + VS | **只有 GeckoLib** |
| 弹体 | 继承 Shao / CBC 的 HSE 弹 | 自写 `SlimMissileEntity`（三阶段制导） |
| 内容 | 8 方块 / 15 导弹 | **13 方块 / 22 物品 / 6 种发射架 / 火控终端 / 多方块** |
| 电力 | 无 | FE 计费 + 缺电降级 |
| 测试 | 49 条 | **318 条** |
| CBC / VS 兼容 | 编译期依赖 CBC 类 | **零依赖**：`CbcInterceptor` 沿继承链识别、`VsCompat` 反射 |

关键点：正式版引以为重的「CBC 集成」在精简版里做得更好——它不 import 任何 CBC 类，
装了 CBC 就自动生效，没装时全部返回空。因此「合并」实际是**把精简版缺的少量功能补过去**，
而不是搬代码。

## 已迁移到精简版的东西

| 能力 | 正式版 | 精简版 | 状态 |
|---|---|---|---|
| 配方 advancement 覆盖 | 23 个 | 36 个 | ✅ 已补（`8c02aae`） |
| 来袭导弹告警 HUD | 有 | **无** | ✅ 已移植（`74a3bde`） |
| 反舰弹数量 | 11 型 | 6 型（含 KH-35） | 按「以精简版为准」裁掉 P-1/P-30/P-45/P-100/P-120/P-300 |
| 装甲块 | 有 | 无 | ❌ 不移植（船体防护交给 VS/CBC 原版） |

## 本仓库仍然有价值的部分：历史

2026-10-29 这一轮的改动都保留在 git 历史里，供追溯：

- `0d7bbba` zh_cn 补 9 个语言键 + 修 `p270_missile` 模型 display
- `b97d5c3` 抽出 `AbstractCiwsBlockEntity`，炮塔同步从每 tick 降到每 4 tick
- `2873783` 引入 `OwnerTracked` / `IffRules`（友军识别）
- `a92cd83` 总控台指令包的距离/频率校验
- `85c6509` `BlockEntityIndex` 替换三处 O(n³) 扫描
- `9fb321f` 停用 `content/` 影子层
- 以及仓库卫生（取消跟踪构建产物、完整纳入源码树）

## 构建（仅供查阅）

```bat
:: 在工作区根目录
build_all.bat -Target full
```

产物 `build/libs/taovmodernnaval-1.0.0-SNAPSHOT.jar`。

> 依赖 `libs/` 下的 5 个第三方 jar，其中 4 个不入库，见 [libs/README.md](libs/README.md)。

## 不要做的事

- 不要再往正式版加功能（会立刻与精简版分叉）。
- 不要同时把 `taovmodernnaval` 和 `taovmnslim` 装进同一个整合包——内容高度重叠，
  会出现两套雷达、两套发射架。
- 不要改精简版的 mod id：现存存档全部挂在 `taovmnslim` 下。
