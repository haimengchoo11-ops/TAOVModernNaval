<#
.SYNOPSIS
    一次构建本工作区内的两个独立工程：正式版 TAOVModernNaval 与精简版 TAOVModernNavalSlim。

.DESCRIPTION
    两个工程各有自己的 build.gradle 与 git 仓库，本脚本只负责按顺序调用、统一 GRADLE_USER_HOME。
    默认离线构建（使用工作区内的 .gradle-home 缓存）。

.PARAMETER Target
    all（默认）| full | slim

.PARAMETER Online
    允许联网解析依赖。首次构建或依赖变动时使用。

.EXAMPLE
    .\build_all.ps1
    .\build_all.ps1 -Target slim
    .\build_all.ps1 -Online
#>
[CmdletBinding()]
param(
    [ValidateSet('all', 'full', 'slim')]
    [string]$Target = 'all',

    [switch]$Online
)

$ErrorActionPreference = 'Stop'

$root = $PSScriptRoot
$slimDir = Join-Path $root 'TAOVModernNavalSlim'

# 受限环境下必须固定到工作区内的缓存目录，否则 wrapper 会在 %USERPROFILE%\.gradle 上撞权限
$env:GRADLE_USER_HOME = Join-Path $root '.gradle-home'

$projects = @(
    [pscustomobject]@{ Name = '正式版 TAOVModernNaval';      Dir = $root;    Kind = 'full' }
    [pscustomobject]@{ Name = '精简版 TAOVModernNavalSlim';  Dir = $slimDir; Kind = 'slim' }
)

if ($Target -ne 'all') {
    $projects = @($projects | Where-Object { $_.Kind -eq $Target })
}

$gradleArgs = @('build')
if (-not $Online) {
    $gradleArgs += '--offline'
}

$failures = @()
$summary = @()

foreach ($project in $projects) {
    $gradlew = Join-Path $project.Dir 'gradlew.bat'
    if (-not (Test-Path $gradlew)) {
        Write-Warning "跳过 $($project.Name)：找不到 $gradlew"
        $failures += $project.Name
        continue
    }

    Write-Host ''
    Write-Host "===== $($project.Name) =====" -ForegroundColor Cyan
    Write-Host "  $($project.Dir)" -ForegroundColor DarkGray

    Push-Location $project.Dir
    $started = Get-Date
    try {
        & $gradlew @gradleArgs
        $exit = $LASTEXITCODE
    } finally {
        Pop-Location
    }

    $elapsed = [int]((Get-Date) - $started).TotalSeconds
    if ($exit -ne 0) {
        $failures += $project.Name
        $summary += [pscustomobject]@{ 工程 = $project.Name; 结果 = 'FAILED'; 耗时秒 = $elapsed }
    } else {
        $summary += [pscustomobject]@{ 工程 = $project.Name; 结果 = 'SUCCESS'; 耗时秒 = $elapsed }
    }
}

Write-Host ''
$summary | Format-Table -AutoSize | Out-String | Write-Host

if ($failures.Count -gt 0) {
    Write-Host "构建失败：$($failures -join '、')" -ForegroundColor Red
    exit 1
}

Write-Host '两个工程均构建成功。' -ForegroundColor Green
exit 0
