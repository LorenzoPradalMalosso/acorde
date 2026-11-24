param([switch]$DemoData)

$ErrorActionPreference = 'Stop'

$repoRoot = Split-Path -Parent $PSScriptRoot
$envPath = Join-Path $repoRoot '.env'
$examplePath = Join-Path $repoRoot '.env.example'

if (-not (Test-Path -LiteralPath $envPath)) {
    if (-not (Test-Path -LiteralPath $examplePath)) {
        throw 'Neither .env nor .env.example exists. Restore the example configuration before starting the API.'
    }
    Copy-Item -LiteralPath $examplePath -Destination $envPath
}

$settings = [ordered]@{}
foreach ($line in [System.IO.File]::ReadAllLines($envPath)) {
    if ($line -match '^\s*#' -or $line -notmatch '^\s*([A-Za-z_][A-Za-z0-9_]*)\s*=\s*(.*)$') { continue }
    $settings[$Matches[1]] = $Matches[2].Trim().Trim('"').Trim("'")
}

$secret = [Environment]::GetEnvironmentVariable('JWT_SECRET_BASE64', 'Process')
if ([string]::IsNullOrWhiteSpace($secret)) { $secret = $settings['JWT_SECRET_BASE64'] }
$secretIsValid = $false
if (-not [string]::IsNullOrWhiteSpace($secret)) {
    try { $secretIsValid = [Convert]::FromBase64String($secret).Length -ge 32 } catch { $secretIsValid = $false }
}
if (-not $secretIsValid) {
    $bytes = [byte[]]::new(32)
    $random = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    try { $random.GetBytes($bytes) } finally { $random.Dispose() }
    $secret = [Convert]::ToBase64String($bytes)
    $settings['JWT_SECRET_BASE64'] = $secret
    $lines = [System.Collections.Generic.List[string]]::new()
    $updated = $false
    foreach ($line in [System.IO.File]::ReadAllLines($envPath)) {
        if ($line -match '^\s*JWT_SECRET_BASE64\s*=') {
            $lines.Add("JWT_SECRET_BASE64=$secret")
            $updated = $true
        } else {
            $lines.Add($line)
        }
    }
    if (-not $updated) { $lines.Add("JWT_SECRET_BASE64=$secret") }
    [System.IO.File]::WriteAllLines($envPath, $lines, [System.Text.UTF8Encoding]::new($false))
}
$settings['JWT_SECRET_BASE64'] = $secret

foreach ($key in $settings.Keys) {
    # Keep Compose and Spring on the same source of truth. Stale shell variables
    # otherwise override .env for Spring while Compose reads the file separately.
    [Environment]::SetEnvironmentVariable($key, $settings[$key], 'Process')
}
if ($DemoData) {
    $demoPassword = [Environment]::GetEnvironmentVariable('ACORDE_DEMO_PASSWORD', 'Process')
    if ([string]::IsNullOrWhiteSpace($demoPassword)) {
        throw 'Set ACORDE_DEMO_PASSWORD in the ignored local .env file before using -DemoData.'
    }
    $demoPasswordBytes = [System.Text.Encoding]::UTF8.GetByteCount($demoPassword)
    if ($demoPasswordBytes -lt 12 -or $demoPasswordBytes -gt 72) {
        throw "ACORDE_DEMO_PASSWORD must be between 12 and 72 UTF-8 bytes in .env (current length: $demoPasswordBytes bytes). Update it and try again; the value is not displayed."
    }
    $env:SPRING_PROFILES_ACTIVE = 'local-demo'
}

Push-Location $PSScriptRoot
try {
    & .\mvnw.cmd spring-boot:run
} finally {
    Pop-Location
}

