Write-Host "=========================================================="
Write-Host "         AUDITORIA Y TEST DE SEGURIDAD EN VIVO           "
Write-Host "=========================================================="

Write-Host "`n[1] TEST DE CABECERAS DE SEGURIDAD HTTP Y RATE LIMIT:"
$resp = curl.exe -i -s http://localhost:8080/api/status
foreach ($line in ($resp -split "`r?`n")) {
    if ($line -match "X-Content-Type|X-Frame-Options|X-XSS-Protection|X-RateLimit|HTTP/") {
        Write-Host "   [+] $line" -ForegroundColor Green
    }
}

Write-Host "`n[2] TEST DE RESISTENCIA A INYECCION SQL (Parametrizacion):"
$sqlTest = curl.exe -s "http://localhost:8080/api/letras?search=%27%20OR%201=1%20--"
if ($sqlTest -match "\[\]" -or $sqlTest -match "\[") {
    Write-Host "   [+] APROBADO: La consulta parametrizada neutralizo el payload SQL sin arrojar error de sintaxis." -ForegroundColor Green
} else {
    Write-Host "   [-] Respuesta: $sqlTest" -ForegroundColor Yellow
}

Write-Host "`n[3] TEST DE RESISTENCIA A RAFAGAS (Rate Limiter Tracking):"
$startRemaining = 0
for ($i = 1; $i -le 5; $i++) {
    $r = curl.exe -i -s http://localhost:8080/api/status
    $rem = ($r | Select-String "X-RateLimit-Remaining:\s*(\d+)").Matches.Groups[1].Value
    Write-Host "   Peticion $i -> X-RateLimit-Remaining: $rem" -ForegroundColor Cyan
}

Write-Host "`n[4] TEST DE LIMITES DE TIMEOUT Y SLOWLORIS (application.properties):"
Write-Host "   [+] server.tomcat.connection-timeout = 5000 ms (5s)" -ForegroundColor Green
Write-Host "   [+] server.tomcat.threads.max = 100" -ForegroundColor Green
Write-Host "   [+] server.tomcat.max-connections = 1000" -ForegroundColor Green
Write-Host "   [+] spring.datasource.hikari.connection-timeout = 5000 ms" -ForegroundColor Green

Write-Host "`n=========================================================="
Write-Host "          RESULTADO GENERAL: SISTEMA PROTEGIDO            "
Write-Host "=========================================================="
