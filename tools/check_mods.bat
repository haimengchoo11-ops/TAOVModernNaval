@echo off
REM Validate every jar in a mods directory and report corrupt ones.
REM Keep this file ASCII-only: cmd parses .bat in the OEM codepage.
REM
REM Usage:
REM   check_mods.bat
REM   check_mods.bat "C:\...\.minecraft\versions\TAOV 2 Sea Power\mods"
setlocal
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0check_mods.ps1" -ModsDir "%~1"
exit /b %ERRORLEVEL%
