$ErrorActionPreference = 'Stop'
function Assert-HttpStatus($Path, $Method, $Headers, $Expected) {
    try {
        $response = Invoke-WebRequest -UseBasicParsing -Uri ('http://localhost:8080' + $Path) -Method $Method -Headers $Headers
        $actual = [int]$response.StatusCode
    } catch {
        if (-not $_.Exception.Response) { throw }
        $actual = [int]$_.Exception.Response.StatusCode
    }
    if ($actual -ne $Expected) { throw "Fallo: $Method $Path devolvió $actual; se esperaba $Expected" }
    Write-Host "OK: $Method $Path -> $Expected"
}
Assert-HttpStatus '/api/usuarios' 'GET' @{} 401
Assert-HttpStatus '/api/pedidos' 'GET' @{ 'X-User-Role' = 'ADMIN'; 'X-Username' = 'admin' } 401
Assert-HttpStatus '/api/auth/login' 'POST' @{} 403
Write-Host 'Comprobaciones de acceso anónimo correctas. No se modificaron datos.'
