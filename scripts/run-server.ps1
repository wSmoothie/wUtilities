$ErrorActionPreference = 'Stop'
Set-StrictMode -Version 2.0

$RepoRoot = Split-Path -Parent $PSScriptRoot
$Support = Get-Content -Raw -LiteralPath (Join-Path $RepoRoot 'supported-versions.json') | ConvertFrom-Json
$UserAgent = 'wWorldMapUtils-launcher/0.1 (https://github.com/wSmoothie/wWorldMapUtils)'
$Headers = @{ 'User-Agent' = $UserAgent }
$ServerKinds = @('fabric', 'paper', 'folia', 'purpur', 'leaf')

function Select-MenuItem([string]$Prompt, [object[]]$Items) {
    Write-Host ''
    Write-Host $Prompt
    for ($index = 0; $index -lt $Items.Count; $index++) {
        Write-Host ('  {0}. {1}' -f ($index + 1), $Items[$index])
    }
    while ($true) {
        $raw = Read-Host 'Selection'
        $selected = 0
        if ([int]::TryParse($raw, [ref]$selected) -and $selected -ge 1 -and $selected -le $Items.Count) {
            return $Items[$selected - 1]
        }
        Write-Warning 'Enter one of the listed numbers.'
    }
}

function Invoke-Api([string]$Uri) {
    Invoke-RestMethod -Uri $Uri -Headers $Headers -Method Get
}

function Get-PrimaryModrinthVersion([string]$Project, [string]$MinecraftVersion) {
    $loaders = [Uri]::EscapeDataString('["fabric"]')
    $versions = [Uri]::EscapeDataString(('[' + ('"' + $MinecraftVersion + '"') + ']'))
    $uri = "https://api.modrinth.com/v2/project/$Project/version?loaders=$loaders&game_versions=$versions&include_changelog=false"
    $matches = @(Invoke-Api $uri | ForEach-Object { $_ })
    if ($matches.Count -eq 0) { throw "No Fabric $MinecraftVersion version found for Modrinth project '$Project'." }
    return $matches[0]
}

function Install-Download(
    [string]$Uri,
    [string]$Destination,
    [string]$BuildId,
    [string]$ExpectedSha256 = ''
) {
    $marker = "$Destination.wworldmap-build"
    if ((Test-Path -LiteralPath $Destination) -and (Test-Path -LiteralPath $marker) -and
        ((Get-Content -Raw -LiteralPath $marker).Trim() -eq $BuildId)) {
        Write-Host "Using cached $BuildId"
        return
    }
    $temporary = "$Destination.download"
    Write-Host "Downloading $BuildId"
    Invoke-WebRequest -UseBasicParsing -Uri $Uri -Headers $Headers -OutFile $temporary
    if ($ExpectedSha256) {
        $actual = (Get-FileHash -Algorithm SHA256 -LiteralPath $temporary).Hash.ToLowerInvariant()
        if ($actual -ne $ExpectedSha256.ToLowerInvariant()) {
            Remove-Item -LiteralPath $temporary -Force
            throw "SHA-256 mismatch for $BuildId"
        }
    }
    Move-Item -LiteralPath $temporary -Destination $Destination -Force
    Set-Content -LiteralPath $marker -Value $BuildId -Encoding UTF8
}

function Resolve-PaperDownload([string]$Project, [string]$MinecraftVersion) {
    $builds = @(Invoke-Api "https://fill.papermc.io/v3/projects/$Project/versions/$MinecraftVersion/builds" |
        ForEach-Object { $_ })
    $selected = $builds | Sort-Object id -Descending | Select-Object -First 1
    if ($null -eq $selected) { throw "No $Project build found for $MinecraftVersion." }
    $download = $selected.downloads.'server:default'
    [pscustomobject]@{
        Id = "$Project-$MinecraftVersion-$($selected.id)"
        Uri = $download.url
        Sha256 = $download.checksums.sha256
        Channel = $selected.channel
    }
}

function Resolve-ServerDownload([string]$Kind, [string]$MinecraftVersion) {
    switch ($Kind) {
        'paper' { return Resolve-PaperDownload 'paper' $MinecraftVersion }
        'folia' { return Resolve-PaperDownload 'folia' $MinecraftVersion }
        'purpur' {
            $metadata = Invoke-Api "https://api.purpurmc.org/v2/purpur/$MinecraftVersion"
            $build = [string]$metadata.builds.latest
            return [pscustomobject]@{
                Id = "purpur-$MinecraftVersion-$build"
                Uri = "https://api.purpurmc.org/v2/purpur/$MinecraftVersion/$build/download"
                Sha256 = ''
                Channel = 'latest'
            }
        }
        'leaf' {
            $metadata = Invoke-Api "https://api.leafmc.one/v2/projects/leaf/versions/$MinecraftVersion/builds"
            $selected = $metadata.builds | Sort-Object build -Descending | Select-Object -First 1
            if ($null -eq $selected) { throw "No Leaf build found for $MinecraftVersion." }
            $name = $selected.downloads.primary.name
            return [pscustomobject]@{
                Id = "leaf-$MinecraftVersion-$($selected.build)"
                Uri = "https://api.leafmc.one/v2/projects/leaf/versions/$MinecraftVersion/builds/$($selected.build)/downloads/$name"
                Sha256 = $selected.downloads.primary.sha256
                Channel = $selected.channel
            }
        }
        default { throw "Unsupported server kind: $Kind" }
    }
}

function Install-FabricServer([string]$MinecraftVersion, [string]$ServerDirectory) {
    $loaders = @(Invoke-Api "https://meta.fabricmc.net/v2/versions/loader/$MinecraftVersion" |
        ForEach-Object { $_ })
    $loader = $loaders | Where-Object { $_.loader.stable } | Select-Object -First 1
    $installers = @(Invoke-Api 'https://meta.fabricmc.net/v2/versions/installer' |
        ForEach-Object { $_ })
    $installer = $installers | Where-Object { $_.stable } | Select-Object -First 1
    if ($null -eq $loader -or $null -eq $installer) { throw 'Could not resolve stable Fabric loader/installer.' }
    $loaderVersion = $loader.loader.version
    $installerVersion = $installer.version
    $uri = "https://meta.fabricmc.net/v2/versions/loader/$MinecraftVersion/$loaderVersion/$installerVersion/server/jar"
    Install-Download $uri (Join-Path $ServerDirectory 'server.jar') "fabric-$MinecraftVersion-$loaderVersion-installer-$installerVersion"

    $mods = Join-Path $ServerDirectory 'mods'
    New-Item -ItemType Directory -Force -Path $mods | Out-Null
    $fabricApi = Get-PrimaryModrinthVersion 'fabric-api' $MinecraftVersion
    $file = @($fabricApi.files | Where-Object { $_.primary })[0]
    if ($null -eq $file) { $file = @($fabricApi.files)[0] }
    $destination = Join-Path $mods 'fabric-api.jar'
    $marker = "$destination.wworldmap-build"
    $id = "fabric-api-$($fabricApi.id)"
    if (-not ((Test-Path $destination) -and (Test-Path $marker) -and
        ((Get-Content -Raw $marker).Trim() -eq $id))) {
        $temporary = "$destination.download"
        Invoke-WebRequest -UseBasicParsing -Uri $file.url -Headers $Headers -OutFile $temporary
        $actual = (Get-FileHash -Algorithm SHA512 -LiteralPath $temporary).Hash.ToLowerInvariant()
        if ($actual -ne ([string]$file.hashes.sha512).ToLowerInvariant()) {
            Remove-Item -LiteralPath $temporary -Force
            throw 'SHA-512 mismatch for Fabric API.'
        }
        Move-Item $temporary $destination -Force
        Set-Content -LiteralPath $marker -Value $id -Encoding UTF8
    }
}

function Confirm-Eula([string]$ServerDirectory) {
    $eulaPath = Join-Path $ServerDirectory 'eula.txt'
    if ((Test-Path $eulaPath) -and (Select-String -Quiet -LiteralPath $eulaPath -Pattern '^eula=true$')) { return $true }
    Write-Host ''
    Write-Host 'Minecraft EULA: https://aka.ms/MinecraftEULA'
    $answer = Read-Host 'Do you accept the Minecraft EULA for this test server? [y/N]'
    if ($answer -notmatch '^(?i:y|yes)$') {
        Write-Host 'EULA was not accepted; server was not launched.'
        return $false
    }
    Set-Content -LiteralPath $eulaPath -Value @('# Accepted by runServer.bat', 'eula=true') -Encoding ASCII
    return $true
}

$kind = [string](Select-MenuItem 'Choose a server implementation:' $ServerKinds)
$version = [string](Select-MenuItem 'Choose a supported Minecraft version:' @($Support.minecraft))
Write-Host ''
Write-Host 'Building wWorldMap Utils...'
$artifactRoot = if ($kind -eq 'fabric') {
    & (Join-Path $RepoRoot 'gradlew.bat') ':1.21.11-fabric:test' ':1.21.11-fabric:remapJar' '--configure-on-demand'
    Join-Path $RepoRoot 'versions\1.21.11-fabric\build\libs'
} else {
    & (Join-Path $RepoRoot 'gradlew.bat') ':bukkit:test' ':bukkit:jar' '--configure-on-demand'
    Join-Path $RepoRoot 'platform\bukkit\build\libs'
}
if ($LASTEXITCODE -ne 0) { throw "Gradle build failed with exit code $LASTEXITCODE." }

$artifact = Get-ChildItem -LiteralPath $artifactRoot -Filter 'wWorldMapUtils*.jar' |
    Where-Object { $_.Name -notmatch '-sources\.jar$' } | Sort-Object LastWriteTime -Descending | Select-Object -First 1
if ($null -eq $artifact) { throw 'Built wWorldMap Utils JAR was not found.' }

$serverDirectory = Join-Path $RepoRoot ("servers\$kind-$version")
New-Item -ItemType Directory -Force -Path $serverDirectory | Out-Null
if ($kind -eq 'fabric') {
    Install-FabricServer $version $serverDirectory
    $installDirectory = Join-Path $serverDirectory 'mods'
} else {
    $download = Resolve-ServerDownload $kind $version
    Write-Host "Resolved $($download.Id) [$($download.Channel)]"
    if (([string]$download.Channel).ToLowerInvariant() -notin @('stable', 'latest')) {
        Write-Warning "$kind $version latest build is on the $($download.Channel) channel."
    }
    Install-Download $download.Uri (Join-Path $serverDirectory 'server.jar') $download.Id $download.Sha256
    $installDirectory = Join-Path $serverDirectory 'plugins'
}

New-Item -ItemType Directory -Force -Path $installDirectory | Out-Null
Get-ChildItem -LiteralPath $installDirectory -Filter 'wWorldMapUtils*.jar' -ErrorAction SilentlyContinue |
    Remove-Item -Force
Copy-Item -LiteralPath $artifact.FullName -Destination (Join-Path $installDirectory 'wWorldMapUtils.jar')
if (-not (Confirm-Eula $serverDirectory)) { exit 0 }

Write-Host ''
Write-Host "Launching $kind $version from $serverDirectory"
Push-Location $serverDirectory
try {
    & java '-Xms1G' '-Xmx2G' '-jar' 'server.jar' 'nogui'
    exit $LASTEXITCODE
} finally {
    Pop-Location
}
