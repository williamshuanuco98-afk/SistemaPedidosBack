$excel = New-Object -ComObject Excel.Application
$excel.Visible = $false
$wb = $excel.Workbooks.Open('c:\Users\User\OneDrive\Escritorio\Proyectos\SistemaWebPedidosBack\nueva letra.xlsx')
$ws = $wb.Sheets.Item(1)

for ($r = 1; $r -le 25; $r++) {
    $line = ""
    for ($c = 1; $c -le 15; $c++) {
        $cell = $ws.Cells.Item($r, $c)
        $text = $cell.Text
        $merge = $cell.MergeArea.Address
        if ($text -and $text.Trim() -ne '') {
            $line += " | C" + $c + " [" + $merge + "]: " + $text
        }
    }
    if ($line -ne '') {
        Write-Output ("ROW " + $r + ":" + $line)
    }
}

$wb.Close($false)
$excel.Quit()
