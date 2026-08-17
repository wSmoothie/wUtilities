[CmdletBinding()]
param(
    [switch]$PrepareOnly
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version 2.0

$UtilsRoot = Split-Path -Parent $PSScriptRoot
$WorkspaceRoot = Split-Path -Parent $UtilsRoot
$WorldMapRoot = Join-Path $WorkspaceRoot 'wworldmap'
$WaypointsRoot = Join-Path $WorkspaceRoot 'wWaypoints'
$MinecraftVersion = '1.21.11'
$ModsDirectory = Join-Path $WorldMapRoot 'run\mods'
$ManifestPath = Join-Path $ModsDirectory '.runClient-modrinth-files.json'
$Headers = @{ 'User-Agent' = 'wWorldMapUtils-client-launcher/0.1 (https://github.com/wSmoothie/wWorldMapUtils)' }
$InstalledProjects = @{}
$DownloadedFiles = New-Object System.Collections.Generic.List[string]

function Invoke-Modrinth([string]$Uri) {
    Invoke-RestMethod -Method Get -Uri $Uri -Headers $Headers
}

function Get-CompatibleVersions([string]$Project) {
    $loaders = [Uri]::EscapeDataString('["fabric"]')
    $versions = [Uri]::EscapeDataString(('[' + ('"' + $MinecraftVersion + '"') + ']'))
    @(Invoke-Modrinth "https://api.modrinth.com/v2/project/$Project/version?loaders=$loaders&game_versions=$versions&include_changelog=false" |
        ForEach-Object { $_ })
}

function Install-Version($Version) {
    if ($null -eq $Version -or $InstalledProjects.ContainsKey([string]$Version.project_id)) { return }
    if ([string]$Version.project_id -eq 'P7dR8mSH') {
        Write-Host 'Fabric API is already supplied by the Loom development runtime.'
        $InstalledProjects[[string]$Version.project_id] = $true
        return
    }
    $InstalledProjects[[string]$Version.project_id] = $true

    foreach ($dependency in @($Version.dependencies | Where-Object { $_.dependency_type -eq 'required' })) {
        if ($dependency.version_id) {
            Install-Version (Invoke-Modrinth "https://api.modrinth.com/v2/version/$($dependency.version_id)")
        } elseif ($dependency.project_id) {
            $matches = Get-CompatibleVersions ([string]$dependency.project_id)
            if ($matches.Count -eq 0) { throw "No compatible required dependency $($dependency.project_id)." }
            Install-Version $matches[0]
        }
    }

    $file = @($Version.files | Where-Object { $_.primary })[0]
    if ($null -eq $file) { $file = @($Version.files)[0] }
    if ($null -eq $file) { throw "Modrinth version $($Version.id) has no downloadable file." }
    $destination = Join-Path $ModsDirectory ([string]$file.filename)
    $temporary = "$destination.download"
    Write-Host "Downloading $($Version.name)"
    Invoke-WebRequest -UseBasicParsing -Uri $file.url -Headers $Headers -OutFile $temporary
    $actual = (Get-FileHash -Algorithm SHA512 -LiteralPath $temporary).Hash.ToLowerInvariant()
    if ($actual -ne ([string]$file.hashes.sha512).ToLowerInvariant()) {
        Remove-Item -LiteralPath $temporary -Force
        throw "SHA-512 mismatch for $($file.filename)."
    }
    Move-Item -LiteralPath $temporary -Destination $destination -Force
    $DownloadedFiles.Add([string]$file.filename)
}

Write-Host 'wWorldMap development client (Minecraft 1.21.11)'
$rawProjects = Read-Host 'Extra Modrinth project slugs/IDs, comma-separated (blank for none)'
$projects = @($rawProjects -split ',' | ForEach-Object { $_.Trim() } | Where-Object { $_ })
Write-Host ''
Write-Host 'This will build and launch wWorldMap + wWaypoints.'
if ($projects.Count -gt 0) { Write-Host ('Extra projects: ' + ($projects -join ', ')) }
$confirmation = Read-Host 'Continue? [y/N]'
if ($confirmation -notmatch '^(?i:y|yes)$') {
    Write-Host 'Launch cancelled.'
    exit 0
}

New-Item -ItemType Directory -Force -Path $ModsDirectory | Out-Null
if (Test-Path -LiteralPath $ManifestPath) {
    foreach ($oldName in @(Get-Content -Raw -LiteralPath $ManifestPath | ConvertFrom-Json)) {
        $oldPath = Join-Path $ModsDirectory ([string]$oldName)
        if (Test-Path -LiteralPath $oldPath) { Remove-Item -LiteralPath $oldPath -Force }
    }
}
Get-ChildItem -LiteralPath $ModsDirectory -Filter 'wWaypoints*.jar' -ErrorAction SilentlyContinue |
    Remove-Item -Force

Write-Host 'Building wWaypoints...'
& (Join-Path $WaypointsRoot 'gradlew.bat') remapJar
if ($LASTEXITCODE -ne 0) { throw "wWaypoints build failed with exit code $LASTEXITCODE." }
$waypointsJar = Get-ChildItem -LiteralPath (Join-Path $WaypointsRoot 'build\libs') -Filter 'wWaypoints-*.jar' |
    Where-Object { $_.Name -notmatch '-sources\.jar$|-obf\.jar$' } |
    Sort-Object LastWriteTime -Descending | Select-Object -First 1
if ($null -eq $waypointsJar) { throw 'Built wWaypoints JAR was not found.' }
Copy-Item -LiteralPath $waypointsJar.FullName -Destination (Join-Path $ModsDirectory $waypointsJar.Name)

foreach ($project in $projects) {
    if ($project -eq 'fabric-api' -or $project -eq 'P7dR8mSH') {
        Write-Host 'Skipping explicit Fabric API; Loom already supplies it.'
        continue
    }
    $matches = Get-CompatibleVersions $project
    if ($matches.Count -eq 0) { throw "No Fabric $MinecraftVersion version found for '$project'." }
    Install-Version $matches[0]
}
$DownloadedFiles.ToArray() | ConvertTo-Json | Set-Content -LiteralPath $ManifestPath -Encoding UTF8

if ($PrepareOnly) {
    Write-Host "Client mods prepared in $ModsDirectory"
    exit 0
}

Write-Host 'Launching the Fabric development client...'
& (Join-Path $WorldMapRoot 'gradlew.bat') runClient '-PuseSiblingWWaypoints' '-PwithoutWWaypoints'
exit $LASTEXITCODE
