$ErrorActionPreference = 'Stop'
$account = Read-Host 'Usuario de prueba'
$secret = Read-Host 'Contraseña' -AsSecureString
$plain = [System.Net.NetworkCredential]::new('', $secret).Password
$payload = @{ username = $account; password = $plain } | ConvertTo-Json
$headers = @{ 'X-Requested-With' = 'XMLHttpRequest' }
try {
    $response = Invoke-RestMethod 'http://localhost:8080/api/auth/login' -Method Post -Body $payload -ContentType 'application/json' -Headers $headers -SessionVariable session
    Write-Host ('Login: ' + $response.success)
    $me = Invoke-RestMethod 'http://localhost:8080/api/auth/me' -WebSession $session
    Write-Host ('Sesión verificada: ' + $me.username)
} finally {
    $plain = $null
    $payload = $null
    if ($session) { Invoke-RestMethod 'http://localhost:8080/api/auth/logout' -Method Post -Headers $headers -WebSession $session | Out-Null }
}
