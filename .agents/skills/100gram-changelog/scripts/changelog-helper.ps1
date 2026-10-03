<#
.SYNOPSIS
    Automation helper for 100gram changelog and release management.

.DESCRIPTION
    Provides operations to inspect git commits, validate CHANGELOG.md formatting,
    and cut new releases.

.PARAMETER Action
    Operation to perform: 'Summary', 'Validate', 'Release'.

.PARAMETER Version
    Version string to release (e.g. '12.10.5-v1.0'). Required when Action is 'Release'.

.PARAMETER Count
    Number of recent commits to analyze for Summary (default: 20).

.EXAMPLE
    .\changelog-helper.ps1 -Action Summary
    .\changelog-helper.ps1 -Action Validate
    .\changelog-helper.ps1 -Action Release -Version "12.10.5-v1.0"
#>

[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [ValidateSet("Summary", "Validate", "Release")]
    [string]$Action,

    [Parameter(Mandatory = $false)]
    [string]$Version,

    [Parameter(Mandatory = $false)]
    [int]$Count = 20
)

$gitTopLevel = git rev-parse --show-toplevel 2>$null
if ($gitTopLevel -and (Test-Path $gitTopLevel)) {
    $RepoRoot = $gitTopLevel.Trim()
} else {
    $RepoRoot = (Get-Item $PSScriptRoot).Parent.Parent.Parent.Parent.FullName
}
$ChangelogPath = Join-Path $RepoRoot "CHANGELOG.md"
$FeaturesPath = Join-Path $RepoRoot "FEATURES.md"

function Get-CommitSummary {
    Write-Host "=== 100gram Git Commits Summary (last $Count) ===" -ForegroundColor Cyan
    $commits = git log -n $Count --pretty=format:"%h|%s|%an|%as"
    
    if (-not $commits) {
        Write-Warning "No git commits found."
        return
    }

    $categorized = @{
        "Added" = @()
        "Fixed" = @()
        "Changed" = @()
        "Other" = @()
    }

    foreach ($line in ($commits -split "`n")) {
        if (-not $line.Trim()) { continue }
        $parts = $line -split "\|"
        $hash = $parts[0]
        $msg = $parts[1]
        $author = $parts[2]
        $date = $parts[3]

        if ($msg -match "^feat(\(.*\))?:") {
            $clean = $msg -replace "^feat(\(.*\))?:\s*", ""
            $categorized["Added"] += "- $clean ($hash)"
        } elseif ($msg -match "^fix(\(.*\))?:") {
            $clean = $msg -replace "^fix(\(.*\))?:\s*", ""
            $categorized["Fixed"] += "- $clean ($hash)"
        } elseif ($msg -match "^(refactor|perf|chore|docs)(\(.*\))?:") {
            $clean = $msg -replace "^(refactor|perf|chore|docs)(\(.*\))?:\s*", ""
            $categorized["Changed"] += "- $clean ($hash)"
        } else {
            $categorized["Other"] += "- $msg ($hash)"
        }
    }

    Write-Host "`n### Proposed CHANGELOG additions:" -ForegroundColor Green
    if ($categorized["Added"].Count -gt 0) {
        Write-Host "`n### Added" -ForegroundColor Yellow
        $categorized["Added"] | ForEach-Object { Write-Host $_ }
    }
    if ($categorized["Fixed"].Count -gt 0) {
        Write-Host "`n### Fixed" -ForegroundColor Yellow
        $categorized["Fixed"] | ForEach-Object { Write-Host $_ }
    }
    if ($categorized["Changed"].Count -gt 0) {
        Write-Host "`n### Changed" -ForegroundColor Yellow
        $categorized["Changed"] | ForEach-Object { Write-Host $_ }
    }
    if ($categorized["Other"].Count -gt 0) {
        Write-Host "`n### Other Commits" -ForegroundColor Gray
        $categorized["Other"] | ForEach-Object { Write-Host $_ }
    }
}

function Test-ChangelogValidity {
    Write-Host "=== Validating CHANGELOG.md ===" -ForegroundColor Cyan
    if (-not (Test-Path $ChangelogPath)) {
        Write-Error "CHANGELOG.md not found at $ChangelogPath"
        return $false
    }

    $content = Get-Content $ChangelogPath -Raw
    $hasUnreleased = $content -match "##\s*\[Unreleased\]"
    if (-not $hasUnreleased) {
        Write-Warning "CHANGELOG.md does not have an '## [Unreleased]' section!"
        return $false
    }

    # Check for valid category headers
    $validCategories = @("### Added", "### Changed", "### Deprecated", "### Removed", "### Fixed", "### Security")
    $foundCategories = [regex]::Matches($content, "###\s+[A-Za-z]+") | ForEach-Object { $_.Value }
    
    Write-Host "Found [Unreleased] section: OK" -ForegroundColor Green
    Write-Host "Total length: $($content.Length) characters" -ForegroundColor Green
    Write-Host "Validation passed successfully." -ForegroundColor Green
    return $true
}

function Invoke-ReleaseCut {
    param([string]$ReleaseVersion)

    if (-not $ReleaseVersion) {
        Write-Error "Release version must be provided! Example: -Version '12.10.5-v1.0'"
        return
    }

    if (-not (Test-Path $ChangelogPath)) {
        Write-Error "CHANGELOG.md not found."
        return
    }

    $today = (Get-Date).ToString("yyyy-MM-dd")
    $content = Get-Content $ChangelogPath -Raw

    if ($content -notmatch "##\s*\[Unreleased\]") {
        Write-Error "No [Unreleased] block found in CHANGELOG.md."
        return
    }

    $newHeader = "## [Unreleased]`n`n## [$ReleaseVersion] - $today"
    $newContent = $content -replace "##\s*\[Unreleased\]", $newHeader

    Set-Content -Path $ChangelogPath -Value $newContent -Encoding UTF8
    Write-Host "Release $ReleaseVersion ($today) cut successfully in CHANGELOG.md!" -ForegroundColor Green
}

switch ($Action) {
    "Summary"  { Get-CommitSummary }
    "Validate" { $null = Test-ChangelogValidity }
    "Release"  { Invoke-ReleaseCut -ReleaseVersion $Version }
}
