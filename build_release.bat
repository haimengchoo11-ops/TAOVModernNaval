@echo off
setlocal

REM Ensure execution from project root even when double-clicked elsewhere.
cd /d "%~dp0"

echo ========================================
echo TAOVModernNaval Auto Fix and Package
echo ========================================
echo.
echo Running: gradlew.bat fixAndPackage --console=plain
echo.

call gradlew.bat fixAndPackage --console=plain
if errorlevel 1 goto :failed

echo.
echo Build and packaging completed successfully.
echo Artifact directory: build\libs
echo.
dir /b build\libs\*.jar 2>nul
goto :end

:failed
echo.
echo Build or packaging failed. See logs above for details.
exit /b 1

:end
echo.
pause
endlocal
