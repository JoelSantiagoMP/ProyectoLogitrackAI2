@echo off
powershell -NoProfile -Command "(Get-Content -LiteralPath '%1') -replace '^pick c843db1528f16fd7ff96105027a82df8aa48d41d\b','edit c843db1528f16fd7ff96105027a82df8aa48d41d' | Set-Content -LiteralPath '%1'"
