# 工作区布局

这个目录同时容纳 **两个自带 git 仓库的独立模组工程**。它们**不是**主从关系，也**不共享构建**：
各自的 `build.gradle`、各自的 mod id、各自的 git 历史。

```
TAOVModernNaval/                     ← 本目录即「正式版」仓库根（.git 在这里）
├── src/  build.gradle  ...          ← 正式版源码
├── libs/                            ← 本地依赖仓库（含素材/整合包 jar，多数被忽略）
├── asset stor/                      ← 素材库唯一来源（被 git 忽略，只读参考）
├── TAOVModernNavalSlim/             ← 精简版（自带 .git，89+ 条历史；被本仓库 .gitignore 忽略）
├── .slim-work/                      ← ⚠ 精简版旧快照（无 git），内容已并入上面，待删
├── .slim-patch/                     ← 历史补丁与提交信息（脚本化改动的存档）
├── WORKSPACE.md                     ← 本文件
└── build_all.ps1                    ← 一次构建两个工程
```

## 两个工程

| | 正式版 | 精简版 |
|---|---|---|
| 目录 | 本目录（仓库根） | `TAOVModernNavalSlim/` |
| mod id | `taovmodernnaval` | `taovmnslim` |
| 显示名 | TAOVModernNaval | TAOV Modern Naval (Slim) |
| 版本 | 1.0.0-SNAPSHOT | 0.33.0 |
| 外部依赖 | GeckoLib + Shao 弹道库 + CBC + Create（+ VS 反射） | **只有 GeckoLib** |
| 弹体 | 继承 Shao / CBC | 自写 `SlimMissileEntity` |
| git | 本目录 `.git` | `TAOVModernNavalSlim/.git`（独立历史） |

两者 mod id 不同，**可以同时装进同一个整合包**，但内容高度重叠（两套雷达 / 发射架 / 导弹）。同时安装只适合做对照测试，不适合作为发行配置。

## 构建

```powershell
# 两个一起（离线，用工作区内的 .gradle-home 缓存）
.\build_all.ps1

# 只构建一个
.\build_all.ps1 -Target full
.\build_all.ps1 -Target slim

# 首次构建或依赖变动时允许联网
.\build_all.ps1 -Online
```

单独构建某个工程也可以，但**必须固定 `GRADLE_USER_HOME`**，否则在受限环境下会因
`%USERPROFILE%\.gradle` 的 wrapper 锁文件写入被拒而失败：

```powershell
$env:GRADLE_USER_HOME = (Join-Path $PWD '.gradle-home')
cd TAOVModernNavalSlim; .\gradlew.bat build --offline
```

产物分别在 `build/libs/` 与 `TAOVModernNavalSlim/build/libs/`（均被各自 `.gitignore` 忽略）。

## 素材库 `asset stor/`

**唯一来源是本目录下的 `asset stor/`**（Blockbench 模型 / 贴图原始文件，被 git 忽略）。
精简版源码里引用它的注释已更新为相对路径 `..\asset stor`；**不要**再往精简版里复制副本
（历史上误复制过 43 个文件，已清理）。

## 关于 `.slim-work/`

这是精简版在 2026-10-26 的一份**无 git 快照**，内容比精简版仓库的 10-09 HEAD 更新
（多 24 个 main 源文件、28 个测试类）。它已经**完整并入 `TAOVModernNavalSlim/`**：
合并时校验过「零个文件只在旧仓库里、所有差异文件都是快照更新」，并把快照漏掉的
`util/FirePattern.java` 从 git HEAD 补回（快照的测试仍引用它）。

保留它只作对照，**不要在上面继续开发**——它不在任何版本控制里。确认无误后可删除。

## 已知状态

- 精简版 `master` 停在 `fd4fba7`（2026-10-09），10-22~10-26 的工作在合并后作为**未提交改动**存在，
  见 `TAOVModernNavalSlim/README` 与 `CHANGELOG.md`。
- 精简版 README 里的规模数字（73 main / 27 test / 22 配方）已滞后，实际 115 / 65 / 35。
- `.slim-patch/` 里的 `patch*.json` + `commit_msg*.txt` 是历史脚本化改动的存档，不是构建输入。
