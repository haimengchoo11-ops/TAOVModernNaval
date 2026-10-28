# libs/ —— 构建所需的第三方 jar（不入库）

本目录是本地开发依赖仓库。**除 `createbigcannons-5.8.3edited.jar` 外，所有 jar 都已被
`.gitignore` 忽略**，不进入版本库（体积 + 第三方再分发风险）。从全新 clone 直接构建前，
需要按下表把 jar 放进来。

## 必需（缺失会导致 `compileJava` 失败）

| 文件名 | 来源 | 许可 |
|---|---|---|
| `jei-1.20.1-forge-15.3.0.4.jar` | JEI 15.3.0.4 for 1.20.1 | MIT |
| `geckolib-forge-1.20.1-4.7.jar` | GeckoLib 4.7 for 1.20.1 | MIT |
| `Shao's Projectile Lib Forge-1.20.1-1.6.4-all.jar` | Shao's Projectile Lib 1.6.4 | 见上游 |
| `create-1.20.1-0.5.1.j.jar` | Create 0.5.1j（CBC 依赖，`mod id = create`） | MIT |
| `createbigcannons-5.8.3edited.jar` | **已入库**（本工程改过的 CBC 5.8.3） | 见上游 |

对应 `build.gradle` 的依赖声明：

```groovy
compileOnly   fg.deobf(files('libs/jei-1.20.1-forge-15.3.0.4.jar'))
implementation fg.deobf(files('libs/geckolib-forge-1.20.1-4.7.jar'))
implementation fg.deobf(files("libs/Shao's Projectile Lib Forge-1.20.1-1.6.4-all.jar"))
implementation files('libs/create-1.20.1-0.5.1.j.jar')
implementation fg.deobf(files('libs/createbigcannons-5.8.3edited.jar'))
```

> 注意：`libs/` 里的 `create` / `geckolib` / `shaolib` 必须是**已重映射**的发布版 jar。
> `run/mods` 里同名的开发包直接塞进这里会让 Create 的 mixin 找不到映射名而崩溃。

## 可选（仅影响开发运行，不影响编译）

`run/mods/` 下的整套整合包 jar 由 `syncLocalDevMods` / `prepareFastDevRuntime` 任务管理，
不需要放进 `libs/`。`verifyRequiredDevMods` 会在 `runClient` 前检查 `create` 是否存在。

## 构建命令

```powershell
$env:GRADLE_USER_HOME = (Join-Path (Get-Location) '.gradle-home')   # 沙箱/CI 下必需
.\gradlew.bat build --offline
```

> `GRADLE_USER_HOME` 必须指向工程内的 `.gradle-home`（已被忽略）：默认的
> `%USERPROFILE%\.gradle` 在受限环境下会因 wrapper 锁文件写入被拒而失败。
