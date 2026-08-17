$payload = @{
    username = "admin"
    password = "admin123"
} | ConvertTo-Json

$response = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" -Method Post -Body $payload -ContentType "application/json"
Write-Host "Admin Login:"
$response | Format-List

$payloadOp = @{
    username = "operaciones"
    password = "operaciones123"
} | ConvertTo-Json

$responseOp = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" -Method Post -Body $payloadOp -ContentType "application/json"
Write-Host "Operaciones Login:"
$responseOp | Format-List
