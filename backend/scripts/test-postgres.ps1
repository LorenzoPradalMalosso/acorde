$ErrorActionPreference = 'Stop'
$testContainer = "acorde-test-$([guid]::NewGuid().ToString('N').Substring(0, 8))"
$envNames = @(
    'ACORDE_TEST_DB_URL', 'ACORDE_TEST_DB_USERNAME', 'ACORDE_TEST_DB_PASSWORD',
    'ACORDE_TEST_DDL_AUTO', 'ACORDE_TEST_FLYWAY_ENABLED', 'DEBUG'
)
$previousValues = @{}
foreach ($name in $envNames) { $previousValues[$name] = [Environment]::GetEnvironmentVariable($name, 'Process') }

docker run --rm -d --name $testContainer -e POSTGRES_DB=acorde_test -e POSTGRES_USER=acorde_test `
    -e POSTGRES_PASSWORD=acorde_test -p 127.0.0.1::5432 postgres:17-alpine | Out-Null
if ($LASTEXITCODE -ne 0) { throw 'Não foi possível iniciar o PostgreSQL temporário.' }

try {
    $ready = $false
    for ($attempt = 0; $attempt -lt 30; $attempt++) {
        docker exec $testContainer pg_isready -U acorde_test -d acorde_test *> $null
        if ($LASTEXITCODE -eq 0) { $ready = $true; break }
        Start-Sleep -Seconds 1
    }
    if (-not $ready) { throw 'O PostgreSQL temporário não ficou pronto.' }

    $port = ((docker port $testContainer 5432/tcp) -split ':')[-1]
    $env:ACORDE_TEST_DB_URL = "jdbc:postgresql://localhost:$port/acorde_test"
    $env:ACORDE_TEST_DB_USERNAME = 'acorde_test'
    $env:ACORDE_TEST_DB_PASSWORD = 'acorde_test'
    $env:ACORDE_TEST_DDL_AUTO = 'none'
    $env:ACORDE_TEST_FLYWAY_ENABLED = 'true'
    $env:DEBUG = 'false'
    Push-Location (Join-Path $PSScriptRoot '..')
    $previousErrorActionPreference = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try {
        .\mvnw.cmd test 2>&1
        $testExitCode = $LASTEXITCODE
    }
    finally {
        $ErrorActionPreference = $previousErrorActionPreference
        Pop-Location
    }
    if ($testExitCode -ne 0) { throw 'A suíte de integração falhou.' }
}
finally {
    docker stop $testContainer | Out-Null
    foreach ($name in $envNames) {
        [Environment]::SetEnvironmentVariable($name, $previousValues[$name], 'Process')
    }
}
