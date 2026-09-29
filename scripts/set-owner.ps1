# Replaces the sm2774us placeholder with your GitHub username.
# Usage (PowerShell, from the repo root):  .\scripts\set-owner.ps1 -Owner sm2774us
param([Parameter(Mandatory = $true)][string]$Owner)

$root = Split-Path -Parent $PSScriptRoot
$utf8 = New-Object System.Text.UTF8Encoding($false)   # UTF-8 without BOM
$pattern = '(?<![A-Za-z])sm2774us(?![A-Za-z])'            # whole word only: leaves CODEsm2774usS alone
$changed = 0

Get-ChildItem -Path $root -Recurse -File -Force |
  Where-Object {
    $_.FullName -notmatch '[\\/](node_modules|\.git|target|dist)[\\/]' -and
    $_.Name -ne 'package-lock.json' -and
    $_.Extension -in '.md', '.yml', '.yaml', '.json', '.ts', '.tf', ''
  } |
  ForEach-Object {
    $text = [System.IO.File]::ReadAllText($_.FullName)
    if ($text -cmatch $pattern) {
      [System.IO.File]::WriteAllText($_.FullName, ($text -creplace $pattern, $Owner), $utf8)
      Write-Host "updated $($_.FullName.Substring($root.Length + 1))"
      $changed++
    }
  }
Write-Host "done: $changed file(s) updated"
