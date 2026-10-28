<#
.SYNOPSIS
    校验一个 mods 目录下的所有 jar 是否完整，定位导致
    "java.util.zip.ZipException: invalid CEN header (bad signature)" 的坏文件。

.DESCRIPTION
    Forge 在 ModLauncher 阶段扫描 mods/ 时遇到坏 jar 会直接崩，但**异常栈里不带文件名**
    （ModDirTransformerDiscoverer.visitFile 只是遍历，不记录路径），只能自己逐个验。

    本脚本做三步检查，逐级收紧：
      1. 长度 >= 22 字节（EOCD 最小长度）
      2. 本地文件头签名 PK\x03\x04、结尾能找到 EOCD 签名 PK\x05\x06
         （下载中断/保存成 HTML 的文件通常第一步就露馅）
      3. 用 .NET ZipArchive 真正打开一次中央目录

.PARAMETER ModsDir
    要校验的 mods 目录。默认取当前目录下的 mods，再退回本仓库的 run\mods。

.EXAMPLE
    .\check_mods.ps1
    .\check_mods.ps1 -ModsDir "C:\...\.minecraft\versions\TAOV 2 Sea Power\mods"
#>
[CmdletBinding()]
param(
    [string]$ModsDir
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.IO.Compression.FileSystem

function Resolve-ModsDir([string]$candidate) {
    if (-not $candidate) { return $null }
    if (-not (Test-Path $candidate)) { return $null }
    return (Resolve-Path $candidate).Path
}

if (-not $ModsDir) {
    $ModsDir = Resolve-ModsDir (Join-Path (Get-Location) 'mods')
    if (-not $ModsDir) { $ModsDir = Resolve-ModsDir (Join-Path $PSScriptRoot '..\run\mods') }
}
$ModsDir = Resolve-ModsDir $ModsDir

if (-not $ModsDir) {
    Write-Error "找不到 mods 目录。请用 -ModsDir 指定，例如：`n  .\check_mods.ps1 -ModsDir `"C:\...\versions\TAOV 2 Sea Power\mods`""
    exit 2
}

Write-Host "校验目录: $ModsDir" -ForegroundColor Cyan

$jars = @(Get-ChildItem $ModsDir -File -Filter *.jar -ErrorAction SilentlyContinue)
if ($jars.Count -eq 0) {
    Write-Warning "该目录下没有 .jar"
    exit 0
}

$bad = New-Object System.Collections.Generic.List[object]

foreach ($jar in $jars) {
    $reason = $null
    try {
        if ($jar.Length -lt 22) { $reason = "文件过小（$($jar.Length) 字节），不是有效 jar" }

        if (-not $reason) {
            $bytes = [System.IO.File]::ReadAllBytes($jar.FullName)
            $localOk = ($bytes.Length -ge 4) -and
                       ($bytes[0] -eq 0x50) -and ($bytes[1] -eq 0x4B) -and
                       ($bytes[2] -eq 0x03) -and ($bytes[3] -eq 0x04)
            if (-not $localOk) {
                $head = -join ($bytes[0..([Math]::Min(3, $bytes.Length - 1))] | ForEach-Object { [char]$_ })
                $reason = "本地文件头不是 PK\x03\x04（实际开头: '$head'）—— 多半是下载中断或存成了网页"
            }
        }

        if (-not $reason) {
            # EOCD 在文件末尾最多 64K 注释之内
            $tailLen = [Math]::Min(65557, $bytes.Length)
            $tail = New-Object byte[] $tailLen
            [Array]::Copy($bytes, $bytes.Length - $tailLen, $tail, 0, $tailLen)
            $eocd = $false
            for ($i = $tailLen - 22; $i -ge 0; $i--) {
                if ($tail[$i] -eq 0x50 -and $tail[$i + 1] -eq 0x4B -and $tail[$i + 2] -eq 0x05 -and $tail[$i + 3] -eq 0x06) {
                    $eocd = $true; break
                }
            }
            if (-not $eocd) { $reason = "找不到中央目录结尾签名 PK\x05\x06 —— jar 被截断" }
        }

        if (-not $reason) {
            $zip = [System.IO.Compression.ZipFile]::OpenRead($jar.FullName)
            try { $null = $zip.Entries.Count } finally { $zip.Dispose() }
        }
    } catch {
        $reason = "ZipArchive 打不开: $($_.Exception.Message)"
    }

    if ($reason) {
        $bad.Add([pscustomobject]@{
            Name   = $jar.Name
            KB     = [math]::Round($jar.Length / 1KB, 1)
            Time   = $jar.LastWriteTime.ToString('yyyy-MM-dd HH:mm:ss')
            Reason = $reason
        })
    }
}

Write-Host ''
if ($bad.Count -eq 0) {
    Write-Host "全部 $($jars.Count) 个 jar 结构完好。" -ForegroundColor Green
    exit 0
}

Write-Host "发现 $($bad.Count) / $($jars.Count) 个损坏的 jar：" -ForegroundColor Red
$bad | Format-List
Write-Host "处理办法：删除这些文件后让启动器重新下载；如果是自己放的 mod，重新复制一份。" -ForegroundColor Yellow
exit 1
