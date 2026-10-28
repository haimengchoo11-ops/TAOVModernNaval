# TAOVModernNavalSlim 用电接线实施报告（契约 M1.4 + M1.5）

**日期**：2026-10-08　**对象工程**：`final,现代战舰俄联邦/TAOVModernNavalSlim`（mod id `taovmnslim`，0.32.0）
**依据**：`INTEGRATION_CONTRACT.md` §3（数值）与 §7.7（违约判定）、`ROADMAP.md` M1.4 / M1.5
**范围**：只改**用电方**；未新增任何发电能力。

---

## 1. 完成内容

### M1.4 指挥仪接入 `COST_DIRECTOR_SCAN`（24 FE/次）

原本指挥仪继承 `RadarBlockEntity`，会按**普通雷达的 8 FE** 计费，而且它覆盖了 `scanInterval()` 直接
`return 10;` —— 等于**全船唯一"缺电也不变慢"的设备**。

| 文件 | 改动 |
|---|---|
| `RadarBlockEntity.java` | 新增 `protected int scanCost()`（默认 `COST_RADAR_SCAN`）；`scanInterval()` 与 `tick()` 的计费**共用**它 |
| `DirectorBlockEntity.java` | 覆盖 `scanCost()` 返回 `COST_DIRECTOR_SCAN`；重写 `scanInterval()`，用 `degradedInterval(DIRECTOR_SCAN_INTERVAL, energyBuffer, scanCost())` **把降级接回来** |

> 关键设计：把耗电抽成 `scanCost()` 而不是在两处各写常量。否则改一处忘一处就会出现
> "**按 24 FE 计费、却按 8 FE 降级**"的隐性不一致——这种 bug 在游戏里只表现为"缺电时减速幅度不对"，极难归因。

### M1.5 导弹发射接入 `COST_MISSILE_LAUNCH`（60 FE/发）

| 文件 | 改动 |
|---|---|
| `LauncherBlockEntity.java` | 加 `energyBuffer`；`tick()` 抽电；`fireNextMissile()` **在取出弹药之前**计费；冷却经 `degradedInterval` 拉长 |
| `LaunchTubeBlockEntity.java` | 同上；另在"未成功发射"时也进 `MAX_DEGRADE` 冷却，避免每 tick 空转重试 |

**覆盖面**：`launcher` / `launcher_dual` / `launcher_hmt` / `launcher_aa` / `launcher_s300` 都继承
`LauncherBlockEntity`，`launch_tube` 与 `hmt` 继承 `LaunchTubeBlockEntity` —— 改基类即全覆盖。

> 关键设计：**先计费、后取弹**。若照原顺序（`missiles.remove(0)` 在前），缺电时会把弹从发射架里
> "吞掉"却不发射——玩家看到的是"弹凭空少了"，比"发射变慢"严重得多。

### 契约 §7.7 机械守门

`FireControlWiringTest.powerConsumersMustActuallyConsume()` 原先只检查**雷达与舰炮**。
现已扩展为：

- `RadarBlockEntity` / `CannonBlockEntity` / `LauncherBlockEntity` / `LaunchTubeBlockEntity` → 必须同时含
  `drawFromNeighbors` + `consume` + `degradedInterval`；
- `DirectorBlockEntity` → 单独校验它**自己的责任**（`COST_DIRECTOR_SCAN` + `scanCost` + `degradedInterval`），
  而不是要求它重复写一遍抽电代码（它的抽电与计费走父类）；
- 仍然禁止任何类 `implements IEnergyStorage`（那是发电方）。

---

## 2. 顺带修掉的跨工程编译耦合

`build.gradle` 原先借用历史完整版的
`../TAOVModernNaval/libs/geckolib-forge-1.20.1-4.7.jar`。
该目录已随完整版迁出本伞目录（迁到 `C:\Users\ROG STRIX\Downloads\TAOVModernNaval`），
这条路径**已经断了**，只能回退到 maven 坐标（离线时不一定解析得到）。

已改为**工程自持**：GeckoLib jar 复制到 `TAOVModernNavalSlim/libs/`，`build.gradle` 指向 `libs/`，
maven 仍保留为回退分支。这同时消除了一处"两个子项目之间的编译期耦合"，
与 `DEPENDENCY_MATRIX.md` 第三节"没有任何编译期依赖"的声明一致。

---

## 3. 验证证据

| 项目 | 命令 / 方法 | 结果 |
|---|---|---|
| 构建 + 测试 | `build_all.ps1 -Target slim`（Gradle 7.6.1，离线） | **BUILD SUCCESSFUL in 23s** |
| 单元测试 | `build/test-results/test/*.xml` | **110 通过 / 0 失败** |
| 指挥仪接线 | 源码检索 | `DirectorBlockEntity.java:70-71` `scanCost()` → `COST_DIRECTOR_SCAN`；`:82-83` `degradedInterval(DIRECTOR_SCAN_INTERVAL, energyBuffer, scanCost())` |
| 发射架接线 | 源码检索 | `LauncherBlockEntity.java:97` 抽电；`:138-139` 计费；`:114-116` 冷却降级 |
| 发射管接线 | 源码检索 | `LaunchTubeBlockEntity.java:113` 抽电；`:135-136` 计费；`:121-123` 冷却降级 |
| 产物 | `TAOVModernNavalSlim/build/libs` | `taovmnslim-0.32.0.jar`（330,852 字节） |

**过程中被测试抓到的两个问题（都不是"测试写错"）**：
1. 守门清单第一版把指挥仪当成"直接抽电设备"——**它会误报**，因为指挥仪的抽电/计费继承自雷达。已按"各自责任"重写。
2. `RadarBlockEntity.java` 是 **CRLF/LF 混合行尾**的文件，按单一 LF 匹配的补丁会静默漏掉锚点。已改成行尾无关匹配。

---

## 4. 备份与回滚

改动前的 6 个文件已备份到：
`C:\Users\ROG STRIX\Downloads\TAOVModernNaval\.slim-patch\backup\`
（文件名形如 `src__main__java__...__RadarBlockEntity.java`）。
补丁本身（可审阅）在 `.slim-patch\patch.json / patch2.json / patch3.json`，落盘器 `apply2.ps1` 可重复使用。

---

## 5. 需要你定的三件事

1. **`libs/geckolib-forge-1.20.1-4.7.jar` 要不要入库**？现在它未被 `.gitignore` 忽略（`git status` 显示 `?? libs/`）。
   入库 = 离线可构建、彻底不外借；不入库 = 保持 `DEPENDENCY_MATRIX` 里"避免重复存放 1MB 二进制"的偏好，但首次构建需要联网。**我没有替你决定，也没有提交任何东西。**
2. **是否提交（M1.6）**？Slim 工作区里现在有：原有未提交的电力改造 WIP（`CannonBlockEntity` / `ShipEnergy` / `BallisticsAndFirePatternTest`）+ 本轮改动。按工程约定"提交前 `build_all.ps1` 两个都必须成功"——我只构建了 Slim，**轮机未构建**，所以我没提交。
3. **伞项目文档同步**：伞项目会话**当前正在活动**（1 秒前刚写入），我没有去改它的文件。以下处已被我的改动变成过时：
   - `INTEGRATION_CONTRACT.md` §3「用电侧接线进度」：指挥仪、导弹发射应改为 ✅
   - `ROADMAP.md` M1.4 / M1.5 → ✅
   - `PROJECT_STATUS.md` §四 WIP 清单
   - `DEPENDENCY_MATRIX.md` §二 GeckoLib "借用完整版 jar" 一行
   - `README.md` §四 目录布局（`TAOVModernNaval/` 已不在伞目录内）与 Slim `.gitignore` 里指向 `..\TAOVModernNaval\asset stor` 的注释
