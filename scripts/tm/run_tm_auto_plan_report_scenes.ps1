# 胎面自动排程 25 场景测试辅助脚本
# 默认只执行 JSON 单测，不连接 MySQL；真实 MySQL 验证由使用者显式执行 SQL 后调用接口完成。
[CmdletBinding()]
param(
    [switch]$SkipMaven,
    [switch]$PrintMysqlCommands,
    [string]$MysqlExe = 'mysql',
    [string]$Database = 'jy_aps',
    [string]$MysqlUser = 'root'
)

$ErrorActionPreference = 'Stop'
$RepoRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$CleanupSql = Join-Path $RepoRoot 'docs/tm/testdata/tm_auto_plan_scene_cleanup.sql'
$DataSql = Join-Path $RepoRoot 'docs/tm/testdata/tm_auto_plan_scene_data.sql'

Write-Host 'TM auto plan report scenes'
Write-Host ("RepoRoot: " + $RepoRoot)
Write-Host ("Cleanup SQL: " + $CleanupSql)
Write-Host ("Data SQL: " + $DataSql)

if ($PrintMysqlCommands) {
    Write-Host ''
    Write-Host 'MySQL commands for user-side verification:'
    Write-Host ($MysqlExe + " -u " + $MysqlUser + " -p " + $Database + " < " + $CleanupSql)
    Write-Host ($MysqlExe + " -u " + $MysqlUser + " -p " + $Database + " < " + $DataSql)
    Write-Host ''
    Write-Host 'Run autoPlan once per scene with factoryCode=TM_SCENE_xxx and scheduleDate=2026-07-xx.'
}

if (-not $SkipMaven) {
    Push-Location $RepoRoot
    try {
        mvn -pl APS-Modules/aps-tm -am "-Dtest=TmAutoPlanTestReportScenarioTest,TmAutoPlanStepTest,TmAutoPlanScenarioTest" -DfailIfNoTests=false test
    } finally {
        Pop-Location
    }
}
