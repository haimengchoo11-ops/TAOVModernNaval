# TAOVModernNaval 完成状态报告

**日期**：2026-10-06
**基线**：Minecraft 1.20.1 / Forge 47.3.0 / Java 17
**构建**：`gradlew build --offline` → **BUILD SUCCESSFUL**（11 个单元测试全绿）
**产物**：`build/libs/taovmodernnaval-1.0.0-SNAPSHOT.jar`

---

## 一、本轮完成的内容

### 1. P-1 火控指挥仪的火控分发链路（此前完全缺失）

问题：导弹端已有 `ShaoMissileProjectile.setDirectorTarget(...)` 接收接口，但**全项目没有任何调用点**。
也就是说"256 格扫描 → 锁定 → 向 64 格内导弹广播火控参数"这条链路只完成了一半：指挥仪只扫描，导弹收不到任何东西，`DIRECTOR_FIRE_CONTROL_SYSTEM.md` 里描述的广播能力实际上是空的。

现在链路已闭环：

```
P1DirectorBlockEntity.tick()
  ├─ 每 tick：雷达天线 +3° 旋转、主目标锁定强度 ±0.05/0.10
  ├─ 每 10 tick：256 格扫描 → 选定最近的有效目标为主目标
  └─ 每 5 tick：向 64 格内所有在飞导弹广播 (目标坐标, 锁定强度, 指挥仪位置)

ShaoMissileProjectile.tick()
  └─ resolveAssignedTargetPos()
       ├─ 1. 指挥仪火控（锁定强度 > 0.3 且 200 tick 内刷新）
       ├─ 2. 发射架指派的实体目标 UUID
       ├─ 3. 指定坐标
       └─ 4. 弹载自主寻的（半径 = seekRange + 32）
```

- 指挥仪锁定强度 ≥ 0.8 时产生 END_ROD 粒子照射效果，便于观察是否真的在导引。
- 指挥仪参数超过 200 tick 未刷新即失效，导弹自动回退到自主寻的（不会追着过期目标飞）。
- 空手右键指挥仪显示火控状态（锁定强度 + 目标坐标），潜行 + 空手右键切换 IFF。

### 2. 单元测试发现的真实缺陷（已修复）

`TARGET_SWITCH_TOLERANCE` 原本是 `1e-3`，却被拿去和 `distanceToSqr(...)`（**平方**距离）比较，
等效容差只有约 0.03 格：敌舰每 tick 的普通移动都会判定为"换了目标"，锁定强度被反复打回 0.3 基线，
导弹几乎无法进入高精度末段制导。现改为 4.0 格容差并做平方换算，并由测试锁定该语义。

### 3. 开发运行配置修复

| 修复项 | 说明 |
|--------|------|
| `runClientFast` 无法启动 | 自定义 run 名不会自动继承 userdev 的主类与 `--add-opens` 模块参数；已建立 `parent(0, null, 'client')` 继承 |
| 快速运行时污染 | `prepareFastDevRuntime` 原本把 `libs/` 中未重映射的同名 Create 塞进 `run-fast/mods`；改为只取 `run/mods` 中已验证可用的 jar，并用 `Sync` 清理 |
| 新增无头入口 | `runServerFast`（`--nogui --port 25599`），用于在无客户端 mixin 的情况下验证注册表与数据包 |

### 4. 清理死代码

删除 MDK 模板残留的 `TAOVMN/mixin/ExampleMixin.java` 与 `taovmodernnaval.mixins.json`。
该 mixin 从未在 `mods.toml` 注册（build.gradle 也没有 manifest `MixinConfigs`），永不生效。

### 5. 语言文件补齐

`message.taovmodernnaval.director_status` / `director_no_target`（中英双语）。

---

## 二、验证证据

| 项目 | 命令 | 结果 |
|------|------|------|
| 编译 + 打包 | `gradlew build --offline` | BUILD SUCCESSFUL，产出 259 KB jar |
| 单元测试 | 同上（`test` 任务） | 11 通过 / 0 失败（`P1DirectorBlockEntityTest` 8、`PhysicsTargetingTest` 3、`NavalSystemsTest` 1） |
| 资源完整性 | 脚本比对注册 ID 与 `models/item`、`blockstates`、`models/block`、`loot_tables`、`lang` | 24 个物品模型、12 个方块（blockstate + 方块模型 + 掉落表）全部齐备；中英语言文件的物品/方块/消息键全部存在 |

---

## 三、尚未完成：游戏内冒烟测试

**当前无法在本工作区启动游戏**，原因**不在本模组代码**，而是 dev 环境下 mixin refmap 未被重映射（SRG → Mojang official），
所有 profile 都在**加载其他模组**时崩溃：

```
[FATAL] [mixin/]: Mixin apply failed valkyrienskies-common.mixins.json:client.MixinMinecraft
  -> @Shadow method m_91092_ was not located in net.minecraft.client.Minecraft
[FATAL] [mixin/]: Mixin apply failed mixins.shaolib.json:MixinMinecraftServer
  -> @Shadow field f_129744_ was not located in net.minecraft.server.MinecraftServer
```

`runClient`（内置 profile）同样如此，且 `run/logs/latest.log` 中 10-02 20:42 的历史运行也以同类错误结束——
说明这是此前就存在的环境问题，不是本次改动引入。

**可行的解决方向**（参考 [Architectury: Fixing ForgeGradle Mixin refmaps](https://docs.architectury.dev/loom/fg_mixin_refmaps/) 与 [Mixin #462](https://github.com/SpongePowered/Mixin/issues/462)）：

1. 用此前能正常启动的 IDE / Eclipse 启动组（`TAOVModernNaval - runClient`）跑冒烟测试；
2. 或在自定义 run 的 `jvmArgs` 里补 `mixin.env.refMapRemappingFile` / `net.minecraftforge.gradle.GradleStart.srg.srg-mcp` 指向 srg→mcp 映射文件。

**需要人工在游戏内确认的清单**：

- [ ] P-1 指挥仪放置后模型显示、右键能显示火控状态
- [ ] 指挥仪 256 格内出现敌对目标后，锁定强度从 0 爬升到 1（右键可读）
- [ ] 锁定 ≥ 0.8 时出现粒子照射线
- [ ] RAK 发射架装弹 → 就近绑定指挥仪 → 红石上升沿发射 → 导弹飞向指挥仪锁定目标
- [ ] 指挥仪被破坏/目标消失后，在飞导弹回退到自主寻的而不失控
- [ ] 潜行右键切换 IFF 后不再锁定友好生物
