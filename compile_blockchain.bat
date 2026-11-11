@echo off
echo ========================================================
echo Compiling MedChain Blockchain System (Java 25)
echo Demonstrating: Blockchain, Cryptography, Smart Contracts
echo ========================================================

set "JDK_BIN=C:\Users\Dithin Mukkati\.p2\pool\plugins\org.eclipse.justj.openjdk.hotspot.jre.full.win32.x86_64_25.0.4.v20260826-0822\jre\bin"

if exist "%JDK_BIN%\javac.exe" (
    set JAVAC="%JDK_BIN%\javac.exe"
) else (
    set JAVAC=javac
)

if not exist bin mkdir bin

echo Compiling all MedChain packages...

%JAVAC% -encoding UTF-8 -d bin ^
    src\com\medchain\crypto\*.java ^
    src\com\medchain\blockchain\*.java ^
    src\com\medchain\model\*.java ^
    src\com\medchain\storage\*.java ^
    src\com\medchain\contract\*.java ^
    src\com\medchain\network\*.java ^
    src\com\medchain\service\*.java ^
    src\com\medchain\web\*.java ^
    src\com\medchain\MedChainDemo.java

if %ERRORLEVEL% equ 0 (
    echo.
    echo ========================================================
    echo BUILD SUCCESSFUL! All class files compiled into bin\
    echo Run run_blockchain.bat to launch MedChainDemo.
    echo ========================================================
) else (
    echo.
    echo BUILD FAILED! Check error messages above.
)
