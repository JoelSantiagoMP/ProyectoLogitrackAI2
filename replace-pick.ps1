param([string]$path)
(Get-Content -LiteralPath $path) -replace "^pick c843db1528f16fd7ff96105027a82df8aa48d41d\b","edit c843db1528f16fd7ff96105027a82df8aa48d41d" | Set-Content -LiteralPath $path
