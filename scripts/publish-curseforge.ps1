<#
.SYNOPSIS
  发布当前构建到 CurseForge（封装 CurseForgeGradle 的 Java 25 daemon 要求）。

.DESCRIPTION
  CurseForgeGradle 1.3.33 要求 daemon JVM >= 25，而日常开发用 Java 17/21。
  本脚本自动探测本机 Java 25+，再以该 JVM 启动 gradle 运行 publishCurseForge，
  避免把机器特定的 JDK 路径写入版本控制。

  鉴权：读取 ~/.gradle/gradle.properties 中的 CURSE_TOKEN。

.PARAMETER ExtraArgs
  透传给 gradlew 的额外参数。例如 -ExtraArgs '--dry-run' 做干跑验证。

.EXAMPLE
  ./scripts/publish-curseforge.ps1
  ./scripts/publish-curseforge.ps1 -ExtraArgs '--dry-run'
#>
[CmdletBinding()]
param(
    [string[]] $ExtraArgs = @()
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot

function Test-JavaVersion([string] $javaHome, [int] $minMajor) {
    $exe = Join-Path $javaHome 'bin/java.exe'
    if (-not (Test-Path $exe)) { return $false }
    # java -version 写到 stderr；PowerShell 在 Stop 模式下会把它当作
    # terminating error，这里临时放松以正常捕获输出。
    $prev = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try {
        $out = & $exe -version 2>&1 | Out-String
        # 形如: openjdk version "25.0.2"  或  "1.8.0_192"
        if ($out -match '"(?:1\.)?(\d+)\.') {
            return [int]$Matches[1] -ge $minMajor
        }
    } catch {} finally {
        $ErrorActionPreference = $prev
    }
    return $false
}

function Find-Java25 {
    # 1) 显式环境变量
    if ($env:JAVA_25_HOME -and (Test-JavaVersion $env:JAVA_25_HOME 25)) {
        return $env:JAVA_25_HOME
    }
    # 2) IntelliJ/.jdks 目录（常见：openjdk-25*, temurin-25*, corretto-25*）
    $jdks = Join-Path $env:USERPROFILE '.jdks'
    if (Test-Path $jdks) {
        $hit = Get-ChildItem $jdks -Directory -ErrorAction SilentlyContinue |
            Sort-Object Name -Descending |
            Where-Object { Test-JavaVersion $_.FullName 25 } |
            Select-Object -First 1
        if ($hit) { return $hit.FullName }
    }
    # 3) Program Files 下的常见发行版路径
    $candidates = @(
        'Eclipse Adoptium', 'Eclipse Temurin', 'Java', 'Microsoft'
    ) | ForEach-Object { Join-Path 'C:\Program Files' $_ }
    foreach ($base in $candidates) {
        if (Test-Path $base) {
            $hit = Get-ChildItem $base -Directory -ErrorAction SilentlyContinue |
                Where-Object { Test-JavaVersion $_.FullName 25 } |
                Select-Object -First 1
            if ($hit) { return $hit.FullName }
        }
    }
    return $null
}

$jvm = Find-Java25
if (-not $jvm) {
    Write-Error @"
未找到 Java 25+。CurseForgeGradle 1.3.33 要求 daemon JVM >= 25。
请安装 Java 25（如 Eclipse Temurin 25），或设置环境变量：
    `$env:JAVA_25_HOME = '<jdk25 路径>'
"@ 
    exit 1
}

Write-Host "使用 JVM: $jvm" -ForegroundColor Cyan
$gradlew = Join-Path $projectRoot 'gradlew.bat'
$args = @('publishCurseForge', "-Dorg.gradle.java.home=$jvm", '--console=plain') + $ExtraArgs
Write-Host "运行: $gradlew $($args -join ' ')" -ForegroundColor DarkGray
& $gradlew @args
exit $LASTEXITCODE
