$modsPath = 'C:\Users\ROG STRIX\Downloads\TAOVModernNaval\run\mods'
Get-ChildItem -Path $modsPath -File | Where-Object {
    $_.Name -match 'cbc|Radars|warshiparise'
} | ForEach-Object {
    Remove-Item $_.FullName -Force
    Write-Host "Deleted: $($_.Name)"
}

Write-Host 'Remaining runtime jars:'
Get-ChildItem -Path $modsPath -File | Sort-Object Name | Select-Object -ExpandProperty Name
