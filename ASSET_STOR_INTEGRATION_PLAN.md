# asset stor 素材库 ↔ 项目 对照清单与分阶段完善计划

**生成时间**：2026-10-03
**项目**：TAOVModernNaval（Minecraft 1.20.1 / Forge 47.3.0，GeckoLib 4.7 + Shao's Projectile Lib 1.6.4 + CBC 5.8.3）
**文档状态**：👉 **待确认**（确认后再进入实施阶段）

---

## 0. 摘要（先说结论）

1. **项目正处于 `REWRITE_PLAN.md` 描述的"重写迁移"中途**：入口 `TAOVModernNaval` 目前**只注册** `ModBootstrap` + `NavalCoreCreativeTab`，而承载了全部 P 系列导弹 / RAK 发射架 / 火控 / 近防炮的**旧注册层（`TAOVMN.registry.*`、`TAOVMN.block.*`、`TAOVMN.item.*`、`TAOVMN.entity.*`、`TAOVMN.client.render.*`）当前并未注册**，属于"死代码"。也就是说：**项目现在能跑的内容很少，大量已有实现没有接回入口。**
2. **`asset stor/` 素材库是"原始素材仓库"**，其中**只有一部分**已经被规范化并复制进 `src/main/resources`；**大部分新素材（火控终端、MR265 雷达、AK230 机炮、各类防空导弹、重型发射管、双联发射架等）尚未接入项目**，在 Java 代码中 0 引用。
3. 素材库自身**存在不完整与命名混乱**问题（大量空目录、空格文件名、重复/错配模型名等），需要先做**命名映射与整理**，不能直接复制。
4. 因此"完善项目"应分两条主线并进：
   - **主线 A（工程）**：决定旧注册层去留，把已有内容接入可运行状态（新 `content` 结构 or 恢复旧结构）。
   - **主线 B（素材）**：把 `asset stor` 的素材规范化并入 `src/main/resources`，并为缺实现的部分补 Java。

> ⚠️ 关键决策点见 **第九节**。本项已确认采用 **决策 C（并存过渡）**，阶段 1 已实施，详见下方「实施记录（阶段 1）」。

---

## 实施记录（阶段 1 · 决策 C 并存过渡）

**状态**：✅ 已实施，`gradlew compileJava` 与 `gradlew build -x test` 均 `BUILD SUCCESSFUL`。

| # | 改动 | 文件 |
|---|------|------|
| 1 | 入口同时注册**旧层**（ModBlocks/ModBlockEntities/ModEntities/ModItems/ModCreativeTabs/ModRecipes）+ **新层**（ModBootstrap/NavalCoreCreativeTab），并恢复 `GeckoLib.initialize()` | `TAOVModernNaval.java` |
| 2 | 解决 `launch_tube` 同名注册冲突：新版骨架层改名 `naval_launch_tube` | `content/NavalBlocks.java`、`content/NavalItems.java` |
| 3 | 修复 `NavalLaunchBlock` 引用未声明的 `LIT` 属性导致的放置崩溃（移除该 `onPlace`） | `content/weapon/NavalLaunchBlock.java` |
| 4 | 补注册旧层方块实体 GeckoLib 渲染器（修复 G2） | `client/ClientEvents.java` |
| 5 | 为 `naval_launch_tube` 补最小资源与语言 | `blockstates/`、`models/block/`、`models/item/`、`lang/en_us.json`、`lang/zh_cn.json` |

**待办（阶段 1 收尾，需人工在游戏内验证）**：运行 `runClient` 做冒烟测试——进入世界，确认 P 系列导弹、RAK 发射架、雷达、AK630 可见可用。

**已知遗留（非本步范围）**：新版骨架层其余方块 `naval_core_block` / `naval_turret` / `radar` 仍无 blockstate/model 资源（原本即缺失），会显示为缺模型，留待阶段 2 处理。

---

## 实施记录（阶段 2 · 资源规范化与清理）

**状态**：✅ 已完成（`gradlew build -x test` 通过）。

| # | 动作 | 明细 |
|---|------|------|
| 2.3 | 清理孤立资源 → `asset stor/_archive/unused-resources/`（保留目录结构） | `blockstates/lockingradar.json`、`models/block/lockingradar.json`、`models/item/lockingradar.json`、`animations/lokingradarscan.json`、`blockstates/ak630.json`、`models/block/ak630.json`、`assets/taovmodernnaval/p1.png`、`assets/taovmodernnaval/rak1.png`、`textures/texture.png` |
| 2.4 | 移出非运行时源文件 → `asset stor/_archive/mod-source/` | `geo/MR800.bbmodel`、`geo/lunch_tube.geo.bbmodel`、`geo/lunch_tube.geo - Converted.bbmodel`、`geo/lunch_tube.geo - Converted.json` |
| — | **保留**（经确认非孤立） | `textures/block/lockinfradar.png`（被 `models/block/p1_director.json` 引用） |

> 未执行项：**2.1**（把 asset stor 新素材复制进 resources）依赖"素材接入范围"决策（第九节决策②）；**2.5**（批量修正 geo identifier）为纯装饰性（GeckoLib 不读取该字段）；**2.6**（重命名 asset stor 内文件）与"asset stor 只读冻结"原则冲突。

---

## 实施记录（阶段 4.1 · AK230 舰炮）

**状态**：✅ 已完成（`gradlew build -x test` 通过）。

| 类别 | 文件 |
|------|------|
| 素材（复制并规范化） | `geo/ak230.geo.json`（identifier 修正为 `geometry.ak230`）、`animations/ak230.animation.json`、`textures/block/ak230.png` |
| 方块 | `block/AK230Block.java` |
| 方块实体 | `block/entity/AK230BlockEntity.java`（复用 AK630 逻辑，参数差异化：扫描 40 格 / 射速 2 tick / 伤害 3.0 / 转速 5.0） |
| 物品 | `item/AK230BlockItem.java` |
| 渲染 | `client/render/AK230BlockModel.java`、`AK230BlockRenderer.java`、`AK230ItemGeoModel.java`、`AK230ItemRenderer.java` |
| 注册 | `ModBlocks`（`ak230_block`）、`ModBlockEntities`（`ak230_block`）、`ModItems`（`ak230`）、`ModCreativeTabs`、`client/ClientEvents`（方块实体渲染器） |
| 资源 | `blockstates/ak230_block.json`、`models/block/ak230_block.json`、`models/item/ak230.json`、`lang/en_us.json`、`lang/zh_cn.json` |

**关键实现点**：AK230 的 geo 与 AK630 共用相同骨骼名（`AK630TURRET` / `AK630CANNON`），因此渲染模型类仅需替换资源路径，行为完全复用。

**待人工验证**：`runClient` 中确认 AK230 放置后模型可见、红石驱动下可追踪目标并开火。

---

## 实施记录（阶段 4.4 · 火控基础：雷达 → 指挥仪 → 发射架/发射管）

**状态**：✅ 已完成（`gradlew compileJava` 与 `gradlew build -x test` 均 `BUILD SUCCESSFUL`）。

**触发方式**：发射一律由**红石上升沿**驱动（沿用 AK630 模式，10 tick 冷却）；右键只负责装填/卸载/绑定/查询/切换 IFF。

| 类别 | 文件 |
|------|------|
| 公共发射逻辑 | `util/MissileFireControl.java`（新增：`isFuzeInstalled` / `findNearbyRadar` / `launch`，统一负责 profile、制导、目标、速度、引信与 spawn） |
| 雷达 | `block/entity/RadarBlockEntity.java`（IFF 开关、`trackedCount`、可覆盖 `scanRange()` / `scanInterval()`、`getUpdateTag` / `getUpdatePacket` 客户端同步）；`block/RadarBlock.java`（潜行+空手右键切 IFF） |
| 火控指挥仪 | `block/entity/P1DirectorBlockEntity.java`（覆盖 256 格 / 10 tick）；`block/P1DirectorBlock.java`（潜行+空手右键切 IFF） |
| RAK 发射架 | `block/entity/RakLauncherBlockEntity.java`（全量重写：红石上升沿发射、装填 2 发、引信预设 {120,200,320,480}、就近绑定雷达、读取雷达目标、NBT + 网络同步、GeckoLib 接口）；`block/RakLauncherBlock.java`（`getTicker` → `RakLauncherBlockEntity::tick`、`setPlacedBy` 消费 `PreBoundRadarPos`、`use` 交互矩阵） |
| 舰载发射管 | `block/entity/LaunchTubeBlockEntity.java`（红石上升沿发射，32 格内就近取雷达目标，10 tick 冷却）；`block/LaunchTubeBlock.java`（`use` 装填/卸载） |
| 总控台 | `block/entity/TotalControlBlockEntity.java`（每 20 tick 汇总附近雷达 `trackedCount` 与当前目标坐标，NBT + `getUpdateTag` 同步到客户端 GUI） |
| 渲染 | `client/render/P1DirectorGeoModel.java`、`P1DirectorGeoRenderer.java`（复用 mr800 资源），在 `client/ClientEvents` 注册 |
| 语言 | `lang/zh_cn.json` 补齐 `launcher_fuze` / `launcher_radar_bound` / `launcher_radar_not_found` / `launcher_radar_required` / `launcher_target_*` / `launcher_iff_*` / `launcher_status` / `launcher_arm_storage_*` / `jei.*` |

**交互矩阵（火控链路）**

| 输入 | 行为 |
|------|------|
| 红石上升沿 → RAK 发射架 | 若已装填则发射一枚（10 tick 冷却），目标取自绑定雷达 |
| 红石上升沿 → 舰载发射管 | 若已装填反舰导弹则按朝向发射，目标取自 32 格内最近雷达 |
| 手持导弹右键 | 装填（RAK 2 发 / 发射管 1 发） |
| 潜行 + 空手右键 | 发射架：卸载，无弹则循环引信预设；雷达 / 指挥仪：切换 IFF |
| 空手右键 RAK 发射架 | 未绑定时就近绑定雷达（16 格），随后显示状态行 |

**待人工验证**：`runClient` 中验证「雷达跟踪 → 发射架读取目标 → 红石发射后导弹飞向目标」；潜行右键切换 IFF 后不再锁定友好生物；总控台 GUI 能读到 `trackedCount` / 目标坐标。

---

## 实施记录（阶段 4.4 续 · 火控分发链路闭环）

**状态**：✅ 代码 + 单元测试完成（`gradlew build` = BUILD SUCCESSFUL，11 个测试全绿）。

**背景**：此前导弹端已有 `ShaoMissileProjectile.setDirectorTarget(...)` 接收器，但**没有任何地方调用它**——P-1 火控指挥仪只会扫描，不会把火控参数分发给在飞导弹，即 `DIRECTOR_FIRE_CONTROL_SYSTEM.md` 描述的"64 格参数广播"实际缺失。

| 类别 | 文件 | 内容 |
|------|------|------|
| 指挥仪火控逻辑 | `block/entity/P1DirectorBlockEntity.java` | 256 格 / 10 tick 扫描；主目标锁定强度推进（+0.05/tick，丢失 -0.10/tick）；切换主目标回退到 0.3 基线；每 5 tick 向 64 格内在飞导弹广播目标位置 + 锁定强度；锁定时粒子照射效果；NBT 持久化 + `getUpdateTag` 客户端同步；右键显示火控状态 |
| 导弹端接收 | `entity/ShaoMissileProjectile.java` | 火控目标优先级提升为最高（强度 > 0.3 且 200 tick 内刷新有效）；满锁定带来最多 15% 末段转向精度收敛；参数过期自动回退到弹载自主寻的 |
| 交互 | `block/P1DirectorBlock.java` | 空手右键 = 显示火控状态；潜行 + 空手右键 = 切换 IFF |
| 语言 | `lang/zh_cn.json`、`lang/en_us.json` | 新增 `director_status` / `director_no_target` |
| 测试 | `src/test/java/TAOVMN/block/entity/P1DirectorBlockEntityTest.java` | 8 个用例覆盖锁定强度推进/衰减/回退、主目标切换判定、容差语义 |
| 清理 | 删除 `mixin/ExampleMixin.java` 与 `taovmodernnaval.mixins.json` | MDK 模板残留：该 mixin 未在 `mods.toml` 注册（也没有 manifest `MixinConfigs`），属于永不生效的死代码 |

**测试发现并修复的真实缺陷**：主目标切换容差原本写作 `distanceToSqr(...) > TARGET_SWITCH_TOLERANCE`，即用 **1e-3 的容差去比平方距离**（等效容差仅约 0.03 格）。敌舰每 tick 的普通位移都会触发"目标切换"，使锁定强度被反复打回 0.3 基线，导弹几乎无法获得有效火控。现改为 `TARGET_SWITCH_TOLERANCE = 4.0` 格并在比较时平方。

**开发运行配置修复**（`build.gradle`）：

| 问题 | 修复 |
|------|------|
| `runClientFast` 报 `No main class specified` | 自定义 run 名不在 userdev `config.json` 的 runs 列表里，不会自动继承主类；为 `clientFast` 建立 `parent(0, null, 'client')` 继承关系 |
| `runClientFast` 缺少 `--add-opens` 模块参数 | 同上，继承内置 client run 的模块参数 |
| `prepareFastDevRuntime` 把 `libs/` 里同名但未重映射的 Create 塞进 `run-fast/mods` | 改为只从 `run/mods` 取已验证可用的 jar，并用 `Sync` 保持目录干净 |
| 缺少无头验证入口 | 新增 `serverFast` run（继承内置 `server`，`--nogui --port 25599`），用于在无客户端 mixin 的情况下验证注册表/数据包 |

**⚠️ 环境级阻塞（非本模组代码问题）**：本工作区的 Gradle 开发运行**目前无法启动游戏**，所有 profile（`runClient`、`runClientFast`、`runServerFast`）都在 mixin 应用阶段崩溃，且是**外部模组**的 mixin 找不到 SRG 名：

```
[FATAL] [mixin/]: Mixin apply failed valkyrienskies-common.mixins.json:client.MixinMinecraft
  -> net.minecraft.client.Minecraft: @Shadow method m_91092_ was not located ...
[FATAL] [mixin/]: Mixin apply failed mixins.shaolib.json:MixinMinecraftServer
  -> net.minecraft.server.MinecraftServer: @Shadow field f_129744_ was not located ...
```

即 dev 环境下 mixin refmap 没有被重映射（SRG → Mojang official），与 `create.mixins.json` 报的是同一类错误。参考 [Architectury: Fixing ForgeGradle Mixin refmaps](https://docs.architectury.dev/loom/fg_mixin_refmaps/) 与 [Mixin#462](https://github.com/SpongePowered/Mixin/issues/462)，可用 `mixin.env.refMapRemappingFile` / `net.minecraftforge.gradle.GradleStart.srg.srg-mcp` 修正；**该问题在本次改动之前就已存在**（`run/logs/latest.log` 中 10-02 20:42 的那次运行同样以 Valkyrien Skies 的同类 `@Shadow` 失败告终）。

因此在修复该环境问题（或改用此前可正常启动的 IDE/Eclipse 启动组）之前，**游戏内冒烟测试无法进行**，只能做到：`gradlew build` 通过 + 11 个单元测试通过 + jar 产出。

---


## 一、项目当前运行状态

### 1.1 入口与注册链

`src/main/java/TAOVMN/TAOVModernNaval.java`：

```java
ModBootstrap.register(modEventBus);      // 新内容层
NavalCoreCreativeTab.register(modEventBus);
```

`src/main/java/TAOVMN/core/ModBootstrap.java` 注册的新内容层：

| 类型 | 注册内容 | 资源命名空间 |
|------|----------|--------------|
| Block | `naval_core_block`、`naval_turret`、`radar`、`launch_tube` | taovmodernnaval |
| Item | `naval_core_block`、`naval_turret`、`radar`、`launch_tube`、`naval_missile` | taovmodernnaval |
| BlockEntity | `naval_turret`、`radar` | taovmodernnaval |
| Entity | `naval_missile` | taovmodernnaval |

### 1.2 已"落地但未注册"的旧层（死代码）

| 文件 | 内容 | 状态 |
|------|------|------|
| `registry/ModBlocks.java` | `rak_launcher`、`launch_tube`、`radar_block`、`total_control_block`、`armor_block`、`p1_director`、`ak630_block` | ❌ 未注册 |
| `registry/ModItems.java` | 上述方块物品 + `missile_fuze` + 11 种 P 系列导弹 | ❌ 未注册 |
| `registry/ModBlockEntities.java` | `rak_launcher`、`launch_tube`、`radar_block`、`total_control_block`、`p1_director`、`ak630_block` | ❌ 未注册 |
| `registry/ModEntities.java` | 11 个 Shao 弹体类型（`p1_missile`…`p800_missile`） | ❌ 未注册 |
| `client/render/*` | AK630/雷达/发射架/发射管/导弹的 GeckoLib 模型与渲染器（约 20 个类） | ❌ 未注册（无 `BlockEntityRenderers` / `RegisterRenderers`） |
| `block/entity/*` | RakLauncher / Radar / LaunchTube / TotalControl / P1Director / AK630 方块实体逻辑 | ❌ 随方块一起闲置 |

> 影响：当前游戏里**看不到** P 系列导弹、RAK 发射架、火控指挥仪、AK630 等既有内容。

### 1.3 结论

项目并非"缺一点资源"，而是**处于结构切换的中间态**。任何"用素材库完善项目"的工作，都必须先确定新/旧结构的主次，否则会出现"实现了却加载不到"的返工。

---

## 二、`asset stor/` 素材库清单

> 说明：`identifier` 指 geo.json 内部字段（GeckoLib 以传入的 `ResourceLocation` 定位文件，该字段基本不参与加载，但可作为"素材来源/血缘"线索）。标 ⚠️ 的为异常项。

### 2.1 BLOCK（方块素材）

| 素材 | 目录 | 文件 | geo identifier | 备注 |
|------|------|------|----------------|------|
| 火控终端 | `BLOCK/fire-control/FIRECONTROL_TERMINAL` | `FIRECONTROL_TERMINAL .geo.json`（⚠️ 文件名含空格）、`texture.png` | `geometry.FIRECONTROL_TERMINAL - Converted` | 完整（geo+贴图） |
| MR265 雷达 | `BLOCK/fire-control/RADAR/MR265` | `MR265 .geo.json`（⚠️ 空格）、`texture.png` | `geometry.unknown` | 完整 |
| 防空导弹架 | `BLOCK/lunch system/AA-MISSILE-RAK` | `AA-MISSILE-RAK.geo.json`、`AA-MISSILE-RAK-ONE .png`（⚠️ 空格） | `geometry.unknown` | 完整 |
| 重型发射管 HMT | `BLOCK/lunch system/HMT` | `HEAVY_MISSILE_TUBE.bbmodel`、`HML-LUNCH.animation.json`、`HML.geo.json`、`HML.png` | `geometry.unknown` | 完整（含动画） |
| 双联导弹架 | `BLOCK/lunch system/missile-rak-dual` | `missile-rak-dual.geo.json`、`missile-rak-dual.png`、`model.animation.json` | `geometry.unknown` | 完整（含动画） |

### 2.2 WEAPON/CANNON（舰炮素材）

| 素材 | 目录 | 文件 | geo identifier | 备注 |
|------|------|------|----------------|------|
| AK230 机炮 | `WEAPON/CANNON/AK230` | `Ak230.animation.json`、`Ak230.geo.json`、`ak230.png` | ⚠️ `geometry.Ak630.geo - Converted - Converted`（与 AK630 相同，疑似复制未改） | 完整 |
| AK630 机炮 | `WEAPON/CANNON/AK630` | `ak630.animation.json`、`ak630.geo.json`、`ak630.png` | ⚠️ `geometry.Ak630.geo - Converted - Converted` | 完整（项目已用 `ak630.*`） |

### 2.3 WEAPON/MISSILE/ANTI AIR（防空导弹素材）

| 类别 | 素材 | 目录 | 文件 | geo identifier | 备注 |
|------|------|------|------|----------------|------|
| heavy | S300 发射装置 | `.../heavy/s300` | `s300 lunch.geo.json`（⚠️ 空格）、`s300.png`、`s300lunch.json` | `geometry.unknown` | 完整 |
| heavy | S300 弹药 5V55RM | `.../heavy/s300/AMMO` | `5v55rm.geo.json`、`5V55RM.png` | `geometry.unknown` | 完整 |
| heavy | S350 | `.../heavy/s350` | （空目录） | — | ❌ 无素材 |
| light | 9K33 | `.../light/9K33` | `9k33.geo.json`、`texture.png` | `geometry.unknown` | 完整（贴图名为 `texture.png`） |
| light | 9k60 | `.../light/9k60` | `9k60.geo.json`、`texture.png` | `geometry.unknown` | 完整 |
| light | 9m100 | `.../light/9m100` | （空目录） | — | ❌ 无素材 |
| light | 9m330 | `.../light/9m330` | （空目录） | — | ❌ 无素材 |
| MIDIUM | 9m317 | `.../MIDIUM/9m317` | （空目录） | — | ❌ 无素材 |
| MIDIUM | 9m38 | `.../MIDIUM/9m38` | ⚠️ `9k33.geo.json`（文件名错，应为 9m38）、`texture.png` | `geometry.unknown` | 文件存在但命名错误 |
| MIDIUM | 9m96 | `.../MIDIUM/9m96` | （空目录） | — | ❌ 无素材 |

### 2.4 WEAPON/MISSILE/ANTI SHIP（反舰导弹素材）

| 素材 | 目录 | 文件 | geo identifier | 备注 |
|------|------|------|----------------|------|
| BASE 贴图 | `.../ANTI SHIP` | `BASEtexture.png` | — | 公共贴图 |
| P1 | `.../ANTI SHIP/P1` | （空目录） | — | ❌ 无素材（项目已有 `geo/p-1.geo.json`） |
| P15 | `.../ANTI SHIP/P15` | `p-15.geo.json` | `geometry.p-15 - Converted` | 仅 geo，无贴图/动画 |
| P120 | `.../ANTI SHIP/P120` | `p-120.geo.json` | `geometry.unknown` | 仅 geo |
| P270 | `.../ANTI SHIP/P270` | （空目录） | — | ❌ 无素材（项目已有 `geo/p-270.geo.json`） |
| P35 | `.../ANTI SHIP/P35` | （空目录） | — | ❌ 无素材，项目也缺此型号 |
| P500 | `.../ANTI SHIP/P500` | `P500.geo.json`（⚠️ 命名不符规范，应 `p-500`）、`P500.png`、`model.animation.json` | `geometry.unknown` | 完整 |
| P70 | `.../ANTI SHIP/P70` | （空目录） | — | ❌ 无素材，项目也缺此型号 |
| P700 | `.../ANTI SHIP/P700` | `p-700.geo.json`、`p-700.png`、`p-700lunch.json` | `geometry.unknown` | 完整（`p-700lunch.json` 疑为发射动画，需甄别） |
| P800 | `.../ANTI SHIP/P800` | `p-800.geo.json`、`p-800.png`、`p-800.geo.bbmodel`、`p-800.geo - Converted.bbmodel` | `geometry.unknown` | 完整（bbmodel 为 Blockbench 源文件） |

### 2.5 `_archive/mod-legacy-geo`（历史归档）

`p-1`、`p-120`、`p-15`、`p-500`、`p-700`、`p-800` 的旧版 `.geo.json`（identifier 如 `geometry.p1_converted`、`geometry.p-15 - Converted`）。属**归档备份**，不参与加载。

---

## 三、现有 `src/main/resources` 清单

命名空间：`assets/taovmodernnaval`、`data/taovmodernnaval`。

### 3.1 资源文件

| 类别 | 文件 |
|------|------|
| `animations/` | `ak630`、`lokingradarscan`、`mr800`、`p-1`、`p-15`、`p-30`、`p-45`、`p-100`、`p-120`、`p-270`、`p-300`、`p-500`、`p-700`、`p-800`、`rak_launcher`（`.animation.json`） |
| `geo/` | `ak630`、`lunch_tube`、`mr800`、`p-1`、`p-15`、`p-30`、`p-45`、`p-100`、`p-120`、`p-270`、`p-300`、`p-500`、`p-700`、`p-800`、`rak_launcher`（`.geo.json`）；另含 bbmodel：`lunch_tube.geo.bbmodel`、`lunch_tube.geo - Converted.bbmodel`、`lunch_tube.geo - Converted.json`、`MR800.bbmodel` |
| `blockstates/` | `ak630`、`ak630_block`、`armor_block`、`launch_tube`、`lockingradar`、`p1_director`、`radar_block`、`rak_launcher`、`total_control_block` |
| `models/block/` | `ak630`、`ak630_block`、`armor_block`、`launch_tube`、`lockingradar`、`p1_director`、`radar_block`、`rak_launcher`、`total_control_block` |
| `models/item/` | `ak630`、`armor_block`、`launch_tube`、`lockingradar`、`p1_director`、`radar_block`、`rak_launcher`、`total_control_block`、`p1_missile`、`p15_missile`、`p30_missile`、`p45_missile`、`p100_missile`、`p120_missile`、`p270_missile`、`p300_missile`、`p500_missile`、`p700_missile`、`p800_missile` |
| `textures/block/` | `ak630`、`lockinfradar`、`lunchtube`、`mr800`、`rak_launcher` |
| `textures/item/` | `p-1`、`p-15`、`p-30`、`p-45`、`p-100`、`p-120`、`p-270`、`p-300`、`p-500`、`p-700`、`p-800`、`rak_launcher` |
| `textures/`（根） | `texture.png`（疑似未归类残留） |
| `lang/` | `en_us.json`、`zh_cn.json` |
| `data/.../munition_properties/block_propellant/` | `p1_missile`、`p15_missile`、`p30_missile`、`p45_missile`、`p100_missile`、`p120_missile`、`p270_missile`、`p300_missile`、`p500_missile`、`p700_missile`、`p800_missile`（`.json`） |
| `data/.../recipes/` | `fuze_install.json`、`missile_fuze.json` |

### 3.2 渲染管线引用的资源（`client/render/*`）

| 渲染器 | Model | Texture | Animation |
|--------|-------|---------|-----------|
| `MissileItemGeoModel` | `geo/p-<型号>.geo.json` | `textures/item/p-<型号>.png`（P700→P800 贴图复用） | `animations/p-<型号>.animation.json` |
| `RadarGeoModel` | `geo/mr800.geo.json` | `textures/block/mr800.png` | `animations/mr800.animation.json` |
| `LaunchTubeGeoModel` | `geo/lunch_tube.geo.json` | `textures/block/lunchtube.png` | `animations/rak_launcher.animation.json` |
| `RakLauncherGeoModel` | `geo/rak_launcher.geo.json` | `textures/block/rak_launcher.png` | `animations/rak_launcher.animation.json` |
| `AK630BlockModel` | `geo/ak630.geo.json` | `textures/block/ak630.png` | `animations/ak630.animation.json` |

> 规范来源：`client/render/MissileAssetNames.java`（`p1_missile` → 资源基名 `p-1`）。

---

## 四、素材 ↔ 项目 对照表（含命名映射与状态）

状态图例：✅ 已接入 ｜ 🟡 素材有、项目无实现 ｜ 🔴 素材与项目都缺 ｜ ⚠️ 存在但命名/字段异常

### 4.1 反舰导弹 P 系列

| 型号 | registry 名（建议） | 资源基名 | geo | 贴图 | 动画 | 素材库 | 项目 | 状态 |
|------|--------------------|----------|-----|------|------|--------|------|------|
| P1 | `p1_missile` | `p-1` | `geo/p-1.geo.json` | `textures/item/p-1.png` | `p-1` | 空目录 | 有 | ✅ |
| P15 | `p15_missile` | `p-15` | `geo/p-15.geo.json` | `textures/item/p-15.png` | `p-15` | 仅 geo | 有 | ✅（素材不完整） |
| P30 | `p30_missile` | `p-30` | `geo/p-30.geo.json` | `textures/item/p-30.png` | `p-30` | 无 | 有 | ✅ |
| P45 | `p45_missile` | `p-45` | `geo/p-45.geo.json` | `textures/item/p-45.png` | `p-45` | 无 | 有 | ✅ |
| P100 | `p100_missile` | `p-100` | `geo/p-100.geo.json` | `textures/item/p-100.png` | `p-100` | 无 | 有 | ✅ |
| P120 | `p120_missile` | `p-120` | `geo/p-120.geo.json` | `textures/item/p-120.png` | `p-120` | 仅 geo | 有 | ✅（素材不完整） |
| P270 | `p270_missile` | `p-270` | `geo/p-270.geo.json` | `textures/item/p-270.png` | `p-270` | 空目录 | 有 | ✅ |
| P300 | `p300_missile` | `p-300` | `geo/p-300.geo.json` | `textures/item/p-300.png` | `p-300` | 无 | 有 | ✅ |
| P500 | `p500_missile` | `p-500` | `geo/p-500.geo.json` | `textures/item/p-500.png` | `p-500` | ✅（`P500.*`） | 有 | ✅ |
| P700 | `p700_missile` | `p-700` | `geo/p-700.geo.json` | `textures/item/p-800.png`（复用） | `p-700` | ✅ | 有 | ✅ |
| P800 | `p800_missile` | `p-800` | `geo/p-800.geo.json` | `textures/item/p-800.png` | `p-800` | ✅ | 有 | ✅ |
| **P35** | `p35_missile` | `p-35` | `geo/p-35.geo.json` | `textures/item/p-35.png` | `p-35` | ❌ 空 | ❌ 无 | 🔴 **素材与实现都缺** |
| **P70** | `p70_missile` | `p-70` | `geo/p-70.geo.json` | `textures/item/p-70.png` | `p-70` | ❌ 空 | ❌ 无 | 🔴 **素材与实现都缺** |

### 4.2 方块 / 舰炮 / 防空

| 素材 | 建议 registry 名 | 建议资源路径 | Java 侧 | 素材 | 状态 |
|------|------------------|--------------|---------|------|------|
| AK630 | `ak630_block` | `geo/ak630.geo.json`、`textures/block/ak630.png`、`animations/ak630.animation.json` | `AK630Block` + `AK630BlockEntity` + 渲染器（已有，未注册） | ✅ | ✅（待注册） |
| **AK230** | `ak230_block` | `geo/ak230.geo.json`、`textures/block/ak230.png`、`animations/ak230.animation.json` | 需复制 AK630 结构新建 | ✅ | 🟡 |
| RAK 发射架 | `rak_launcher` | `geo/rak_launcher.geo.json`、`textures/block/rak_launcher.png`、`animations/rak_launcher.animation.json` | `RakLauncherBlock(+Entity)`（已有，未注册） | ✅ | ✅（待注册） |
| 发射管 | `launch_tube` | `geo/lunch_tube.geo.json`、`textures/block/lunchtube.png` | `LaunchTubeBlock(+Entity)`（已有，未注册） | ✅ | ✅（待注册） |
| **HMT 重型发射管** | `heavy_missile_tube` | `geo/heavy_missile_tube.geo.json`、`textures/block/heavy_missile_tube.png`、`animations/heavy_missile_tube.animation.json` | 需新实现 | ✅ | 🟡 |
| **双联导弹架** | `missile_rak_dual` | `geo/missile_rak_dual.geo.json`、`textures/block/missile_rak_dual.png`、`animations/missile_rak_dual.animation.json` | 需新实现 | ✅ | 🟡 |
| **AA 导弹架** | `aa_missile_rak` | `geo/aa_missile_rak.geo.json`、`textures/block/aa_missile_rak.png` | 需新实现 | ✅ | 🟡 |
| 雷达 MR800 | `radar_block` | `geo/mr800.geo.json`、`textures/block/mr800.png`、`animations/mr800.animation.json` | `RadarBlock(+Entity)`（已有，未注册） | ✅ | ✅（待注册） |
| **雷达 MR265** | `radar_mr265` | `geo/mr265.geo.json`、`textures/block/mr265.png` | 需新实现 | ✅ | 🟡 |
| 火控指挥仪 | `p1_director` | `geo/...`（当前无对应素材） | `P1DirectorBlock(+Entity)`（已有，未注册） | ❌ | ✅（待注册） |
| **火控终端** | `fire_control_terminal` | `geo/fire_control_terminal.geo.json`、`textures/block/fire_control_terminal.png` | 需新实现 | ✅ | 🟡 |
| 总控台 | `total_control_block` | 现有 `models/block/total_control_block.json` | `TotalControlBlock(+Entity)`（已有，未注册） | ❌（仅 item 图标） | ✅（待注册） |
| 装甲块 | `armor_block` | 现有 | `ArmorBlock`（已有，未注册） | ❌ | ✅（待注册） |

### 4.3 防空导弹

| 素材 | 建议弹药物品 registry 名 | 建议资源路径 | 发射装置 | 状态 |
|------|--------------------------|--------------|----------|------|
| S300（5V55RM） | `s300_missile` | `geo/s300_missile.geo.json`、`textures/item/5v55rm.png` | `s300_launcher`（`geo/s300_launcher.geo.json`） | 🟡 |
| 9K33 | `9k33_missile` | `geo/9k33.geo.json`、`textures/item/9k33.png` | 需新实现 | 🟡 |
| 9k60 | `9k60_missile` | `geo/9k60.geo.json`、`textures/item/9k60.png` | 需新实现 | 🟡 |
| 9m38 | `9m38_missile` | `geo/9m38.geo.json`、`textures/item/9m38.png` | 需新实现 | ⚠️ 素材文件名错（现为 `9k33.geo.json`） |
| 9m100 / 9m330 / 9m317 / 9m96 / S350 | — | — | — | 🔴 素材与实现都缺 |

---

## 五、缺口分析

### 5.1 工程缺口（最优先）

| 编号 | 缺口 | 说明 |
|------|------|------|
| G1 | **入口未注册旧层** | P 系列导弹、RAK、火控、AK630 等全部不在 `TAOVModernNaval` 的注册链上，游戏内不可见 |
| G2 | **方块实体渲染器未注册** | 无 `EntityRenderersEvent.RegisterRenderers`（方块实体）注册，GeckoLib 方块不会显示模型 |
| G3 | **物品渲染器未注册** | 无 `RegisterClientExtensionsEvent` / 物品 GeoRenderer 注册，导弹/舰炮物品无 3D 模型 |
| G4 | **两套结构并存** | `content/*`（新）与 `registry/block/item/entity/*`（旧）重复且语义重叠，需统一 |
| G5 | **创造模式标签单一** | 仅 `NavalCoreCreativeTab` 列出新层 5 项，旧物品未进任何标签 |

### 5.2 素材缺口

| 编号 | 缺口 | 影响 |
|------|------|------|
| M1 | P35 / P70 型号**素材与实现都缺** | 无法补齐，需先建模 |
| M2 | 防空型号 9m100、9m330、9m317、9m96、S350 目录为空 | 无法实现，需先建模 |
| M3 | 反舰 P1/P15/P120/P270 素材目录为空或仅 geo | 现有资源可能为二手来源，血缘不明 |
| M4 | 火控指挥仪 `p1_director` 无专属 geo 素材 | 只能用现有资源/占位 |
| M5 | `texture.png` 在多个素材目录**重名** | 复制时必须重命名，否则覆盖 |

### 5.3 命名 / 数据质量缺口

| 编号 | 问题 | 位置 | 处理建议 |
|------|------|------|----------|
| Q1 | 文件名含空格 | `FIRECONTROL_TERMINAL .geo.json`、`MR265 .geo.json`、`AA-MISSILE-RAK-ONE .png`、`s300 lunch.geo.json` | 复制时去空格 + 全小写 + 下划线 |
| Q2 | 文件名大小写混用 | `Ak230.*` vs `ak230.png`；`5v55rm.geo.json` vs `5V55RM.png` | 统一小写 |
| Q3 | 模型名与目录名不符 | `MIDIUM/9m38/9k33.geo.json` | 重命名为 `9m38.geo.json` |
| Q4 | `P500.geo.json` 不符规范 | 应 `p-500.geo.json` | 重命名 |
| Q5 | geo `identifier` 异常 | AK230 与 AK630 同名；大量 `geometry.unknown` | 建议批量修正为 `geometry.<资源名>` |
| Q6 | `.bbmodel` 源文件混入资源目录 | `geo/*.bbmodel`、`geo/lunch_tube.geo - Converted.json` | 移出 `resources`（放素材库/忽略） |
| Q7 | `p-700lunch.json` / `s300lunch.json` 语义不明 | 素材库 | 甄别是动画还是发射逻辑配置 |

### 5.4 项目内孤岛资源

| 编号 | 问题 | 说明 |
|------|------|------|
| R1 | `blockstates/lockingradar.json` + `models/block/lockingradar.json` + `textures/block/lockinfradar.png` | 无 `lockingradar` 方块注册 → 孤立 |
| R2 | `blockstates/ak630.json` 与 `ak630_block.json` 并存 | 注册名只有一个 `ak630_block` → 其一孤立 |
| R3 | `animations/lokingradarscan.json` | 无渲染器引用（拼写 `loking`） |
| R4 | `textures/texture.png`（根目录） | 未归类 |
| R5 | `p1.png`、`rak1.png`（`assets/taovmodernnaval/` 根） | 未归类 |

---

## 六、问题与风险清单

| 风险 | 等级 | 说明 | 缓解 |
|------|------|------|------|
| 迁移方向未定导致返工 | 🔴 高 | 新/旧结构并存 | 先确认第九节决策 |
| 素材命名不规范直接复制 | 🟠 中 | 空格/重名/大小写会导致覆盖或加载失败 | 建立映射表，复制时重命名 |
| 空目录被误认为"有素材" | 🟠 中 | P35/P70/多型防空为空 | 以本清单为准 |
| `identifier` 字段误导 | 🟢 低 | GeckoLib 不依赖该字段 | 可后置修正 |
| 旧层注册后资源冲突 | 🟠 中 | 旧 `launch_tube` 与新 `launch_tube` 同名 | 决定唯一注册名 |
| 渲染器未注册 | 🔴 高 | 模型不显示 | 补 `RegisterRenderers` 事件 |
| 大范围改动缺少测试 | 🟠 中 | 仅 `NavalSystemsTest` 一个测试 | 每阶段手动跑 `runClient` + 编译 |

---

## 七、分阶段完善计划

> 原则：**先让已有内容可运行，再做资源规范化，最后才扩张新内容**。每阶段结束都要能编译 + `runClient` 冒烟通过。

### 阶段 0：决策与冻结（预计 0.5 天）

- [ ] 确认迁移方向（见第九节决策 ①）→ 决定后续所有命名与注册方式
- [ ] 确认本次素材接入范围（P 系列 / 方块 / 防空 / 全都要）
- [ ] 冻结 `asset stor` 只读，改动都落到 `src/main/resources`

### 阶段 1：工程基线，让现有内容跑起来（预计 1 天）🔴 必做

| 任务 | 涉及文件 | 产出 |
|------|----------|------|
| 1.1 入口接回注册链 | `TAOVModernNaval.java`、`registry/ModBlocks/ModItems/ModEntities/ModBlockEntities.java` | 旧内容可加载（或按方向迁移到 `content`） |
| 1.2 注册方块实体渲染器 | 新增/完善 `client/ClientEvents.java` 的 `RegisterRenderers` | AK630/雷达/发射架/发射管显示模型 |
| 1.3 注册物品渲染器 | GeckoLib `RegisterClientExtensionsEvent`（或 `IClientItemExtensions`） | 导弹物品 / AK630 物品显示 3D 模型 |
| 1.4 整理创造标签 | `registry/ModCreativeTabs.java` / `core/NavalCoreCreativeTab.java` | 所有物品可取出 |
| 1.5 冒烟验证 | `gradlew build -x test` + `runClient` | 编译通过、进入世界、方块/导弹可见 |

### 阶段 2：资源规范化（预计 0.5–1 天）🟠 建议

- [ ] 2.1 按第四节映射表，把 `asset stor` 素材复制并**重命名**进 `src/main/resources`
- [ ] 2.2 统一命名：去空格、全小写、下划线；`texture.png` → `<资源名>.png`
- [ ] 2.3 清理孤岛（R1–R5）：`lockingradar`、重复 `ak630` 状态、根目录散图
- [ ] 2.4 移出 `.bbmodel` 与 `* - Converted.json` 源文件（不打包进 jar）
- [ ] 2.5 修正 geo `identifier`（Q5）为 `geometry.<资源名>`
- [ ] 2.6 重命名 `P500.geo.json` → `p-500.geo.json`、`9m38/9k33.geo.json` → `9m38.geo.json`

### 阶段 3：反舰 P 系列补齐（预计 0.5 天，**依赖素材**）🟡

- [ ] 3.1 P35：`MissileProfile.p35()` + `ModItems.P35_MISSILE` + `ModEntities.P35_SHAO` + 语言 + 资源（素材缺失，需先建模或暂用占位）
- [ ] 3.2 P70：同上
- [ ] 3.3 更新 `MISSILE_TYPES` 与 `MissileProfile.resolve`

### 阶段 4：防空 / 舰炮 / 发射装置体系（预计 2–4 天）🟡 大工程

按子系统拆分，每个子系统独立可交付：

| 子阶段 | 内容 | 参考现有实现 |
|--------|------|--------------|
| 4.1 AK230 舰炮 | `AK230Block(+Entity)` + 模型/动画/贴图 + 渲染器；差异仅弹速/射速 | 复制 `block/AK630Block*` + `client/render/AK630*` |
| 4.2 防空导弹弹药 | `s300_missile`(5V55RM)、`9k33_missile`、`9k60_missile`、`9m38_missile` | 复制 `item/MissileItem` + `util/MissileProfile` |
| 4.3 发射装置 | `s300_launcher`、`aa_missile_rak`、`missile_rak_dual`、`heavy_missile_tube` | 参考 `LauncherBlockItem` / `block/LaunchTubeBlock` |
| 4.4 雷达与火控 | `radar_mr265`、`fire_control_terminal` | 参考 `block/RadarBlock*`、`block/P1DirectorBlock*` |

### 阶段 5：整合与验证（预计 1 天）

- [ ] 5.1 补齐 `lang/en_us.json`、`zh_cn.json`
- [ ] 5.2 配方（`data/.../recipes`）与 `munition_properties`
- [ ] 5.3 JEI 信息刷新（`compat/jei/TaovJeiPlugin`）
- [ ] 5.4 编译 + `runClient` 全流程验证 + 更新本文档为"已实施"

---

## 八、工作量估算

| 阶段 | 内容 | 预估 | 依赖 |
|------|------|------|------|
| 0 | 决策与冻结 | 0.5 天 | 你确认 |
| 1 | 工程基线（注册 + 渲染器） | 1 天 | 阶段 0 |
| 2 | 资源规范化（映射 + 复制 + 清理） | 0.5–1 天 | 阶段 1 |
| 3 | 反舰 P35/P70 | 0.5 天 | **素材缺失，需先建模** |
| 4.1 | AK230 舰炮 | 0.5 天 | 阶段 1 |
| 4.2 | 防空弹药（S300/9K33/9k60/9m38） | 1 天 | 阶段 1 |
| 4.3 | 发射装置（S300/AA/双联/HMT） | 1.5 天 | 阶段 1 |
| 4.4 | 雷达/火控（MR265/火控终端） | 1 天 | 阶段 1 |
| 5 | 语言/配方/JEI/测试 | 1 天 | 全部 |

**合计（不含缺失素材的建模）**：约 **6–8 个工作日**。
**缺口素材（P35/P70/9m100/9m330/9m317/9m96/S350）需额外美术工时**，无法由代码侧补齐。

> 建议按"**阶段 1 → 阶段 2**"先交付一个"现有内容可运行 + 资源规范"的中间版本，再逐个子系统（4.x）迭代。

---

## 九、待确认决策

### 决策 ①（最关键）迁移方向

| 选项 | 含义 | 影响 |
|------|------|------|
| A. 以**旧 registry 层**为主 | 把 `ModBlocks/ModItems/ModEntities/ModBlockEntities` 接回入口，放弃/搁置 `content` 骨架 | 改动小、可立刻恢复 P 系列 / RAK / 火控 / AK630；与 `REWRITE_PLAN.md` 目标相悖 |
| B. 以**新 content 层**为主 | 继续 `REWRITE_PLAN.md`，把旧层逐步迁移进 `content`，旧层最终删除 | 结构干净、符合既定计划；工作量大，需重写注册 |
| C. **并存过渡** | 新层继续，同时把旧层接回入口，二者共存先跑起来 | 最快见效；但存在 `launch_tube`/`radar` 等同名注册冲突，需改名 |

> 建议：若目标是"尽快用素材库完善可见内容"→ 选 **C（并存过渡）**；若目标是"长期工程质量"→ 选 **B**。

### 决策 ②：素材接入范围

- [ ] 仅 P 系列（补齐 + 规范化）
- [ ] P 系列 + 方块/舰炮
- [ ] 全部（含防空体系）—— 阶段 4 全做

### 决策 ③：命名规范

- [ ] 沿用现有规范：物品 `pX_missile` / 资源 `p-<型号>`（推荐）
- [ ] 其他（请说明）

### 决策 ④：缺失素材（P35/P70/9m100…）如何处理

- [ ] 先跳过，等美术素材
- [ ] 用现有模型临时占位

---

## 十、验收标准（实施后逐条核对）

- [ ] `gradlew build -x test` 编译成功、0 错误
- [ ] `runClient` 进入世界无崩溃
- [ ] `asset stor` 中**已确认接入范围**的素材全部有对应 `geo`/`textures`/`animations` 且命名规范
- [ ] 每个新 registry 名都有 `lang/en_us.json` + `zh_cn.json` 条目
- [ ] 所有方块实体有已注册的 GeckoLib 渲染器
- [ ] 所有导弹/舰炮物品有已注册的物品渲染器
- [ ] 无孤立资源（无对应注册的 blockstate/model/geo/animation）
- [ ] 本文档状态更新为"已实施"

---

## 附录 A：命名映射速查（源 → 目标）

| 源（asset stor） | 目标资源名 | 类型 |
|------------------|-----------|------|
| `FIRECONTROL_TERMINAL .geo.json` / `texture.png` | `fire_control_terminal.geo.json` / `fire_control_terminal.png` | block |
| `MR265 .geo.json` / `texture.png` | `mr265.geo.json` / `mr265.png` | block |
| `AA-MISSILE-RAK.geo.json` / `AA-MISSILE-RAK-ONE .png` | `aa_missile_rak.geo.json` / `aa_missile_rak.png` | block |
| `HML.geo.json` / `HML.png` / `HML-LUNCH.animation.json` | `heavy_missile_tube.geo.json` / `.png` / `.animation.json` | block |
| `missile-rak-dual.geo.json` / `.png` / `model.animation.json` | `missile_rak_dual.geo.json` / `.png` / `.animation.json` | block |
| `Ak230.geo.json` / `ak230.png` / `Ak230.animation.json` | `ak230.geo.json` / `ak230.png` / `ak230.animation.json` | block |
| `s300 lunch.geo.json` / `s300.png` | `s300_launcher.geo.json` / `s300_launcher.png` | block |
| `5v55rm.geo.json` / `5V55RM.png` | `s300_missile.geo.json` / `5v55rm.png` | item |
| `9K33/9k33.geo.json` / `texture.png` | `9k33.geo.json` / `9k33.png` | item |
| `9k60/9k60.geo.json` / `texture.png` | `9k60.geo.json` / `9k60.png` | item |
| `MIDIUM/9m38/9k33.geo.json` / `texture.png` | `9m38.geo.json` / `9m38.png` | item |
| `P500/P500.geo.json` / `.png` / `model.animation.json` | `p-500.geo.json` / `.png` / `.animation.json` | item |

---

## 附录 B：本次调研依据（可复现）

- 目录树：`_tree_asset.txt`、`_tree_res.txt`、`_tree_java.txt`
- 素材 identifier：`_asset_ids.txt`
- 现有资源 identifier：`_res_ids.txt`
- 关键源码：`TAOVModernNaval.java`、`core/ModBootstrap.java`、`registry/*`、`content/*`、`client/render/*`

> 上述 `_tree_*.txt` / `_*_ids.txt` 为本次调研临时产物，可在实施阶段清理或移入 `_archive`。

---

**文档结束** ｜ 状态：👉 **待确认**（请回复第九节决策 ①–④，我即按计划进入实施）


