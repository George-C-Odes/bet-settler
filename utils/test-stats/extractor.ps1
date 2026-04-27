$scriptDirectory = Split-Path -Parent $MyInvocation.MyCommand.Path
$repositoryRoot = Split-Path -Parent (Split-Path -Parent $scriptDirectory)
$reports = Get-ChildItem (Join-Path $repositoryRoot "target\surefire-reports\TEST-*.xml")

function New-CoverageTotals {
  @{
    LINE = @{ missed = 0; covered = 0 }
    BRANCH = @{ missed = 0; covered = 0 }
    INSTRUCTION = @{ missed = 0; covered = 0 }
    METHOD = @{ missed = 0; covered = 0 }
  }
}

function Add-CoverageRow {
  param(
    [hashtable]$Totals,
    $Row
  )

  $Totals.LINE.missed += [int]$Row.LINE_MISSED
  $Totals.LINE.covered += [int]$Row.LINE_COVERED
  $Totals.BRANCH.missed += [int]$Row.BRANCH_MISSED
  $Totals.BRANCH.covered += [int]$Row.BRANCH_COVERED
  $Totals.INSTRUCTION.missed += [int]$Row.INSTRUCTION_MISSED
  $Totals.INSTRUCTION.covered += [int]$Row.INSTRUCTION_COVERED
  $Totals.METHOD.missed += [int]$Row.METHOD_MISSED
  $Totals.METHOD.covered += [int]$Row.METHOD_COVERED
}

function Format-CoverageLine {
  param(
    [string]$Name,
    [hashtable]$Totals,
    [string]$Kind
  )

  $covered = $Totals[$Kind].covered
  $total = $covered + $Totals[$Kind].missed
  $pct = if ($total -eq 0) { 0 } else { [math]::Round(($covered * 100.0) / $total, 1) }
  return ($Name + "=" + $pct + "% (" + $covered + "/" + $total + ")")
}

$areaPackages = @{
  "domain.application" = @(
    "io.github.georgecodes.betsettler.domain.model",
    "io.github.georgecodes.betsettler.domain.validation",
    "io.github.georgecodes.betsettler.domain.service",
    "io.github.georgecodes.betsettler.application.dto",
    "io.github.georgecodes.betsettler.application.model.audit",
    "io.github.georgecodes.betsettler.application.model.dispatch",
    "io.github.georgecodes.betsettler.application.port.out",
    "io.github.georgecodes.betsettler.application.service"
  )
  "infrastructure" = @(
    "io.github.georgecodes.betsettler.infrastructure.config",
    "io.github.georgecodes.betsettler.infrastructure.observability",
    "io.github.georgecodes.betsettler.infrastructure.rest.controller",
    "io.github.georgecodes.betsettler.infrastructure.rest.dto",
    "io.github.georgecodes.betsettler.infrastructure.rest.error",
    "io.github.georgecodes.betsettler.infrastructure.rest.mapper",
    "io.github.georgecodes.betsettler.infrastructure.persistence.adapter",
    "io.github.georgecodes.betsettler.infrastructure.persistence.entity",
    "io.github.georgecodes.betsettler.infrastructure.persistence.mapper",
    "io.github.georgecodes.betsettler.infrastructure.messaging.kafka.consumer",
    "io.github.georgecodes.betsettler.infrastructure.messaging.kafka.dto",
    "io.github.georgecodes.betsettler.infrastructure.messaging.kafka.mapper",
    "io.github.georgecodes.betsettler.infrastructure.messaging.kafka.producer",
    "io.github.georgecodes.betsettler.infrastructure.messaging.settlement.dto",
    "io.github.georgecodes.betsettler.infrastructure.messaging.settlement.mapper",
    "io.github.georgecodes.betsettler.infrastructure.messaging.settlement.payload",
    "io.github.georgecodes.betsettler.infrastructure.messaging.settlement.publisher",
    "io.github.georgecodes.betsettler.infrastructure.messaging.rocketmq.publisher"
  )
}

$areaCoverage = @{}
foreach ($areaName in $areaPackages.Keys) {
  $areaCoverage[$areaName] = New-CoverageTotals
}

$suites = $reports.Count
$tests = 0
$failures = 0
$errors = 0
$skipped = 0
$totalTime = 0.0
foreach ($report in $reports) {
  [xml]$xml = Get-Content $report.FullName
  $suite = $xml.testsuite
  $tests += [int]$suite.tests
  $failures += [int]$suite.failures
  $errors += [int]$suite.errors
  $skipped += [int]$suite.skipped
  $totalTime += [double]$suite.time
}
$coverage = New-CoverageTotals
Import-Csv (Join-Path $repositoryRoot "target\jacoco-report\jacoco.csv") | ForEach-Object {
  Add-CoverageRow -Totals $coverage -Row $_

  $packageName = ($_.PACKAGE -replace '/', '.')
  foreach ($areaName in $areaPackages.Keys) {
    if ($areaPackages[$areaName] -contains $packageName) {
      Add-CoverageRow -Totals $areaCoverage[$areaName] -Row $_
    }
  }
}
[xml]$spotbugs = Get-Content (Join-Path $repositoryRoot "target\spotbugsXml.xml")
$spotbugsFindings = [int]$spotbugs.BugCollection.FindBugsSummary.total_bugs
$lines = @(
  "suites=$suites",
  "tests=$tests",
  "failures=$failures",
  "errors=$errors",
  "skipped=$skipped",
  ("time={0:N3}" -f $totalTime)
)
foreach ($kind in "LINE","BRANCH","INSTRUCTION","METHOD") {
  $lines += Format-CoverageLine -Name $kind.ToLower() -Totals $coverage -Kind $kind
}
foreach ($areaName in "domain.application","infrastructure") {
  foreach ($kind in "LINE","BRANCH") {
    $lines += Format-CoverageLine -Name ($areaName + "." + $kind.ToLower()) -Totals $areaCoverage[$areaName] -Kind $kind
  }
}
$lines += "spotbugs=$spotbugsFindings"
Set-Content -Path (Join-Path $scriptDirectory "stats.txt") -Value $lines
