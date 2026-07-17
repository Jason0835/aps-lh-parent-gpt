# 胎面自动排程统一场景测试辅助脚本
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
$SceneResourceDir = Join-Path $RepoRoot 'APS-Modules/aps-tm/src/test/resources/tm-auto-plan'
$ScenarioTestFile = Join-Path $RepoRoot 'APS-Modules/aps-tm/src/test/java/com/zlt/aps/tm/autoplan/TmAutoPlanScenarioTest.java'
$LegacyScenePattern = 'ca' + 'se_' + '*.json'
$OldReportTestClassName = 'TmAutoPlanTestReport' + 'ScenarioTest'
$ReportScenarioTestFile = Join-Path $RepoRoot ('APS-Modules/aps-tm/src/test/java/com/zlt/aps/tm/autoplan/' + $OldReportTestClassName + '.java')

function Test-TmAutoPlanSceneConvention {
    $legacySceneFiles = @(Get-ChildItem -LiteralPath $SceneResourceDir -Filter $LegacyScenePattern -File)
    if ($legacySceneFiles.Count -gt 0) {
        $legacyNames = ($legacySceneFiles | Select-Object -ExpandProperty Name) -join ', '
        throw ('检测到未迁移的旧场景文件：' + $legacyNames)
    }

    if (Test-Path -LiteralPath $ReportScenarioTestFile) {
        throw ('检测到未删除的报告专用场景测试类：' + $OldReportTestClassName + '.java')
    }

    if (-not (Test-Path -LiteralPath $ScenarioTestFile)) {
        throw '未找到统一场景测试入口：TmAutoPlanScenarioTest.java'
    }

    $scenarioTestContent = Get-Content -Raw -LiteralPath $ScenarioTestFile
    if (-not $scenarioTestContent.Contains('loadAllScenes')) {
        throw '统一场景测试入口尚未使用 loadAllScenes 自动发现 tm_scene_*.json'
    }
}

Test-TmAutoPlanSceneConvention

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
        mvn -pl APS-Modules/aps-tm -am "-Dtest=TmAutoPlanScenarioTest,TmAutoPlanStepTest" -DfailIfNoTests=false test
        if ($LASTEXITCODE -ne 0) {
            throw ('胎面自动排程 JSON 回归 Maven 执行失败，退出码：' + $LASTEXITCODE)
        }

        $surefireReportDir = Join-Path $RepoRoot 'APS-Modules/aps-tm/target/surefire-reports'
        $selectedReports = @(
            'TEST-com.zlt.aps.tm.autoplan.TmAutoPlanScenarioTest.xml',
            'TEST-com.zlt.aps.tm.autoplan.TmAutoPlanStepTest.xml'
        )
        foreach ($reportName in $selectedReports) {
            $reportPath = Join-Path $surefireReportDir $reportName
            if (-not (Test-Path -LiteralPath $reportPath)) {
                throw ('未生成 Surefire 报告：' + $reportName)
            }
            [xml]$report = Get-Content -Raw -LiteralPath $reportPath
            $failureCount = [int]$report.testsuite.failures
            $errorCount = [int]$report.testsuite.errors
            if ($failureCount -gt 0 -or $errorCount -gt 0) {
                throw ('Surefire 报告存在失败：' + $reportName + '，failures=' + $failureCount + '，errors=' + $errorCount)
            }
        }
    } finally {
        Pop-Location
    }
}
