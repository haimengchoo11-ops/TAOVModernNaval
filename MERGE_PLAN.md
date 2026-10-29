# 合并计划：正式版 → 精简版（单一工程）

**决策**：合并成一个工程，**以精简版 `TAOVModernNavalSlim` 为主线**，正式版降级为历史参考。
**日期**：2026-10-29

---

## 1. 为什么这项工作比预估小得多

原本按「2~3 周」估，是因为以为要把正式版的 CBC / VS 深度集成搬进精简版。
实际核查后**不用搬**——精简版已经自带，而且实现更好：

| 能力 | 正式版 | 精简版 |
|---|---|---|
| CBC 炮弹识别 | `CbcCompat`（依赖 CBC 类，需编译期 jar） | **`CbcInterceptor`（234 行，零依赖）**：沿 `getSuperclass()` 匹配 `rbasamoyai.createbigcannons` 包名，不 import 任何 CBC 类；没装 CBC 时全部返回空 |
| VS 船体感知 | `PhysicsTargeting`（508 行，反射） | **`VsCompat`（660 行，反射，软依赖）**：`VSGameUtilsKt.getAllShips` + `ShipTransform.getPositionInWorld` + `getShipAABB`，覆盖更全 |
| 外部依赖 | GeckoLib + Shao 弹道库 + CBC + Create + VS | **只有 GeckoLib** |

结论：**「吸收 CBC/VS」这一阶段直接删掉**，剩余工作集中在「补齐精简版缺的少量功能 + 内容裁决 + 归档」。

---

## 2. 差距清单（精简版缺什么）

逐项核对后，正式版真正独有、且值得保留的只有三项：

| # | 能力 | 精简版现状 | 处置 |
|---|---|---|---|
| 1 | **配方的 advancement 覆盖** | 36 个配方 / **0 个 advancement** → 配方书里看不到 | **已完成（P0）** |
| 2 | **来袭导弹告警 HUD** | 无任何 `IGuiOverlay` / `RegisterGuiOverlaysEvent` | P1 移植 |
| 3 | `armor_block` + CBC 炮弹交互 | 无装甲块；有 `ShipImpactRule` / `MissileBlast` | **不移植**（船体防护交给 VS/CBC 原版，避免两套护甲语义打架） |

其余正式版内容都**不该搬**，因为精简版已有更强的替代：

| 正式版 | 精简版替代 |
|---|---|
| `total_control_block`（示波器 GUI） | `firecontrol_terminal`：接触列表点选、逐类开关、过滤模式、射程档位、火力通道 |
| `TargetDomain`（反舰/防空/通用） | `TargetCategory`（8 类：PLAYER/PROJECTILE/ITEM/HOSTILE/ANIMAL/MOB/MISC/VS2）+ 逐类开关 |
| `RadarBlockEntity` 单一雷达 | `radar` / `radar_mr265` / `radar_mr750` / `phased_array` / `signal_repeater` |
| `RakLauncherBlockEntity` | 6 种发射架 + 多方块 + 弹药架 |
| `ShaoMissileProjectile` | `SlimMissileEntity`（自写推进/三阶段制导/近炸/破甲） |

---

## 3. 内容裁决（「以精简版为准」的落地）

导弹名单以精简版为准，正式版独有的 **5 个型号直接裁掉**：

| 型号 | 正式版 | 精简版 | 裁决 |
|---|---|---|---|
| P-15 / P-270 / P-500 / P-700 / P-800 | 有 | 有 | 保留，**数值取精简版** |
| KH-35 | 无 | 有 | 保留 |
| 9K33 / 9K60 / 9M38 / 5V55RM | 有 | 有 | 保留，数值取精简版 |
| **P-1 / P-30 / P-45 / P-100 / P-120 / P-300** | 有 | **无** | **裁掉** |

> 注意：裁掉的是**导弹型号**。`P-1` 作为「火控指挥仪」的名字仍然存在于正式版历史文档里，
> 精简版的对应方块叫 `director`，不需要改名。

数值以精简版为准的关键差异（正式版 → 精简版）：

| 型号 | 当量 | 射程 |
|---|---|---|
| P-15 | 6.0 → **45** | 340 → **200** |
| P-270 | 12.0 → **55** | 460 → **270** |
| P-500 | 16.0 → **80** | 560 → **360** |
| P-700 | 20.0 → **95** | 640 → **400** |
| P-800 | 22.0 → **65** | 720 → **330** |

精简版还有正式的 **引信档位**（写入 `MissileProfile` 的第 7 个参数：
P-15 240 / KH-35 320 / P-270 300 / P-500 400 / P-700 420 / P-800 340），正式版没有这个概念。

---

## 4. 阶段

### P0 · advancement 覆盖 —— 已完成

- 为精简版全部 36 个配方生成 advancement（`data/taovmnslim/advancements/recipes/misc/*.json`）
- 每个含两条 criteria：`recipe_unlocked` + `inventory_changed`（以该配方首个材料为条件）
- 校验：配方 36 / advancement 36，无缺失、无孤儿、JSON 全部可解析

### P1 · 来袭告警 HUD 移植（约 1 天）

精简版没有现成的「来袭弹体」数据源，需要新写探测路径：

1. **新增 `IncomingMissiles`（纯函数 + 枚举）**
   - 枚举附近弹体：`SlimMissileEntity` + `CbcInterceptor.isCbcProjectile` 命中的实体 + 其它 `Projectile`
   - 用「速度在"指向本舰"方向上的投影 > 0」判定接近（正式版 `ThreatRanking.approach` 的判定可直接照搬，它已是纯函数且有单测）
2. **服务端推送**：挂在雷达 `scan()` 之后，向扫描范围内玩家发 `S2CThreatAlert`
   - 复用现有 `TerminalActionPacket.CHANNEL`，新增 message id = 1（当前只用了 0），**不新增 channel**
3. **客户端**：`SlimClientEvents` 注册 `RegisterGuiOverlaysEvent`，新增 `ThreatAlertOverlay`
   - 顶部居中红条 + 计数 + 闪烁 + 尾部淡出，尊重 F1
   - 从无告警转有告警时播一次警笛（`SoundEvents.RAID_HORN.value()`，2.5 秒防连响）
4. **lang**：中英各 1 键 `gui.taovmnslim.threat_alert`

> 正式版的实现可直接参考但**不能整类照搬**：它依赖 `ShaoMissileProjectile.activeWithin`
> 这个静态集合，精简版没有等价物。

### P2 · 正式版冻结（一次性）

- `TAOVModernNaval/README.md` 顶部加「已归档，见 TAOVModernNavalSlim」
- `mods.toml` 的 `description` 标注 legacy
- **不再向正式版加功能**；只保留修 bug 的能力

### P3 · 清理

- 删除 `.slim-work/`（内容已并入，无版本控制）
- `.slim-patch/` 归档为 `docs/history/` 或直接删（51 个 patch.json 是历史脚本化改动，不是构建输入）
- 删除工作区外那份停在 10-09 的 `..\final,现代战舰俄联邦\TAOVModernNavalSlim`

---

## 5. 风险

| 风险 | 说明 | 缓解 |
|---|---|---|
| **存档** | 合并后只剩 `taovmnslim`。正式版 `taovmodernnaval` 的方块/物品会变成未知（丢失） | 正式版本来就只在开发实例里用过；若要保，先出「迁移说明」而不是代码兼容 |
| **mod id** | 保持 `taovmnslim`，**不要改成 `taovmodernnaval`**——那会让所有现存 `taovmnslim` 存档失效 | 明确写进 README |
| **两套弹道语义** | 正式版弹体继承 CBC HE 弹（有穿甲/引信），精简版自写 | 已经选了精简版；文档里说明「不再兼容 CBC 弹药作为导弹」 |
| **依赖声明** | 精简版不内嵌 GeckoLib，玩家必须自行安装 4.7 | 已在 build.gradle 注释里记录，README 需重申 |

---

## 6. 验收

- [x] 精简版配方 36 / advancement 36
- [ ] `build_all.ps1 -Target slim` 通过，测试数不低于当前的 313
- [ ] 游戏内：配方书能看到全部 36 个配方并在拿到材料时解锁
- [ ] 游戏内：雷达扫到来袭弹体时出现 HUD 告警条 + 警笛
- [ ] 正式版 README 标注归档
- [ ] `.slim-work` / `.slim-patch` 清理完毕
