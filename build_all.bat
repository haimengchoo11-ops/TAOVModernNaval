@echo off
REM Build both projects in this workspace (full + slim).
REM NOTE: keep this file ASCII-only. cmd.exe parses .bat in the OEM codepage,
REM       so non-ASCII comments corrupt it and get executed as commands.
REM
REM Usage:
REM   build_all.bat                 build both (offline)
REM   build_all.bat -Target full    build full version only
REM   build_all.bat -Target slim    build slim version only
REM   build_all.bat -Online         allow network dependency resolution
setlocal
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0build_all.ps1" %*
set "CODE=%ERRORLEVEL%"
echo.
if not "%CODE%"=="0" (
    echo [FAILED] at least one project failed, exit code %CODE%
) else (
    echo [OK] all projects built successfully
)
exit /b %CODE%
