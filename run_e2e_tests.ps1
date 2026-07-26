<#
=============================================================================
OmniCare EMR - E2E Integration Test Suite PowerShell Wrapper
Author: Worker E2E
Usage: .\run_e2e_tests.ps1 [-ApiUrl "http://localhost:8080"] [-DbHost "localhost"] [-DbPort 5432]
=============================================================================
#>
[CmdletBinding()]
Param(
    [string]$ApiUrl = "http://localhost:8080",
    [string]$DbHost = "localhost",
    [int]$DbPort = 5432,
    [string]$DbName = "omnicare_db",
    [string]$DbUser = "omnicare_user",
    [string]$DbPass = "omnicare_pass"
)

$ErrorActionPreference = "Stop"

Write-Host "=================================================================" -ForegroundColor Cyan
Write-Host "         OmniCare EMR - Opaque-Box E2E Test Suite               " -ForegroundColor Cyan
Write-Host "=================================================================" -ForegroundColor Cyan

# 1. Database Port Readiness Probe
Write-Host "`n[1/3] Probing PostgreSQL TCP Port ($DbHost:$DbPort)..." -ForegroundColor Yellow
$tcpClient = New-Object System.Net.Sockets.TcpClient
try {
    $asyncResult = $tcpClient.BeginConnect($DbHost, $DbPort, $null, $null)
    $success = $asyncResult.AsyncWaitHandle.WaitOne(3000, $false)
    if ($success) {
        $tcpClient.EndConnect($asyncResult)
        Write-Host "✅ PostgreSQL TCP Port $DbPort is OPEN and accepting connections." -ForegroundColor Green
    } else {
        Write-Host "❌ Timed out connecting to PostgreSQL port $DbPort on $DbHost." -ForegroundColor Red
        Exit 1
    }
} catch {
    Write-Host "❌ Failed to connect to PostgreSQL port $DbPort: $_" -ForegroundColor Red
    Exit 1
} finally {
    $tcpClient.Close()
}

# 2. Spring Boot API Health / Liveness Probe
Write-Host "`n[2/3] Probing Spring Boot API Liveness ($ApiUrl)..." -ForegroundColor Yellow
try {
    $healthResponse = Invoke-WebRequest -Uri "$ApiUrl/actuator/health" -TimeoutSec 5 -UseBasicParsing -ErrorAction SilentlyContinue
    if ($healthResponse.StatusCode -eq 200) {
        Write-Host "✅ Spring Boot API is HEALTHY (HTTP status 200)." -ForegroundColor Green
    } else {
        Write-Host "⚠️ Actuator returned status $($healthResponse.StatusCode), probing patient endpoint..." -ForegroundColor Yellow
    }
} catch {
    Write-Host "⚠️ Health probe failed, continuing to test execution..." -ForegroundColor Yellow
}

# 3. Execute Python E2E Test Suite
Write-Host "`n[3/3] Executing Python E2E Test Suite (Tiers 1-5)..." -ForegroundColor Yellow
Write-Host "-----------------------------------------------------------------" -ForegroundColor Gray

$scriptPath = Join-Path $PSScriptRoot "e2e_test_suite.py"

python $scriptPath --api-url $ApiUrl --db-host $DbHost --db-port $DbPort --db-name $DbName --db-user $DbUser --db-pass $DbPass
$exitCode = $LASTEXITCODE

Write-Host "-----------------------------------------------------------------" -ForegroundColor Gray
if ($exitCode -eq 0) {
    Write-Host "🎉 ALL E2E TESTS PASSED SUCCESSFULLY! (Exit Code 0)" -ForegroundColor Green
    Exit 0
} else {
    Write-Host "❌ E2E TEST SUITE ENCOUNTERED FAILURES! (Exit Code $exitCode)" -ForegroundColor Red
    Exit $exitCode
}
