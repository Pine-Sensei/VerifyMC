$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $PSScriptRoot
$utf8NoBom = [System.Text.UTF8Encoding]::new($false)

function Read-Utf8File {
    param([string]$Path)
    return [System.IO.File]::ReadAllText($Path, $utf8NoBom)
}

function Write-Utf8File {
    param(
        [string]$Path,
        [string]$Content
    )

    $normalized = $Content -replace "`r`n", "`n"
    [System.IO.File]::WriteAllText($Path, $normalized, $utf8NoBom)
}

function Update-File {
    param(
        [string]$RelativePath,
        [string]$Pattern,
        [scriptblock]$Transform,
        [int]$ExpectedMinimumMatches = 1
    )

    $path = Join-Path $root $RelativePath
    $content = Read-Utf8File -Path $path
    $regex = [System.Text.RegularExpressions.Regex]::new(
        $Pattern,
        [System.Text.RegularExpressions.RegexOptions]::Multiline
    )

    $matches = $regex.Matches($content)
    if ($matches.Count -lt $ExpectedMinimumMatches) {
        throw "No replacements made in $RelativePath"
    }

    $updated = & $Transform $content $regex
    if ($updated -ne $content) {
        Write-Utf8File -Path $path -Content $updated
        Write-Output $RelativePath
    }
}

$versionYmlPath = Join-Path $root 'version.yml'
$versionYml = Read-Utf8File -Path $versionYmlPath
$versionMatch = [regex]::Match($versionYml, '^version:\s*([^\s]+)\s*$', 'Multiline')
if (-not $versionMatch.Success) {
    throw 'Failed to read version from version.yml'
}

$version = $versionMatch.Groups[1].Value

Update-File -RelativePath 'plugin/pom.xml' -Pattern '(<artifactId>verifymc</artifactId>\s*<version>)([^<]+)(</version>)' -Transform {
    param($content, $regex)
    return $regex.Replace($content, [System.Text.RegularExpressions.MatchEvaluator]{
        param($match)
        return $match.Groups[1].Value + $version + $match.Groups[3].Value
    }, 1)
}

Update-File -RelativePath 'plugin-proxy/pom.xml' -Pattern '(<artifactId>verifymc-proxy</artifactId>\s*<version>)([^<]+)(</version>)' -Transform {
    param($content, $regex)
    return $regex.Replace($content, [System.Text.RegularExpressions.MatchEvaluator]{
        param($match)
        return $match.Groups[1].Value + $version + $match.Groups[3].Value
    }, 1)
}

Update-File -RelativePath 'frontend/glassx/package.json' -Pattern '("version":\s*")([^"]+)(")' -Transform {
    param($content, $regex)
    return $regex.Replace($content, [System.Text.RegularExpressions.MatchEvaluator]{
        param($match)
        return $match.Groups[1].Value + $version + $match.Groups[3].Value
    }, 1)
}

Update-File -RelativePath 'frontend/glassx/package-lock.json' -Pattern '("version":\s*")([^"]+)(")' -ExpectedMinimumMatches 2 -Transform {
    param($content, $regex)
    return $regex.Replace($content, [System.Text.RegularExpressions.MatchEvaluator]{
        param($match)
        return $match.Groups[1].Value + $version + $match.Groups[3].Value
    }, 2)
}

Update-File -RelativePath 'README.md' -Pattern 'verifymc-(?:proxy-)?\d+\.\d+\.\d+\.jar' -ExpectedMinimumMatches 2 -Transform {
    param($content, $regex)
    return $regex.Replace($content, {
        param($match)
        return [regex]::Replace($match.Value, '\d+\.\d+\.\d+', $version, 1)
    })
}

Update-File -RelativePath 'README_zh.md' -Pattern 'verifymc-(?:proxy-)?\d+\.\d+\.\d+\.jar' -ExpectedMinimumMatches 2 -Transform {
    param($content, $regex)
    return $regex.Replace($content, {
        param($match)
        return [regex]::Replace($match.Value, '\d+\.\d+\.\d+', $version, 1)
    })
}

Update-File -RelativePath 'release_notes_zh.md' -Pattern 'v\d+\.\d+\.\d+' -Transform {
    param($content, $regex)
    return $regex.Replace($content, "v$version", 2)
}

$releaseNotesPath = Join-Path $root 'release_notes_zh.md'
$releaseNotes = Read-Utf8File -Path $releaseNotesPath
$releaseNotesLines = $releaseNotes -split "`n", -1
$releaseNotesHeading = "# VerifyMC v$version 更新日志"
if ($releaseNotesLines.Count -lt 3) {
    throw 'release_notes_zh.md does not have the expected header structure'
}
if ($releaseNotesLines[2] -ne $releaseNotesHeading) {
    $releaseNotesLines[2] = $releaseNotesHeading
    Write-Utf8File -Path $releaseNotesPath -Content ($releaseNotesLines -join "`n")
    Write-Output 'release_notes_zh.md'
}

if (Test-Path (Join-Path $root 'plugin/dependency-reduced-pom.xml')) {
    Update-File -RelativePath 'plugin/dependency-reduced-pom.xml' -Pattern '(<artifactId>verifymc</artifactId>\s*<name>VerifyMC</name>\s*<version>)([^<]+)(</version>)' -Transform {
        param($content, $regex)
        return $regex.Replace($content, [System.Text.RegularExpressions.MatchEvaluator]{
            param($match)
            return $match.Groups[1].Value + $version + $match.Groups[3].Value
        }, 1)
    }
}
