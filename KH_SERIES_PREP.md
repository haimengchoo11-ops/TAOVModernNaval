# KH 系列反舰导弹资料与接入准备

> 用途：为下一版"Kh 系列反舰弹"做参数与素材准备。数据来自公开资料（维基百科条目及其引用的
> Rosoboronexport 出口目录），**已标注来源**；带「待核」的条目没有在本次检索到的来源里出现，
> 落地前需要再确认。

## 0. 结论先行

1. **素材缺口在 Kh 系列本身**：`asset stor/WEAPON/MISSILE/ANTI SHIP/KH-series/` **是空目录**，
   而 `p-series/` 里还有 **P-120 / P-35 / P-70** 三个已经建模、但尚未接入模组的型号 ——
   下一版"零新建模"就能加的量，比 Kh 系列更直接。
2. **Kh 系列几乎全是空射弹**（Kh-31A / Kh-35U / Kh-59MK / Kh-41）。本模组目前没有航空平台，
   所以要么先做"舰载版 Uran（Kh-35 的 3M24 舰载型）"，要么等空射平台一起做。
3. 若走空射路线，Kh-41 就是 P-270 的空射型（同一弹体），**可直接复用 P270 的数值与建模**，
   只改发射平台与射程档。

## 1. 真实参数（公开资料）

| 型号 | 北约代号 | 平台 | 全长 | 弹径 | 翼展 | 重量 | 战斗部 | 速度 | 射程 | 制导 |
|---|---|---|---|---|---|---|---|---|---|---|
| **Kh-31A** | AS-17 Krypton | 空射（Su-27/30/34/35、MiG-29M/K、Su-33） | 4.7 m（Mod 1）/ 5.3 m（Mod 2 AD/PD） | 0.36 m | 0.914 m | 610 kg | 94 kg 穿甲/半穿甲 | 2,160–2,520 km/h | 7.5–70 km | 惯导 + 主动雷达 |
| **Kh-31AD** | AS-17 | 空射 | 5.3 m | 0.36 m | 0.914 m | ~610 kg | 同 A | 同上 | 增程型（约 120–160 km，待核） | 同上 |
| **Kh-35（舰载=3M24 Uran）** | AS-20 Kayak / SS-N-25 Switchblade | 舰射 / 岸基 Bal / 直升机 / 空射 | 舰/岸 4.40 m，空射 3.85 m | 0.42 m | 1.33 m | 舰/岸 610 kg，空射 520 kg | 145 kg 破片成型装药 | Mach 0.8–0.85 | 130 km（Kh-35），7–260 km（Kh-35U），2015 升级型 300 km | 惯导 + ARGS-35E 主动雷达（导引头 20 km；Kh-35U 50 km） |
| **Kh-59MK** | AS-18（Kazoo 家族的反舰型） | 空射（Su-30MK） | 5.70 m | 0.38 m | 1.30 m | 930 kg | 320 kg（或 285 kg 破片） | Mach 0.72–0.88 | 285 km | 惯导 + ARGS-59 主动雷达 |
| **Kh-41** | SS-N-22 Sunburn（P-270 空射型） | 空射 | 9.745 m | 0.80 m | 2.10 m | 4,500 kg | 300 kg | Mach 2–3 | 120–250 km（舰射 3M80 为 90–120 km） | 惯导 + 末段主动雷达 |

补充要点：
- Kh-35 掠海高度：巡航 **10–15 m**、末段 **4 m** —— 这是它"低可探测"的核心，接模组时应体现为
  **低空巡航高度**（本工程 `MissileGuidance` 已有掠海逻辑，见 `TERMINAL_RANGE`）。
- Kh-31A 是"先固体助推到 Mach 1.8、再冲压到 Mach 2+"的两段动力，体积小、速度高、战斗部小（94 kg）——
  适合做成"快、准、战斗部小"的一档。
- Kh-41 与 P-270 共弹体：本工程已有 `MissileProfile.P270`（1.15 速度 / 55 威力 / 270 寻的 / 300 tick），
  空射型建议只改发射方式与射程档，不重做数值。

## 2. 与本工程 `MissileProfile` 的映射建议

现有字段：`surface(maxSpeed, steering, blastPower, seekRange, proximityFuse, iffSafeRadius, fuzeTicks)`，
其中 `maxSpeed` 是格/tick 量级的**压缩值**（P800=1.35 ≈ Mach 2.5–3，P15=0.95 ≈ Mach 0.9），
`seekRange` 大致是"真实射程 km × 0.6~0.7"（P500=360↔550 km、P700=400↔550+ km）。

| 建议物品 | 建议 profile | 理由 |
|---|---|---|
| `missile_kh31a` | `surface(1.45, 0.30, 25, 200, 1.2, 8, 200)` | 最快、转向最灵、战斗部最小（94 kg）；射程 70 km 按 P15（200）同档 |
| `missile_kh35u`（舰载称 Uran） | `surface(0.90, 0.26, 30, 300, 1.5, 10, 320)` | 亚音速掠海；145 kg 战斗部；260 km ≈ P270 档（270→300） |
| `missile_kh59mk` | `surface(0.92, 0.22, 58, 320, 1.8, 12, 340)` | 320 kg 战斗部接近 P500 的 80 威力档；285 km |
| `missile_kh41` | 复用 `P270`（或 `surface(1.20, 0.24, 55, 310, 1.9, 10, 300)`） | 同弹体空射型，只把射程档略加上去 |

爆炸半径（`MissileBlast`）：反舰弹 ×2、上限 16 格。按上表 `blastPower`：
Kh-31A 25 → 基础 6 格 → **12 格**；Kh-35U 30 → 6.6 → **13.2 格**；Kh-59MK 58 → 8（封顶）→ **16 格**。

## 3. 素材现状（决定下一版能做多少）

| 目录 | 内容 | 状态 |
|---|---|---|
| `ANTI SHIP/KH-series/` | **空** | 需要新建模（geo + 128×128 贴图 + 动画空壳） |
| `ANTI SHIP/p-series/P120/` | `p-120.geo.json` | **可直接接**（缺贴图需确认） |
| `ANTI SHIP/p-series/P35/` | 目录空 | 需建模 |
| `ANTI SHIP/p-series/P70/` | 目录空 | 需建模 |
| `ANTI SHIP/p-series/P1/` | 目录空 | 需建模 |
| 已接入 | P15 / P270 / P500 / P700 / P800 | ✅ |

> P-120 Malakhit（SS-N-9 Siren）：约 3,180 kg、6 m 级、战斗部 70 万吨级当量/常规 500 kg 级（公开资料不一），
> P-70 Ametist（SS-N-7）：约 3,500 kg、6 m、战斗部 1,000 kg、射程 65 km —— 接入前按现有 P 系列口径再核一遍。

## 4. 需要你决定的三个点

1. **平台**：只做舰载（则上 Kh-35U/Uran，Kh-41 复用作"岸基/舰载高超音速"档），还是同时做空射平台？
2. **挂载档位**：Kh-31A（610 kg）算轻档、可上"基础/双联架"吗？现有规则是**反舰弹只能上重型装置**
   （`acceptsAntiShipMissiles`），Kh 系列若要上小艇就需要开一档"轻型反舰"。
3. **Kh-41 与 P-270**：做成两个物品（外观/平台不同）还是同一个物品的两种发射方式？

## 5. 落地清单（确认后照此执行）

- [ ] 素材：Kh 系列 geo + 贴图 + `animations/*.animation.json`（空壳即可，见 `AssetIntegrityTest` 规则）
- [ ] `MissileProfile`：新增上述 3–4 个常量 + `SlimRegistry` 物品注册（`SlimMissileItem` + geo 名）
- [ ] `MissileDimensions`：按新 geo 的包围盒补 `rawSize` / `centerYUnits`（`MissileModelCenterTest` 会逐型核对）
- [ ] 语言键（中/英）、配方、创造标签、掉落（`AssetIntegrityTest` + `CreativeTabCoverageTest` 守门）
- [ ] 守门测试：新弹种的档位规则 / 数值上下限 / 素材完整性

## 6. 数据来源

- Kh-31：<https://en.wikipedia.org/wiki/Kh-31>（引 Rosoboronexport 出口目录）
- Kh-35 / Kh-35U / Uran / Bal：<https://en.wikipedia.org/wiki/Kh-35>
- Kh-59 / Kh-59MK：<https://en.wikipedia.org/wiki/Kh-59>
- P-270 Moskit / Kh-41：<https://en.wikipedia.org/wiki/P-270_Moskit>
- P-120 Malakhit：<https://en.wikipedia.org/wiki/P-120_Malakhit>
- P-70 Ametist：<https://en.wikipedia.org/wiki/P-70_Amethyst>
- Rosoboronexport 出口目录（Kh-31A 条目引用源）：<http://roe.ru/eng/catalog/>
