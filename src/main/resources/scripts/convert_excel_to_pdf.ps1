param(
    [Parameter(Mandatory=$true)]
    [string]$InputXlsx,
    [Parameter(Mandatory=$true)]
    [string]$OutputPdf
)

$excel = $null
$wb = $null
try {
    $excel = New-Object -ComObject Excel.Application
    $excel.Visible = $false
    $excel.DisplayAlerts = $false

    if (Test-Path $OutputPdf) {
        Remove-Item $OutputPdf -Force
    }

    $wb = $excel.Workbooks.Open($InputXlsx)
    $ws = $wb.ActiveSheet

    $ws.PageSetup.Orientation = 1 # xlPortrait
    $ws.PageSetup.PaperSize = 9 # xlPaperA4
    $ws.PageSetup.Zoom = $false
    $ws.PageSetup.FitToPagesWide = 1
    $ws.PageSetup.FitToPagesTall = 1

    $ws.ExportAsFixedFormat(0, $OutputPdf)
    $wb.Close($false)
    $excel.Quit()
    Write-Host "PDF_CONVERT_SUCCESS"
} catch {
    Write-Error "PDF_CONVERT_ERROR: $_"
    exit 1
} finally {
    if ($wb) { [System.Runtime.Interopservices.Marshal]::ReleaseComObject($wb) | Out-Null }
    if ($excel) { [System.Runtime.Interopservices.Marshal]::ReleaseComObject($excel) | Out-Null }
    [System.GC]::Collect()
    [System.GC]::WaitForPendingFinalizers()
}
