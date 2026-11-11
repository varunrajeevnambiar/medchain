@echo off
echo ========================================================
echo Running MedChain Blockchain Demo
echo ========================================================

set "JDK_BIN=C:\Users\Dithin Mukkati\.p2\pool\plugins\org.eclipse.justj.openjdk.hotspot.jre.full.win32.x86_64_25.0.4.v20260826-0822\jre\bin"

if exist "%JDK_BIN%\java.exe" (
    set JAVA="%JDK_BIN%\java.exe"
) else (
    set JAVA=java
)

if not exist bin (
    echo ERROR: bin\ directory not found. Run compile_blockchain.bat first.
    pause
    exit /b 1
)

echo Using JRE: %JAVA%
echo.
%JAVA% -cp bin com.medchain.MedChainDemo

pause
